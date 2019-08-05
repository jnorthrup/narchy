package nars.link;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import jcog.data.list.FasterList;
import jcog.math.FloatRange;
import jcog.math.IntRange;
import jcog.pri.Forgetting;
import jcog.pri.ScalarValue;
import jcog.pri.bag.Sampler;
import jcog.pri.op.PriForget;
import jcog.pri.op.PriMerge;
import nars.NAL;
import nars.NAR;
import nars.Task;
import nars.attention.What;
import nars.concept.Concept;
import nars.term.Term;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * essentially a wrapper for a TaskLink bag for use as a self-contained attention set
 */
public class TaskLinks implements Sampler<TaskLink> {

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


    public TaskLinks(int linksCapacity) {
        this();
        links.capacity(linksCapacity);
    }

    public TaskLinks(/*TODO bag as parameter */) {
        int c = linksMax.intValue();

        links = new nars.link.TaskLinkBag(
                c, merge

        );

        links.setCapacity(c);
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
                    m = Multimaps.newListMultimap(new UnifiedMap<>(1), ()-> new FasterList<>(1));
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


    public void grow(TaskLink link, Task task, Term forward) {
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


    private void link(Term s, Term u, byte punc, float p) {
        link(AtomicTaskLink.link(s, u).priSet(punc, p));
    }

    public final TaskLink link(TaskLink x) {
        return links.put(x);
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
        return link(AtomicTaskLink.link(task.term()).priSet(task.punc(), pri));
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



}
