package nars.task.util;

import jcog.WTF;
import jcog.data.pool.MetalPool;
import jcog.data.set.MetalLongSet;
import jcog.sort.FloatRank;
import jcog.sort.RankedTopN;
import jcog.sort.TopN;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.table.TaskTable;
import nars.term.Term;
import nars.term.util.Intermpolate;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.dynamic.DynEvi;
import nars.truth.polation.TruthIntegration;
import nars.truth.polation.TruthPolation;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.Predicate;

import static nars.Op.*;
import static nars.time.Tense.ETERNAL;
import static nars.truth.func.TruthFunctions.c2wSafe;

/**
 * heuristic task ranking for matching of evidence-aware truth values may be computed in various ways.
 */
public final class Answer implements AutoCloseable {

    public final static int BELIEF_MATCH_CAPACITY =
            //Param.STAMP_CAPACITY - 1;
            Math.max(1, Param.STAMP_CAPACITY / 2);

    public static final int BELIEF_SAMPLE_CAPACITY = Math.max(1, BELIEF_MATCH_CAPACITY / 2);
    public static final int QUESTION_SAMPLE_CAPACITY = 1;

    private final FloatRank<Task> rank;


    boolean ditherTruth = false;

    public final NAR nar;
    public int triesRemain;
    public TimeRangeFilter time;
    public Term template = null;

    public final static ThreadLocal<MetalPool<RankedTopN>> topTasks = TopN.newRankedPool();

    public RankedTopN<Task> tasks;
    public final Predicate<Task> filter;

    private Answer(int capacity, FloatRank<Task> rank, @Nullable Predicate<Task> filter, NAR nar) {
        this(capacity, Math.round(Param.ANSWER_COMPLETENESS * capacity), rank, filter, nar);
    }

    /** TODO filter needs to be more clear if it refers to the finished task (if dynamic) or a component in creating one */
    private Answer(int capacity, int maxTries, FloatRank<Task> rank, @Nullable Predicate<Task> filter, NAR nar) {
        this.nar = nar;
        this.tasks = TopN.pooled(topTasks, capacity, this.rank = rank.filter(filter));
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
        return rank;
    }


    /**
     * for belief or goals (not questions / quests
     */
    @Deprecated
    public static Answer relevance(boolean beliefOrQuestion, int capacity, long start, long end, @Nullable Term template, @Nullable Predicate<Task> filter, NAR nar) {

        if (!beliefOrQuestion && capacity > 1)
            throw new WTF("questions are not merged so the capacity need not exceed 1");

        FloatRank<Task> r = relevance(beliefOrQuestion, start, end, template);

        return new Answer(capacity, r, filter, nar)
                .time(new TimeRangeFilter(start, end, true))
                .template(template);
    }

    public static FloatRank<Task> relevance(boolean beliefOrQuestion, long start, long end, @Nullable Term template) {





        FloatRank<Task> strength =
                beliefOrQuestion ?
                        FloatRank.the(beliefStrength(start, end)) : questionStrength(start, end);

        FloatRank<Task> r;
        if (template == null || !template.hasAny(Temporal) || template.equals(template.concept()) /* <- means it will match anything */ ) {
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


    /** TODO FloatRank not FloatFunction */
    public static FloatFunction<TaskRegion> mergeability(Task x) {
        MetalLongSet xStamp = Stamp.toSet(x);
        xStamp.trim();

        long xStart = x.start();
        long xEnd = x.end();

        FloatFunction<TaskRegion> f = (TaskRegion t) -> {

            if (t==x || (!Param.ALLOW_REVISION_OVERLAP_IF_DISJOINT_TIME /* TODO: && !disjointTime(x,y) */
                    && Stamp.overlapsAny(xStamp, ((Task) t).stamp())))
                return Float.NaN;

            return
                -(1 + Math.abs(t.start() - xStart) + Math.abs(t.end() - xEnd));
        };

        Term xt = x.term();
        if (xt.hasAny(Op.Temporal)) {

            return (t) -> {
                float v1 = f.floatValueOf(t); //will be negative
                if (v1 != v1) return Float.NaN;

                Term tt = ((Task) t).term();
                return v1 * (1f + Intermpolate.dtDiff(xt, tt));
            };
        } else {
            return f;
        }
    }

    public static FloatRank<Task> complexTaskStrength(FloatRank<Task> strength, Term template) {
        return (x, min) -> {

            float dtDiff = Intermpolate.dtDiff(template, x.term());
            if (!Float.isFinite(dtDiff))
                return Float.NaN;
            float d = 1 / (1 + dtDiff);
            if (d < min)
                return Float.NaN;

            float r = strength.rank(x, min);
            if (r!=r || r < min)
                return Float.NaN;


            float s = r * d;
            return s;
        };
    }

    public static FloatFunction<Task> beliefStrength(long start, long end) {
        if (start == ETERNAL) {
            return eternalTaskStrength();
        } else {
            return temporalTaskStrength(start, end);
        }
    }

    public static FloatRank<Task> questionStrength(long start, long end) {

        return
                (start == ETERNAL) ?
                        (t, m) -> t.pri()
                        :
                        (t, m) -> {
                            float pri = t.pri(); // * t.originality();
                            if (pri == pri && pri > m)
                                return pri / (1f + t.minTimeTo(start, end) );
                            return Float.NaN;
                        };

    }


    public static FloatFunction<Task> eternalTaskStrength() {
        return x -> (x.isEternal() ? x.evi() : x.eviEternalized() * x.range());
    }

    public static FloatFunction<Task> temporalTaskStrength(long start, long end) {
        long dur = (1 + (end-start))/2;
        return x -> (TruthIntegration.evi(x, start, end, dur /*1*/ /*0*/));
    }




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
                    t = tasks.get(0).x;
                    break;
                default: {
                    @Nullable Task root = taskFirst(topOrSample);
                    switch (root.punc()) {
                        case BELIEF:
                        case GOAL: {
                            if (tryMerge)
                                t = taskMerge(root);
                            else
                                t = root;

                            if (ditherTruth) {
                                if (t.evi() < eviMin())
                                    return null;
                            }
                        }
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
                        t = Task.project(t, ss, ee, nar, ditherTruth, false);
                    }
                }
            }

            return t;
        } finally {
            close();
        }
    }

    public float eviMin() {
        return ditherTruth ? c2wSafe(nar.confMin.floatValue()) : Float.MIN_NORMAL;
    }

    /**
     * clears the cache and tasks before returning
     */
    @Nullable
    public Truth truth() {
        try {

            //quick case: 1 item, and it's eternal => its truth
            Task zeroth;
            if (tasks.size()==1 && (zeroth = tasks.get(0).x).isEternal()) {
                Truth trEte = zeroth.truth();
                return (trEte.evi() < eviMin()) ? null : trEte;
            }

            TruthPolation p = truthpolation();
            if (p == null)
                return null;

            TruthPolation tp = p.filtered();
            if (tp!=null)
                return truth(tp);
            return null;
        } finally {
            close();
        }
    }

    @Nullable private Truth truth(TruthPolation tp) {
        return tp.truth(nar, eviMin());
    }

    private Task taskFirst(boolean topOrSample) {
        return (topOrSample ? tasks.get(0) :  tasks.getRoulette(nar.random())).x;
    }

    private Task taskMerge(@Nullable Task root) {

        @Nullable DynEvi d = dynTruth();
        if (d.size() <= 1)
            return root;

        TruthPolation tp = truthpolation(d, nar.dur());
        tp.filterCyclic(root, false);
        if (tp.size() == 1)
            return root;


        @Nullable Truth tt = truth(tp);
        if (tt == null)
            return root;

        if (ditherTruth) {
            tt = tt.dithered(nar);
            if (tt == null)
                return root;
        }


        long s = tp.start;
        long e = tp.end;
        if (d.allSatisfy(Task::isEternal)) {
            s = e = ETERNAL;
        }

        Task dyn = d.task(tp.term, tt, (rng) -> {

            @Nullable MetalLongSet stampSet = Stamp.toSet(Param.STAMP_CAPACITY, tp.size(), tp); //calculate stamp after filtering and after intermpolation filtering
            if (stampSet.size() > Param.STAMP_CAPACITY) {
                return Stamp.sample(Param.STAMP_CAPACITY, stampSet, rng);
            } else {
                return stampSet.toSortedArray();
            }

        }, root.isBelief(), s, e, nar);

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
    protected DynEvi dynTruth() {
        int s = tasks.size();
        if (s == 0)
            return null;
        return new DynEvi(s, tasks.itemsArray(Task[]::new));
    }



    @Nullable public TruthPolation truthpolation() {
        return truthpolation(nar.dur());
    }

    @Nullable public TruthPolation truthpolation(int dur) {
        DynEvi d = dynTruth();
        return d == null ? null : truthpolation(d, dur);
    }

    /**
     * this does not filter cyclic; do that manually
     */
    private TruthPolation truthpolation(DynEvi d, int dur) {
        long s = d.start();
        long e = s != ETERNAL ? d.end() : ETERNAL;
        if (s!=ETERNAL) {
             if (this.time.start!=ETERNAL) {
                 //project to the question time range
                 s = this.time.start;
                 e = this.time.end;
             } else {
                 //use the answered time range
             }
        }

        TruthPolation tp = Param.truth(s, e, dur);
        tp.ensureCapacity(d.size());
        d.forEach(r -> tp.add(r.task()));
        return tp;
    }

    public final Answer match(TaskTable t) {
        t.match(this);
        return this;
    }
    public final Answer sample(TaskTable t) {
        t.sample(this);
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

    public void close() {
        if (tasks!=null) {
            TopN.unpool(topTasks, tasks);
            tasks = null;
        }
    }

    public Answer time(TimeRangeFilter time) {
        this.time = time;
        return this;
    }


    public final boolean isEmpty() {
        return tasks.isEmpty();
    }

    @Nullable public Task any() {
        return isEmpty() ? null : tasks.top().x;
    }

    /** consume a limited 'tries' iteration. also applies the filter.
     *  a false return value should signal a stop to any iteration supplying results */
    public final boolean tryAccept(Task t) {
        int remain = --triesRemain;
        if (remain >= 0) {
            if (filter == null || filter.test(t)) {
                accept(t);
            }
        }

        return remain > 0;
    }

    private void accept(Task task) {
        assert(task!=null);

        //if (tasks.capacity() == 1 || !(((CachedFloatFunction)(tasks.rank)).containsKey(task))) {
            //if (time == null || time.accept(task.start(), task.end())) {
            tasks.addRanked(task);
        //}
    }

    public boolean active() {
        return triesRemain > 0;
    }

    public final Random random() {
        return nar.random();
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
