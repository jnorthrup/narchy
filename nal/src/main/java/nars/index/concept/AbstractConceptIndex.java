package nars.index.concept;

import jcog.math.FloatRange;
import jcog.math.IntRange;
import jcog.pri.ScalarValue;
import jcog.pri.bag.Bag;
import jcog.pri.bag.impl.ArrayBag;
import jcog.pri.bag.impl.hijack.PriHijackBag;
import jcog.pri.op.PriMerge;
import nars.NAR;
import nars.Param;
import nars.attention.BufferedBag;
import nars.attention.PriBuffer;
import nars.concept.Concept;
import nars.control.DurService;
import nars.link.Activate;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Stream;

/** implements a multi-level cache using a Concept Bag as a sample-able short-term memory */
abstract public class AbstractConceptIndex extends ConceptIndex {

    /** short term memory, TODO abstract and remove */
    public Bag<Term, Activate> active = Bag.EMPTY;

    public final FloatRange activationRate = new FloatRange(0.5f, ScalarValue.EPSILONsqrt, 2f);

    public final FloatRange conceptForgetRate = new FloatRange(0.9f, 0f, 1f /* 2f */);

    /**
     * TODO make dynamicalyl adjustable thru MutableInteger etc
     */
    public final IntRange activeCapacity = new IntRange(256, 0, 2024) {
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


        active =
                //nar.exe.concurrent() ?

                  //      arrayBag()
                        //hijackBag()

                        //new FastPutProxyBag<>(arrayBag,
                        //      1024)

                    //    :

            new BufferedBag.DefaultBufferedBag<>(arrayBag(), new PriBuffer<Concept>(Param.conceptMerge)) {

                @Override
                protected Term keyInternal(Concept c) {
                    return c.term();
                }

                @Override protected Activate valueInternal(Concept c, float pri) {
                    return new Activate(c, pri);
                }

            };




        active.setCapacity(activeCapacity.intValue());


        //nar.onCycle(this::updateConcepts);
        DurService.on(nar, n->updateConcepts());

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
                return Param.conceptMerge;
            }
        };
    }

    protected Bag<Term, Activate> arrayBag() {
        return new ArrayBag<>(
                activeCapacity.intValue(),
                Param.conceptMerge,
                new HashMap<>(activeCapacity.intValue() * 2, 0.99f)
        ) {

            @Override
            public Term key(Activate value) {
                return value.term();
            }

        };
    }



    private void updateConcepts() {
        active.commit(nar.attn.forgetting.forget(active, 1f, conceptForgetRate.floatValue()));
    }

    @Override
    public void sample(Random rng, Function<? super Activate, SampleReaction> each) {
        active.sample(rng, each);
    }

    @Override
    public void activate(Concept c, float pri) {
        ((BufferedBag)active).put(c, pri * activationRate.floatValue());
    }

    @Override
    public float pri(Termed concept, float ifMissing) {
        return active.pri(concept.term(), ifMissing);
    }

}
