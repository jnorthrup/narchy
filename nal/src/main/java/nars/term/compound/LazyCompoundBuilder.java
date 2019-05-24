package nars.term.compound;

import com.google.common.primitives.Ints;
import jcog.Util;
import jcog.WTF;
import jcog.data.byt.DynBytes;
import jcog.data.list.FasterList;
import jcog.util.ArrayUtil;
import nars.$;
import nars.NAL;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.atom.Int;
import nars.term.util.TermException;
import nars.term.util.builder.TermBuilder;
import nars.term.util.map.ByteAnonMap;
import nars.term.util.transform.AbstractTermTransform;
import nars.term.util.transform.InlineFunctor;
import nars.term.util.transform.InstantFunctor;
import nars.term.util.transform.TermTransform;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static nars.Op.*;
import static nars.term.atom.Bool.Null;
import static nars.time.Tense.DTERNAL;

/**
 * memoizable supplier of compounds
 * fast to construct but not immediately usable (or determined to be valid)
 * without calling the .the()
 * consists of a tape flat linear tape of instructions which
 * when executed construct the target
 */
public class LazyCompoundBuilder {
    private final static int INITIAL_CODE_SIZE = 16;
    private final static int INITIAL_ANON_SIZE = 8;

    final ByteAnonMap sub = new ByteAnonMap(INITIAL_ANON_SIZE);
    final DynBytes code = new DynBytes(INITIAL_CODE_SIZE);

    private final FasterList<DeferredEval> eval = new FasterList();

    /** incremental mutation counter to detect unmodified segments that can be inlined / interned atomically */
    private int change = 0;


    /** when true, non-dynamic constant sequences will be propagated to inline future repeats in instantiation.
     *  if a non-deterministic functor evaluation occurrs, it must not propagate
     *  because that will just cause the same value to be assumed when it should not be.
     * */
    int volRemain;
    private final TermBuilder builder;

    public LazyCompoundBuilder() {
        this(Op.terms);
    }

    public LazyCompoundBuilder(TermBuilder builder) {
        this.builder = builder;
    }

    public void clear() {
        sub.clear();
        code.clear();
        eval.clear();
        change = 0;
    }

    public boolean updateMap(Function<Term, Term> m) {
        return sub.updateMap(m);
    }

    final LazyCompoundBuilder compoundStart(Op o) {
        compoundStart(o, DTERNAL);
        return this;
    }

    /**
     * append compound
     */
    public final LazyCompoundBuilder appendCompound(Op o, Term... subs) {
        return appendCompound(o, DTERNAL, subs);
    }

    /**
     * append compound
     */
    public LazyCompoundBuilder appendCompound(Op o, int dt, Term... subs) {
        int n = subs.length;
        assert (n < Byte.MAX_VALUE);
        return compoundStart(o, dt).subsStart((byte) n).subs(subs).compoundEnd(o);
    }

    public LazyCompoundBuilder compoundEnd(Op o) {
        return this;
    }


    public LazyCompoundBuilder subsStart(byte subterms) {
        code.writeByte(subterms);
        return this;
    }

    public LazyCompoundBuilder compoundStart(Op o, int dt) {
        DynBytes c = this.code;

        byte oid = o.id;

        if (!o.temporal)
            c.writeByte(oid);
        else
            c.writeByteInt(oid, dt);

//        else
//            assert (dt == DTERNAL);

        return this;
    }

    private final static byte MAX_CONTROL_CODES = (byte) Op.unique();


    public final LazyCompoundBuilder negStart() {
        compoundStart(NEG, DTERNAL);
        return this;
    }


    /**
     * add an already existent sub
     */
    public LazyCompoundBuilder append(Term x) {
        if (x instanceof Atomic) {
            return appendAtomic(x);
        } else {
            return append((Compound) x);
        }
    }

    private LazyCompoundBuilder append(Compound x) {
        Op o = x.op();
        switch (o) {
            case NEG:
                return negStart().append(x.unneg()).compoundEnd(NEG);
            case FRAG:
                return appendAtomic(x); //store atomically until construction
            default:
                return compoundStart(o, x.dt()).appendSubterms(x.subterms()).compoundEnd(o);
        }
    }

    private LazyCompoundBuilder appendSubterms(Subterms s) {
        return subsStart((byte) s.subs()).subs(s).subsEnd();
    }

    /** interns the term as-is, encapsulated as an atomic symbol */
    public LazyCompoundBuilder appendAtomic(Term x) {
        code.writeByte(MAX_CONTROL_CODES + intern(x));
        return this;
    }

    private byte intern(Term x) {
        //assert(!(x == null || x == Null || x.op() == NEG));
        return sub.intern(x);
    }

    private LazyCompoundBuilder subs(Iterable<Term> subs) {
        subs.forEach(this::append);
        return this;
    }

    final LazyCompoundBuilder subs(Term... subs) {
        for (Term x : subs)
            append(x);
        return this;
    }



    public Term get() {
        return get(NAL.term.COMPOUND_VOLUME_MAX);
    }
    /**
     * run the construction process
     */
    public Term get(int volMax) {
        this.volRemain = volMax;

        Term y = nextSubterm(code.arrayDirect(), new int[]{0, code.len});

        if (y instanceof Compound && !eval.isEmpty())
            y = DeferredEvaluator.apply(y);

        return y;
    }




    private Term nextSubterm(byte[] bytes, int[] range) {

        if (range[0] >= range[1])
            throw new WTF();

        int end = range[1];
        int start = range[0];
        byte ctl;
        do {
            ctl = bytes[range[0]++];
            if (ctl >= MAX_CONTROL_CODES)
                return nextAtom(ctl, bytes, range);
            else if (ctl == NEG.id)
                return nextSubterm(bytes, range).neg();

        } while (ctl == 0);



        Op op = Op.the(ctl);

        if (op.atomic)  //alignment error or something
            throw new TermException(LazyCompoundBuilder.class + ": atomic found where compound op expected: " + op.atomic);

        int dt = op.temporal ? dt(bytes, range) : DTERNAL;

        int volRemaining = this.volRemain - 1 /* one for the compound itself */;

        byte len = bytes[range[0]++];
        if (len == 0)
            return emptySubterms(op);

        Term[] subterms = null;

        subterms:
        for (int i = 0; i < len; ) {
            //assert(range[0] <= range[1]) //check if this is < or <=

            Term nextSub = nextSubterm(bytes, range);

            if (nextSub == Null)
                return Null;

            if (nextSub.op()==FRAG) {
                //append fragment subterms
                int fragmentLen = nextSub.subs();
                len += fragmentLen - 1;
                if (len == 0) {
                    //empty fragment was the only subterm; early exit
                    return emptySubterms(op);
                } else {
                    subterms = nextFrag(subterms, i, nextSub, len, fragmentLen);
                    volRemaining -= Util.sum(Term::volume, i, i+fragmentLen, subterms);
                    i += fragmentLen;
                }
            } else {
                //append next subterm
                if (subterms == null)
                    subterms = new Term[len];

                volRemaining -= nextSub.volume();

                subterms[i++] = nextSub;
            }

            if ((this.volRemain = volRemaining) <= 0)
                return Null; //capacity reached

        }

        return nextCompound(op, dt, subterms, bytes, range, start);
    }

    private Term nextCompound(Op op, int dt, Term[] subterms, byte[] bytes, int[] range, int start) {
        if (op==INH && evalInline() && subterms[1] instanceof InlineFunctor && subterms[0].op()==PROD) {

            return eval(subterms);

        } else {

            Term next = compound(op, dt, subterms); //assert (next != null);

            if (next != Null) {
                replaceAhead(bytes, range, start, next);
            }

            return next;
        }
    }

    /** constructs a new compound term */
    protected Term compound(Op op, int dt, Term[] subterms) {
        return op.the(builder, dt, subterms);
    }


    private static final TermTransform DeferredEvaluator = new AbstractTermTransform.NegObliviousTermTransform() {
        @Override
        protected Term applyPosCompound(Compound x) {
            if (x instanceof DeferredEval) {
                DeferredEval e = (DeferredEval) x;


                Term y = e.eval();
                if (y instanceof Bool)
                    return y;
                else
                    return apply(y); //recurse
            } else
                return super.applyPosCompound(x);
        }

        @Override
        public boolean evalInline() {
            return true;
        }
    };

    public int uniques() {
        return this.sub.idToTerm.size();
    }


    private static final class DeferredEval extends LightCompound {
    //private static final class DeferredEval extends LighterCompound {

        final static AtomicInteger serial = new AtomicInteger(0);

        /** https://unicode-table.com/en/1F47E/ */
        final static Atom DeferredEvalPrefix = Atomic.atom("⚛");
        //final static String DeferredEvalPrefix = ("⚛");

        private final InlineFunctor f;
        private Subterms args;

        /** cached value, null if not computed yet */
        private transient Term value = null;

        DeferredEval(InlineFunctor f, Subterms args) {
            super(PROD, DeferredEvalPrefix , Int.the(serial.incrementAndGet()));
            this.f = f;
            this.args = args;
        }


        @Override
        public @Nullable Term normalize(byte varOffset) {
            return null;
        }

        @Override
        public boolean isNormalized() {
            return true;
        }

        @Override
        public String toString() {
            return "(" + sub(0) + "=" + f + "(" + args + "))";
        }

        public final Term eval() {
            if (this.value!=null) {
                return this.value; //cached
            } else {

                Term argsY = DeferredEvaluator.apply($.pFast(args)); //recurse apply to arguments before eval
                Term e;
                if (argsY == Null)
                    e = Null;
                else {
                    e = f.applyInline(this.args = ((Subterms)argsY));
                    if (e == null)
                        e = Null; //HACK
                }
                return this.value = e;
            }
        }
    }

    /** adds a deferred evaluation */
    private Term eval(Term[] s) {

        InlineFunctor func = (InlineFunctor) s[1];
        Subterms args = s[0].subterms();

        boolean deferred = !(func instanceof InstantFunctor);

        if (deferred) {
            DeferredEval e = new DeferredEval(func, args);
            eval.add(e); //TODO check for duplicates?
            return e;
        } else {
            Term e = func.applyInline(args);
            if (e == null)
                e = Null;
            return e;
        }
    }


    private Term nextAtom(byte ctl, byte[] ii, int[] range) {
        Term next = nextInterned(ctl);
        if(next==null)
            throw new NullPointerException();

        int at = range[0];
        int end = range[1];
        if (at<end && ii[at] == 0) {
            //skip trailing zero-padding
            while ((ii[at]) == 0)
                if (++at >= end)
                    break;

            range[0] = at;
        }

        return next;
    }

    private Term emptySubterms(Op op) {

        if (op == PROD)
            return EmptyProduct;
        else
            throw new WTF();

    }

    private int dt(byte[] ii, int[] range) {
        int dt;
        int p = range[0];
        range[0] += 4;
        dt = Ints.fromBytes(ii[p++], ii[p++], ii[p++], ii[p/*++*/]);
        return dt;
    }

    protected boolean evalInline() {
        return true;
    }

    /** constant propagate matching spans further ahead in the construction process */
    private void replaceAhead(byte[] ii, int[] range, int from, Term next) {
        int to = range[0];
        int end = range[1];
        int span = to - from;
        if (span <= NAL.term.LAZY_COMPOUND_MIN_REPLACE_AHEAD_SPAN)
            return;

        if (end - to >= span) {
            //search for repeat occurrences of the start..end sequence after this
            int afterTo = to;
            byte n = 0;
            do {
                int match = ArrayUtil.nextIndexOf(ii, afterTo, end, ii, from, to);

                if (match != -1) {
                    //System.out.println("repeat found");
                    if (n == 0)
                        n = (byte) (MAX_CONTROL_CODES + intern(next)); //intern for re-substitute
                    code.set(match, n);

                    code.fillBytes((byte) 0, match + 1, match + span); //zero padding, to be ignored
                    afterTo = match + span;
                } else
                    break;

            } while (afterTo < end);
        }

    }

    private Term nextInterned(byte ctl) {
        return sub.interned((byte) (ctl - MAX_CONTROL_CODES));
    }

    /** expand a fragment */
    private Term[] nextFrag(Term[] subterms, int i, Term fragment, byte len, int fragmentLen) {
        if (subterms == null)
            subterms = new Term[len];
        else if (subterms.length != len) {
            //used to grow OR shrink
            subterms = Arrays.copyOf(subterms, len);
        }
        if (fragmentLen > 0) {
            fragment.addAllTo(subterms, i); //TODO recursively evaluate?
        }
        return subterms;
    }

    public final int change() {
        return change;
    }

    public final void changed() {
        change++;
    }

    public int pos() {
        return code.len;
    }

    public void rewind(int codePos) {
        code.len = codePos;
    }

    public void rewind(int codePos, int uniques) {
        FasterList<Term> id2Term = sub.idToTerm;
        int s = id2Term.size();
        if (uniques > s) {
            ObjectByteHashMap<Term> term2Id = sub.termToId;
            for (int i = uniques; i < s; i++)
                term2Id.remove(id2Term.get(i));
            id2Term.removeAbove(uniques);
        }
        rewind(codePos);
    }

    public LazyCompoundBuilder subsEnd() {
        return this;
    }

    public boolean isEmpty() {
        return code.len==0 && (sub==null || sub.isEmpty());
    }


}
