package nars.subterm;

import nars.Op;
import nars.The;
import nars.term.Term;
import nars.term.compound.UnitCompound;

import static nars.Op.NEG;

public final class Neg extends UnitCompound implements The {

    private final Term sub;

    public static Term the(Term x) {
        switch (x.op()) {
            case BOOL:
                return x.neg();
            case NEG:
                return x.unneg();
            default:
                return new Neg(x);
        }
    }

    @Override
    public Term neg() {
        return sub;
    }

    @Override
    public Term unneg() {
        return sub;
    }

    private Neg(Term negated) {
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

}
