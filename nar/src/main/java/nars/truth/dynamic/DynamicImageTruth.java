package nars.truth.dynamic;

import jcog.util.ObjectLongLongPredicate;
import nars.NAL;
import nars.NAR;
import nars.Task;
import nars.concept.util.ConceptBuilder;
import nars.table.BeliefTable;
import nars.table.dynamic.ImageBeliefTable;
import nars.task.util.TaskList;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.Truth;

import java.util.List;

public class DynamicImageTruth {

    public static final AbstractDynamicTruth ImageDynamicTruthModel = new AbstractDynamicTruth() {

        @Override
        public BeliefTable newTable(Term t, boolean beliefOrGoal, ConceptBuilder cb) {
            return new ImageBeliefTable(t, beliefOrGoal);
        }

        @Override
        public Truth truth(TaskList var1, NAL<NAL<NAR>> NAL) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean evalComponents(Compound superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Term reconstruct(Compound superterm, List<Task> c, NAL<NAL<NAR>> NAL, long start, long end) {
            throw new UnsupportedOperationException();
        }

    };
}
