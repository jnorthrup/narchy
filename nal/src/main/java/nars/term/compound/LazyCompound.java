package nars.term.compound;

import com.google.common.primitives.Ints;
import jcog.WTF;
import jcog.data.byt.DynBytes;
import jcog.util.ArrayUtils;
import nars.$;
import nars.Op;
import nars.term.Functor;
import nars.term.ProxyTerm;
import nars.term.Term;
import nars.term.util.map.ByteAnonMap;
import nars.unify.ellipsis.EllipsisMatch;
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
    private boolean changed;

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

    public LazyCompound compoundEnd(Op o) {
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
//        else
//            assert (dt == DTERNAL);

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
        code.writeByte(MAX_CONTROL_CODES + intern(x));
        return this;
    }

    protected byte intern(Term x) {
        if (x == null || x==Null || x.op()==NEG)
            throw new WTF();
        return sub().intern(x);
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
            return getNext(c.arrayDirect(), new int[]{0, c.len});
        }
    }

    private Term getNext(byte[] ii, int[] range) {
        int from;
        byte ctl = ii[(from = range[0])];
        //System.out.println("ctl=" + ctl + " @ " + from);
        range[0]++;

        Term next;
        if (ctl < MAX_CONTROL_CODES) {
            Op op = Op.ops[ctl];
            if (op == NEG)
                next = getNext(ii, range).neg();
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
                Term[] s = getNext(subterms, ii, range);
                if (s == null)
                    next = Null;
                else {

//            for (Term x : s)
//                if (x == null)
//                    throw new NullPointerException();

                    next = op.the(dt, s);
                    assert (next != null);

                    if (next != Null) {

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

                }
            }



        } else {
            next = next(ctl);
            //skip zero padding suffix
            while (range[0] < range[1] && code.at(range[0]) == 0) {
                ++range[0];
            }
        }

        return next;


    }

    protected Term next(byte ctl) {
        Term n = sub.interned((byte) (ctl - MAX_CONTROL_CODES));
        if (n == null)
            throw new NullPointerException();
        return n;
    }

    @Nullable
    private Term[] getNext(byte n, byte[] ii, int[] range) {
        Term[] t = null;
        //System.out.println(range[0] + ".." + range[1]);
        for (int i = 0; i < n; i++) {
            Term y;
            //System.out.println("\t" + s + "\t" + range[0] + ".." + range[1]);
            if (range[0] >= range[1]) {
                throw new ArrayIndexOutOfBoundsException();
                //return Arrays.copyOfRange(t, 0, i); //hopefully this is becaues of an ellipsis that got inlined and had no effect
            }
            if ((y = getNext(ii, range)) == Null)
                return null;
            if (y == null)
                throw new NullPointerException(); //WTF
            if (t == null)
                t = new Term[n];

            if (y instanceof EllipsisMatch) {
                //expand
                int yy = y.subs();
                n += yy -1;
                if (t.length!=n) {
                    t = Arrays.copyOf(t, n);
                }
                if (yy > 0) {
                    for (Term e : ((EllipsisMatch)y)) {
                        t[i++] = e;
                    }
                }
            } else {
                t[i] = y;
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

    public void subsEnd() {
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
                Term[] a = c.getNext(arity, args, new int[]{0, args.length});
                if (a == null) {
                    return Null;
                } else {
                    Term t = f.applyInline($.vFast(a));
                    return t;
                }
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
        public LazyCompound compoundEnd(Op o) {
            if (o == INH) {
                //assert(inhStack!=null);
                inhPop();
            }
            return super.compoundEnd(o);
        }

        private void inhPop() {
            inhStack.removeAtIndex(inhStack.size() - 1);
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

        @Override
        public void rewind(int pos) {
            if (pos != pos()) {
                while (inhStack != null && !inhStack.isEmpty() && inhStack.getLast() >= pos)
                    inhStack.removeAtIndex(inhStack.size() - 1);
                super.rewind(pos);
            }
        }

        /**
         * deferred functor evaluation
         */
        private Eval evalLater(Functor.InlineFunctor f, int start, int end) {
            DynBytes code1 = this.code;
            DynBytes c = code1;
            Eval e = new Eval(f, c.at(start + 1), c.subBytes(start + 2, end));
            rewind(start - 2);
            //inhPop();

            return e;
        }

        @Override
        protected Term next(byte ctl) {
            Term t = super.next(ctl);
            if (t instanceof Eval) {
                Term n = ((Eval) t).eval(this);
                if (n == null)
                    throw new NullPointerException();
                return n;
            }
            if (t == null)
                throw new NullPointerException();
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
