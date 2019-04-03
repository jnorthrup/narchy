package nars.task.util;

import jcog.math.LongInterval;
import jcog.sort.FloatRank;
import jcog.sort.RankedN;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.table.TaskTable;
import nars.task.proxy.SpecialTruthAndOccurrenceTask;
import nars.term.Term;
import nars.term.util.Intermpolate;
import nars.time.Tense;
import nars.truth.Truth;
import nars.truth.dynamic.TaskList;
import nars.truth.polation.TruthIntegration;
import nars.truth.polation.TruthProjection;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.Predicate;

import static nars.Op.*;
import static nars.time.Tense.ETERNAL;
import static nars.truth.func.TruthFunctions.c2wSafe;

/**
 * heuristic task ranking for matching of evidence-aware truth values may be computed in various ways.
 * designed to be reusable
 */
public final class Answer {

    public final static int BELIEF_MATCH_CAPACITY =
            //Param.STAMP_CAPACITY - 1;
            //Math.max(1, Param.STAMP_CAPACITY / 2);
            Math.max(1, 2 * (int) Math.ceil(Math.sqrt(Param.STAMP_CAPACITY)));
            //3;

    public static final int BELIEF_SAMPLE_CAPACITY = BELIEF_MATCH_CAPACITY/2;
    public static final int QUESTION_SAMPLE_CAPACITY = 2;

    public final NAR nar;

    @Nullable private Term term = null;

    public final RankedN<Task> tasks;
    public TimeRangeFilter time;
    public final Predicate<Task> filter;
    private final FloatRank<Task> rank;
    public boolean ditherTruth = false;

    /**
     * time to live, # of tries remain
     */
    public int ttl;

    /**
     * truthpolation duration in result evidence projection
     */
    public int dur = 0;


    public Answer clear(int ttl) {
        tasks.clear();
        this.ttl = ttl;
        return this;
    }

    /**
     * TODO filter needs to be more clear if it refers to the finished task (if dynamic) or a component in creating one
     */
    private Answer(FloatRank<Task> rank, @Nullable Predicate<Task> filter, int capacity, NAR nar) {
        this.nar = nar;
        this.tasks = //TopN.pooled(topTasks, capacity, this.rank = rank.filter(filter));
                new RankedN<>(new Task[capacity], this.rank = rank.filter(filter));
        this.filter = filter;
    }

    /**
     * compose filter from one or two filters
     */
    public static Predicate<Task> filter(@Nullable Predicate<Task> a, @Nullable Predicate<Task> b) {
        if (a == null) return b;
        if (b == null) return a;
        return x -> a.test(x) && b.test(x);
    }

//    public static FloatFunction<Task> taskStrengthWithFutureBoost(long now, long futureThresh, float presentAndFutureBoost, int dur) {
//        float pastDiscount = 1f - (presentAndFutureBoost - 1f);
//        return (Task x) -> {
//            float evi =
//                    //x.evi(now, dur, min);
//                    x.evi(); //avg
//
//            //long e = x.end();
//            //long range = (e - x.start());
//            //float adjRange = (1 + range) / (1f + max(0, (x.maxTimeTo(now) - range / 2f))); //proportional to max time distance. so it can't constantly grow and maintain same strength
//            return (e < futureThresh ? pastDiscount : 1f) *
//                    //evi * range;
//                    //evi;
//                    //x.evi(now, dur) * (1 + (e-s)/2f)/*x.range()*/;
//                    //evi * x.originality();
//                    //evi * x.originality() * range;
//                    evi
//                    * adjRange
//                    //* (1f/(1+x.term().volume())) //TODO only if table for temporable concept
//                    //* x.originality()
//                    ;
//        };
//
//        //(TruthIntegration.eviAvg(x, 0))/ (1 + x.maxTimeTo(now)/((float)dur));
//        ///w2cSafe(TruthIntegration.evi(x));
//        //w2cSafe(TruthIntegration.evi(x)) / (1 + x.midTimeTo(now)/((float)dur));
//        //w2cSafe(x.evi(now, dur));
//        //(x.evi(now, dur)) * x.range();
//        //w2cSafe(x.evi(now, dur)) * (float)Math.log(x.range());
////        };
//    }

    /**
     * sorts nearest to the end of a list
     */
    public FloatFunction<TaskRegion> temporalDistanceFn() {

//        TimeRange target;
//        if (time.start == ETERNAL) {
//            //target = new TimeRange(nar.time()); //prefer closer to the current time
//            throw new WTF();
//        } else
//            target = time;

        return temporalDistanceFn(time);

    }

    static final FloatFunction<TaskRegion> EviAbsolute =
            x -> x instanceof Task ? (float) TruthIntegration.evi((Task) x) : (x.confMax() * x.range());

    public static FloatFunction<TaskRegion> temporalDistanceFn(TimeRange target) {
        long targetStart = target.start;
        if (targetStart == ETERNAL) {
            return EviAbsolute;
        } else if (targetStart != target.end) {
            long targetEnd = target.end;
            //return b -> -(Util.mean(b.minTimeTo(a.start), b.minTimeTo(a.end))) -b.range()/tableDur;
            //return b -> -(Util.mean(b.midTimeTo(a.start), b.minTimeTo(a.end))); // -b.range()/tableDur;
            // -b.minTimeTo(a.start, a.end); // -b.range()/tableDur;
            return x -> -LongInterval.minTimeTo(x, targetStart, targetEnd);//-target.minTimeTo(x);
//            return b -> {
//
//                return a.minTimeTo(b);
//long bs = b.start(), be = b.end();
//                long abs = a.minTimeTo(bs);
//                float r = -(bs!=be ? Util.mean(abs, a.minTimeTo(be)) : abs);
//                return r; //TODO make sure that the long cast to float is ok
//            };
        } else {
            return x -> -LongInterval.minTimeTo(x, targetStart); // -b.range()/tableDur;
        }
    }

    public FloatRank<Task> rank() {
        return rank;
    }


    /**
     * for belief or goals (not questions / quests
     */
    public static Answer relevant(boolean beliefOrQuestion, int capacity, long start, long end, @Nullable Term template, @Nullable Predicate<Task> filter, NAR nar) {

        FloatRank<Task> r = relevant(beliefOrQuestion, start, end, template);

        return new Answer(r, filter, capacity, nar)
                .time(start, end)
                .template(template)
                .clear(Math.round(Param.ANSWER_COMPLETENESS * capacity));
    }


    static FloatRank<Task> relevant(boolean beliefOrQuestion, long start, long end, @Nullable Term template) {

        FloatRank<Task> strength =
                beliefOrQuestion ?
                        beliefStrength(start, end) : questionStrength(start, end);

        FloatRank<Task> r;
        if (template == null || !template.hasAny(Temporal) || template.equals(template.concept()) /* <- means it will match anything */) {
            r = strength;
        } else {
            r = complexTaskStrength(strength, template);
        }
        return r;
    }

    public Answer template(@Nullable Term template) {
        if (template!=null && !template.op().taskable)
            throw new TaskException(template, "not Answerable");

        this.term = template;
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
                                return pri / (1f + t.minTimeTo(start, end));
                            return Float.NaN;
                        };

    }


    /**
     * TODO use FloatRank min
     */
    public static FloatRank<Task> eternalTaskStrength() {
        //return (x, min) -> (x.isEternal() ? x.evi() : x.eviEternalized() * x.range())
        //* x.originality()

        return (x, min) -> (float) x.evi();

    }


    public static FloatRank<Task> temporalTaskStrength(long start, long end) {
        int dur = Tense.occToDT(1 + (end - start)/2 /*half the range*/ );
        return temporalTaskStrength(start, end, dur);
    }

    /**
     * TODO use FloatRank min
     */
    public static FloatRank<Task> temporalTaskStrength(long start, long end, int dur) {
        return (x, min) -> (float) TruthIntegration.evi(x, start, end, dur)
                //* x.originality()
                ;
    }

    public Answer ditherTruth(boolean ditherTruth) {
        this.ditherTruth = ditherTruth;
        return this;
    }

    public Task task(boolean topOrSample, boolean forceProject, boolean dither) {
        return task(topOrSample, forceProject, dither, dither);
    }

    /**
     * matches, and projects to the specified time-range if necessary
     * note: if forceProject, the result may be null if projection doesnt succeed.
     * only useful for precise value summarization for a specific time.
     * <p>
     * <p>
     * clears the cache and tasks before returning
     */
    private Task task(boolean topOrSample, boolean forceProject, boolean ditherTruth, boolean ditherTime) {

        int s = tasks.size();
        if (s == 0)
            return null;

        ditherTruth(ditherTruth); //enable/disable truth dithering

        Task root = tasks.first();

        Task t;
        if (s == 1)
            t = root;
        else {
            if (topOrSample) {
                //compare alternate roots, as they might match better with tasks below
                switch (root.punc()) {
                    case BELIEF:
                    case GOAL:
                        t = Truth.stronger(newTask(), tasks.first());
                        break;

                    case QUESTION:
                    case QUEST:
                    default:
                        throw new UnsupportedOperationException();
                }
            } else {
                t = tasks.getRoulette(random());
                assert (!forceProject);
            }
        }

        double eviMin = eviMin();
        if (t.evi() < eviMin)
            return null;

        if (forceProject) { //dont bother sub-projecting eternal here.
            long ss = time.start;
            if (ss != ETERNAL) { //dont eternalize here
                long ee = time.end;
                @Nullable Task t2 = Task.project(t, ss, ee, eviMin, ditherTime, this.ditherTruth, nar);
                if (t2 == null)
                    return null;
                t = t2;
            }
        }

        return t;

    }

    private Task newTask() {
        int n = tasks.size();
        assert (n > 0);

        Task root = tasks.first();
        if (n == 1) {
            return root;
        } else {
            return newTask(truthpolation(taskList()), root.isBeliefOrGoal());
        }
    }

    public double eviMin() {
        return ditherTruth ? c2wSafe(nar.confMin.floatValue()) : Double.MIN_NORMAL;
    }

    /**
     * clears the cache and tasks before returning
     */
    @Nullable
    public Truth truth() {

        //quick case: 1 item, and it's eternal => its truth
//
//        int s = tasks.size();
//        if (s == 0)
//            return null;

//            if (s == 1) {
//                //simple case where the projected time is exactly right
//                //TODO dither if dither
//                Task only = tasks.get(0);
//                long onlyStart = only.start();
//                if ((onlyStart==ETERNAL) || (onlyStart == time.start && only.end() == time.end)) {
//                    Truth trEte = only.truth();
//                    return (trEte.evi() < eviMin()) ? null : trEte;
//                }
//            }

        TruthProjection tp = truthpolation(taskList());
        if (tp != null) {
            assert (!ditherTruth); assert (eviMin() <= Param.TRUTH_EVI_MIN);

            return tp.truth(Param.TRUTH_EVI_MIN, false, false /* give the value at specified range, no matter how sparse */, nar);
        }

        return null;
    }


    @Nullable
    private Task newTask(@Nullable TruthProjection tp, boolean beliefOrGoal) {

        if (tp == null)
            return null;

        @Nullable Truth tt = tp.truth(eviMin(), ditherTruth, true, nar);
        if (tt == null)
            return null;

        //HACK TODO do this without creating a temporary TaskList
        if (tp.size() == 1) {
            //proxy to the individual task being projected
            Task only = tp.get(0).task();
            if (!only.isEternal() && (only.start() != tp.start() || only.end() != tp.end()))
                return new SpecialTruthAndOccurrenceTask(only,
                        tp.start(), tp.end(), false, tt);
            else
                return only; //as-is
        } else {
            return tp.list().merge(tp.term, tt, tp::stamper, beliefOrGoal, tp.start(), tp.end(), nar);
        }
    }


    @Nullable
    public TaskList taskList() {
        int t = tasks.size();
        if (t == 0)
            return null;
        else return new TaskList(tasks, tasks.size()); //copy because it can be modified
    }

    /**
     * this does not filter cyclic; do that separately
     */
    private TruthProjection truthpolation(TaskList tt) {

        if (tt == null)
            return null;

//        long s = tt.start(), e;
//        if (s != ETERNAL) {
//            e = tt.end();
//            TimeRangeFilter t = this.time;
//            long ts = t.start;
//            if (ts != ETERNAL) {
////                if (!trim) {
//                //project to the question time range
//                s = t.start;
//                e = t.end;
////                } else {
////
////                    //shrinkwrap
////                    if (Longerval.contains(t.start, t.end, s, e)) {
////                        //long s0 = s, e0 = e;
////                        s = Math.max(ts, s);
////                        e = Math.min(Math.max(s, t.end), e);
//////                        if (s0 != s || e0 != e) {
//////                            if (e == s)
//////                                System.out.println(s0 + ".." + e0 + " -> " + s + ".." + e + "\td=" + ((e - s) - (e0 - s0)));
//////                        }
////                    }
////                }
//
//            } else {
//                //use the answered time range
//            }
//        } else {
//            e = ETERNAL;
//        }
        long s = time.start, e = time.end;
        if (s == ETERNAL) {
            //auto-crop if currently eternal
            s = tt.start();
            e = tt.end();
        }
        TruthProjection tp = nar.projection(s, e, dur);
        tp.ensureCapacity(tt.size());
        tt.forEach(tp::add);

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


    public Answer time(TimeRangeFilter time) {
        this.time = time;
        return this;
    }


    public final boolean isEmpty() {
        return tasks.isEmpty();
    }

    @Nullable
    public Task any() {
        return isEmpty() ? null : tasks.top();
    }

    /**
     * consume a limited 'tries' iteration. also applies the filter.
     * a false return value should signal a stop to any iteration supplying results
     */
    public final boolean tryAccept(Task t) {
        if (time.accept(t)) {
            int remain = --ttl;
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
        assert (task != null);

        //if (tasks.capacity() == 1 || !(((CachedFloatFunction)(tasks.rank)).containsKey(task))) {
        //if (time == null || time.accept(task.start(), task.end())) {
        tasks.add(task);
        //}
    }

    public boolean active() {
        return ttl > 0;
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
        if (this.dur != dur) {
            this.dur = dur;
            //time(time.start, time.end); //update the time filter
        }
        return this;
    }

    /**
     * term template
     */
    @Nullable public Term term() {
        return term;
    }
}
