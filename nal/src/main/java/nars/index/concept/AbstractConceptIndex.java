package nars.index.concept;

import jcog.math.FloatRange;
import jcog.math.IntRange;
import jcog.pri.ScalarValue;
import jcog.pri.bag.Bag;
import jcog.pri.bag.impl.ArrayBag;
import nars.NAR;
import nars.Param;
import nars.link.TaskLink;
import nars.link.TaskLinkBag;
import nars.term.Termed;

import java.util.HashMap;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Stream;

import static jcog.pri.op.PriMerge.plus;

/** implements a multi-level cache using a Concept Bag as a sample-able short-target memory */
abstract public class AbstractConceptIndex extends ConceptIndex {

    /** short target memory, TODO abstract and remove */
    //public Bag<Term, Activate> active = Bag.EMPTY;

    public TaskLinkBag active = null;

    /** tasklink activation */
    @Deprecated public final FloatRange activationRate = new FloatRange(1f, Param.tasklinkMerge == plus ? ScalarValue.EPSILON : 1, 1);

    public final FloatRange forgetRate = new FloatRange(0.5f, 0f, 1f /* 2f */);


    public final IntRange activeCapacity = new IntRange(256, 0, 2024) {
        @Override
        protected void changed() {
            TaskLinkBag a = AbstractConceptIndex.this.active;
            if (a !=null)
                a.setCapacity(intValue());
        }
    };


    @Override
    public Stream<TaskLink> active() {
        return active.stream();
    }

    protected AbstractConceptIndex() {

    }

    @Override
    public void start(NAR nar) {
        super.start(nar);


        active = new TaskLinkBag( arrayBag() /*hijackBag()*/, forgetRate, nar.exe.concurrent());

        active.setCapacity(activeCapacity.intValue());

        nar.onCycle(active::forget);
        //DurService.on(nar, n->updateConcepts());

    }

    @Override
    public void clear() {
        active.clear();
    }
//
//    @NotNull
//    public PriHijackBag<Term, Activate> hijackBag() {
//        return new PriHijackBag<>(Math.round(activeCapacity.intValue() * 1.5f /* estimate */), 4) {
//
//            @Override
//            public Term key(Activate value) {
//                return value.term();
//            }
//
//            @Override
//            protected PriMerge merge() {
//                return Param.tasklinkMerge;
//            }
//        };
//    }

    protected Bag<TaskLink, TaskLink> arrayBag() {
        return new ArrayBag<>(
                activeCapacity.intValue(),
                Param.tasklinkMerge,
                new HashMap<>(activeCapacity.intValue() * 2, 0.99f)
        ) {

            @Override
            public TaskLink key(TaskLink value) {
                return value;
            }

            @Override
            protected float merge(TaskLink existing, TaskLink incoming) {
                return existing.merge(incoming, Param.tasklinkMerge);
            }
        };
    }




    @Override
    public void sample(Random rng, Function<? super TaskLink, SampleReaction> each) {
        active.sample(rng, each);
    }

//    @Override
//    public void activate(Concept c, float pri) {
//        //((BufferedBag)active).put(c, pri * activationRate.floatValue());
//    }

    @Override
    public float pri(Termed concept, float ifMissing) {
        //return active.pri(concept.term(), ifMissing);
        return 0; //TODO
    }

}
