package nars.term.util.transform;

import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

public abstract class MapSubstWithStructFilter extends MapSubst {
    protected final int structure;

    MapSubstWithStructFilter(int structure) {
        this.structure = structure;
    }

    @Override
    public @Nullable Term applyCompound(Compound x) {
        return !x.hasAny(structure) ? x : super.applyCompound(x);
    }

    @Nullable
    @Override
    public abstract Term xy(Term t);
}
