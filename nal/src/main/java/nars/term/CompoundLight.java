package nars.term;

import nars.Op;
import nars.subterm.Subterms;

import static nars.time.Tense.DTERNAL;

public class CompoundLight implements Compound {

    final Subterms subs;
    private final Op op;
    private final int hash;

    public CompoundLight(Op o, Subterms s) {
        this.op = o;
        this.subs = s;
        this.hash = s.hashWith(o);
    }

    @Override
    public boolean equals(Object obj) {
        return (this == obj) ||
                (obj instanceof Compound) && Compound.equals(this, (Term) obj);
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return Compound.toString(this);
    }

    @Override
    public final Op op() {
        return op;
    }

    @Override
    public final Subterms subterms() {
        return subs;
    }

    @Override
    public final int dt() {
        return DTERNAL;
    }
}
