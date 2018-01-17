package nars.subterm;

import nars.term.Term;

import java.util.List;

public class ProxySubterms implements Subterms {

    protected final Subterms ref;

    public ProxySubterms(Subterms ref) {
        this.ref = ref;
    }

    @Override
    public int hashCode() {
        return Subterms.hash((List)this);
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
