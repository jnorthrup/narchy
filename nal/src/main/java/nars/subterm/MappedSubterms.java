package nars.subterm;

import nars.subterm.util.TermMetadata;
import nars.term.Termlike;

public abstract class MappedSubterms<S extends Termlike> extends ProxySubterms<S> {

    private boolean normalizedKnown = false, normalized = false;

    MappedSubterms(S ref) {
        super(ref);
    }

    @Override
    public final boolean hasXternal() {
        return ref.hasXternal();
    }

    @Override
    public final boolean these() {
        return ref.these();
    }

    @Override
    public final boolean isNormalized() {
        if (!normalizedKnown && !normalized) {
            normalized = TermMetadata.normalized(this);
            normalizedKnown = true;
        }
        return normalized;
    }

    @Override
    public final void setNormalized() {
        this.normalizedKnown = this.normalized = true;
    }

    @Override
    public final String toString() {
        return Subterms.toString(this);
    }


}
