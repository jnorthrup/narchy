package nars.subterm;

import com.google.common.io.ByteArrayDataOutput;
import nars.Op;
import nars.The;
import nars.term.Term;
import nars.term.compound.UnitCompound;

import static nars.Op.NEG;

public final class Neg extends UnitCompound implements The {


    private final Term sub;

    public Neg(Term negated) {
        this.sub = negated;
    }


    @Override
    public Op op() {
        return NEG;
    }

    @Override
    public Term sub() {
        return sub;
    }


    /** condensed NEG compound byte serialization - elides length byte */
    @Override public final void appendTo(ByteArrayDataOutput out) {
        out.writeByte(Op.NEG.id);
        sub.appendTo(out);
    }

    @Override
    public Term neg() {
        return sub;
    }

    @Override
    public Term unneg() {
        return sub;
    }


    @Override
    public final boolean equalsNeg(Term t) {
        return sub.equals(t);
    }

}
