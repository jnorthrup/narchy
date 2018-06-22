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

                x.appendTo((ByteArrayDataOutput) this);

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
        return (Compound) IO.bytesToTerm(bytes);
    }

    @Override
    public final /*@NotNull*/ Op op() {
        return Op.values()[bytes[0]];
    }

    @Override
    public Term sub(int i) {
        return subterms().sub(i); //HACK TODO slow
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
        throw new UnsupportedOperationException(); 
    }

    @Override
    public int hashCodeSubterms() {
        throw new UnsupportedOperationException(); 
    }

    @Override
    public boolean equals(Object obj) {
        throw new UnsupportedOperationException(); 
    }

    @Override
    public @NotNull Subterms subterms() {
        return build().subterms(); 
    }





    @Override
    public boolean isNormalized() {
        return false;
    }

    @Override
    public int dt() {
        
        Op o = op();
        if (o.temporal) {
            int p = this.len;
            final byte[] b = bytes;
            return Ints.fromBytes(b[p-3], b[p-2], b[p-1], b[p]);
        } else {
            return DTERNAL;
        }
    }










































}



























