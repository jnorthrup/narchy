package nars.term.compound;

import com.google.common.primitives.Ints;
import jcog.data.byt.DynBytes;
import nars.Op;
import nars.term.Term;
import nars.term.util.map.ByteAnonMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.term.atom.Bool.Null;
import static nars.time.Tense.DTERNAL;

/**
 * memoizable supplier of compounds
 * fast to construct but not immediately usable (or determined to be valid)
 * without calling the .the()
 * consists of a tape flat linear tape of instructions which
 * when executed construct the term
 * */
public class LazyCompound {
    private ByteAnonMap sub = null;
    private DynBytes code = null;

    final int INITIAL_CODE_SIZE = 16;
    final int INITIAL_ANON_SIZE = 8;

    public LazyCompound() {

    }

    public LazyCompound compound(Op o, Term... subs) {
        return compound(o, DTERNAL, subs);
    }

    public LazyCompound compound(Op o, int dt, Term... subs) {
        int n = subs.length;
        assert(n < Byte.MAX_VALUE);
        return compound(o, dt, (byte) n).addAll(subs);
    }

    public LazyCompound compound(Op o, byte subterms) {
        return compound(o, DTERNAL, subterms);
    }

    /** signal to create a new compound */
    public LazyCompound compound(Op o, int dt, byte subterms) {
        DynBytes c = compound(o, dt);

        //elide # subterms if NEG
        c.writeByte(subterms);

        //expects N subterms to follow...
        return this;
    }
    public LazyCompound subs(byte subterms) {
        code.writeByte(subterms);
        return this;
    }

    @NotNull
    public DynBytes compound(Op o, int dt) {
        DynBytes c = code();
        c.writeByte(o.id);

        if (o.temporal)
            c.writeInt(dt);
        else
            assert(dt == DTERNAL);
        return c;
    }

    final static byte MAX_CONTROL_CODES = (byte) Op.ops.length;

    /** add an already existent sub */
    public LazyCompound add(Term sub) {
        code().writeByte( MAX_CONTROL_CODES + sub().intern(sub) );
        return this;
    }
    public LazyCompound addAll(Term... subs) {
        DynBytes c = code();
        ByteAnonMap s = sub();
        for (Term x : subs)
            c.writeByte( MAX_CONTROL_CODES + s.intern(x) );
        return this;
    }

    private ByteAnonMap sub() {
        ByteAnonMap code = this.sub;
        return code == null ? (this.sub = new ByteAnonMap(INITIAL_ANON_SIZE)) : code;
    }

    private DynBytes code() {
        DynBytes code = this.code;
        return code == null ? (this.code = new DynBytes(INITIAL_CODE_SIZE)) : code;
    }

    /** run the construction process */
    @Nullable public Term get() {

        DynBytes c = code;
        if (code == null)
            return null; //nothing
        else {
            sub.readonly(); //optional
            return getNext(c.arrayDirect(), new int[]{0});
        }
    }

    @Nullable Term getNext(byte[] ii,  int[] pos) {
        byte ctl = ii[pos[0]++];
        Term next;
        if (ctl < MAX_CONTROL_CODES) {
            Op op = Op.ops[ctl];

            int dt;
            if (op.temporal) {
                int p = pos[0];
                pos[0]+=4;
                dt = Ints.fromBytes(ii[p++], ii[p++], ii[p++], ii[p/*++*/]);
            } else
                dt = DTERNAL;

            byte subterms = ii[pos[0]++];
            Term[] s = getNext(subterms, ii, pos);
            if (s == null)
                return null;

            next = op.the(dt, s);
            if (next == Null)
                return null;
        } else {
            next = sub.interned((byte) (ctl - MAX_CONTROL_CODES));
        }
        return next;
    }

    @Nullable
    private Term[] getNext(byte n, byte[] ii, int[] pos) {
        Term[] t = new Term[n];
        for (int s= 0; s < n; s++) {
            if ((t[s] = getNext(ii, pos)) == null)
                return null;
        }
        return t;
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
