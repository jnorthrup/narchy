package nars.subterm;

import nars.subterm.util.TermMetadata;
import nars.term.Termlike;

public abstract class MappedSubterms<S extends Termlike> extends ProxySubterms<S> {

    private boolean normalizedKnown = false, normalized = false;

    public MappedSubterms(S ref) {
        super(ref);
    }


    @Override
    public final boolean hasXternal() {
        return ref.hasXternal();
    }
    @Override
    public boolean these() {
        return ref.these();
    }

    @Override
    public boolean isNormalized() {
        if (!normalizedKnown && !normalized) {
            normalized = TermMetadata.normalized(this);
            normalizedKnown = true;
        }
        return normalized;
    }

    @Override
    public String toString() {
        return Subterms.toString(this);
    }

    @Override
    public void setNormalized() {
        this.normalizedKnown = this.normalized = true;
    }


}
