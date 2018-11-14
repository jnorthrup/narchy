package nars.attention;

import jcog.math.IntRange;
import jcog.pri.bag.Bag;
import jcog.pri.bag.Sampler;
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
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * default bag-based model
 */
public class ActiveConcepts implements Sampler<Concept> {
    public Bag<Term, Activate> active = Bag.EMPTY;

    /**
     * TODO make dynamicalyl adjustable thru MutableInteger etc
     */
    public final IntRange capacity = new IntRange(0, 0, 2024) {
        @Override
        protected void changed() {
            active.setCapacity(intValue());
        }
    };

    private NAR nar;


    public ActiveConcepts() {
        super();
    }
    public ActiveConcepts(int initialCapacity) {
        this();
        setCapacity(initialCapacity);
    }

    public void clear() {
        active.clear();
    }

    public int capacity() {
        return active.capacity();
    }

    public final void setCapacity(int newCapacity) {
        capacity.set(newCapacity);
    }

    /**
     * TODO abstract
     */
    public void activate(Activate a) {
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

    protected void starting(NAR nar) {

        this.nar = nar;

        Bag<Term, Activate> arrayBag = new ArrayBag<>(
                capacity.intValue(),
                Param.conceptMerge,
                new HashMap<>(capacity.intValue() * 2, 0.99f)
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


    }

    protected void run(NAR n, long dt) {
    }

    public Stream<Activate> active() {
        Iterable<Activate> a = active;
        return a == null ? Stream.empty() : active.stream();
    }

    protected void stopping(NAR nar) {
        //if (active != null) {
        Bag<Term, Activate> a = active;
        active = Bag.EMPTY;
        a.clear();

        //active = null;
        //}

    }

    @Override
    public void sample(Random rng, Function<? super Concept, SampleReaction> each) {
        active.sample(rng, (Activate a) -> each.apply(a.get()));
    }

    /** the current priority value of the concept */
    public float pri(Termed concept, float ifMissing) {
        return active.pri(concept.term(), ifMissing);
    }
}
