package nars.index.concept;

import jcog.math.FloatRange;
import jcog.math.IntRange;
import jcog.pri.bag.Bag;
import jcog.pri.bag.impl.ArrayBag;
import nars.NAR;
import nars.Param;
import nars.concept.Concept;
import nars.link.Activate;
import nars.term.Term;
import nars.term.Termed;

import java.util.HashMap;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Stream;

/** implements a multi-level cache using a Concept Bag as a sample-able short-term memory */
abstract public class AbstractConceptIndex extends ConceptIndex {

    /** short term memory, TODO abstract and remove */
    public Bag<Term, Activate> active = Bag.EMPTY;


    public final FloatRange conceptForgetRate = new FloatRange(1f, 0f, 1f);

    /**
     * TODO make dynamicalyl adjustable thru MutableInteger etc
     */
    public final IntRange activeCapacity = new IntRange(512, 0, 2024) {
        @Override
        protected void changed() {
            active.setCapacity(intValue());
        }
    };


    @Override
    public Stream<Activate> active() {
        return active.stream();
    }

    protected AbstractConceptIndex() {

    }

    @Override
    public void start(NAR nar) {
        super.start(nar);

        Bag<Term, Activate> arrayBag = new ArrayBag<>(
                activeCapacity.intValue(),
                Param.conceptMerge,
                new HashMap<>(activeCapacity.intValue() * 2, 0.99f)
        ) {

            @Override
            public Term key(Activate value) {
                return value.term();
            }

        };
        active =
                nar.exe.concurrent() ?
//
//                        new PriHijackBag<>(Math.round(concepts * 1.5f /* estimate */), 4) {
//
//                            @Override
//                            public Term key(Activate value) {
//                                return value.term();
//                            }
//
//
//                        }

                        //new FastPutProxyBag<>(arrayBag,
                        //      1024)

                        arrayBag

                        :
                        arrayBag;


        active.setCapacity(activeCapacity.intValue());


        nar.onCycle(this::updateConcepts);
        //DurService.on(nar, (n)->updateConcepts(active));
        nar.eventClear.on(this.active::clear);
    }


    private void updateConcepts() {
        active.commit(nar.attn.forgetting.forget(active, conceptForgetRate.floatValue()));
    }

    @Override
    public void sample(Random rng, Function<? super Activate, SampleReaction> each) {
        active.sample(rng, each);
    }

    @Override
    public void activate(Concept c, float pri) {
        active.putAsync(new Activate(c, pri));
    }

    @Override
    public float pri(Termed concept, float ifMissing) {
        return active.pri(concept.term(), ifMissing);
    }

}
