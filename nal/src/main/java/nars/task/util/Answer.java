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
import nars.truth.polation.TruthIntegration;
import nars.truth.polation.TruthPolation;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

import static nars.Op.*;
import static nars.time.Tense.ETERNAL;

/**
 * heuristic task ranking for matching of evidence-aware truth values may be computed in various ways.
 */
public class Answer implements Consumer<Task> {

    public final static int TASK_LIMIT_DEFAULT =
            Param.STAMP_CAPACITY-1;
            //Param.STAMP_CAPACITY / 2;


    public final NAR nar;
    private int triesRemain;
    public TimeRangeFilter time;
    public Term template = null;

    public final TopN<Task> tasks;
    public final Predicate<Task> filter;

    public Answer(int capacity, FloatRank<Task> rank, @Nullable Predicate<Task> filter, NAR nar) {
        this(capacity, Math.round(Param.ANSWER_COMPLETENESS * capacity), rank, filter, nar);
    }

    /** TODO filter needs to be more clear if it refers to the finished task (if dynamic) or a component in creating one */
    public Answer(int capacity, int maxTries, FloatRank<Task> rank, @Nullable Predicate<Task> filter, NAR nar) {
        this.nar = nar;
        this.tasks = TopN.pooled(capacity, rank.filter(filter));
        this.filter = filter;
        this.triesRemain = maxTries;
    }

    /**
     * compose filter from one or two filters
     */
    public static Predicate<Task> filter(@Nullable Predicate<Task> a, @Nullable Predicate<Task> b) {
        if (a == null) return b;
        if (b == null) return a;
        return (x) -> a.test(x) && b.test(x);
    }

    public FloatRank<Task> rank() {
        return tasks.rank;
    }


    @Deprecated
    public static Answer relevance(boolean beliefOrQuestion, long start, long end, @Nullable Term template, @Nullable Predicate<Task> filter, NAR nar) {
        return relevance(beliefOrQuestion,
                beliefOrQuestion ? TASK_LIMIT_DEFAULT : 1, start, end, template, filter, nar);
    }

    /**
     * for belief or goals (not questions / quests
     */
    @Deprecated
    public static Answer relevance(boolean beliefOrQuestion, int limit, long start, long end, @Nullable Term template, @Nullable Predicate<Task> filter, NAR nar) {

        FloatRank<Task> r = relevance(beliefOrQuestion, start, end, template, nar);

        return new Answer(limit, r, filter, nar)
                .time(new TimeRangeFilter(start, end, true))
                .template(template);
    }

    public static FloatRank<Task> relevance(boolean beliefOrQuestion, long start, long end, @Nullable Term template, NAR nar) {
        int dur =
                nar.dur();
                //1;

        FloatRank<Task> strength =
                beliefOrQuestion ?
                        FloatRank.the(beliefStrength(start, end, dur)) : questionStrength(start, end, dur);

        FloatRank<Task> r;
        if (template == null || !template.hasAny(Temporal)) {
            r = FloatRank.the(strength);
        } else {
            r = complexTaskStrength(strength, template);
        }
        return r;
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

            if (t.equals(x)) // || Stamp.overlapsAny(xStamp, ((Task) t).stamp()))
                return Float.NaN;

            return
                -(1 + Math.abs(t.start() - xStart) + Math.abs(t.end() - xEnd));
        };

        Term xt = x.term();
        if (xt.hasAny(Op.Temporal)) {

            return (t) -> {
                float v1 = f.floatValueOf(t); //will be negative
                if (v1 != v1) return Float.NaN;

                return v1 * (1f + Revision.dtDiff(xt, ((Task) t).term()));
            };
        } else {
            return f;
        }
    }

    public static FloatRank<Task> complexTaskStrength(FloatRank<Task> strength, @Nullable Term template) {
        return (x, min) -> {
            float base = (1 + strength.rank(x, min));
            if (base < min || base != base)
                return Float.NaN;

            return base * (1 / (1+Revision.dtDiff(template, x.term())));
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
                            if (pri == pri && pri > m)
                                return pri / (1 + ((float) (t.minTimeTo(start, end) / ((double) dur))));
                            return Float.NaN;
                        };

    }


    public static FloatFunction<Task> eternalTaskStrength() {
        return x -> /*w2cSafe*/(x.isEternal() ? x.evi() : x.eviEternalized() * x.range());
    }

    public static FloatFunction<Task> temporalTaskStrength(long start, long end, int dur) {
        return x -> /*w2cSafe*/(TruthIntegration.evi(x, start, end, dur));
    }

    boolean ditherTruth = false;


    public Answer ditherTruth(boolean ditherTruth) {
        this.ditherTruth = ditherTruth;
        return this;
    }

    /**
     * matches, and projects to the specified time-range if necessary
     * note: if forceProject, the result may be null if projection doesnt succeed.
     * only useful for precise value summarization for a specific time.
     * <p>
     * <p>
     * clears the cache and tasks before returning
     */
    public Task task(boolean topOrSample, boolean tryMerge, boolean forceProject) {
        try {
            int s = tasks.size();
            Task t;
            switch (s) {
                case 0:
                    t = null;
                    break;
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

            if (forceProject && t != null) {
                long ss = time.start;
                if (ss != ETERNAL) { //dont eternalize here
                    long ee = time.end;
                    if (t.isEternal() || !t.containedBy(ss, ee)) {
                        t = Task.project(t, ss, ee, nar, true, false);
                    }
                }
            }

            return t;
        } finally {
            end();
        }
    }

    /**
     * clears the cache and tasks before returning
     */
    @Nullable
    public Truth truth() {
        try {
            TruthPolation p = truthpolation();
            if (p!=null) {
                TruthPolation t = p.filtered();
                if (t!=null)
                    return t.truth(nar);
            }
            return null;
        } finally {
            end();
        }
    }

    private Task taskFirst(boolean topOrSample) {
        if (topOrSample) {
            return tasks.get(0);
        } else {
            return tasks.get(nar.random());
        }
    }

    private Task taskMerge(@Nullable Task root) {

        @Nullable DynTruth d = dynTruth();
        if (d.size() <= 1)
            return root;

        TruthPolation tp = truthpolation(d, nar.dur());
        @Nullable MetalLongSet stampSet = tp.filterCyclic(root, true);
        if (tp.size() == 1)
            return root;


        @Nullable Truth tt = tp.truth(nar);
        if (tt == null)
            return root;

        if (ditherTruth) {
            tt = tt.dithered(nar);
            if (tt == null)
                return root;
        }

        Task dyn = d.task(tp.term, tt, (rng) -> {
            assert (stampSet != null);
            if (stampSet.size() > Param.STAMP_CAPACITY) {
                return Stamp.sample(Param.STAMP_CAPACITY, stampSet, rng);
            } else {
                return stampSet.toSortedArray();
            }
        }, root.isBelief(), nar);

        if (dyn == null)
            return root;
        if (root.isDeleted())
            return dyn; //which could have occurred by now

        return Truth.stronger(root, dyn);
    }

    /**
     * TODO merge DynTruth and TruthPolation
     */
    @Nullable
    @Deprecated
    protected DynTruth dynTruth() {
        int s = tasks.size();
        if (s == 0)
            return null;
        DynTruth d = new DynTruth(s);
        //noinspection UseBulkOperation
        tasks.forEach(d::add);
        return d;
    }



    @Nullable public TruthPolation truthpolation() {
        return truthpolation(nar.dur());
    }

    @Nullable public TruthPolation truthpolation(int dur) {
        DynTruth d = dynTruth();
        if (d == null) return null;
        return truthpolation(d, dur);
    }

    /**
     * this does not filter cyclic; do that manually
     */
    private TruthPolation truthpolation(DynTruth d, int dur) {
        TruthPolation tp = Param.truth(time.start, time.end, dur);
        tp.ensureCapacity(d.size());
        d.forEach(r -> tp.add(r.task()));
        return tp;
    }

    public final Answer match(TaskTable t) {
        t.match(this);
        return this;
    }

//    final static ThreadLocal<DequePool<CachedFloatRank<Task>>> pool =
//            //HEAP
//            //() -> new CachedFloatRank<>(64);
//
//            ThreadLocal.withInitial(()->
//                    new DequePool<CachedFloatRank<Task>>() {
//                        @Override
//                        public CachedFloatRank<Task> create() {
//                            return new CachedFloatRank<>(64);
//                        }
//                    }
//            );
//
//    static protected CachedFloatRank<Task> start(FloatRank<Task> rank) {
//        //return new CachedFloatFunction<>(4, 256, rank);
//        CachedFloatRank<Task> x = pool.get().get().value(rank);
//        assert (x.isEmpty());
//        //System.out.println(Thread.currentThread() + " got " + System.identityHashCode(x));
//        return x;
//    }

    private void end() {
        TopN.unpool(tasks);
    }

    public Answer time(TimeRangeFilter time) {
        this.time = time;
        return this;
    }

    @Override
    public final void accept(Task task) {
        if (task != null) {
            if (!(((CachedFloatFunction)(tasks.rank)).containsKey(task))) {
                //if (time == null || time.accept(task.start(), task.end())) {
                tasks.accept(task);
            }
        }
        //}
    }

    public boolean isEmpty() {
        return tasks.isEmpty();
    }

    @Nullable public Task any() {
        if (tasks.isEmpty())
            return null;
        return tasks.top();
    }

    /** consume a limited 'tries' iteration. also applies the filter.
     *  a false return value should signal a stop to any iteration supplying results */
    public boolean tryAccept(Task t) {
        if (triesRemain-- < 0)
            return false;

        if (filter==null || filter.test(t)) {
            accept(t);
        }

        return true;
    }

//
//    @Nullable
//    private Truth truth(TruthPolation p) {
//        p.ensureCapacity(tasks.size());
//        p.add(tasks);
//        p.filterCyclic(false);
//        return p.truth();
//    }


}
