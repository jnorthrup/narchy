package nars.link;

import jcog.pri.PLinkHashCached;
import nars.concept.Concept;
import nars.term.Term;
import nars.term.Termed;


/**
 * represents the current activation level of a concept
 */
public class Activate extends PLinkHashCached<Concept> implements Termed {

    public Activate(Concept c, float pri) {
        super(c, pri);
    }

    @Override
    public Term term() {
        return id.term();
    }

}
