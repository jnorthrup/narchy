package nars.subterm;

import com.google.common.io.ByteArrayDataOutput;
import nars.Op;
import nars.The;
import nars.term.Term;
import nars.term.compound.UnitCompound;
import org.jetbrains.annotations.Nullable;

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

    @Override
    public Term root() {
        Term x = unneg().root();
        if (x!=this)
            return x.neg();
        return this;
    }

    @Override
    public Term concept() {
        return unneg().concept();
    }


    @Override
    public @Nullable Term normalize() {
        Term x = unneg().normalize();
        if (x!=this)
            return x.neg();
        return this;
    }

    @Override
    public @Nullable Term normalize(byte varOffset) {
        Term x = unneg().normalize(varOffset);
        if (x!=this)
            return x.neg();
        return this;
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

    @Override
    public boolean equals(@Nullable Object that) {
        if (this == that) return true;
        if (that instanceof Neg) {
            return sub.equals(((Neg)that).sub);
        } else {
            return super.equals(that);
        }
    }
}
