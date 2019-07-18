package nars.link;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import jcog.data.NumberX;
import jcog.data.list.FasterList;
import jcog.data.list.table.Table;
import jcog.decide.Roulette;
import jcog.math.FloatRange;
import jcog.math.IntRange;
import jcog.pri.Forgetting;
import jcog.pri.ScalarValue;
import jcog.pri.bag.Sampler;
import jcog.pri.bag.impl.PriArrayBag;
import jcog.pri.bag.impl.hijack.PriHijackBag;
import jcog.pri.op.PriForget;
import jcog.pri.op.PriMerge;
import jcog.pri.op.PriReturn;
import nars.NAL;
import nars.NAR;
import nars.Task;
import nars.attention.What;
import nars.concept.Concept;
import nars.derive.model.Derivation;
import nars.term.Term;
import nars.term.atom.Atom;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * essentially a wrapper for a TaskLink bag for use as a self-contained attention set
 */
abstract public class TaskLinks implements Sampler<TaskLink> {

    /**
     * tasklink forget rate
     */
    public final FloatRange decay = new FloatRange(0.75f, 0, 1f /* 2f */);
    /**
     * (post-)Amp: tasklink propagation rate
     */
    public final FloatRange amp = new FloatRange(0.5f, 0, 2f /* 2f */);
    /**
     * tasklink retention rate:
     * 0 = deducts all propagated priority from source tasklink (full resistance)
     * 1 = deducts no propagated priority (superconductive)
     **/
    public final FloatRange sustain = new FloatRange(1f, 0, 1f);
    private final PriMerge merge = NAL.tasklinkMerge;

    private Predicate<TaskLink> processor = (x) -> true;

    /**
     * short target memory, TODO abstract and remove, for other forms of attention that dont involve TaskLinks or anything like them
     */
    public nars.link.TaskLinkBag links = null;
    /**
     * capacity of the links bag
     */
    public final IntRange linksMax = new IntRange(256, 0, 8192) {
        @Override
        @Deprecated
        protected void changed() {
            nars.link.TaskLinkBag a = links;
            if (a != null)
                a.setCapacity(intValue());
        }
    };


    public TaskLinks(/*TODO bag as parameter */) {
        int c = linksMax.intValue();

        links = new nars.link.TaskLinkBag(
                c, merge

        );

        links.setCapacity(c);
    }


    /** sets the tasklink processor/filter, applied before insert */
    public void pre(Predicate<TaskLink> each) {
        this.processor = each;
    }

    @Nullable public Multimap<Term,TaskLink> get(Predicate<Term> f, boolean sourceOrTargetMatch) {
        return get((t)->{
            Term tgt = sourceOrTargetMatch ? t.from() : t.to();
            return f.test(tgt) ? tgt : null;
        });
    }

    @Nullable public Multimap<Term,TaskLink> get(Function<TaskLink,Term> f) {
        Multimap<Term,TaskLink> m = null;
        for (TaskLink x : links) {
            @Nullable Term y = f.apply(x);
            if (y!=null) {
                if (m == null)
                    m = Multimaps.newListMultimap(new UnifiedMap(1), ()-> new FasterList(1));
                m.put(y, x);
            }
        }
        return m;
    }
    /**
     * updates
     */
    public final void commit(What w) {
        PriForget.PriMult<TaskLink> f = (PriForget.PriMult) Forgetting.forget(links, decay.floatValue());
//        Consumer<TaskLink> g;
//        if (f!=null) {
//            NAR n = w.nar;
//            float F = f.mult;
//            //TODO normalize the components
//            float beliefForget = F * (1 - n.beliefPriDefault.amp()),
//                  goalForget = F * (1 - n.goalPriDefault.amp()),
//                  questionForget = F * (1 - n.questionPriDefault.amp()),
//                  questForget = F * (1 - n.questPriDefault.amp());
//            g = t -> {
//                t.priMult(beliefForget, goalForget, questionForget, questForget);
//            };
//        } else
//            g = null;


        links.commit(f /*g*/);
    }

    @Override
    public void sample(Random rng, Function<? super TaskLink, Sampler.SampleReaction> each) {
        links.sample(rng, each);
    }

    @Override
    public void sampleUnique(Random rng, Predicate<? super TaskLink> predicate) {
        links.sampleUnique(rng, predicate);
    }

    /**
     * resolves and possibly sub-links a link target
     * TODO abstract this as one possible strategy
     */
    @Nullable
    public Term term(TaskLink link, Task task, Derivation d) {

        Term target = link.target(task, d);
        if (target == null)
            return null;

        Term reverse = target.op().conceptualizable ? reverse(target, link, task, d) : null;
        if (reverse != null && reverse!=target)
            return reverse;

        Term forward = link.forward(target, link, task, d);
        if (forward != null)
            grow(link, task, forward);

        return target;
    }

    void grow(TaskLink link, Task task, Random r) {
        Term forward = DynamicTermLinker.Weighted.sample(task.term(),r);
        if (forward != null)
            grow(link, task, forward);
    }

    void grow(TaskLink link, Task task, Term forward) {
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
        byte punc = task.punc();

        float p =
                link.priPunc(punc);
        float pFwd = p * amp.floatValue();
        Term from = link.from();

        //CHAIN pattern
        link(from, forward, punc, pFwd); //forward (hop)
        //link(u, s, punc, pAmp); //reverse (hop)
        //link(t, u, punc, pAmp); //forward (adjacent)
        //link(u, t, punc, pAmp); //reverse (adjacent)

        float toUnsustain = pFwd * (1 - this.sustain.floatValue());
        if (toUnsustain> ScalarValue.EPSILON) {
            float unsustained = link.take(punc, toUnsustain);
            links.bag.depressurize(unsustained);
        }


        //link(s, t, punc, ); //redundant
        //link(t, s, punc, pp); //reverse echo

//                if (self)
//                    t = u;

//                } else {
//////                int n = 1;
//////                float pp = p * conductance / n;
//////
//////                link(t, s, punc, pp); //reverse echo
////
//                }
//            }
    }


    /**
     * @param target the final target of the tasklink (not necessarily link.to() in cases where it's dynamic)
     * resolves reverse termlink
     * return null to avoid reversal
     */
    @Nullable
    protected abstract Term reverse(Term target, TaskLink link, Task task, Derivation d);

    private void link(Term s, Term u, byte punc, float p) {
        link(AtomicTaskLink.link(s, u).priSet(punc, p));
    }

    public final TaskLink link(TaskLink x) {
        return processor.test(x) ? links.put(x) : null;
    }


    /**
     * initial tasklink activation for an input task
     *
     * @return
     */
    public TaskLink link(Task task) {
        TaskLink tl = link(task, task.pri());

//        //pre-seed
//        double ii = 1 + Math.sqrt(task.term().volume());
//        for (int i = 0; i < ii; i++)
//            grow(tl, task, ThreadLocalRandom.current() /* HACK */);

//        if (tl == null && task.isInput()) {
//            System.err.println("tasklinks rejected input task: " + task);
//        }


        return tl;
    }

    protected TaskLink link(Task task, float pri) {
        AbstractTaskLink link = AtomicTaskLink.link(task.term()).priSet(task.punc(), pri);
        return link(link);
    }

    public Stream<Term> terms() {
        return links.stream()
                .flatMap(x -> Stream.of(x.from(), x.to()).distinct())
                .distinct();
    }

    public Stream<Term> terms(Predicate<Term> filter) {
        return links.stream()
                .flatMap(x -> Stream.of(x.from(), x.to()).distinct().filter(filter))
                .distinct();
    }

    public final Stream<Concept> concepts(NAR n) {
        return terms(x -> x.op().conceptualizable)
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

    /**
     * samples the tasklink bag for a relevant reversal
     * memoryless, determined entirely by tasklink bag, O(n)
     */
    public static class DirectTangentTaskLinks extends TaskLinks {

        public static final DirectTangentTaskLinks the = new DirectTangentTaskLinks();

        private DirectTangentTaskLinks() {

        }

        @Override
        protected Term reverse(Term target, TaskLink link, Task task, Derivation d) {

            //< 1 .. 1.0 isnt good
            float probBase =
                    0.5f;
                    //0.33f;
            float probDirect =
                    //(float) (probBase / Util.sqr(Math.pow(2, target.volume()-1)));
                    (float) (probBase / Math.pow(target.volume(), 2));
                    //(float) (probBase / Math.pow(2, target.volume()-1));
                    //probBase * (target.volume() <= 2 ? 1 : 0);
                    //probBase * 1f / Util.sqr(Util.sqr(target.volume()));
                    //probBase * 1f / term.volume();
                    //probBase * 1f / Util.sqr(term.volume());
                    //probBase *  1f / (term.volume() * Math.max(1,(link.from().volume() - term.volume())));

            if (d.random.nextFloat() >= probDirect)
                return null; //term itself

            return sampleReverseMatch(target, link, d);

        }

        @Nullable public Term sampleReverseMatch(Term target, TaskLink link, Derivation d) {
            final Term[] T = {target};
            sampleUnique(d.random, (ll) ->{
                if (ll != link) {
                    Term t = ll.reverseMatch(target);
                    if (t != null) {
                        T[0] = t;
                        return false; //done
                    }
                }
                return true;
            });
            return T[0];
        }
    }

    /**
     * caches ranked reverse atom termlinks in concept meta table
     */
    public static class TangentConceptCachingTaskLinks extends TaskLinks {

        int ATOM_TANGENT_REFRESH_DURS = 1;

        protected boolean cache(Term target) {
            return target instanceof Atom;
            //return target.volume() <= 3;
        }
        @Override
        protected Term reverse(Term target, TaskLink link, Task task, Derivation d) {

            float probability =
                    //0.5f;
                    (float) (0.5f / Math.pow(target.volume(), 3));
            //1f/Math.max(2,link.from().volume());
            //1-1f/Math.max(2,link.from().volume());
            //1-1f/(Math.max(1,link.from().volume()-1));
            //1f / (1 + link.from().volume()/2f);
            //1f / (1 + link.from().volume());
            //1 - 1f / (1 + s.volume());
            //1 - 1f / (1 + t.volume());

            if (!(d.random.nextFloat() <= probability))
                return null;

            if (cache(target)) {
                Concept T = d.nar.conceptualize(target);
                if (T != null)
                    return sampleCached(T, link, task, d);
            }

            return DirectTangentTaskLinks.the.sampleReverseMatch(target, link, d);
        }

        @Nullable private Term sampleCached(Concept T, TaskLink link, Task task, Derivation d) {
            //sample active tasklinks for a tangent match to the atom
            //Predicate<TaskLink> filter = x -> !link.equals(x);
            Predicate<TaskLink> filter = ((Predicate<TaskLink>) link::equals).negate();

            Term z = atomTangent(T, task.punc(), filter, d.time(),
                    Math.round(d.dur() * ATOM_TANGENT_REFRESH_DURS), d.random);
            return z;
        }

        /**
         * acts as a virtual tasklink bag associated with an atom concept allowing it to otherwise act as a junction between tasklinking compounds which share it
         */
        @Nullable final Term atomTangent(Concept src, byte punc, Predicate<TaskLink> filter, long now, int minUpdateCycles, Random rng) {
            return tangent(links,
                    src, punc, filter,
                    false, true,
                    now, minUpdateCycles, rng);
        }

        @Nullable
        static Term tangent(TaskLinkBag bag, Concept src, byte punc, Predicate<TaskLink> filter, boolean in, boolean out, long now, int minUpdateCycles, Random rng) {

            //        System.out.println(src);

            String id = bag.id(in, out);


            //        Reference<TermLinks> matchRef = src.meta(id);
            //        TermLinks match = matchRef != null ? matchRef.get() : null;

            Object _match = src.meta(id);
            TaskLinkSnapshot match;
            if (_match!=null) {
                if (_match instanceof Reference)
                    match = (TaskLinkSnapshot) ((Reference) _match).get();
                else
                    match = (TaskLinkSnapshot) _match;
            } else {
                match = new TaskLinkSnapshot(now, minUpdateCycles);
                src.meta(id, new SoftReference<>(match));
                //src.meta(id, match);
            }

            return match.sample(src.term(), bag, punc, filter, in, out, now, minUpdateCycles, rng);
        }


        /**
         * caches an array of tasklinks tangent to an atom
         */
        @Deprecated static final class TaskLinkSnapshot {

//            final static TaskLink[] EmptyTaskLinksArray = new TaskLink[0];
//            private final FasterList<TaskLink> links = new FasterList(0, EmptyTaskLinksArray);
            final PriArrayBag<TaskLink> links = new PriArrayBag<TaskLink>(PriMerge.replace, 0);
            private final AtomicBoolean busy = new AtomicBoolean(false);
            private volatile long updated;

            private TaskLinkSnapshot(long now, int minUpdateCycles) {
                this.updated = now - minUpdateCycles;
            }

            /**
             * caches an AtomLinks instance in the Concept's meta table, attached by a SoftReference
             */

            protected int cap(int bagSize) {
                return Math.max(4, (int) Math.ceil(1f * Math.sqrt(bagSize)) /* estimate */);
                //return bagSize;
            }

            public boolean refresh(Term x, Iterable<TaskLink> items, int itemCount, boolean reverse, long now, int minUpdateCycles) {
                if (now - updated >= minUpdateCycles) {

                    if (busy.compareAndSet(false, true)) {
                        try {
                            commit(x, items, itemCount, reverse);
                            updated = now;
                        } finally {
                            busy.set(false);
                        }
                    }

                }

                return !links.isEmpty();
            }

            public void commit(Term x, Iterable<TaskLink> items, int itemCount, boolean reverse) {

                links.capacity(cap(itemCount));

                links.commit();

                int i = 0;
                for (TaskLink t : items) {
                    //                if (t == null)
                    //                    continue; //HACK

                    float xp = t.priElseZero();

                    Term y = t.other(x, reverse);
                    if (y != null)
                        links.put(t.clone(1));

                    i++;
                }
            }


            @Nullable
            public Term sample(Predicate<TaskLink> filter, byte punc, Random rng) {
                @Nullable Object[] ll = links.items();
                int lls = Math.min(links.size(), ll.length);
                if (lls == 0)
                    return null;
                else {
                    TaskLink l;
                    if (lls == 1) l = (TaskLink) ll[0];
                    else {
                        int li = Roulette.selectRouletteCached(lls, (int i) -> {

                            TaskLink x = (TaskLink) ll[i];
                            return x != null && filter.test(x) ?
                                Math.max(ScalarValue.EPSILON, x.priPunc(punc))
                                :
                                Float.NaN;

                        }, rng::nextFloat);
                        l = li >= 0 ? (TaskLink)ll[li] : null;
                    }
                    return l != null ? l.from() : null;
                }

            }

            public final Term sample(Term srcTerm, Table<?, TaskLink> bag, byte punc, Predicate<TaskLink> filter, boolean in, boolean out, long now, int minUpdateCycles, Random rng) {
                return sample(srcTerm, bag, bag.capacity(), punc, filter, in, out, now, minUpdateCycles, rng);
            }

            public final Term sample(Term srcTerm, Iterable<TaskLink> items, int itemCount, byte punc, Predicate<TaskLink> filter, boolean in, boolean out, long now, int minUpdateCycles, Random rng) {
                return refresh(srcTerm, items, itemCount, out, now, minUpdateCycles) ?
                        sample(filter, punc, rng) : null;
            }


        }
    }



}
