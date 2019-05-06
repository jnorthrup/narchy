package nars.term.compound;

import com.google.common.primitives.Ints;
import jcog.Util;
import jcog.WTF;
import jcog.data.byt.DynBytes;
import jcog.util.ArrayUtil;
import nars.NAL;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.util.builder.TermBuilder;
import nars.term.util.map.ByteAnonMap;
import nars.term.util.transform.InlineFunctor;

import java.util.Arrays;
import java.util.function.Function;

import static nars.Op.*;
import static nars.term.atom.Bool.Null;
import static nars.term.atom.Bool.True;
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

    private boolean changed;


    /** when true, non-dynamic constant sequences will be propagated to inline future repeats in instantiation.
     *  if a non-deterministic functor evaluation occurrs, it must not propagate
     *  because that will just cause the same value to be assumed when it should not be.
     * */
    private boolean constantPropagate = true;
    int volRemain;

    public LazyCompoundBuilder() {

    }
    public void clear() {
        sub.clear();
        code.clear();
        changed = false;
        constantPropagate = true;
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
    public final LazyCompoundBuilder compound(Op o, Term... subs) {
        return compound(o, DTERNAL, subs);
    }

    /**
     * append compound
     */
    public LazyCompoundBuilder compound(Op o, int dt, Term... subs) {
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

    private LazyCompoundBuilder appendAtomic(Term x) {
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


    public final Term get() {
        return get(
                Op.terms
                //HeapTermBuilder.the
        );
    }

    public Term get(TermBuilder b) {
        return get(b, NAL.term.COMPOUND_VOLUME_MAX);
    }
    /**
     * run the construction process
     */
    public Term get(TermBuilder b, int volMax) {
        this.volRemain = volMax;

        return nextSubterm(b, code.arrayDirect(), new int[]{0, code.len});
    }



    private Term nextSubterm(TermBuilder b, byte[] ii, int[] range) {

        if (range[0] >= range[1])
            throw new WTF();

        int end = range[1];
        int from = range[0];
        byte ctl = ii[range[0]++];

        if (ctl >= MAX_CONTROL_CODES)
            return nextAtom(ctl, ii, range);
        else if (ctl == NEG.id)
            return nextSubterm(b, ii, range).neg();

        Op op = Op.the(ctl);

        if (op.atomic)
            throw new WTF(); //alignment error or something

        int dt = op.temporal ? dt(ii, range) : DTERNAL;

        int volRemainLocal = this.volRemain - 1 /* one for the compound itself */;

        byte subterms = ii[range[0]++];
        if (subterms == 0) {

            return emptySubterms(op);

        } else {

            /**
             * @return null as a termination signal to callee (resulting in it returning Null, etc),
             *         or a Term[] array (of >=0 length) containing the subterms of the compound the callee is constructing
             */
            //@Nullable private Term[] nextSubterms(TermBuilder b, byte subterms, byte[] ii, int[] range) {
            //s = nextSubterms(b, subterms, ii, range);
            Term[] s = null;
            {


                subterms: for (int i = 0; i < subterms; ) {
                    if (range[0] > range[1])
                        throw new ArrayIndexOutOfBoundsException();

                    Term x;
                    if ((x = nextSubterm(b, ii, range)) == Null)
                        return Null;

                    if (x.op()==FRAG) { //if (y instanceof EllipsisMatch) {
                        int en = x.subs();
                        subterms += en - 1;
                        if (subterms == 0) {
                            //return EmptyTermArray; //the only element and the fragment was empty
                            s = EmptyTermArray;
                            break;
                        } else {
                            s = nextFrag(s, i, x, subterms, en);
                            volRemainLocal -= Util.sum(Term::volume, i, i+en, s);
                            i += en;
                        }
                    } else {
                        if (s == null)
                            s = new Term[subterms];

                        volRemainLocal -= x.volume();

                        s[i++] = x;
                    }

                    if ((this.volRemain = volRemainLocal) <= 0) {
                        //capacity reached
                        //range[0] = range[1] = -1;
                        return Null;
                    }

                }

            }

            if (s == null) {
                return Null;
            } else {

                if (op==INH && evalInline() && s[1] instanceof InlineFunctor && s[0].op()==PROD) {

                    //TODO defer evaluation until end
                    return eval(s);

                } else {

                    Term next = op.the(b, dt, s); //assert (next != null);

                    if (next != Null) {

                        if (constantPropagate)
                            replaceAhead(ii, range, from, next);

                    }

                    return next;
                }


            }
        }

    }

    private Term eval(Term[] s) {
        InlineFunctor f = (InlineFunctor) s[1];
        Term next = f.applyInline(s[0].subterms());

//        //disable constant propagation if constantPropagate does not need disabled (specially marked "deterministic" functors)
//        if (!(f instanceof DeterministicFunctor))
        constantPropagate = false;

        //TODO if Functor.isDeterministic { replaceAhead...

        return next!=null ? next : Null;
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
        else if (op == CONJ)
            return True;
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

//    @Nullable private Term[] nextSubterms(TermBuilder b, byte subterms, byte[] ii, int[] range) {
//        Term[] s = null;
//
//        int volRemainLocal = this.volRemain;
//
//        for (int i = 0; i < subterms; ) {
////            if (range[0] >= range[1])
////                throw new ArrayIndexOutOfBoundsException();
//
//            Term x;
//            if ((x = nextSubterm(b, ii, range)) == Null)
//                return null;
//
//            if (x.op()==FRAG) { //if (y instanceof EllipsisMatch) {
//                int en = x.subs();
//                subterms += en - 1;
//                if (subterms == 0)
//                    return EmptyTermArray; //the only element and the fragment was empty
//                else {
//                    s = nextFrag(s, i, x, subterms, en);
//                    volRemainLocal -= Util.sum(Term::volume, i, i+en, s);
//                    i += en;
//                }
//            } else {
//                if (s == null)
//                    s = new Term[subterms];
//
//                volRemainLocal -= x.volume();
//
//                s[i++] = x;
//            }
//
//            if ((this.volRemain = volRemainLocal) <= 0) {
//                //capacity reached
//                return null;
//            }
//
//        }
//
////        if (ArrayUtil.indexOf(t, (Object)null)!=-1)
////            throw new NullPointerException(); //TEMPORARY for debugging
//
//        return s;
//    }

    /** expand a fragment */
    private Term[] nextFrag(Term[] t, int i, Term y, byte n, int en) {
        if (t == null)
            t = new Term[n];
        else if (t.length != n) {
            //used to grow OR shrink
            t = Arrays.copyOf(t, n);
        }
        if (en > 0) {
            //TODO recursively evaluate?
            y.addAllTo(t, i);
        }
        return t;
    }

    public boolean changed() {
        return changed;
    }

    public void setChanged(boolean c) {
        changed = c;
    }

    public int pos() {
        return code.len;
    }

    public void rewind(int pos) {
        code.len = pos;
    }

    public LazyCompoundBuilder subsEnd() {
        return this;
    }

    public boolean isEmpty() {
        return code.len==0 && (sub==null || sub.isEmpty());
    }

//    /**
//     * ability to lazily evaluate and rewrite functors
//     */
//    public static class LazyEvalCompound extends LazyCompound {
//
//        ShortArrayList inhStack = null;
//
//        private static class Eval extends UnnormalizedVariable {
//            final Functor.InlineFunctor f;
//            byte arity;
//            final byte[] args;
//
//            public Eval(Functor.InlineFunctor f, byte arity, byte[] args) {
//                super(Op.VAR_PATTERN, ";");
//                this.arity = arity;
//                this.f = f;
//                this.args = args;
//            }
//
//            @Override
//            public boolean equals(Object o) {
//                return this == o;
//            }
//
//            @Override
//            public int hashCode() {
//                return System.identityHashCode(this);
//            }
//
//            @Override
//            public String toString() {
//                return f + "<" + Arrays.toString(args) + ">";
//            }
//
//            public Term eval(TermBuilder b, LazyCompound c) {
//                Term[] a = c.getNext(b, arity, args, new int[]{0, args.length});
//                if (a == null) {
//                    return Null;
//                } else {
//                    Term t = f.applyInline($.vFast(a));
//                    return t;
//                }
//            }
//        }
//
//        @Override
//        public LazyCompound compoundStart(Op o, int dt) {
//
//            if (o == INH) {
//                if (inhStack == null)
//                    inhStack = new ShortArrayList(8);
//
//                inhStack.add((short) code.length()); //record compound at this position
//            }
//
//            return super.compoundStart(o, dt);
//        }
//
//        @Override
//        public LazyCompound compoundEnd(Op o) {
//            if (o == INH) {
//                //assert(inhStack!=null);
//                inhPop();
//            }
//            return super.compoundEnd(o);
//        }
//
//        private void inhPop() {
//            inhStack.removeAtIndex(inhStack.size() - 1);
//        }
//
//        @Override
//        protected LazyCompound append(Atomic x) {
//            if (x instanceof Functor.InlineFunctor) {
//                //
//                // ((arg1,arg,...)-->F)
//                //
//                //scan backwards, verifying preceding product arguments contained within inheritance
//                if (inhStack != null && !inhStack.isEmpty()) {
//                    DynBytes c = this.code;
//                    int lastInh = inhStack.getLast();
//
//                    byte[] cc = c.arrayDirect();
//                    int lastProd = lastInh + 2;
//                    if (c.length() >= lastProd && cc[lastProd] == PROD.id) {
//                        int pos = c.length();
//                        return append(evalLater((Functor.InlineFunctor) x, lastProd, pos /* after adding the functor atomic */));
//                    }
//
//                }
//
//            }
//
//            return super.append(x);
//        }
//
//        @Override
//        public void rewind(int pos) {
//            if (pos != pos()) {
//                while (inhStack != null && !inhStack.isEmpty() && inhStack.getLast() > pos)
//                    inhStack.removeAtIndex(inhStack.size() - 1);
//                super.rewind(pos);
//            }
//        }
//
//        /**
//         * deferred functor evaluation
//         */
//        private Eval evalLater(Functor.InlineFunctor f, int start, int end) {
//            DynBytes c = code;
//            Eval e = new Eval(f, code.at(start + 1), c.subBytes(start + 2, end));
//            rewind(start - 2);
//            //inhPop();
//
//            return e;
//        }
//
//        @Override
//        protected Term next(TermBuilder b, byte ctl) {
//            Term t = super.next(b, ctl);
//            if (t instanceof Eval) {
//                t = ((Eval) t).eval(b, this);
//            }
//            if (t == null)
//                throw new NullPointerException();
//            return t;
//        }
//
//
//    }


//    static class Int1616 extends Int {
//
//        public Int1616(short low, short hi) {
//            super(low | (hi << 16));
//        }
//
//        public short low() {
//            return (short) (id & 0xffff);
//        }
//        public short high() {
//            return (short) ((id >> 16) & 0xffff);
//        }
//
//    }

}
