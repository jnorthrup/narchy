package nars.link;

import jcog.list.FasterList;
import nars.NAR;
import nars.concept.Concept;
import nars.term.Term;
import org.apache.commons.lang3.ArrayUtils;

public class TermlinkTemplates extends FasterList<Term> {

    static final TermlinkTemplates EMPTY = new TermlinkTemplates(Term.EmptyArray) {
        {
            concepts = 0;
        }
    };

    /** index of the last concept template; any others beyond this index are non-conceptualizable */
    byte concepts = -1;

    public TermlinkTemplates(Term[] terms) {
        super(terms.length, terms);

        if (size > 1)
            sortThisByBoolean((t) -> !conceptualizable(t));

        //scan backwards to find the index where the first non-conceptualizable occurrs
        int lastConcept = size-1;
        for (; lastConcept >= 0; lastConcept--) {
            if (conceptualizable(get(lastConcept)))
                break;
        }
        assert(lastConcept < 127);
        concepts = (byte)(lastConcept+1);
    }

    /** creates a sub-array of the conceptualizable terms and shuffles them */
    public Concept[] conceptsShuffled(NAR nar, boolean conceptualize) {
        int concepts = this.concepts;
        if (concepts == 0)
            return Concept.EmptyArray;

        Concept[] x = new Concept[concepts];
        int nulls = 0;
        for (int i = 0; i < concepts; i++) {
            if ((x[i] = nar.concept(items[i], conceptualize)) == null)
                nulls++;
        }
        if (nulls == concepts)
            return Concept.EmptyArray; //none conceptualizable
        else if (nulls > 0) {
            x = ArrayUtils.removeNulls(x, Concept[]::new);
        }

        if (x.length > 1)
            ArrayUtils.shuffle(x, nar.random());

        return x;
    }

    public static boolean conceptualizable(Term x) {
        return x.op().conceptualizable;
    }

}
