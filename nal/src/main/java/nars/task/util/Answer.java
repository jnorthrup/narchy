package nars.task.util;

import jcog.WTF;
import jcog.sort.FloatRank;
import jcog.sort.RankedTopN;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.table.TaskTable;
import nars.term.Term;
import nars.term.util.Intermpolate;
import nars.truth.Truth;
import nars.truth.dynamic.TaskList;
import nars.truth.polation.TruthIntegration;
import nars.truth.polation.TruthPolation;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.Predicate;

import static jcog.math.SloppyMath.max;
import static nars.Op.*;
import static nars.time.Tense.ETERNAL;
import static nars.truth.func.TruthFunctions.c2wSafe;

/**
 * heuristic task ranking for matching of evidence-aware truth values may be computed in various ways.
 */
public final class Answer implements AutoCloseable {

    public final static int BELIEF_MATCH_CAPACITY =
            //Param.STAMP_CAPACITY - 1;
            //Math.max(1, Param.STAMP_CAPACITY / 2);
            Math.max(1, 2 * (int) Math.ceil(Math.sqrt(Param.STAMP_CAPACITY)));
            //3;


    public static final int BELIEF_SAMPLE_CAPACITY = 2; //Math.max(1, BELIEF_MATCH_CAPACITY / 2);
    public static final int QUESTION_SAMPLE_CAPACITY = 1;

    private final FloatRank<Task> rank;


    boolean ditherTruth = false;

    public final NAR nar;
    public int triesRemain;
    public TimeRangeFilter time;
    public Term template = null;

    //public final static ThreadLocal<MetalPool<RankedTopN>> topTasks = RankedTopN.newRankedPool();

    public RankedTopN<Task> tasks;
    public final Predicate<Task> filter;

    /** truthpolation duration */
    public int dur = 0;

    private Answer(int capacity, FloatRank<Task> rank, @Nullable Predicate<Task> filter, NAR nar) {
        this(capacity, Math.round(Param.ANSWER_COMPLETENESS * capacity), rank, filter, nar);
    }

    /** TODO filter needs to be more clear if it refers to the finished task (if dynamic) or a component in creating one */
    private Answer(int capacity, int maxTries, FloatRank<Task> rank, @Nullable Predicate<Task> filter, NAR nar) {
        this.nar = nar;
        this.tasks = //TopN.pooled(topTasks, capacity, this.rank = rank.filter(filter));
                new RankedTopN<>(new Task[capacity], this.rank = rank.filter(filter));
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

    public static FloatFunction<Task> taskStrengthWithFutureBoost(long now, long futureThresh, float presentAndFutureBoost, int dur) {
        float pastDiscount = 1f - (presentAndFutureBoost - 1f);
        return (Task x) -> {
            float evi =
                //x.evi(now, dur, min);
                x.evi(); //avg

            long e = x.end();
            long range = (e - x.start());
            float adjRange = (1+range)/(1f + max(0,(x.maxTimeTo(now) - range/2f))); //proportional to max time distance. so it can't constantly grow and maintain same strength
            return (e < futureThresh ? pastDiscount : 1f) *
                    //evi * range;
                    //evi;
                    //x.evi(now, dur) * (1 + (e-s)/2f)/*x.range()*/;
                    //evi * x.originality();
                    //evi * x.originality() * range;
                    evi
                        * adjRange
                        //* (1f/(1+x.term().volume())) //TODO only if table for temporable concept
                        //* x.originality()
                    ;
        };

        //(TruthIntegration.eviAvg(x, 0))/ (1 + x.maxTimeTo(now)/((float)dur));
        ///w2cSafe(TruthIntegration.evi(x));
        //w2cSafe(TruthIntegration.evi(x)) / (1 + x.midTimeTo(now)/((float)dur));
        //w2cSafe(x.evi(now, dur));
        //(x.evi(now, dur)) * x.range();
        //w2cSafe(x.evi(now, dur)) * (float)Math.log(x.range());
//        };
    }

    /** sorts nearest to the end of a list */
    public FloatFunction<TaskRegion> temporalDistanceFn() {

        TimeRange target;
        if (time.start==ETERNAL)
            target = new TimeRange(nar.time()); //prefer closer to the current time
        else
            target = time;

        return temporalDistanceFn(target);

    }


    public static FloatFunction<TaskRegion> temporalDistanceFn(TimeRange target) {
        long targetStart = target.start;
        if (targetStart == ETERNAL) {
            return x -> -(x instanceof Task ? TruthIntegration.evi((Task)x) : (x.confMax() * x.range()));
        } else if (targetStart != target.end) {
            //return b -> -(Util.mean(b.minTimeTo(a.start), b.minTimeTo(a.end))) -b.range()/tableDur;
            //return b -> -(Util.mean(b.midTimeTo(a.start), b.minTimeTo(a.end))); // -b.range()/tableDur;
            // -b.minTimeTo(a.start, a.end); // -b.range()/tableDur;
            return x -> -target.minTimeTo(x);
//            return b -> {
//
//                return a.minTimeTo(b);
//long bs = b.start(), be = b.end();
//                long abs = a.minTimeTo(bs);
//                float r = -(bs!=be ? Util.mean(abs, a.minTimeTo(be)) : abs);
//                return r; //TODO make sure that the long cast to float is ok
//            };
        } else {
            return b -> -b.minTimeTo(targetStart); // -b.range()/tableDur;
        }
    }

    public FloatRank<Task> rank() {
        return rank;
    }


    /**
     * for belief or goals (not questions / quests
     */
    public static Answer relevance(boolean beliefOrQuestion, int capacity, long start, long end, @Nullable Term template, @Nullable Predicate<Task> filter, NAR nar) {

        if (!beliefOrQuestion && capacity > 1)
            throw new WTF("questions are not merged so the capacity need not exceed 1");

        FloatRank<Task> r = relevance(beliefOrQuestion, start, end, template);

        return new Answer(capacity, r, filter, nar)
                .time(start, end)
                .template(template);
    }



    static FloatRank<Task> relevance(boolean beliefOrQuestion, long start, long end, @Nullable Term template) {

        FloatRank<Task> strength =
                beliefOrQuestion ?
                        beliefStrength(start, end) : questionStrength(start, end);

        FloatRank<Task> r;
        if (template == null || !template.hasAny(Temporal) || template.equals(template.concept()) /* <- means it will match anything */ ) {
            r = strength;
        } else {
            r = complexTaskStrength(strength, template);
        }
        return r;
    }

    public Answer template(Term template) {
        this.template = template;
        return this;
    }


//    /** TODO FloatRank not FloatFunction */
//    public static FloatFunction<TaskRegion> mergeability(Task x) {
//        LongPredicate xStamp = Stamp.toContainment(x);
//
//        long xStart = x.start(), xEnd = x.end();
//
//        FloatFunction<TaskRegion> f = (TaskRegion t) -> {
//
//            if (t==x || (!Param.ALLOW_REVISION_OVERLAP_IF_DISJOINT_TIME /* TODO: && !disjointTime(x,y) */
//                    && Stamp.overlapsAny(xStamp, ((Task) t).stamp())))
//                return Float.NaN;
//
//            return
//                1f/(1 + (Math.abs(t.start() - xStart) + Math.abs(t.end() - xEnd))/2f);
//        };
//
//        Term xt = x.term();
//        if (xt.hasAny(Op.Temporal)) {
//
//            return (t) -> {
//                float v1 = f.floatValueOf(t);
//                if (v1 != v1) return Float.NaN;
//
//                Term tt = ((Task) t).term();
//                return v1 / (1f + Intermpolate.dtDiff(xt, tt));
//            };
//        } else {
//            return f;
//        }
//    }

    public static FloatRank<Task> complexTaskStrength(FloatRank<Task> strength, Term template) {
        return (x, min) -> {

            float dtDiff = Intermpolate.dtDiff(template, x.term());
            if (!Float.isFinite(dtDiff))
                return Float.NaN;

            float d = 1 / (1 + dtDiff);
            if (d < min)
                return Float.NaN;

            return strength.rank(x, min) * d;
        };
    }

    public static FloatRank<Task> beliefStrength(long start, long end) {
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


    /** TODO use FloatRank min */
    public static FloatRank<Task> eternalTaskStrength() {
        return (x,min) -> (x.isEternal() ? x.evi() : x.eviEternalized() * x.range())
                //* x.originality()
                ;
    }

    /** TODO use FloatRank min */
    public static FloatRank<Task> temporalTaskStrength(long start, long end) {
        //long dur = Math.max(1,(1 + (end-start))/2);
        //return (x,min) -> TruthIntegration.evi(x, start, end, dur /*1*/ /*0*/)
        return (x,min) -> TruthIntegration.evi(x, start, end, 1)
                //* x.originality()
                ;
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
                    t = tasks.get(0);
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

            if (forceProject && t != null && !t.isEternal()) { //dont bother sub-projecting eternal here.
                long ss = time.start;
                if (ss != ETERNAL) { //dont eternalize here
                    long ee = time.end;
                    if (/*t.isEternal() || */!t.containedBy(ss, ee)) {
                        t = Task.project(t, ss, ee, true, ditherTruth, false, nar);
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

            int s = tasks.size();
            if (s == 0)
                return null;

            if (s == 1) {
                Task only = tasks.get(0);
                if (only.isEternal() || (only.start()==time.start && only.end()==time.end)) {
                    Truth trEte = only.truth();
                    return (trEte.evi() < eviMin()) ? null : trEte;
                }
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
        return (topOrSample ? tasks.get(0) :  tasks.getRoulette(random()));
    }

    private Task taskMerge(@Nullable Task root) {

        @Nullable TaskList d = dynTruth();
        if (d.size() <= 1)
            return root;

        TruthPolation tp = truthpolation(d);

        tp.filterCyclic(root, false);

        if (tp.size() == 1)
            return root;

        tp.refocus(nar);

        @Nullable Truth tt = truth(tp);
        if (tt == null)
            return root;

        if (ditherTruth) {
            tt = tt.dithered(nar);
            if (tt == null)
                return null;
        }

        long s = tp.start(), e = tp.end();
//        if (allSatisfy(Task::isEternal)) {
//            s = e = ETERNAL;
//        }


        Task dyn = tp.task(d, tt, root.isBeliefOrGoal(), s, e, nar);

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
    protected TaskList dynTruth() {
        int s = tasks.size();
        if (s == 0)
            return null;
        return new TaskList(s, tasks.itemsArray());
    }


    @Nullable public TruthPolation truthpolation() {
        TaskList d = dynTruth();
        return d == null ? null : truthpolation(d);
    }

    /**
     * this does not filter cyclic; do that manually
     */
    private TruthPolation truthpolation(TaskList d) {
        long s = d.start();
        long e = s != ETERNAL ? d.end() : ETERNAL;
        if (s!=ETERNAL) {
            TimeRangeFilter t = this.time;
            if (t.start!=ETERNAL) {
                 //project to the question time range
                 s = t.start;
                 e = t.end;
             } else {
                 //use the answered time range
             }
        }

        TruthPolation tp = Param.truth(s, e, dur);
        tp.ensureCapacity(d.size());
        d.forEach(tp::add);
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
            //TopN.unpool(topTasks, tasks);
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
        return isEmpty() ? null : tasks.top();
    }

    /** consume a limited 'tries' iteration. also applies the filter.
     *  a false return value should signal a stop to any iteration supplying results */
    public final boolean tryAccept(Task t) {
        if (time.accept(t.start(), t.end())) {
            int remain = --triesRemain;
            if (remain >= 0) {
                if (filter == null || filter.test(t)) {
                    accept(t);
                }
            }
            return remain > 0;
        } else {
            return true;
        }
    }

    private void accept(Task task) {
        assert(task!=null);

        //if (tasks.capacity() == 1 || !(((CachedFloatFunction)(tasks.rank)).containsKey(task))) {
            //if (time == null || time.accept(task.start(), task.end())) {
        tasks.add(task);
        //}
    }

    public boolean active() {
        return triesRemain > 0;
    }

    public final Random random() {
        return nar.random();
    }

    public Answer time(long start, long end) {
        return time(TimeRangeFilter.the(start, end,
                TimeRangeFilter.Mode.Near
                //start!=ETERNAL && dur == 0 ? TimeRangeFilter.Mode.Intersects : TimeRangeFilter.Mode.Near
        ));
    }

    public Answer dur(int dur) {
        if (this.dur!=dur) {
            this.dur = dur;
            //time(time.start, time.end); //update the time filter
        }
        return this;
    }

//
//    @Nullable
//    private Truth truth(TruthPolation p) {
//        p.ensureCapacity(tasks.size());
//        p.addAt(tasks);
//        p.filterCyclic(false);
//        return p.truth();
//    }


}
