package nars.control;

import jcog.data.map.SaneObjectFloatHashMap;
import nars.NAR;
import nars.concept.Concept;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;

public class BatchActivation {

    final ObjectFloatHashMap<Concept> a = new SaneObjectFloatHashMap<>(64);

    final static ThreadLocal<BatchActivation> the = ThreadLocal.withInitial(BatchActivation::new);

    //final static LongHashSet active = new LongHashSet();

    public static BatchActivation get() {
        return the.get();
    }

    BatchActivation() {

    }


    public void commit(NAR nar) {
        if (!a.isEmpty()) {

            a.forEachKeyValue(nar::activate);

            a.clear();

        }
    }

    public void put(Concept c, float pri) {
        a.addToValue(c, pri);
    }


//        public static class BatchActivateCommit extends NativeTask {
//
//            private final Activate[] activations;
//
//            public BatchActivateCommit(Activate[] l) {
//                this.activations = l;
//            }
//
//            @Override
//            public String toString() {
//                return "ActivationBatch x" + activations.length;
//            }
//
//            @Override
//            public @Nullable Iterable<? extends ITask> run(NAR n) {
//                n.input(activations);
//                return null;
//            }
//        }
}
