package nars.concept;

import jcog.math.FloatRange;
import nars.Param;
import nars.concept.util.ConceptBuilder;
import nars.table.BeliefTable;
import nars.term.Term;

/**
 * base class for concepts which are sensors
 * providing some form of belief feedback
 * and may also support some type of actuation
 * (via goals or otherwise)
 * <p>
 * this usually requires some specific management of
 * beliefs to prevent influence from derivations that the reasoner may form
 * in contradiction with provided values.
 * <p>
 * warning: using action and sensor concepts with a term that can be structurally transformed
 * culd have unpredictable results because their belief management policies
 * may not be consistent with the SensorConcept.  one solution may be to
 * create dummy placeholders for all possible transforms of a sensorconcept term
 * to make them directly reflect the sensor concept as the authority.
 */
public class Sensor extends TaskConcept implements PermanentConcept {

    public final FloatRange resolution = new FloatRange(Param.TRUTH_EPSILON, 0f, 1f);


    protected Sensor(Term term, ConceptBuilder b) {
        super(term, b);
    }

    protected Sensor(Term term, BeliefTable beliefs, BeliefTable goals, ConceptBuilder b) {
        super(term, beliefs, goals, b);
    }


}
