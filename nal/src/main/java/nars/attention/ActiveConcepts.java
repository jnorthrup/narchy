//package nars.attention;
//
//import jcog.math.IntRange;
//import jcog.pri.bag.Bag;
//import jcog.pri.bag.Sampler;
//import nars.NAR;
//import nars.concept.Concept;
//import nars.link.Activate;
//import nars.target.Term;
//import nars.target.Termed;
//
//import java.util.Random;
//import java.util.function.Function;
//import java.util.function.Predicate;
//import java.util.stream.Stream;
//
///**
// * default bag-based model
// */
//@Deprecated public class ActiveConcepts implements Sampler<Concept> {
//    public Bag<Term, Activate> active = Bag.EMPTY;
//
//
//
//    private NAR nar;
//
//
//    public ActiveConcepts() {
//        super();
//    }
//
//    public ActiveConcepts(int initialCapacity) {
//        this();
//        setCapacity(initialCapacity);
//    }
//
//    public void clear() {
//        active.clear();
//    }
//
//    public int capacity() {
//        return active.capacity();
//    }
//
//    public final void setCapacity(int newCapacity) {
//        capacity.set(newCapacity);
//    }
//
//    /**
//     * TODO abstract
//     */
//    public void activate(Activate a) {
//        active.putAsync(a);
//    }
//
//    /**
//     * TODO abstract
//     */
//    @Deprecated
//    public Activate fire() {
//        return active.sample(nar.random());
//    }
//
//
//    /**
//     * invoke predicate while it returns true
//     * TODO abstract
//     */
//    @Deprecated
//    public void fire(Predicate<Activate> each) {
//        active.sample(nar.random(), each);
//    }
//
//    public void start(NAR nar) {
//
//        this.nar = nar;
//
//    }
//
//
//
//    public Stream<Activate> active() {
//        Iterable<Activate> a = active;
//        return a == null ? Stream.empty() : active.stream();
//    }
//
////    protected void stopping(NAR nar) {
////        //if (active != null) {
////        Bag<Term, Activate> a = active;
////        active = Bag.EMPTY;
////        a.clear();
////
////        //active = null;
////        //}
////
////    }
//
//    @Override
//    public void sample(Random rng, Function<? super Concept, SampleReaction> each) {
//        active.sample(rng, (Activate a) -> each.apply(a.get()));
//    }
//
//    /** the current priority value of the concept */
//    public float pri(Termed concept, float ifMissing) {
//        return active.pri(concept.target(), ifMissing);
//    }
//}
