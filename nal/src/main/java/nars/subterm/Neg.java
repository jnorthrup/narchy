package nars.subterm;

import com.google.common.io.ByteArrayDataOutput;
import nars.Op;
import nars.The;
import nars.term.Term;
import nars.term.anon.Anom;
import nars.term.compound.UnitCompound;
import org.jetbrains.annotations.Nullable;

import static nars.Op.NEG;

public final class Neg extends UnitCompound implements The {

    private final Term sub;

    public static Term the(Term x) {
        switch (x.op()) {
            case ATOM:
                if (x instanceof Anom) {
                    return x.neg();
                }
                break;
            case BOOL:
                return x.neg();
            case NEG:
                return x.unneg();
        }
        return new Neg(x);
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
    public boolean equals(@Nullable Object that) {
        if (that instanceof Neg) {
            return this==that ||
                    (sub.equals(((Neg)that).sub));
        }
        return super.equals(that);
    }

    @Override
    public final boolean equalsNeg(Term t) {
        return sub.equals(t);
    }

}
