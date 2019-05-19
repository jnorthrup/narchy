package nars.link;

import jcog.data.NumberX;
import jcog.data.list.FasterList;
import jcog.data.list.table.Table;
import jcog.decide.Roulette;
import jcog.math.FloatRange;
import jcog.math.IntRange;
import jcog.pri.Forgetting;
import jcog.pri.PriMap;
import jcog.pri.ScalarValue;
import jcog.pri.bag.Sampler;
import jcog.pri.bag.impl.ArrayBag;
import jcog.pri.bag.impl.hijack.PriHijackBag;
import jcog.pri.op.PriMerge;
import jcog.pri.op.PriReturn;
import jcog.sort.RankedN;
import nars.NAL;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.derive.model.Derivation;
import nars.term.Term;
import nars.term.atom.Atom;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** essentially a wrapper for a TaskLink bag for use as a self-contained attention set */
abstract public class TaskLinks implements Sampler<TaskLink> {

    /**
     * short target memory, TODO abstract and remove, for other forms of attention that dont involve TaskLinks or anything like them
     */
    public nars.link.TaskLinkBag links = null;

    /** tasklink forget rate */
    public final FloatRange decay = new FloatRange(0.5f,  0, 1f /* 2f */);

    /** (post-)Amp: tasklink propagation rate */
    public final FloatRange amp = new FloatRange(0.5f,  0, 2f /* 2f */);

    /** tasklink retention rate:
     *  0 = deducts all propagated priority from source tasklink (full resistance)
     *  1 = deducts no propagated priority (superconductive)
     **/
    public final FloatRange sustain = new FloatRange(0.5f,  0, 1f );



    /** capacity of the links bag */
    public final IntRange linksMax = new IntRange(256, 0, 8192) {
        @Override
        @Deprecated protected void changed() {
            nars.link.TaskLinkBag a = links;
            if (a != null)
                a.setCapacity(intValue());
        }
    };



    private final PriMerge merge = NAL.tasklinkMerge;


    public TaskLinks(/*TODO bag as parameter */) {
        int c = linksMax.intValue();

        links = new nars.link.TaskLinkBag(
                new TaskLinkArrayBag(c, merge)
                //new TaskLinkHijackBag(c, 5)
        );

        links.setCapacity(c);
    }


    /** prevents multiple threads from commiting the bag at once */
    private final AtomicBoolean busy = new AtomicBoolean(false);

    /** updates */
    public final void commit() {
        if (busy.compareAndSet(false,true)) {
            try {
                links.commit(
                        Forgetting.forget(links, decay.floatValue())
                );
            } finally {
                busy.set(false);
            }
        }
    }

    @Override
    public void sample(Random rng, Function<? super TaskLink, Sampler.SampleReaction> each) {
        links.sample(rng, each);
    }

    /** resolves and possibly sub-links a link target
     * TODO abstract this as one possible strategy
     * */
    @Nullable public Term term(TaskLink link, Task task, Derivation d) {

        final Term t = link.to();

        if (t instanceof Atom) {
            return termAtom((Atom)t, link, task, d);
        }

        if (t.op().conceptualizable) {

            Concept T;
            byte punc = task.punc();

            float p =
                    link.priPunc(punc);
            //task.priElseZero();

            float sustain = this.sustain.floatValue();

            NAR nar = d.nar();

//            boolean self = s.equals(t);

            T = nar.conceptualize(t);
            if (T != null) {

                Term u = null;
                Term tt = t.term();
                TermLinker linker = d.deriver.linker(tt);
                if (linker != null)
                    //grow-ahead: s -> t -> u
                    u = linker.sample(tt, d.random);

                else {
                    //loopback


                }


                if (u != null && !t.equals(u)) {


//                //TODO abstact activation parameter object
//                float subRate =
//                        1f;
//                //1f/(t.volume());
//                //(float) (1f/(Math.sqrt(s.volume())));
//
//
//                float inflation = 1; //TODO test inflation<1
//                float want = p * subRate / 2;
//                float p =
//                        inflation < 1 ? Util.lerp(inflation, link.take(punc, want*inflation), want) : want;

                    float pAmp = p * amp.floatValue();
                    final Term s = link.from();

                    //CHAIN pattern
                    link(s, u, punc, pAmp); //forward (hop)
                    //link(u, s, punc, pAmp); //reverse (hop)
                    //link(t, u, punc, pAmp); //forward (adjacent)
                    //link(u, t, punc, pAmp); //reverse (adjacent)


                    if (sustain < 1) {
                        link.take(punc, pAmp * (1 - sustain));
                    }


                    //link(s, t, punc, ); //redundant
                    //link(t, s, punc, pp); //reverse echo

//                if (self)
//                    t = u;

                } else {
//                int n = 1;
//                float pp = p * conductance / n;
//
//                link(t, s, punc, pp); //reverse echo

                }
            }
        }
        //link.take(punc, pp*n);

        //System.out.println(s + "\t" + t + "\t" + u);


        return t;
    }

    /** caches ranked reverse atom termlinks in concept meta table */
    public static class AtomTangentTaskLinks extends TaskLinks {

        @Override
        protected Term termAtom(Atom t, TaskLink link, Task task, Derivation d) {
                float probability =
                        0.5f;
                //1f/Math.max(2,link.from().volume());
                //1-1f/Math.max(2,link.from().volume());
                //1-1f/(Math.max(1,link.from().volume()-1));
                //1f / (1 + link.from().volume()/2f);
                //1f / (1 + link.from().volume());
                //1 - 1f / (1 + s.volume());
                //1 - 1f / (1 + t.volume());

                if (d.random.nextFloat() <= probability) {
                    Concept T = d.nar.conceptualize(t);
                    if (T!=null) {
                        //sample active tasklinks for a tangent match to the atom
                        //Predicate<TaskLink> filter = x -> !link.equals(x);
                        Predicate<TaskLink> filter = ((Predicate<TaskLink>) link::equals).negate();

                        Term z = atomTangent(T, task.punc(), filter, d.time(),
                                d.dur() * NAL.ATOM_TANGENT_REFRESH_DURS, d.random);
                        return z;
                    }

//                        if (u!=null && u.equals(s)) {
////                            u = links.atomTangent(ct, ((TaskLink x)->!link.equals(x)), d.time, 1, d.random);//TEMPORARY
//                            throw new WTF();
//                        }

//                        } else {
//
//
//                            //link(t, s, punc, p*subRate); //reverse echo
//                        }
                }

                return t;

        }

        /**
         * acts as a virtual tasklink bag associated with an atom concept allowing it to otherwise act as a junction between tasklinking compounds which share it
         */
        final Term atomTangent(Concept src, byte punc, Predicate<TaskLink> filter, long now, int minUpdateCycles, Random rng) {
            return TermLinks.tangent(links,
                    src, punc, filter,
                    false, true,
                    now, minUpdateCycles, rng);
        }

        /** caches an array of tasklinks tangent to an atom */
        static final class TermLinks {

            private volatile long updated;

            final static TaskLink[] EmptyTaskLinksArray = new TaskLink[0];
            private final FasterList<TaskLink> links = new FasterList(0, EmptyTaskLinksArray);

            private final AtomicBoolean busy = new AtomicBoolean(false);

            protected int cap(int bagSize) {
                return Math.max(2, (int) Math.ceil(1.5f * Math.sqrt(bagSize)) /* estimate */);
                //return bagSize;
            }

            private TermLinks(long now, int minUpdateCycles) {
                this.updated = now - minUpdateCycles;
            }

            /** caches an AtomLinks instance in the Concept's meta table, attached by a SoftReference */
            @Nullable
            static Term tangent(TaskLinkBag bag, Concept src, byte punc, Predicate<TaskLink> filter, boolean in, boolean out, long now, int minUpdateCycles, Random rng) {

        //        System.out.println(src);

                String id = bag.id(in, out);


        //        Reference<TermLinks> matchRef = src.meta(id);
        //        TermLinks match = matchRef != null ? matchRef.get() : null;

                TermLinks match = src.meta(id);

                if (match == null) {
                    //src.meta(id, new SoftReference<>(match));
                    match = new TermLinks(now, minUpdateCycles);
                    src.meta(id, match);
                }

                return match.sample( src.term(), bag, punc, filter, in, out, now, minUpdateCycles, rng);
            }

            public boolean refresh(Term x, Iterable<TaskLink> items, int itemCount, boolean reverse, long now, int minUpdateCycles) {
                if (now - updated >= minUpdateCycles) {

                    if (!busy.compareAndSet(false, true))
                        return false;
                    try {
                        /*synchronized (links)*/ {
                            int cap = cap(itemCount);

                            RankedN<TaskLink> match = null;

                            int i = 0;
                            for (TaskLink t : items) {
        //                if (t == null)
        //                    continue; //HACK

                                float xp = t.priElseZero();
                                if (match == null || /*match instanceof Set ||*/ (match instanceof RankedN && xp > match.minValueIfFull())) {
                                    Term y = t.other(x, reverse);
                                    if (y != null) {

                                        if (match == null) {
        //                            @Nullable RankedN<TaskLink> existingLinks = this.links;
        //                            if (existingLinks==null)
                                            match = new RankedN<>(new TaskLink[Math.min(itemCount - i, cap)], (FloatFunction<TaskLink>) TaskLink::pri);
        //                            else {
        //                                //recycle
        //                                //  this will affect other threads that might be reading from it.
        //                                //  so use 'clearWeak' atleast it wont nullify values while they might be reading from the array
        //                                existingLinks.clearWeak();
        //                                match = existingLinks;
        //                            }
                                        }

                                        match.add(t);
                                    }
                                }
                                i++;
                            }


                            links.clear();
                            if (match != null) {
                                int ms = match.size();
                                if (ms > 0) {
                                    links.ensureCapacity(ms);
                                    match.forEach(links::addFast);
                                }
                            }

                        }
                    } finally {
                        busy.set(false);
                    }

                    updated = now;
                }

                return !links.isEmpty();
            }


            @Nullable public Term sample(Predicate<TaskLink> filter, byte punc, Random rng) {
                TaskLink l;
                @Nullable FasterList<TaskLink> ll = links;
                int lls = ll.size();
                if (lls == 0)
                    return null;
                else {
                    TaskLink[] lll = ll.array();
                    lls = Math.min(lll.length, lls);
                    if (lls == 0) return null;
                    else if (lls == 1) l = lll[0];
                    else {
                        int li = Roulette.selectRouletteCached(lls, (int i) -> {
                            TaskLink x = lll[i];
                            return (x != null && filter.test(x)) ?
                                    Math.max(ScalarValue.EPSILON, x.priPunc(punc)) : Float.NaN;
                        }, rng::nextFloat);
                        l = li >= 0 ? lll[li] : null;
                    }
                    return l != null ? l.from() : null;
                }

            }

            public final Term sample(Term  srcTerm, Table<?, TaskLink> bag, byte punc, Predicate<TaskLink> filter, boolean in, boolean out, long now, int minUpdateCycles, Random rng) {
                return sample(srcTerm, bag, bag.capacity(), punc, filter, in, out, now, minUpdateCycles, rng);
            }

            public final Term sample(Term srcTerm, Iterable<TaskLink> items, int itemCount, byte punc, Predicate<TaskLink> filter, boolean in, boolean out, long now, int minUpdateCycles, Random rng) {
                return refresh(srcTerm, items, itemCount, out, now, minUpdateCycles) ?
                        sample(filter, punc, rng) : null;
            }


        }
    }

    /** resolves termlink for tasklink with Atom target */
    protected abstract Term termAtom(Atom t, TaskLink link, Task task, Derivation d);

    private void link(Term s, Term u, byte punc, float p) {
        link(new AtomicTaskLink(s, u, punc, p));
    }

    public void link(TaskLink x) {
        links.putAsync(x);
    }

    public void link(TaskLink... xx) {
        for (TaskLink x : xx)
            link(x);
    }

    /** initial tasklink activation for an input task
     * @return*/
    public void link(Task task, Concept tc, NAR n) {

        link(new AtomicTaskLink(task.term(), task.punc(), task.pri()));

    }

    @Deprecated public Stream<Term> terms() {
        return links.stream()
                .flatMap(x -> Stream.of(x.from(), x.to()))
                .distinct();
    }

    public final Stream<Concept> concepts(NAR n) {
        return terms()
            .map(n::concept)
            .filter(Objects::nonNull)
        ;
    }

    public final void clear() {
        links.clear();
    }

    public final Iterator<TaskLink> iterator() {
        return links.iterator();
    }


    private static class TaskLinkArrayBag extends ArrayBag<TaskLink, TaskLink> {

        public TaskLinkArrayBag(int initialCapacity, PriMerge merge) {
            super(merge, initialCapacity, PriMap.newMap(false));
        }

        @Override
        protected float merge(TaskLink existing, TaskLink incoming, float incomingPri) {
            return existing.merge(incoming, merge(), PriReturn.Overflow);
        }
        //        @Override
//        protected float sortedness() {
//            return 0.33f;
//        }

        @Override
        public TaskLink key(TaskLink value) {
            return value;
        }

    }

    private static class TaskLinkHijackBag extends PriHijackBag<TaskLink, TaskLink> {

        public TaskLinkHijackBag(int initialCap, int reprobes) {
            super(initialCap, reprobes);
        }

        @Override
        public TaskLink key(TaskLink value) {
            return value;
        }

        @Override
        protected TaskLink merge(TaskLink existing, TaskLink incoming, NumberX overflowing) {
            float o = existing.merge(incoming, merge(), PriReturn.Overflow);
            if (overflowing!=null)
                overflowing.add(o);
            return existing;
        }
    }


}
