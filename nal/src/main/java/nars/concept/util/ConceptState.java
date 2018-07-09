package nars.concept.util;

import nars.concept.Concept;
import nars.concept.TaskConcept;

/**
 * interface for a management model responsible for concept resource allocation:
 * --budget (time)
 * --memory (space)
 */
public abstract class ConceptState  {


    private final String id;

    protected ConceptState(String id) {
        this.id = id;
    }

    public abstract int linkCap(Concept concept, boolean termOrTask);

    public abstract int beliefCap(TaskConcept concept, boolean beliefOrGoal, boolean eternalOrTemporal);

    public abstract int questionCap(TaskConcept concept, boolean questionOrQuest);

    private static class EmptyConceptState extends ConceptState {


        EmptyConceptState(String name) {
            super(name);
        }

        @Override
        public int linkCap(Concept concept, boolean termOrTask) {
            return 0;
        }

        @Override
        public int beliefCap(TaskConcept concept, boolean beliefOrGoal, boolean eternalOrTemporal) {
            return 0;
        }

        @Override
        public int questionCap(TaskConcept concept, boolean questionOrQuest) {
            return 0;
        }
    }

    public static final ConceptState New = new EmptyConceptState("new");
    public static final ConceptState Deleted = new EmptyConceptState("deleted");



}
