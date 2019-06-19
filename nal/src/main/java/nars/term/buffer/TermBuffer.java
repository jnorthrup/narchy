package nars.term.buffer;

import com.google.common.primitives.Ints;
import jcog.Util;
import jcog.WTF;
import jcog.data.byt.DynBytes;
import jcog.data.list.FasterList;
import jcog.util.ArrayUtil;
import nars.NAL;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.util.TermException;
import nars.term.util.builder.TermBuilder;
import nars.term.util.map.ByteAnonMap;
import nars.term.var.ellipsis.Fragment;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.Function;

import static nars.NAL.term.TERM_BUFFER_MIN_INTERN_VOL;
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
public class TermBuffer {
    private final static int INITIAL_CODE_SIZE = 16;
    public final static int INITIAL_ANON_SIZE = 64 + 32;

    public final ByteAnonMap sub;
    final DynBytes code = new DynBytes(INITIAL_CODE_SIZE);


    /** incremental mutation counter to detect unmodified segments that can be inlined / interned atomically */
    private int change = 0;


    /** when true, non-dynamic constant sequences will be propagated to inline future repeats in instantiation.
     *  if a non-deterministic functor evaluation occurrs, it must not propagate
     *  because that will just cause the same value to be assumed when it should not be.
     * */
    int volRemain;
    private final TermBuilder builder;

    public TermBuffer clone() {
        TermBuffer b = new TermBuffer(builder, sub);
        b.code.write(code);
        //TODO volRemain ?
        return b;
    }

    public TermBuffer() {
        this(Op.terms, new ByteAnonMap(INITIAL_ANON_SIZE));
    }

    public TermBuffer(TermBuilder builder, ByteAnonMap sub) {

        this.builder = builder;
        this.sub = sub;
    }

    public final void clear() {
        clear(true, true);
    }

    public void clear(boolean code, boolean uniques) {
        if (uniques) {
            sub.clear();
        }
        if (code) {
            this.code.clear();
            change = 0;
        }
    }

    public boolean updateMap(Function<Term, Term> m) {
        return sub.updateMap(m);
    }

    final TermBuffer compoundStart(Op o) {
        compoundStart(o, DTERNAL);
        return this;
    }

    /**
     * append compound
     */
    final TermBuffer appendCompound(Op o, Term... subs) {
        return appendCompound(o, DTERNAL, subs);
    }

    /**
     * append compound
     */
    TermBuffer appendCompound(Op o, int dt, Term... subs) {
        int n = subs.length;
        assert (n < Byte.MAX_VALUE);
        return compoundStart(o, dt).subsStart((byte) n).subs(subs).compoundEnd(o);
    }

    private TermBuffer compoundEnd(Op o) {
        return this;
    }


    TermBuffer subsStart(byte subterms) {
        code.writeByte(subterms);
        return this;
    }

    private TermBuffer compoundStart(Op o, int dt) {
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


    private final TermBuffer negStart() {
        compoundStart(NEG, DTERNAL);
        return this;
    }


    /**
     * add an already existent sub
     */
    public TermBuffer append(Term x) {
        if (x instanceof Atomic) {
            return appendInterned(x);
        } else {
            return append((Compound) x);
        }
    }

    private TermBuffer append(Compound x) {
        Op o = x.op();
        switch (o) {
            case NEG:
                return negStart().append(x.unneg()).compoundEnd(NEG);
            case FRAG:
                return appendInterned(x); //store atomically until construction
            default:
                return compoundStart(o, x.dt()).appendSubterms(x.subterms()).compoundEnd(o);
        }
    }

    private TermBuffer appendSubterms(Subterms s) {
        return subsStart((byte) s.subs()).subs(s).subsEnd();
    }

    /** interns the term as-is, encapsulated as an atomic symbol */
    private TermBuffer appendInterned(Term x) {
        return appendInterned(intern(x));
    }

    private TermBuffer appendInterned(byte i) {
        assert(i < Byte.MAX_VALUE);
        code.writeByte(MAX_CONTROL_CODES + i);
        return this;
    }

    private byte intern(Term x) {
        //assert(!(x == null || x == Null || x.op() == NEG));
        return sub.intern(x);
    }

    private TermBuffer subs(Iterable<Term> subs) {
        subs.forEach(this::append);
        return this;
    }

    final TermBuffer subs(Term... subs) {
        for (Term x : subs)
            append(x);
        return this;
    }



    public Term term() {
        return get(NAL.term.COMPOUND_VOLUME_MAX);
    }

    /**
     * run the construction process
     */
    public Term get(int volMax) {
        this.volRemain = volMax;

        Term y = nextTerm(code.arrayDirect(), new int[]{0, code.len});


        return y;
    }




    protected Term nextTerm(byte[] bytes, int[] range) {

        if (range[0] >= range[1])
            throw new WTF("byte range overflow: " + range[0] + " >= " + range[1]);

        int end = range[1];
        int start = range[0];
        byte ctl = bytes[range[0]++];
        if (ctl >= MAX_CONTROL_CODES)
            return nextInterned(ctl, bytes, range);
        else if (ctl == NEG.id)
            return nextTerm(bytes, range).neg();

        Op op = Op.the(ctl);

        if (op.atomic)  //alignment error or something
            throw new TermException(TermBuffer.class + ": atomic found where compound op expected: " + op.atomic);

        int dt = op.temporal ? dt(bytes, range) : DTERNAL;

        int volRemaining = this.volRemain - 1 /* one for the compound itself */;

        byte len = bytes[range[0]++];
        if (len == 0)
            return emptySubterms(op);

        Term[] subterms = null;

        subterms:
        for (int i = 0; i < len; ) {
            //assert(range[0] <= range[1]) //check if this is < or <=

            Term nextSub = nextTerm(bytes, range);

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

    protected Term nextCompound(Op op, int dt, Term[] subterms, byte[] bytes, int[] range, int start) {
            Term next = newCompound(op, dt, subterms); //assert (next != null);

            if (next != Null) {
                //replaceAhead(bytes, range, start, next);
            }

            return next;

    }

    /** constructs a new compound term */
    protected Term newCompound(Op op, int dt, Term[] subterms) {
        return op.the(builder, dt, subterms);
    }



    private int uniques() {
        return this.sub.idToTerm.size();
    }

    public final byte term(Term x) {
        return sub.interned(x);
    }




    private Term nextInterned(byte ctl, byte[] ii, int[] range) {
        Term next = nextInterned(ctl);
        if(next==null)
            throw new NullPointerException();

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

    /** constant propagate matching spans further ahead in the construction process */
    private void replaceAhead(byte[] ii, int[] range, int from, Term next) {
        int to = range[0];

        int span = to - from;
        if (span <= NAL.term.TERM_BUFFER_MIN_REPLACE_AHEAD_SPAN)
            return;

        int end = range[1];

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

    private void rewind(int codePos) {
        code.len = codePos;
    }

    private void rewind(int codePos, int uniques) {
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

    private TermBuffer subsEnd() {
        return this;
    }

    public boolean isEmpty() {
        return code.len==0 && (sub==null || sub.isEmpty());
    }

    public boolean append(Term x, Function<Term,Term> f) {
        if (x instanceof Compound) {
            byte interned = this.term(x);
            if (interned!=Byte.MIN_VALUE) {
                this.appendInterned(interned);
                return true;
            } else {
                return appendCompound((Compound) x, f);
            }
        } else {
            @Nullable Term y = f.apply(x);
            if (y == null || y == Bool.Null)
                return false;
            else {
                if (y.op() == FRAG) {
                    Subterms s = y.subterms();
                    if (s.subs() > 0) {
                        Subterms s2 = s.transformSubs(f, null);
                        if (s2 != s) {
                            if (s2 == null)
                                return false;
                            y = new Fragment(s2);
                        }
                    }
                }
                this.append(y);
                if (y != x)
                    this.changed();
                return true;
            }
        }
    }


    public boolean appendCompound(Compound x, Function<Term,Term> f, int volRemain) {
        this.volRemain = volRemain;
        return appendCompound(x, f);
    }

    private boolean appendCompound(Compound x, Function<Term, Term> f) {
        int c = this.change(), u = this.uniques();
        int p = this.pos();

        Op o = x.op();
        if (o == NEG) {

            this.negStart();

            if (!append(x.sub(0), f))
                return false;

            this.compoundEnd(NEG);

        } else {
            this.compoundStart(o, o.temporal ? x.dt() : DTERNAL);

            if (!transformSubterms(x.subterms(), f))
                return false;

            this.compoundEnd(o);
        }

        if (this.change()==c && x.volume() >= TERM_BUFFER_MIN_INTERN_VOL) {
            //unchanged constant; rewind and pack the exact Term as an interned symbol
            this.rewind(p, u);
            this.appendInterned(x);
        }
        return true;
    }

    private boolean transformSubterms(Subterms s, Function<Term, Term> t) {
        this.subsStart((byte) s.subs());
        if (s.ANDwithOrdered(this::append, t)) {
            this.subsEnd();
            return true;
        }
        return false;
    }

}
