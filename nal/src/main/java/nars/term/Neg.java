package nars.term;

import com.google.common.io.ByteArrayDataOutput;
import nars.Op;
import nars.The;
import nars.term.compound.UnitCompound;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

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
        Term x = unneg();
        Term y = x.root();
        if (y!=x)
            return y.neg();
        return this;
    }

    @Override
    public Term replace(Term from, Term to) {
        boolean fNeg = from.op()==NEG;
        if (fNeg) {
            if (this.equals(from))
                return to;
        } else {
            Term x = sub();
            if (x.equals(from))
                return to.neg();
            else {
                Term y = x.replace(from, to);
                if (y != x)
                    return y.neg();
            }
        }
        return this;
    }

    @Override
    public @Nullable Term replace(Map<? extends Term, Term> m) {
        Term n = m.get(this);
        if (n!=null)
            return n;

        Term x = sub();
        Term y = x.replace(m);
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
        if (that instanceof Compound) {
            Compound c = (Compound)that;
            return c.op()==NEG && sub.equals(c.sub(0));
        }
        return false;
//        if (that instanceof Neg) {
//            return sub.equals(((Neg)that).sub);
//        } else {
//            return Compound.equals(this, that);
//        }
    }
}
