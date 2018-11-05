package nars.exe;

import jcog.pri.bag.Bag;
import jcog.pri.bag.Sampler;
import jcog.pri.bag.impl.ArrayBag;
import nars.NAR;
import nars.Param;
import nars.concept.Concept;
import nars.control.DurService;
import nars.link.Activate;
import nars.term.Term;
import nars.term.Termed;

import java.util.HashMap;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * default bag-based model
 */
public class Attention extends DurService implements Sampler<Concept> {

    /**
     * TODO make dynamicalyl adjustable thru MutableInteger etc
     */
    private final int concepts;

    public Bag<Term, Activate> active;


    public Attention(int concepts) {
        super((NAR) null);

        this.concepts = concepts;
    }

    @Override
    public void clear() {
        active.clear();
    }

    public int capacity() {
        return active.capacity();
    }

    public void setCapacity(int newCapacity) {
        active.setCapacity(newCapacity);
    }

    /**
     * TODO abstract
     */
    protected void activate(Activate a) {
        active.putAsync(a);
    }

    /**
     * TODO abstract
     */
    @Deprecated
    public Activate fire() {
        return active.sample(nar.random());
    }


    /**
     * invoke predicate while it returns true
     * TODO abstract
     */
    @Deprecated
    public void fire(Predicate<Activate> each) {
        active.sample(nar.random(), each);
    }

    @Override
    protected void starting(NAR nar) {


        ArrayBag<Term, Activate> arrayBag = new ArrayBag<>(
                concepts,
                Param.conceptMerge,
                new HashMap<>(concepts * 2, 0.99f)
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


        on(
                nar.eventClear.on(this::clear),
                nar.eventActivate.on(this::activate)
        );

        super.starting(nar);

    }

    @Override
    protected void run(NAR n, long dt) {
        float temperature = 1f - (float) Math.exp(-(((double) dt) / n.dur()) / n.memoryDuration.floatValue());
        active.commit(active.forget(temperature));
    }

    public Stream<Activate> active() {
        Iterable<Activate> a = active;
        return a == null ? Stream.empty() : active.stream();
    }

    @Override
    protected void stopping(NAR nar) {
        //if (active != null) {
        active.clear();
        //active = null;
        //}

        super.stopping(nar);
    }

    @Override
    public void sample(Random rng, Function<? super Concept, SampleReaction> each) {
        active.sample(rng, (Activate a) -> each.apply(a.get()));
    }

    public float pri(Termed id, float ifMissing) {
        return active.pri(id.term(), ifMissing);
    }
}
