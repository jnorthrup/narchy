package nars.term.compound;

import nars.The;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

public abstract class SemiCachedUnitCompound extends UnitCompound implements The {

    protected final Term sub;

    /** hash including this compound's op (cached) */
    protected final int chash;

    protected SemiCachedUnitCompound(int opID, Term sub) {
        this(sub, Compound.hash1(opID, sub));
    }

    protected SemiCachedUnitCompound(Term sub, int chash) {
        this.sub = sub;
        this.chash = chash;
    }

    @Override
    public final Term sub() {
        return sub;
    }

    @Override
    public final int hashCode() {
        return chash;
    }

    @Override
    public final boolean equals(@Nullable Object that) {
        return Compound.equals(this, that, true);
    }
}
