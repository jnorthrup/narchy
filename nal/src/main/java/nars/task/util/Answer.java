package nars.task.util;

import jcog.data.set.MetalLongSet;
import jcog.math.CachedFloatFunction;
import jcog.sort.FloatRank;
import jcog.sort.TopN;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.table.TaskTable;
import nars.task.Revision;
import nars.term.Term;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.dynamic.DynTruth;
import nars.truth.polation.FocusingLinearTruthPolation;
import nars.truth.polation.TruthIntegration;
import nars.truth.polation.TruthPolation;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

import static nars.Op.*;
import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.TIMELESS;
import static nars.truth.TruthFunctions.w2cSafe;

/** heuristic task ranking for matching of evidence-aware truth values may be computed in various ways.
 */
public class Answer implements Consumer<Task> {

    public final static int TASK_LIMIT =
            //Param.STAMP_CAPACITY-1;
            Param.STAMP_CAPACITY/2;


    public final NAR nar;
    public TimeRangeFilter time;
    public Term template = null;

    protected final CachedFloatFunction<Task> cache;
    final TopN<Task> tasks;



    final FloatRank<Task> rank;

    public Answer(int limit, FloatRank<Task> rank, NAR nar) {
        this.nar = nar;
        this.rank = rank;
        this.cache = cache(rank);
        this.tasks = new TopN<>(new Task[limit], cache);
    }


    public void clear() {
        cache.clear();
        tasks.clear();
    }

    @Deprecated public static Answer relevance(boolean beliefOrQuestion, long start, long end, @Nullable Term template, @Nullable Predicate<Task> filter, NAR nar) {
        return relevance(beliefOrQuestion, beliefOrQuestion ? TASK_LIMIT : 1, start, end, template, filter, nar);
    }

    /** for belief or goals (not questions / quests */
    @Deprecated public static Answer relevance(boolean beliefOrQuestion, int limit, long start, long _end, @Nullable Term template, @Nullable Predicate<Task> filter, NAR nar) {

        long end;
        if (start == ETERNAL && _end == ETERNAL)
            end = TIMELESS; //to cover the whole range of values when ETERNAL,ETERNAL is provided
        else
            end = _end;

        int dur = nar.dur();

        FloatRank<Task> strength =
                beliefOrQuestion ?
                    FloatRank.the(beliefStrength(start, end, dur)) : questionStrength(start, end, dur);

        FloatRank<Task> r;
        if (template == null || !template.hasAny(Temporal)) {
            r = FloatRank.the(strength);
        } else {
            r = complexTaskStrength(strength, template);
        }

        return new Answer(limit, r.filter(filter), nar)
                .time(new TimeRangeFilter(start, end, true))
                .template(template);
    }

    public Answer template(Term template) {
        this.template = template;
        return this;
    }


    public static FloatFunction<TaskRegion> mergeability(Task x) {
        MetalLongSet xStamp = Stamp.toSet(x);
        xStamp.trim();

        long xStart = x.start();
        long xEnd = x.end();

        FloatFunction<TaskRegion> f = (TaskRegion t) -> {

            if (Stamp.overlapsAny(xStamp, ((Task) t).stamp()))
                return Float.NaN;

            return
                    (1 + 1f / (1f +
                            (Math.abs(t.start() - xStart) + Math.abs(t.end() - xEnd))));
        };

        Term xt = x.term();
        if (xt.hasAny(Op.Temporal)) {

            return (t) -> {
                float v1 = f.floatValueOf(t);
                if (v1!=v1) return Float.NaN;

                return 1f / (1f + Revision.dtDiff(xt, ((Task)t).term()));
            };
        } else {
            return f;
        }
    }

    public static FloatRank<Task> complexTaskStrength(FloatRank<Task> strength, @Nullable Term template) {
        return (x,min) -> {
            float base = (1 + strength.rank(x, min));
            if (base < min || base!=base)
                return Float.NaN;

            return base * (1 + 1/ Revision.dtDiff(template, x.term()));
        };
    }

    public static FloatFunction<Task> beliefStrength(long start, long end, int dur) {
        if (start == ETERNAL) {
            return eternalTaskStrength();
        } else {
            return temporalTaskStrength(start, end, dur);
        }
    }
    public static FloatRank<Task> questionStrength(long start, long end, int dur) {

        return
                (start == ETERNAL) ?
                        (t, m) -> t.pri()
                        :
                        (t, m) -> {
                            float pri = t.pri(); // * t.originality();
                            if (pri==pri && pri > m)
                                return pri * (1 / ((float) (t.minTimeTo(start, end) / ((double) dur))));
                            return Float.NaN;
                        };

    }

    @NotNull
    public static FloatFunction<Task> eternalTaskStrength() {
        return x -> w2cSafe(x.isEternal() ? x.evi() : x.eviEternalized() * x.range());
    }

    public static FloatFunction<Task> temporalTaskStrength(long start, long end, int dur) {
        return x -> w2cSafe(TruthIntegration.eviInteg(x, start, end, dur));
    }

    boolean ditherTruth = false;


    public Answer ditherTruth(boolean ditherTruth) {
        this.ditherTruth = ditherTruth;
        return this;
    }

    /**
     * matches, and projects to the specified time-range if necessary
     * note: if forceProject, the result may be null if projection doesnt succeed.
     *   only useful for precise value summarization for a specific time.
     */
    public Task task(boolean topOrSample, boolean tryMerge, boolean forceProject) {

        int s = tasks.size();
        Task t;
        switch (s) {
            case 0:
                return null;
            case 1:
                t = tasks.get(0);
                break;
            default: {
                @Nullable Task root = taskFirst(topOrSample);
                switch (root.punc()) {
                    case BELIEF:
                    case GOAL:
                        if (tryMerge)
                            t = taskMerge(root);
                        else
                            t = root;
                        break;
                    case QUESTION:
                    case QUEST:
                        t = root;
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
                break;
            }
        }

        if (forceProject && t!=null) {
            long ss = time.start;
            if (ss != ETERNAL) { //dont eternalize here
                long ee = time.end;
                if (t.isEternal() || !t.containedBy(ss, ee)) {
                    return Task.project(t, ss, ee, nar);
                }
            }
        }

        return t;
    }

    private Task taskFirst(boolean topOrSample) {
        if (topOrSample) {
            return tasks.get(0);
        } else{
            return tasks.get(nar.random());
        }
    }

    private Task taskMerge(@Nullable Task root) {

        @Nullable DynTruth d = dynTruth();
        if (d.size() <= 1)
            return root;

        TruthPolation tp = truthpolation(d).filtered(root);
        if (tp.size()==1)
            return root;

        @Nullable Truth tt = tp.truth(nar);
        if (tt==null)
            return root;

        boolean beliefOrGoal = root.isBelief();
        Task dyn = d.task(tp.term, tt, beliefOrGoal, ditherTruth, nar);
        if (dyn == null)
            return root;
        if (root.isDeleted())
            return dyn; //which could have occurred by now

        return Truth.stronger(root, dyn);
    }

    /** TODO merge DynTruth and TruthPolation */
    @Nullable @Deprecated protected DynTruth dynTruth() {
        int s = tasks.size();
        if (s == 0)
            return null;
        DynTruth d = new DynTruth(s);
        tasks.forEach(d::add);
        return d;
    }


    @Nullable public Truth truth() {
        TruthPolation p = truthpolation();
        @Nullable TruthPolation t = p!=null ? p.filtered() : null;
        return t != null ? t.truth(nar) : null;
    }

    @Nullable public TruthPolation truthpolation() {
        DynTruth d = dynTruth();
        if (d == null) return null;
        return truthpolation(d);
    }

    /** this does not filter cyclic; do that manually */
    private TruthPolation truthpolation(DynTruth d) {
        TruthPolation tp = Param.truth(time.start, time.end, nar.dur());
        tp.ensureCapacity(d.size());
        d.forEach(r -> tp.add(r.task()));
        return tp;
    }

    public Answer match(TaskTable t) {
        t.match(this);
        return this;
    }

    final static ThreadLocal<CachedFloatFunction<Task>> caches = ThreadLocal.withInitial(()->
            new CachedFloatFunction<>(4)
            );

    static protected CachedFloatFunction<Task> cache(FloatRank<Task> rank) {
        //return new CachedFloatFunction<>(4, 256, rank);
        CachedFloatFunction<Task> x = caches.get().value(rank);
        assert(x.isEmpty());
        //System.out.println(Thread.currentThread() + " got " + System.identityHashCode(x));
        return x;
    }

    public Answer time(TimeRangeFilter time) {
        this.time = time;
        return this;
    }

    @Override
    public final void accept(Task task) {
        if (task != null) {
            if (!cache.containsKey(task)) {
                //if (time == null || time.accept(task.start(), task.end())) {
                tasks.accept(task);
            }
        }
        //}
    }

    public boolean isEmpty() { return tasks.isEmpty(); }

    @Nullable public Truth truth(long s, long e, int dur) {
        return isEmpty() ? null : truth(new FocusingLinearTruthPolation(s, e, dur));
    }

    @Nullable public Truth truth(TruthPolation p) {
        p.ensureCapacity(tasks.size());
        p.add(tasks);
        p.filterCyclic(false);
        return p.truth();
    }
}
