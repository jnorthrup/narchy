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

import static jcog.Util.PHI_min_1f;

/**
 * essentially a wrapper for a TaskLink bag for use as a self-contained attention set
 */
public class TaskLinks implements Sampler<TaskLink> {

    /**
     * tasklink forget rate
     *   0 = no decay
     *  <1 = active remembering
     * 1.0 = balanced with incoming pressure
     *  >1 = active forget
     */
    public final FloatRange decay = new FloatRange(1f, 0, 2);


    /** pre-amplifier */
    public final FloatRange preAmp = new FloatRange(1f, 0, 2f /* 2f */);

    /**
     * (post-)Amp: tasklink conductance, propagation rate
     */
    public final FloatRange grow = new FloatRange(1 - PHI_min_1f, 0, 1f /* 2f */);

    /**
     * tasklink retention rate:
     * 0 = deducts all propagated priority from source tasklink (full resistance)
     * 1 = deducts no propagated priority (superconductive)
     **/
    public final FloatRange sustain = new FloatRange(0.5f, 0, 1f);
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

    @Nullable
    @Override
    public final TaskLink sample(Random rng) {
        return links.sample(rng);
    }

    @Override
    public final void sample(Random rng, Function<? super TaskLink, Sampler.SampleReaction> each) {
        links.sample(rng, each);
    }

    @Override
    public final void sampleUnique(Random rng, Predicate<? super TaskLink> predicate) {
        links.sampleUnique(rng, predicate);
    }


    public void grow(TaskLink parent, Term from, Term to, byte punc) {
        float take = grow.floatValue();
        if (take > ScalarValue.EPSILON)
            transfer(from, to, parent, take, sustain.floatValue(), punc);
    }



    /** returns the total priority released
     *  sustain=1: copy
     *  sustain=0: move
     *
     * @return*/
    private void transfer(Term s, Term u, TaskLink parent, float fraction, float sustain, byte punc) {
        TaskLink l = AtomicTaskLink.link(s, u);
        l.transfer(parent, fraction, sustain, punc);
        link(l);
    }

    public final TaskLink link(TaskLink x) {
        return links.put(x);
    }

    public TaskLink link(Task task, float pri) {
        return link(AtomicTaskLink.link(task.term()).priSet(task.punc(), pri * preAmp.floatValue()));
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

    public final boolean isEmpty() {
        return links.isEmpty();
    }
}
