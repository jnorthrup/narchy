package nars.term.compound;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.primitives.Ints;
import jcog.data.byt.DynBytes;
import nars.IO;
import nars.Op;
import nars.Param;
import nars.The;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static nars.time.Tense.DTERNAL;

/**
 * TODO
 * compound which is stored simply as a byte[] of its serialization
 * which is optimized for streaming and lazy/de-duplicated batched construction
 * purposes.
 * <p>
 * see IO.writeTerm()
 */
public class SerialCompound extends DynBytes implements Compound, The {

    final byte volume;

    public SerialCompound(Compound c) {
        this(c.op(), c.dt(), c.arrayShared());
    }

    public SerialCompound(Op op, int dt, Term[] subterms) {
        super(subterms.length * 4 /* estimate */);

        writeByte(op.id);
        writeByte(subterms.length);

        int v = 1;

        try {

            for (int i = 0; i < subterms.length; i++) {
                Term x = subterms[i];

                x.append((ByteArrayDataOutput) this);

                v += x.volume();
            }

            IO.writeCompoundSuffix(this, dt, op);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        assert(v < Param.COMPOUND_VOLUME_MAX);
        this.volume = (byte) v;

    }


    public Compound build() {
        return (Compound) IO.termFromBytes(bytes);
    }

    @Override
    public final /*@NotNull*/ Op op() {
        return Op.values()[bytes[0]];
    }

    @Override
    public int subs() {
        return bytes[1];
    }

    @Override
    public int volume() {
        return volume;
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException(); //TODO impl in a subclass
    }

    @Override
    public int hashCodeSubterms() {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public boolean equals(Object obj) {
        throw new UnsupportedOperationException(); //TODO impl in a subclass
    }

    @Override
    public @NotNull Subterms subterms() {
        return build().subterms(); //HACK just a temporary way of accessing them
    }

//    static class SerialTermVector implements TermContainer {
//
//    }

    @Override
    public boolean isNormalized() {
        return false;
    }

    @Override
    public int dt() {
        //last two bytes
        Op o = op();
        if (o.temporal) {
            int p = this.len;
            final byte[] b = bytes;
            return Ints.fromBytes(b[p-3], b[p-2], b[p-1], b[p]);
        } else {
            return DTERNAL;
        }
    }

//        public ByteSource in;
//
//    final byte volume;
//    boolean normalized;
//
//
//    public static SerialCompound heap(Compound c) {
//        return heap(c.op(), c.dt(), c.theArray());
//    }
//
//    public static SerialCompound heap(Op op, int dt, Term... subterms) {
//
//
//        UncheckedBytes h = new UncheckedBytes(Bytes.allocateDirect(new byte[64 * subterms.length /* est */]));
//        h.writeByte(op.id);
//        h.writeByte((byte) subterms.length);
//
//        int v = 1;
//
//        try {
//
//            for (int i = 0; i < subterms.length; i++) {
//                Term x = subterms[i];
//
//                x.append((ByteArrayDataOutput) this);
//
//                v += x.volume();
//            }
//
//            IO.writeCompoundSuffix(this, dt, op);
//
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//
//        assert(v < Param.COMPOUND_VOLUME_MAX);
//        this.volume = (byte) v;
//
//    }


}

//    public SerialCompound(/*@NotNull*/ Op op, int dt, @NotNull TermContainer subs) {
//        super(op, dt, subs);
//
//        uniqueSubs = new ObjectByteHashMap<>(volume());
//        recurseTerms((x) -> uniqueSubs.getIfAbsentPut(x, ()->(byte)uniqueSubs.size()));
//
//        new SubtermVisitorXY(term()) {
//
//            @NotNull
//            @Override
//            public SubtermVisitorXY.Next accept(int subterm, Compound superterm, int depth) {
//                System.out.println(superterm + "(" + subterm + "): " + superterm.term(subterm) + ", depth=" + depth);
//                return Next.Next;
//            }
//        };
//
//        uniqueSubIndex = new Term[uniqueSubs.size()];
//        uniqueSubs.forEachKeyValue((k,v) -> {
//            uniqueSubIndex[v] = k;
//        });
//
//    }
//
//    public void print() {
//        System.out.println(toString() + "\n\tuniqueSubs=" + uniqueSubs);
//    }
