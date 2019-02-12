package nars.term.compound;

import com.google.common.primitives.Ints;
import jcog.WTF;
import jcog.data.byt.DynBytes;
import nars.$;
import nars.Op;
import nars.term.Functor;
import nars.term.ProxyTerm;
import nars.term.Term;
import nars.term.util.map.ByteAnonMap;
import org.eclipse.collections.impl.list.mutable.primitive.ShortArrayList;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

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
public class LazyCompound {
    final static int INITIAL_CODE_SIZE = 16;

    ByteAnonMap sub = null;
    final DynBytes code = new DynBytes(INITIAL_CODE_SIZE);

    final static int INITIAL_ANON_SIZE = 8;

    public LazyCompound() {

    }

    public final LazyCompound compoundStart(Op o) {
        compoundStart(o, DTERNAL);
        return this;
    }

    public LazyCompound compound(Op o, Term... subs) {
        return compound(o, DTERNAL, subs);
    }

    public LazyCompound compound(Op o, int dt, Term... subs) {
        int n = subs.length;
        assert (n < Byte.MAX_VALUE);
        return compoundStart(o, dt).subs((byte) n).subs(subs).compoundEnd(o);
    }

    protected LazyCompound compoundEnd(Op o) {
        return this;
    }


    public LazyCompound subs(byte subterms) {
        code.writeByte(subterms);
        return this;
    }

    public LazyCompound compoundStart(Op o, int dt) {
        DynBytes c = this.code;
        c.writeByte(o.id);

        if (o.temporal)
            c.writeInt(dt);
        else
            assert (dt == DTERNAL);

        return this;
    }

    final static byte MAX_CONTROL_CODES = (byte) Op.ops.length;


    public final void negStart() {
        compoundStart(NEG, DTERNAL);
    }

    /**
     * add an already existent sub
     */
    public LazyCompound add(Term x) {
        if (x.op() == NEG) {
            negStart();
            x = x.unneg();
        }
        code.writeByte(MAX_CONTROL_CODES + sub().intern(x));
        return this;
    }

    public LazyCompound subs(Term... subs) {
        for (Term x : subs)
            add(x);
        return this;
    }


    private ByteAnonMap sub() {
        ByteAnonMap sub = this.sub;
        return this.sub == null ? (this.sub = new ByteAnonMap(INITIAL_ANON_SIZE)) : sub;
    }

    /**
     * run the construction process
     */
    public Term get() {

        DynBytes c = code;
        if (code == null)
            return Null; //nothing
        else {
            if (sub != null)
                sub.readonly(); //optional
            return getNext(c.arrayDirect(), new int[]{0});
        }
    }

    Term getNext(byte[] ii, int[] pos) {
        byte ctl = ii[pos[0]++];
        Term next;
        if (ctl < MAX_CONTROL_CODES) {
            Op op = Op.ops[ctl];
            if (op == NEG)
                return getNext(ii, pos).neg();


            if (op.atomic)
                throw new WTF(); //alignment error or something

            int dt;
            if (op.temporal) {
                int p = pos[0];
                pos[0] += 4;
                dt = Ints.fromBytes(ii[p++], ii[p++], ii[p++], ii[p/*++*/]);
            } else
                dt = DTERNAL;

            byte subterms = ii[pos[0]++];
            Term[] s = getNext(subterms, ii, pos);
            if (s == null)
                return Null;

            next = op.the(dt, s);
        } else {
            next = next(ctl);
        }
        if (next == null) {
            //throw new WTF();
            return Null;
        }
        return next;
    }

    protected Term next(byte ctl) {
        return sub.interned((byte) (ctl - MAX_CONTROL_CODES));
    }

    @Nullable
    private Term[] getNext(byte n, byte[] ii, int[] pos) {
        Term[] t = new Term[n];
        for (int s = 0; s < n; s++) {
            if ((t[s] = getNext(ii, pos)) == Null)
                return null;
        }
        return t;
    }


    /**
     * ability to lazily evaluate and rewrite functors
     */
    public static class LazyEvalCompound extends LazyCompound {

        ShortArrayList inhStack = null;

        private static class Eval extends ProxyTerm {
            final Functor.InlineFunctor f;
            byte arity;
            final byte[] args;

            public Eval(Functor.InlineFunctor f, byte arity, byte[] args) {
                super((Term) f);
                this.arity = arity;
                this.f = f;
                this.args = args;
            }

            @Override
            public boolean equals(Object o) {
                return this == o;
            }

            @Override
            public int hashCode() {
                return System.identityHashCode(this);
            }

            @Override
            public String toString() {
                return f + "<" + Arrays.toString(args) + ">";
            }

            public Term eval(LazyCompound c) {
                Term[] a = c.getNext(arity, args, new int[1]);
                if (a == null)
                    return Null;
                return f.applyInline($.vFast(a));
            }
        }

        @Override
        public LazyCompound compoundStart(Op o, int dt) {

            if (o == INH) {
                if (inhStack == null)
                    inhStack = new ShortArrayList(8);

                inhStack.add((short) code.length()); //record compound at this position
            }

            return super.compoundStart(o, dt);
        }

        @Override
        protected LazyCompound compoundEnd(Op o) {
            if (o==INH) {
                //assert(inhStack!=null);
                inhPop();
            }
            return super.compoundEnd(o);
        }

        private void inhPop() {
            inhStack.removeAtIndex(inhStack.size()-1);
        }

        @Override
        public LazyCompound add(Term x) {
            if (x instanceof Functor.InlineFunctor) {
                //
                // ((arg1,arg,...)-->F)
                //
                //scan backwards, verifying preceding product arguments contained within inheritance
                if (inhStack != null && !inhStack.isEmpty()) {
                    DynBytes c = this.code;
                    int lastInh = inhStack.getLast();

                    byte[] cc = c.arrayDirect();
                    int lastProd = lastInh + 2;
                    if (c.length() >= lastProd && cc[lastProd] == PROD.id) {
                        int pos = c.length();
                        x = evalLater((Functor.InlineFunctor) x, lastProd, pos /* after adding the functor atomic */);
                    }

                }

            }

            return super.add(x);
        }

        /**
         * deferred functor evaluation
         */
        private Eval evalLater(Functor.InlineFunctor f, int start, int end) {
            DynBytes code1 = this.code;
            DynBytes c = code1;
            Eval e = new Eval(f, c.at(start + 1), c.subBytes(start + 2, end));
            c.len = start - 2; //rewind to beginning of -->
            inhPop();
//            while (inhStack!=null && !inhStack.isEmpty() && inhStack.getLast() > c.len)
//                inhStack.removeAtIndex(inhStack.size()-1);
            return e;
        }

        @Override
        protected Term next(byte ctl) {
            Term t = super.next(ctl);
            if (t instanceof Eval) {
                return ((Eval) t).eval(this);
            }
            return t;
        }


    }


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
