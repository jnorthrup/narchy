package nars.truth.dynamic;

import nars.NAR;
import nars.Task;
import nars.concept.util.ConceptBuilder;
import nars.table.BeliefTable;
import nars.term.Term;
import nars.term.util.Image;
import nars.truth.Truth;

import java.util.List;

public class DynamicImageTruth {

    public static final AbstractDynamicTruth ImageDynamicTruthModel = new AbstractDynamicTruth() {

        @Override
        public BeliefTable newTable(Term t, boolean beliefOrGoal, ConceptBuilder cb) {
            return new Image.ImageBeliefTable(t, beliefOrGoal);
        }

        @Override
        public Truth truth(DynEvi var1, NAR nar) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Term reconstruct(Term superterm, List<Task> c, NAR nar, long start, long end) {
            throw new UnsupportedOperationException();
        }

    };
}
