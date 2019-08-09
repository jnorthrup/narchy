package nars.task.util;

import jcog.sort.FloatRank;
import jcog.sort.RankedN;
import nars.NAL;
import nars.NAR;
import nars.Task;
import nars.table.TaskTable;
import nars.term.Term;
import nars.term.util.Intermpolate;
import nars.time.Tense;
import nars.truth.Truth;
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


    /** for use only in temporal belief tables; eternal tasks not supported since i dont know how to directly compare them with temporals for the purposes of this interface */
    public static FloatFunction<TaskRegion> beliefStrength(long targetStart, long targetEnd, double dur) {
        return t -> beliefStrength(t, targetStart, targetEnd, dur);
    }

    public static float beliefStrength(TaskRegion t, long now, double dur) {
        return beliefStrength(t, now, now, dur);
    }

    public static float beliefStrength(TaskRegion t, long qStart, long qEnd, double dur) {
        return (float)(evidence(t, dur) / (1 + distance(t, qStart, qEnd, dur)));
    }

//    /** temporal distance to point magnitude */
//    private static double distance(TaskRegion t, long now, double dur) {
//        return t.minTimeTo(now)/dur;
//    }
    /** temporal distance to range magnitude */
    private static double distance(TaskRegion t, long qStart, long qEnd, double dur) {
        return t.minTimeTo(qStart, qEnd)/dur;
    }

    /** evidence magnitude */
    private static double evidence(TaskRegion t, double dur) {
        return (t.eviMean() * t.range()/dur);
    }

    /**
     * for belief or goals (not questions / quests
     */
    public static Answer taskStrength(boolean beliefOrQuestion, int capacity, long start, long end, @Nullable Term template, @Nullable Predicate<Task> filter, NAR nar) {
        return new Answer(
                taskStrength(beliefOrQuestion, start, end, template),
                filter, capacity, nar)
                .time(start, end)
                .term(template)
                .clear((int) Math.ceil((beliefOrQuestion ? NAL.ANSWER_TRYING : 1) * capacity));
    }


    static FloatRank<Task> taskStrength(boolean beliefOrQuestion, long start, long end, @Nullable Term template) {

        FloatRank<Task> strength =
                beliefOrQuestion ?
                    (start == ETERNAL ?
                        beliefStrengthInEternity()
                        :
                        beliefStrengthInInterval(start, end))
                    :
                    questionStrength(start, end);


        return (template == null || !template.hasAny(Temporal) || template.equals(template.concept())) ? /* <- means it will match anything */
            strength
            :
            intermpolateStrength(strength, template);
    }

    public Answer term(@Nullable Term template) {
        if (template!=null && !template.op().taskable)
            throw new TaskException(template, "not Answerable");

        this.term = template;
        return this;
    }


    private static FloatRank<Task> intermpolateStrength(FloatRank<Task> strength, Term template) {
        return (x, min) -> {
            float str = strength.rank(x, min);
            if (str < min)
                return Float.NaN; //already below thresh

            Term xt = x.term();
            float dtDiff = Intermpolate.dtDiff(template, xt);
            if (!Float.isFinite(dtDiff)) {
//                /* probably safe to ignore caused by a Dynamic task result that doesnt quite match what is being sought
//                   TODO record a misfire event. this will measure how much dynamic task generation is reducing efficiency
//                 */
//
//                if (template.op() == CONJ && template.containsRecursively(xt)) {
//                    //HACK a dynamic conjunction to revision collapse
//                    dtDiff = template.volume();
//                } else {
//                    if (NAL.DEBUG)
//                        throw new TermException("mismatch for Answer template: " + template, x);
//                    else {
                        return Float.NaN;
//                    }
//                }

            }

            float d = 1 / (1 + dtDiff);
            return d < min ? Float.NaN : str * d;
        };
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
    public static FloatRank<Task> beliefStrengthInEternity() {
        return (x, min) -> (float) x.evi();
    }


    /** HACK needs double precision */
    public static FloatRank<Task> beliefStrengthInInterval(long start, long end) {
        return (x,min) -> (float) TruthIntegration.eviFast(x, start, end);
    }


    public Answer ditherTruth(boolean ditherTruth) {
        this.ditherTruth = ditherTruth;
        return this;
    }

    public final Task task(boolean topOrSample, boolean forceProject, boolean ditherTruth) {
        return task(topOrSample, forceProject, ditherTruth, ditherTruth);
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

        long ss = time.start, ee = time.end;

        Task root = tasks.first();
        Task t = s == 1 ? root : merge(topOrSample, forceProject, root, ss, ee);

        double eviMin = eviMin();
        if (t.evi() < eviMin)
            return null;

        if (forceProject && ss!=ETERNAL) //dont bother sub-projecting eternal here.
            t = Task.project(t, ss, ee, eviMin, ditherTruth, ditherTime ? nar.dtDither() : 1, dur, nar);

        return t;
    }

    private Task merge(boolean topOrSample, boolean forceProject, Task root, long ss, long ee) {
        Task t;
        if (topOrSample) {
            //compare alternate roots, as they might match better with tasks below
            switch (root.punc()) {
                case BELIEF:
                case GOAL: {
                    t = Truth.stronger(newTask(root.isBelief(),forceProject), root, ss, ee);
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
        return t;
    }

    private Task newTask(boolean beliefOrGoal, boolean forceProject) {
        if (!forceProject && tasks.size() == 1) {
            return tasks.get(0);
        }

        TruthProjection tp = truthProjection();
        return tp!=null ? tp.newTask(eviMin(), ditherTruth, beliefOrGoal, forceProject, nar) : null;
    }

    private double eviMin() {
        return ditherTruth ? nar.confMin.evi() : NAL.truth.EVI_MIN;
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
    public final TruthProjection truthProjection() {
        int n = tasks.size();
        return n == 0 ? null :
            nar.newProjection(time.start, time.end, dur).ditherDT(n).add(n, this.tasks.items);
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
                long[] se = Tense.dither(new long[] { start, end }, dtDither);
                start = se[0];
                end = se[1];
            }
        }
        return time(TimeRangeFilter.the(start, end,
                TimeRangeFilter.Mode.Near
                //start!=ETERNAL && dur == 0 ? TimeRangeFilter.Mode.Intersects : TimeRangeFilter.Mode.Near
        ));
    }

    public Answer dur(float dur) {
//        if (this.dur != dur) {
            this.dur = dur;
            //time(time.start, time.end); //update the time filter
//        }
        return this;
    }

    /**
     * term template
     */
    @Nullable public Term term() {
        return term;
    }

    @Nullable public Task sample() {
        return tasks.isEmpty() ? null : tasks.getRoulette(random());
    }
}
