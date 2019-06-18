package nars.term.util.transform;

import nars.term.Term;
import org.jetbrains.annotations.Nullable;

public abstract class MapSubstWithStructFilter extends MapSubst {
    protected final int structure;

    MapSubstWithStructFilter(int structure) {
        this.structure = structure;
    }

    @Override
    public Term apply(Term x) {
        return !x.hasAny(structure) ? x : super.apply(x);
    }

    @Nullable
    @Override
    public abstract Term xy(Term t);
}
