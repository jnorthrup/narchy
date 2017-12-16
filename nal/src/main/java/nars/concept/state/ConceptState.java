package nars.concept.state;

import nars.concept.TaskConcept;
import nars.concept.Concept;
import nars.term.atom.Atom;
import org.jetbrains.annotations.NotNull;

/**
 * interface for a management model responsible for concept resource allocation:
 * --budget (time)
 * --memory (space)
 */
public abstract class ConceptState extends Atom {



    protected ConceptState(@NotNull String id) {
        super(id);
    }

    public abstract int linkCap(Concept concept, boolean termOrTask);

    public abstract int beliefCap(TaskConcept concept, boolean beliefOrGoal, boolean eternalOrTemporal);

    public abstract int questionCap(TaskConcept concept, boolean questionOrQuest);


    public static final ConceptState New = new EmptyConceptState("new");
    public static final ConceptState Deleted = new EmptyConceptState("deleted");

    /**
     * used by Null concept builder, used by built-in static Functors, and other shared/ system facilities
     */
    public static final ConceptState Abstract = new ConceptState("abstract") {


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
    };

    private static class EmptyConceptState extends ConceptState {


        public EmptyConceptState(String name) {
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
}
