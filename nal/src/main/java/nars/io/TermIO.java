package nars.io;

import com.google.common.io.ByteArrayDataOutput;
import jcog.TODO;
import jcog.data.byt.util.IntCoding;
import nars.$;
import nars.Narsese;
import nars.Op;
import nars.Param;
import nars.subterm.AnonSubterms;
import nars.subterm.RemappedSubterms;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Term;
import nars.term.anon.Anom;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.atom.Int;
import nars.term.compound.SeparateSubtermsCompound;
import nars.term.util.TermException;
import nars.time.Tense;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static nars.io.IO.SPECIAL_BYTE;
import static nars.io.IO.subType;
import static nars.term.anon.AnonID.termPos;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

/**
 * term i/o codec
 */
public interface TermIO {

    Term read(DataInput in) throws IOException;

    void write(Term t, ByteArrayDataOutput out);

    void writeSubterms(Subterms subs, ByteArrayDataOutput out);

    default void writeSubterms(Term[] subs, ByteArrayDataOutput out) {
        int n = subs.length;
        //assert(n < Byte.MAX_VALUE);
        out.writeByte(n);
        for (Term s : subs)
            write(s, out);
    }

    /**
     * default term codec
     */
    DefaultTermIO the = new DefaultTermIO();



    class DefaultTermIO implements TermIO {

        /** lower 5 bits (bits 0..4) = base op */
        public static final byte OP_MASK = (0b00011111);
        /** upper control flags for the op byte */
        protected static final byte TEMPORAL_BIT_0 = 1 << 5;
        protected static final byte TEMPORAL_BIT_1 = 1 << 6;

        //static { assert(TEMPORAL_BIT_0 == OP_MASK + 1); }
        static { assert(Op.values().length < OP_MASK); }
        static { for (Op o : Op.values()) assert !o.temporal || (o.id != OP_MASK); /* sanity test to avoid temporal Op id appearing as SPECIAL_BYTE if the higher bits were all 1's */
        }

        @Override
        public Term read(DataInput in) throws IOException {
            byte opByte = in.readByte();
            if (opByte == SPECIAL_BYTE) {
                try {
                    return Narsese.term(in.readUTF(), false);
                } catch (Narsese.NarseseException e) {
                    throw new IOException(e);
                }
            }
            Op o = Op.ops[opByte & OP_MASK];
            switch (o) {
                case VAR_DEP:
                case VAR_INDEP:
                case VAR_PATTERN:
                case VAR_QUERY:
                    return $.v(o, in.readByte());
                case IMG:
                    return in.readByte() == ((byte) '/') ? Op.ImgExt : Op.ImgInt;
                case BOOL:
                    byte code = in.readByte();
                    switch (code) {
                        case -1:
                            return Bool.Null;
                        case 0:
                            return Bool.False;
                        case 1:
                            return Bool.True;
                        default:
                            throw new UnsupportedEncodingException();
                    }
                case ATOM:
                    switch (subType(opByte)) {
                        case 0:
                            return Atomic.the(in.readUTF());
                        case 1:
                            return Anom.the(in.readByte());
                        default:
                            throw new TODO();
                    }
                case INT:
                    return Int.the(IntCoding.readZigZagInt(in));
                case NEG:
                    return read(in).neg();
                default: {

                    int temporalFlags = (opByte & (TEMPORAL_BIT_0|TEMPORAL_BIT_1)) >> 5;
                    int dt;
                    switch (temporalFlags) {
                        case 0: dt = DTERNAL; break;
                        case 1: dt = XTERNAL; break;
                        case 2: dt = 0; break;
                        default: /*case 3:*/ dt = IntCoding.readZigZagInt(in); break;
                    }

                    int siz = in.readByte();

                    assert (siz < Param.SUBTERMS_MAX);

                    Term[] s = new Term[siz];
                    for (int i = 0; i < siz; i++) {
                        Term read = (s[i] = read(in));
                        if (read == null)
                            throw new TermException("invalid", Op.PROD /* consider the termvector as a product */, s);
                    }

                    Term[] v = s;

                    Term y = o.the(dt, v);
                    if (!(y instanceof Compound))
                        throw new TermException("read invalid compound", o, dt, v);

                    return y;
                }

            }
        }

        @Override
        public void write(Term t, ByteArrayDataOutput out) {
            if (t instanceof Neg) {
                out.writeByte(Op.NEG.id);
                t = t.unneg();
            }

            if (t instanceof Atomic) {
                out.write(((Atomic) t).bytes());
            } else {
                Op o = t.op();

                writeCompoundPrefix(o, o.temporal ? t.dt() : DTERNAL, out);

                writeSubterms((t instanceof SeparateSubtermsCompound ? t.subterms() : ((Subterms) t)), out);


            }
        }

        public void writeCompoundPrefix(Op o, int dt, ByteArrayDataOutput out) {

            boolean temporal = o.temporal && dt != DTERNAL;

            if (!temporal) {
                out.writeByte(o.id);
            } else {
                byte opByte = o.id;
                boolean dtSpecial = false;
                switch (dt) {
                    case DTERNAL: /* nothing */ break;
                    case XTERNAL: opByte |= TEMPORAL_BIT_0; break;
                    case 0: opByte |= TEMPORAL_BIT_0 | TEMPORAL_BIT_1; break;
                    default: opByte |= TEMPORAL_BIT_0 | TEMPORAL_BIT_1; dtSpecial = true; break;
                }
                out.writeByte(opByte);
                if (dtSpecial)
                    writeDT(dt, out);
            }

        }

        protected void writeDT(int dt, ByteArrayDataOutput out) {
            IntCoding.writeZigZagInt(dt, out);
        }

        @Override
        public void writeSubterms(Subterms tt, ByteArrayDataOutput out) {
            /*if (tt instanceof RemappedSubterms.ArrayRemappedSubterms) {
                RemappedSubterms.ArrayRemappedSubterms ttt = (RemappedSubterms.ArrayRemappedSubterms) tt;
                byte[] xx = ttt.map;
                out.writeByte(xx.length);
                for (byte x : xx) {
                    if (x < 0) {
                        out.writeByte(Op.NEG.id);
                        x = (byte) -x;
                    }
                    write(ttt.mapSub(x), out);
                }
            } else */if (tt instanceof RemappedSubterms) {
                RemappedSubterms ttt = (RemappedSubterms) tt;
                int s = ttt.subs();
                out.writeByte(s);
                for (int i = 0; i < s; i++) {
                    int x = ttt.subMap(i);
                    if (x < 0) {
                        out.writeByte(Op.NEG.id);
                        x = (byte) -x;
                    }
                    write(ttt.mapTerm(x), out);
                }
            } else if (tt instanceof AnonSubterms) {
                AnonSubterms ttt = (AnonSubterms) tt;
                short[] ss = ttt.subterms;
                out.writeByte(ss.length);
                for (short s : ss) {
                    if (s < 0) {
                        out.writeByte(Op.NEG.id);
                        s = (short) -s;
                    }
                    write(termPos(s), out);
                }
            } else {
                int n = tt.subs();
                out.writeByte(n);
                for (int i = 0; i < n; i++)
                    write(tt.sub(i), out);
            }
        }
    }

    /**
     * destructive
     */
    class DeferredTemporalTermIO extends DefaultTermIO {
        @Nullable
        public IntArrayList dts = null;

        @Override
        protected void writeDT(int dt, ByteArrayDataOutput out) {
            if (this.dts == null)
                this.dts = new IntArrayList(4);
            dts.add(dt);
        }


        /**
         * canonical heuristic
         */
        public void writeDTs(int dtDither, ByteArrayDataOutput out) {
            if (dts == null)
                return;
            int n = dts.size();
            if (n == 1) {
                //only one: canonicalize to either 0 or +/- dtDither
                int x = dts.get(0);
                dts.set(0, ditherUniform(dtDither, x));
            } else {
                boolean same = true;
                for (int i = 0; i < n - 1; i++) {
                    int a = dts.get(i);
                    int b = dts.get(i + 1);
                    if (a != b) {
                        same = false;
                        break;
                    }
                }
                if (same) {
                    int x = dts.get(0);
                        int y = ditherUniform(dtDither, x);
                        for (int i = 0; i < n; i++) {
                            dts.set(i, y);
                        }

                } else {

                    //TODO if different, use lowest common denominator or something
                }
            }

            for (int i = 0; i < n; i++)
                super.writeDT(dts.get(i), out);
        }

        public static int ditherUniform(int dtDither, int x) {
            int y = Tense.dither(x, dtDither);
            if (y != 0)
                y = y > 0 ? dtDither : -dtDither; //destroying most of the the temporal data in canonicalization
            return y;
        }

        @Override
        public final Term read(DataInput in) throws IOException {
            throw new UnsupportedEncodingException();
        }
    }
}
