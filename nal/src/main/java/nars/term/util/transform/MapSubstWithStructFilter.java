package nars.term.util.transform;

import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

public abstract class MapSubstWithStructFilter implements Subst {
    protected final int structure;

    MapSubstWithStructFilter(int structure) {
        this.structure = structure;
        assert(structure!=0);
    }

    @Override
    public @Nullable Term applyCompound(Compound x) {
        return !x.hasAny(structure) ? x : Subst.super.applyCompound(x);
    }

    @Nullable
    @Override
    public abstract Term xy(Term t);
}
