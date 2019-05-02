package nars.term.compound;

import com.google.common.primitives.Ints;
import jcog.WTF;
import jcog.data.byt.DynBytes;
import jcog.util.ArrayUtils;
import nars.NAL;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.util.builder.TermBuilder;
import nars.term.util.map.ByteAnonMap;
import nars.term.util.transform.InlineFunctor;
import org.jetbrains.annotations.Nullable;

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
public class LazyCompound {
    private final static int INITIAL_CODE_SIZE = 16;
    private final static int INITIAL_ANON_SIZE = 8;

    ByteAnonMap sub = null;
    final DynBytes code = new DynBytes(INITIAL_CODE_SIZE);

    private boolean changed;


    /** when true, non-dynamic constant sequences will be propagated to inline future repeats in instantiation.
     *  if a non-deterministic functor evaluation occurrs, it must not propagate
     *  because that will just cause the same value to be assumed when it should not be.
     * */
    private boolean constantPropagate = true;
    int volRemain;

    public LazyCompound() {

    }
    public void clear() {
        if (sub!=null) sub.clear();
        code.clear();
        changed = false;
        constantPropagate = true;
    }

    public boolean updateMap(Function<Term, Term> m) {
        if (sub == null) return false;
        return sub.updateMap(m);
    }

    final LazyCompound compoundStart(Op o) {
        compoundStart(o, DTERNAL);
        return this;
    }

    /**
     * append compound
     */
    public final LazyCompound compound(Op o, Term... subs) {
        return compound(o, DTERNAL, subs);
    }

    /**
     * append compound
     */
    public LazyCompound compound(Op o, int dt, Term... subs) {
        int n = subs.length;
        assert (n < Byte.MAX_VALUE);
        return compoundStart(o, dt).subsStart((byte) n).subs(subs).compoundEnd(o);
    }

    public LazyCompound compoundEnd(Op o) {
        return this;
    }


    public LazyCompound subsStart(byte subterms) {
        code.writeByte(subterms);
        return this;
    }

    public LazyCompound compoundStart(Op o, int dt) {
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


    public final LazyCompound negStart() {
        compoundStart(NEG, DTERNAL);
        return this;
    }


    /**
     * add an already existent sub
     */
    public LazyCompound append(Term x) {
        if (x instanceof Atomic) {
            return appendAtomic(x);
        } else {
            return append((Compound) x);
        }
    }

    private LazyCompound append(Compound x) {
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

    private LazyCompound appendSubterms(Subterms s) {
        return subsStart((byte) s.subs()).subs(s).subsEnd();
    }

    private LazyCompound appendAtomic(Term x) {
        code.writeByte(MAX_CONTROL_CODES + intern(x));
        return this;
    }

    private byte intern(Term x) {
        //assert(!(x == null || x == Null || x.op() == NEG));
        return sub().intern(x);
    }

    private LazyCompound subs(Iterable<Term> subs) {
        subs.forEach(this::append);
        return this;
    }

    final LazyCompound subs(Term... subs) {
        for (Term x : subs)
            append(x);
        return this;
    }


    private ByteAnonMap sub() {
        ByteAnonMap sub = this.sub;
        return this.sub == null ? (this.sub = new ByteAnonMap(INITIAL_ANON_SIZE)) : sub;
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
        DynBytes c = code;
//        if (code == null)
//            return Null; //nothing
//        else {
//            if (sub != null)
//                sub.readonly(); //optional

        return getNext(b, c.arrayDirect(), new int[]{0, c.len});
    }



    private Term getNext(TermBuilder b, byte[] ii, int[] range) {
        if (volRemain <= 0)
            return Null;

        int from;
        byte ctl = ii[(from = range[0]++)];
        //System.out.println("ctl=" + ctl + " @ " + from);

        Term next;
        if (ctl < MAX_CONTROL_CODES) {

            /** -1 volume for the compound; the subterms are counted elsewhere */
            this.volRemain--;

            Op op = Op.the(ctl);
            if (op == NEG)
                next = getNext(b, ii, range).neg();
            else {
                if (op.atomic)
                    throw new WTF(); //alignment error or something

                int dt;
                if (op.temporal) {
                    int p = range[0];
                    range[0] += 4;
                    dt = Ints.fromBytes(ii[p++], ii[p++], ii[p++], ii[p/*++*/]);
                } else
                    dt = DTERNAL;

                byte subterms = ii[range[0]++];
                if (subterms == 0) {
                    if (op == PROD)
                        next = EmptyProduct;
                    else if (op == CONJ)
                        next = True;
                    else {
                        throw new WTF();
                    }
                } else {
                    int vBefore = volRemain;

                    Term[] s = getNext(b, subterms, ii, range);

                    if (s == null)
                        return Null;
                    else {

                        int vAfter = volRemain;

                        if (op==INH && evalInline() && s[1] instanceof InlineFunctor && s[0].op()==PROD) {

                            next = ((InlineFunctor)s[1]).applyInline(s[0].subterms());
                            if (next == null)
                                return Null;

                            //TODO determine if constantPropagate does not need disabled (specially marked "deterministic" functors)
                            constantPropagate = false; //HACK

                            //TODO if Functor.isDeterministic { replaceAhead...

                        } else {

                            next = op.the(b, dt, s); //assert (next != null);

                            if (next != Null) {

                                //adjust volume count for compound term reductions, etc.
                                volRemain = vBefore + 1 - next.volume();

                                if (constantPropagate)
                                    replaceAhead(ii, range, from, next);
                            }

                            //TODO
//                            if (!constantPropagate && !next.hasAny)
//                                constantPropagate = true; //return to constant propagation mode now that
                        }

                    }
                }
            }

        } else {
            next = next(ctl);
            volRemain -= next.volume();

            //skip zero padding suffix
            int r0 = range[0], r1 = range[1];
            while (r0 < r1 && code.at(r0) == 0) { ++r0; }
            range[0] = r0;

        }

        return next;
    }

    protected boolean evalInline() {
        return true;
    }

    private void replaceAhead(byte[] ii, int[] range, int from, Term next) {
        int to = range[0];
        int end = range[1];
        int span = to - from;
        if (end - to >= span) {
            //search for repeat occurrences of the start..end sequence after this
            int afterTo = to;
            byte n = 0;
            do {
                int match = ArrayUtils.nextIndexOf(ii, afterTo, end, ii, from, to);

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

    private Term next(byte ctl) {
        Term n = sub.interned((byte) (ctl - MAX_CONTROL_CODES));
        //assert(n!=null); //        if (n == null)        throw new NullPointerException();
        return n;
    }

    @Nullable
    private Term[] getNext(TermBuilder b, byte n, byte[] ii, int[] range) {
        Term[] t = null;
        //System.out.println(range[0] + ".." + range[1]);
        for (int i = 0; i < n; ) {
            //System.out.println("\t" + s + "\t" + range[0] + ".." + range[1]);
            if (range[0] >= range[1])
                throw new ArrayIndexOutOfBoundsException();
            //return Arrays.copyOfRange(t, 0, i); //hopefully this is becaues of an ellipsis that got inlined and had no effect

            Term y;
            if ((y = getNext(b, ii, range)) == Null)
                return null;

            //if (y == null) throw new NullPointerException(); //WTF

            if (y.op()==FRAG) { //if (y instanceof EllipsisMatch) {
                int en = y.subs();
                n += en - 1;
                t = getNextFrag(t, i, y, n, en);
                i += en;
            } else {
                if (t == null)
                    t = new Term[n];
                t[i++] = y;
            }
        }
        return t;
    }

    /** expand a fragment */
    private Term[] getNextFrag(Term[] t, int i, Term y, byte n, int en) {
        if (t == null)
            t = (n == 0) ? EmptyTermArray : new Term[n];
        else if (t.length != n) {
            t = (n == 0) ? EmptyTermArray : Arrays.copyOf(t, n);
        }
        if (en > 0) {
            for (Term e : y.subterms()) {
                // if (e == null || e == Null) throw new NullPointerException();
                t[i++] = e; //TODO recursively process?
            }
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

    public LazyCompound subsEnd() {
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
