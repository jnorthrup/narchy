package nars.index.concept;

import jcog.math.FloatRange;
import jcog.math.IntRange;
import jcog.pri.bag.Bag;
import jcog.pri.bag.impl.ArrayBag;
import jcog.pri.bag.impl.hijack.PriHijackBag;
import jcog.pri.op.PriMerge;
import nars.NAR;
import nars.Param;
import nars.attention.BufferedBag;
import nars.attention.PriBuffer;
import nars.link.Activate;
import nars.link.TaskLink;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Stream;

/** implements a multi-level cache using a Concept Bag as a sample-able short-target memory */
abstract public class AbstractConceptIndex extends ConceptIndex {

    /** short target memory, TODO abstract and remove */
    //public Bag<Term, Activate> active = Bag.EMPTY;

    public Bag<TaskLink, TaskLink> active = Bag.EMPTY;

    @Deprecated public final FloatRange activationRate = new FloatRange(1f,1, 1);

    public final FloatRange forgetRate = new FloatRange(0.9f, 0f, 1f /* 2f */);


    public final IntRange activeCapacity = new IntRange(256, 0, 2024) {
        @Override
        protected void changed() {
            active.setCapacity(intValue());
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


        active =
            new BufferedBag.SimplestBufferedBag<>( arrayBag(), //hijackBag()
                new PriBuffer<>(Param.tasklinkMerge,
                nar.exe.concurrent())
            );

        active.setCapacity(activeCapacity.intValue());


        nar.onCycle(this::updateConcepts);
        //DurService.on(nar, n->updateConcepts());

    }

    @Override
    public void clear() {
        active.clear();
    }

    @NotNull
    public PriHijackBag<Term, Activate> hijackBag() {
        return new PriHijackBag<>(Math.round(activeCapacity.intValue() * 1.5f /* estimate */), 4) {

            @Override
            public Term key(Activate value) {
                return value.term();
            }

            @Override
            protected PriMerge merge() {
                return Param.tasklinkMerge;
            }
        };
    }

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

        };
    }



    private void updateConcepts() {
        active.commit(nar.attn.forgetting.forget(active, 1f, forgetRate.floatValue()));
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
