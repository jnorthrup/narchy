package nars.truth.dynamic;

import jcog.util.ObjectLongLongPredicate;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.Truth;

public class DynamicImageTruth {

    public static final AbstractDynamicTruth ImageDynamicTruthModel = new AbstractDynamicTruth() {


        @Override
        public Truth truth(DynTaskify d) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean evalComponents(Compound superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Term reconstruct(Compound superterm, long start, long end, DynTaskify d) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int componentsEstimate() {
            return 1;
        }
    };
}
