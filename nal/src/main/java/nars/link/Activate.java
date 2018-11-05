package nars.link;

import jcog.pri.PLinkHashCached;
import nars.concept.Concept;
import nars.term.Term;
import nars.term.Termed;


/**
 * represents the current activation level of a concept
 */
public final class Activate extends PLinkHashCached<Concept> implements Termed {

    private Activate(Concept c, int hash, float pri) {
        super(c, hash, pri);
    }

    public Activate(Concept c, float pri) {
        this(c, c.hashCode(), pri);
    }

    public Activate(Concept c) {
        this(c, 0);
    }

    @Override
    public Term term() {
        return id.term();
    }

    public Activate clone() {
        return new Activate(id, hash, pri());
    }
}
