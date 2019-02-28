package nars.term;

import com.google.common.io.ByteArrayDataOutput;
import nars.Op;
import nars.The;
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
    public int opBit() {
        return NEG.bit;
    }


    @Override
    public Term sub() {
        return sub;
    }

    @Override
    public Term root() {
        Term x = unneg();
        Term y = x.root();
        if (y!=x)
            return y.neg();
        return this;
    }

    @Override
    public Term concept() {
        return unneg().concept();
    }

    @Override
    public @Nullable Term normalize(byte varOffset) {
        Term x = unneg();
        Term y = x instanceof Variable ? ((Variable)x).normalizedVariable((byte) (varOffset+1)) : x.normalize(varOffset);
        if (y!=x)
            return y.neg();
        return this;
    }

    @Override
    public Term neg() {
        return sub;
    }

    @Override
    public Term negIf(boolean negate) {
        return negate ? sub : this;
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
