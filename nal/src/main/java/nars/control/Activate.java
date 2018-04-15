package nars.control;

import jcog.pri.PLink;
import nars.concept.Concept;
import nars.term.Term;
import nars.term.Termed;


/**
 * represents the current activation level of a concept
 */
public class Activate extends PLink<Concept> implements Termed {

    public Activate(Concept c, float pri) {
        super(c, pri);
    }

    @Override
    public Term term() {
        return id.term();
    }

}
