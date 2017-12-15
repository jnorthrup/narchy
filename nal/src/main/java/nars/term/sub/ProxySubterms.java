package nars.term.sub;

import jcog.TODO;
import nars.term.Term;

/** TODO */
public class ProxySubterms implements Subterms {

    protected final Subterms ref;

    public ProxySubterms(Subterms ref) {
        this.ref = ref;
    }

    @Override
    public int hashCode() {
        throw new TODO();
    }

    @Override
    public boolean equals(Object obj) {
        throw new TODO();
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
