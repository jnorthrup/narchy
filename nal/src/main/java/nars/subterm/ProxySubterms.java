package nars.subterm;

import nars.term.Term;
import nars.term.Termlike;

public class ProxySubterms<S extends Termlike> implements Subterms {

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
        return this == obj || equalTerms((Subterms) obj);
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
