package nars.exe;

import jcog.TODO;
import jcog.pri.bag.Bag;
import jcog.pri.bag.Sampler;
import jcog.pri.bag.impl.CurveBag;
import jcog.pri.bag.impl.hijack.PriorityHijackBag;
import nars.NAR;
import nars.Param;
import nars.concept.Concept;
import nars.link.Activate;
import nars.control.DurService;

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

    public Bag<?, Activate> active;

    public Attention(int concepts) {
        super((NAR)null);

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
   public void activate(Activate a) {
        active.putAsync(a);
    }

    /**
     * TODO abstract
     */
    @Deprecated public Activate fire() {
        return active.sample(nar.random());
    }


    /**
     * invoke predicate while it returns true
     * TODO abstract
     */
    @Deprecated public void fire(Predicate<Activate> each) {
        active.sample(nar.random(), each);
    }

    @Override
    protected void starting(NAR nar) {


        ons.add(nar.eventClear.on(this::clear));
        ons.add(nar.eventActivate.on(this::activate));

        active =
                nar.exe.concurrent() ?

                        new PriorityHijackBag<>(Math.round(concepts * 1.5f /* estimate */), 4) {
                            @Override
                            public Activate key(Activate value) {
                                return value;
                            }


                        }

                        :

                        new CurveBag<>(
                                Param.conceptMerge,
                                new HashMap<>(concepts * 2),
                                concepts) {
                        }
        ;

        super.starting(nar);

    }

    @Override
    protected void run(NAR n, long dt) {
        active.commit(active.forget(n.forgetRate.floatValue()));
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
        throw new TODO();
    }
}
