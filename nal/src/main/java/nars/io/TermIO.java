package nars.io;

import com.google.common.io.ByteArrayDataOutput;
import jcog.data.byt.util.IntCoding;
import nars.$;
import nars.NAL;
import nars.Narsese;
import nars.Op;
import nars.subterm.IntrinSubterms;
import nars.subterm.RemappedSubterms;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Term;
import nars.term.anon.Anom;
import nars.term.atom.*;
import nars.term.compound.SeparateSubtermsCompound;
import nars.term.util.TermException;
import nars.time.Tense;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static nars.Op.NEG;
import static nars.io.IO.SPECIAL_BYTE;
import static nars.io.IO.encoding;
import static nars.term.anon.Intrin._term;
import static nars.term.atom.IdempotentBool.Null;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

/**
 * term i/o codec
 */
public interface TermIO {

    Term read(DataInput in) throws IOException;

    void write(Term t, ByteArrayDataOutput out);

    void writeSubterms(Subterms subs, ByteArrayDataOutput out);

    default void writeSubterms(ByteArrayDataOutput out, Term... subs) {
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
        static final byte OP_MASK = (byte) (0b00011111);
        /** upper control flags for the op byte */
        static final byte TEMPORAL_BIT_0 = (byte) (1 << 5);
        static final byte TEMPORAL_BIT_1 = (byte) (1 << 6);

        //static { assert(TEMPORAL_BIT_0 == OP_MASK + 1); }
        static {
            assert(Op.values().length < (int) OP_MASK);
            for (Op o : Op.values()) assert !o.temporal || ((int) o.id != (int) OP_MASK); /* sanity test to avoid temporal Op id appearing as SPECIAL_BYTE if the higher bits were all 1's */
        }

        @Override
        public Term read(DataInput in) throws IOException {
            byte opByte = in.readByte();
            if ((int) opByte == (int) SPECIAL_BYTE) {
                try {
                    return Narsese.term(in.readUTF(), false);
                } catch (Narsese.NarseseException e) {
                    throw new IOException(e);
                }
            }
            Op o = Op.the((int) opByte & (int) OP_MASK);
            switch (o) {
                case VAR_DEP:
                case VAR_INDEP:
                case VAR_PATTERN:
                case VAR_QUERY:
                    return $.INSTANCE.v(o, in.readByte());
                case IMG:
                    return (int) in.readByte() == (int) ((byte) '/') ? Op.ImgExt : Op.ImgInt;
                case BOOL:
                    byte code = in.readByte();
                    switch (code) {
                        case -1:
                            return Null;
                        case 0:
                            return IdempotentBool.False;
                        case 1:
                            return IdempotentBool.True;
                        default:
                            throw new UnsupportedEncodingException();
                    }
                case ATOM:
                    switch (encoding(opByte)) {
                        case 0:
                            return Atomic.the(in.readUTF());
                        case 1:
                            return Anom.the((int) in.readByte());
                        case 2:
                            return AtomBytes.atomBytes(in);
                        default:
                            throw new IOException("unknown ATOM encoding: " + encoding(opByte));
                    }
                case INT:
                    return IdempotInt.the(IntCoding.readZigZagInt(in));
                case INTERVAL:
                    return Interval.read(in);
                case NEG:
                    return read(in).neg();
                default: {

                    int temporalFlags = ((int) opByte & ((int) TEMPORAL_BIT_0 | (int) TEMPORAL_BIT_1)) >> 5;
                    int dt;
                    switch (temporalFlags) {
                        case 0: dt = DTERNAL; break;
                        case 1: dt = XTERNAL; break;
                        case 2: dt = 0; break;
                        default: /*case 3:*/ dt = IntCoding.readZigZagInt(in); break;
                    }

                    int siz = (int) in.readByte();

                    assert (siz < NAL.term.SUBTERMS_MAX);

                    Term[] s = new Term[siz];
                    for (int i = 0; i < siz; i++) {
                        Term read = (s[i] = read(in));
                        if (read == null)
                            throw new TermException("read invalid", Op.PROD /* consider the termvector as a product */, s);
                    }

                    Term y = o.the(dt, s);
                    if (!(y instanceof Compound))
                        throw new TermException("read invalid compound", o, dt, s);

                    return y;
                }

            }
        }

        @Override
        public void write(Term t, ByteArrayDataOutput out) {
            if (t == Null)
                throw new NullPointerException("null");

            if (t instanceof Neg) {
                outNegByte(out);
                t = t.unneg();
            }

            if (t instanceof Atomic) {

                out.write(((Atomic) t).bytes());

            } else {


                Op o = t.op();

                writeCompoundPrefix(o, o.temporal ? t.dt() : DTERNAL, out);


                Subterms s = t instanceof SeparateSubtermsCompound ? t.subterms() : ((Subterms) t);
                int ss = s.subs();
                out.writeByte(ss);
                for (int i = 0; i < ss; i++)
                    write(s.sub(i), out);

            }
        }

        public void writeCompoundPrefix(Op o, int dt, ByteArrayDataOutput out) {

            byte opByte = o.id;
            boolean dtSpecial = false;
            if (dt != DTERNAL && o.temporal) {
                switch (dt) {
                    case XTERNAL:
                        opByte = (byte) ((int) opByte | (int) TEMPORAL_BIT_0); break;
                    case 0:
                        opByte = (byte) ((int) opByte | (int) TEMPORAL_BIT_0 | (int) TEMPORAL_BIT_1); break;
                    default:
                        opByte = (byte) ((int) opByte | (int) TEMPORAL_BIT_0 | (int) TEMPORAL_BIT_1); dtSpecial = true; break;
                }
            }
            out.writeByte((int) opByte);
            if (dtSpecial)
                writeDT(dt, out);
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
                        outNegByte(out);
                        x = (int) (byte) -x;
                    }
                    write(ttt.mapTerm(x), out);
                }
            } else if (tt instanceof IntrinSubterms) {
                IntrinSubterms ttt = (IntrinSubterms) tt;
                short[] ss = ttt.subterms;
                out.writeByte(ss.length);
                for (short s : ss) {
                    if ((int) s < 0) {
                        outNegByte(out);
                        s = (short) -(int) s;
                    }
                    write(_term(s), out);
                }
            } else {
                out.writeByte(tt.subs());
                tt.forEachWith(this::write, out);
            }
        }
    }

    static void outNegByte(ByteArrayDataOutput out) {
        out.writeByte((int) NEG.id);
    }

    /**
     * destructive
     * TODO test
     */
    class DeferredTemporalTermIO extends DefaultTermIO {
        @Nullable IntArrayList dts = null;

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
            IntArrayList d = this.dts;
            if (d == null)
                return;
            int n = d.size();
            if (n == 1) {
                //only one: canonicalize to either 0 or +/- dtDither
                int x = d.get(0);
                d.set(0, ditherUniform(dtDither, x));
            } else {
                boolean same = true;
                for (int i = 0; i < n - 1; i++) {
                    int a = d.get(i);
                    int b = d.get(i + 1);
                    if (a != b) {
                        same = false;
                        break;
                    }
                }
                if (same) {
                    int x = d.get(0);
                    int y = ditherUniform(dtDither, x);
                    for (int i = 0; i < n; i++) {
                        d.set(i, y);
                    }

                } else {

                    //TODO if different, use lowest common denominator or something
                }
            }

            for (int i = 0; i < n; i++)
                super.writeDT(d.get(i), out);
        }

        static int ditherUniform(int dtDither, int x) {
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
