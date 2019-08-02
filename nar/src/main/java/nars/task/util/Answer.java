package nars.task.util;

import jcog.math.LongInterval;
import jcog.sort.FloatRank;
import jcog.sort.RankedN;
import nars.NAL;
import nars.NAR;
import nars.Task;
import nars.table.TaskTable;
import nars.term.Term;
import nars.term.util.Intermpolate;
import nars.term.util.TermException;
import nars.time.Tense;
import nars.truth.Truth;
import nars.truth.dynamic.DynTaskify;
import nars.truth.proj.TruthIntegration;
import nars.truth.proj.TruthProjection;
import nars.util.Timed;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.Predicate;

import static nars.Op.*;
import static nars.time.Tense.ETERNAL;

/**
 * heuristic task ranking for matching of evidence-aware truth values may be computed in various ways.
 * designed to be reusable
 */
public final class Answer implements Timed, Predicate<Task> {

    public final static int BELIEF_MATCH_CAPACITY =
            (int)Math.ceil(NAL.STAMP_CAPACITY * NAL.ANSWER_DETAIL);
            //Param.STAMP_CAPACITY - 1;
            //Math.max(1, Param.STAMP_CAPACITY / 2);
            //Math.max(1, 2 * (int) Math.ceil(Math.sqrt(NAL.STAMP_CAPACITY)));
            //3;

    public static final int BELIEF_SAMPLE_CAPACITY = BELIEF_MATCH_CAPACITY/2;
    public static final int QUESTION_SAMPLE_CAPACITY = 1;

    public final NAR nar;

    @Nullable private Term term = null;

    public final RankedN<Task> tasks;
    public TimeRangeFilter time;
    public final Predicate<Task> filter;

    public boolean ditherTruth = false;

    /**
     * time to live, # of tries remain
     */
    public int ttl;

    /**
     * truthpolation duration in result evidence projection
     */
    public float dur = 0;


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
                new RankedN<>(new Task[capacity], rank.filter(filter));
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
            x -> x instanceof Task ? (float) TruthIntegration.evi((Task) x) : (x.confMean() * x.range());

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


    /**
     * for belief or goals (not questions / quests
     */
    public static Answer relevance(boolean beliefOrQuestion, int capacity, long start, long end, @Nullable Term template, @Nullable Predicate<Task> filter, NAR nar) {
        return new Answer(
                relevance(beliefOrQuestion, start, end, template),
                filter, capacity, nar)
                .time(start, end)
                .term(template)
                .clear((int) Math.ceil(NAL.ANSWER_TRYING * capacity));
    }


    static FloatRank<Task> relevance(boolean beliefOrQuestion, long start, long end, @Nullable Term template) {

        FloatRank<Task> strength =
                beliefOrQuestion ?
                        beliefStrength(start, end) : questionStrength(start, end);


        return (template == null || !template.hasAny(Temporal) || template.equals(template.concept())) ? /* <- means it will match anything */
            strength
            :
            complexTaskStrength(strength, template);
    }

    public Answer term(@Nullable Term template) {
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

    private static FloatRank<Task> complexTaskStrength(FloatRank<Task> strength, Term template) {
        return (x, min) -> {

            Term xt = x.term();
            float dtDiff = Intermpolate.dtDiff(template, xt);
            if (!Float.isFinite(dtDiff)) {
                /* probably safe to ignore caused by a Dynamic task result that doesnt quite match what is being sought
                   TODO record a misfire event. this will measure how much dynamic task generation is reducing efficiency
                 */

                if (template.op() == CONJ && template.containsRecursively(xt)) {
                    //HACK a dynamic conjunction to revision collapse
                    dtDiff = template.volume();
                } else {
                    if (NAL.DEBUG)
                        throw new TermException("mismatch for Answer template: " + template, x);
                    else {
                        return Float.NaN;
                    }
                }

            }

            float d = 1 / (1 + dtDiff);
            if (d < min)
                return Float.NaN;

            return strength.rank(x, min) * d;
        };
    }

    public static FloatRank<Task> beliefStrength(long start, long end) {
        return start == ETERNAL ? eternalTaskStrength() : temporalTaskStrength(start, end);
    }

    public static FloatRank<Task> questionStrength(long start, long end) {

        return
                (start == ETERNAL) ?
                        (t, m) -> t.pri()
                        :
                        (t, m) -> {
                            float pri = t.pri(); // * t.originality();
                            if (pri == pri && pri > m)
                                return (float) (pri / (1 + t.minTimeTo(start, end)));
                                //return (float) (pri / (1 + Math.log(1+t.minTimeTo(start, end))));
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


    /** HACK needs double precision */
    public static FloatRank<Task> temporalTaskStrength(long start, long end) {
//        float dur = 1f + (end - start)/2f;
//        return (x, min) -> (float) TruthIntegration.evi(x, start, end, dur)
//                //* x.originality()
//                ;

        return (x,min) -> (float) TruthIntegration.eviFast(x, start, end);
    }


    public Answer ditherTruth(boolean ditherTruth) {
        this.ditherTruth = ditherTruth;
        return this;
    }

    public Task task(boolean topOrSample, boolean forceProject, boolean ditherTruth) {
        boolean ditherTime = ditherTruth;
        return task(topOrSample, forceProject, ditherTruth, ditherTime);
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

        long ss = time.start, ee = time.end;

        Task t;
        if (s == 1)
            t = root;
        else {
            if (topOrSample) {
                //compare alternate roots, as they might match better with tasks below
                switch (root.punc()) {
                    case BELIEF:
                    case GOAL: {
                        t = Truth.stronger(newTask(root.isBelief()), root, ss, ee);
                        break;
                    }

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

        if (forceProject && ss!=ETERNAL) { //dont bother sub-projecting eternal here.

            t = Task.project(t, ss, ee, eviMin, ditherTruth, ditherTime ? nar.dtDither() : 1, dur, nar);

        }

        return t;

    }

    private Task newTask(boolean beliefOrGoal) {
        TruthProjection tp = truthProjection();
        return tp!=null ? newTask(tp, beliefOrGoal) : null;
    }

    private double eviMin() {
        return ditherTruth ? nar.confMin.evi() : NAL.truth.EVI_MIN;
        //return nar.confMin.evi();
    }

    /**
     * clears the cache and tasks before returning
     */
    @Nullable
    public Truth truth() {
        TruthProjection tp = truthProjection();
        if (tp != null) {
            assert (!ditherTruth); //assert (eviMin() <= NAL.truth.EVI_MIN);

            return tp.truth(NAL.truth.EVI_MIN, false, false, nar);
        }

        return null;
    }

    @Nullable
    public TruthProjection truthProjection() {
        int n = tasks.size();
        if (n ==0)
            return null;

        long s = time.start, e = time.end;
//        if (s == ETERNAL) {
//            //auto-crop if currently eternal
//
//            boolean ditherTime = ditherTruth;
//            if (ditherTime) {
//                int dither = nar.dtDither();
//                s = Tense.dither(((TaskList) null).start(), dither, -1);
//                e = Tense.dither(((TaskList) null).end(), dither, +1);
//            }
//        }

        TruthProjection tp = nar.projection(s, e, dur);
        tp.add(n, this.tasks.items);
        return tp;
    }


    @Nullable
    private Task newTask(TruthProjection tp, boolean beliefOrGoal) {
        @Nullable Truth tt = tp.truth(eviMin(), ditherTruth, true, nar);
        if (tt == null)
            return null;

        return DynTaskify.merge(tp::arrayCommit, tp.term, tt, tp.stamp(nar.random()), beliefOrGoal, tp.start(), tp.end(), nar);
    }


    public final Answer match(TaskTable t) {
        t.match(this);
        return this;
    }


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
    public final boolean test(Task t) {
        assert (t != null);
        if (time.accept(t)) {
            int remain = --ttl;
            if (remain >= 0) {
                if (filter == null || filter.test(t)) {
                    tasks.add(t);
                }
            }
            return remain > 0;
        } else {
            return true;
        }
    }

    @Override
    public final float dur() {
        return dur;
    }

    public final Random random() {
        return nar.random();
    }

    @Override
    public final long time() {
        return nar.time();
    }

    public final Answer time(long start, long end) {
        return time(start, end, true);
    }

    public final Answer time(long start, long end, boolean dither) {
        if (dither && start!=ETERNAL) {
            int dtDither = nar.dtDither();
            if (dtDither > 1) {
                start = Tense.dither(start, dtDither);
                end = Tense.dither(end, dtDither);
            }
        }
        return time(TimeRangeFilter.the(start, end,
                TimeRangeFilter.Mode.Near
                //start!=ETERNAL && dur == 0 ? TimeRangeFilter.Mode.Intersects : TimeRangeFilter.Mode.Near
        ));
    }

    public Answer dur(float dur) {
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
