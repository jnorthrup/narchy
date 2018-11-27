package nars.subterm;

import nars.term.Term;

public class ProxySubterms<S extends Subterms> implements Subterms {

    protected final S ref;

    protected ProxySubterms(S ref) {
        this.ref = ref;
    }

    @Override
    public int hashCode() {
        return Subterms.hash(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        return ((Subterms)obj).equalTerms(this);
    }

    @Override
    public String toString() {
        return Subterms.toString(this);
    }

    @Override
    public Term sub(int i) {
        return ref.sub(i);
    }

    @Override
    public int subs() {
        return ref.subs();
    }

}
