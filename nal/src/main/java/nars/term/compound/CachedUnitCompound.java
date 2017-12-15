package nars.term.compound;

import nars.Op;
import nars.term.Term;


/** 1-element Compound impl */
public class CachedUnitCompound extends UnitCompound {

    private final Op op;

    /** hash including this compound's op (cached) */
    transient private final int chash;

    /** structure including this compound's op (cached) */
    transient private final int cstruct;

    private final Term sub;

    public CachedUnitCompound(/*@NotNull*/ Op op, /*@NotNull*/ Term sub) {
        this.sub = sub;
        this.op = op;
        this.chash = super.hashCode();
        this.cstruct = op.bit | sub.structure();
    }


    @Override
    public final Term sub() {
        return sub;
    }

    @Override
    public final int structure() {
        return cstruct;
    }


    @Override
    public int hashCode() {
        return chash;
    }


    @Override
    public final /*@NotNull*/ Op op() {
        return op;
    }


}
