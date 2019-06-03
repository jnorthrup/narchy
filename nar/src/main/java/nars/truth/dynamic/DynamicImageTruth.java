package nars.truth.dynamic;

import jcog.util.ObjectLongLongPredicate;
import nars.concept.util.ConceptBuilder;
import nars.table.BeliefTable;
import nars.table.dynamic.ImageBeliefTable;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.Truth;

public class DynamicImageTruth {

    public static final AbstractDynamicTruth ImageDynamicTruthModel = new AbstractDynamicTruth() {

        @Override
        public BeliefTable newTable(Term t, boolean beliefOrGoal, ConceptBuilder cb) {
            return new ImageBeliefTable(t, beliefOrGoal);
        }

        @Override
        public Truth truth(DynTaskify d) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean evalComponents(Compound superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Term reconstruct(Compound superterm, DynTaskify d, long start, long end) {
            throw new UnsupportedOperationException();
        }

    };
}
