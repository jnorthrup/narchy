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
import static nars.time.Tense.TIMELESS;

/**
 * heuristic task ranking for matching of evidence-aware truth values may be computed in various ways.
 * designed to be reusable
 */
public final class Answer implements Timed, Predicate<Task> {

    public final NAR nar;

    @Nullable private Term term = null;

    public final RankedN<Task> tasks;

    public long start = ETERNAL, end = ETERNAL;

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
        this.tasks = new RankedN<>(new Task[capacity], rank.filter(filter));
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


    /** for use only in temporal belief tables; eternal tasks not supported since i dont know how to directly compare them with temporals for the purposes of this interface */
    public static FloatFunction<TaskRegion> beliefStrength(long targetStart, long targetEnd, double dur) {
        return t -> beliefStrength(t, targetStart, targetEnd, dur);
    }

    public static float beliefStrength(TaskRegion t, long now, double dur) {
        return beliefStrength(t, now, now, dur);
    }

    public static float beliefStrength(TaskRegion t, long qStart, long qEnd, double dur) {
        return (float)(evidence(t, dur) / (1 + distanceMin(t, qStart, qEnd)));
    }

    public static FloatFunction<TaskRegion> regionNearness(long qStart, long qEnd) {
        return qStart == qEnd ?
            (x -> -distanceMin(x, qStart)) :
            (x -> -((float) distanceMin(x, qStart, qEnd))) ;
    }

    /** temporal distance to point magnitude */
    private static double distanceMid(TaskRegion t, long now, double dur) {
        return t.meanTimeTo(now)/(1+dur);
    }
    private static float distanceMin(TaskRegion t, long now) {
        return t.minTimeTo(now);
    }
    /** temporal distance to range magnitude */
    private static double distanceMin(TaskRegion t, long qStart, long qEnd) {
        return t.minTimeTo(qStart, qEnd);
    }
    private static double distanceMid(TaskRegion t, long qStart, long qEnd, double dur) {
        return t.meanTimeTo(qStart, qEnd)/(1+dur);
    }

    /** evidence magnitude */
    private static double evidence(TaskRegion t, double dur) {
        return (t.eviMean() * t.range()/(1+dur));
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
                    }
//                }

//            }
            //dtDiff = dtDiff > 0 ? (float) Math.log(1+dtDiff) : 0; //HACK

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



        Task root = tasks.first();
        Task t = s == 1 ? root : merge(topOrSample, forceProject, root);

        double eviMin = eviMin();
        if (t.evi() < eviMin)
            return null;

        //dont bother sub-projecting eternal here.
        if (forceProject) {
            long ss = start;
            if (ss != ETERNAL) {
                long ee = end;
                t = Task.project(t, ss, ee, eviMin, ditherTruth, ditherTime ? nar.dtDither() : 1, dur, nar);
            }
        }

        return t;
    }

    private Task merge(boolean topOrSample, boolean forceProject, Task root) {
        Task t;
        if (topOrSample) {
            //compare alternate roots, as they might match better with tasks below
            switch (root.punc()) {
                case BELIEF:
                case GOAL: {
                    long ss, ee;
                    if (forceProject) {
                        ss = start; ee = end;
                    } else {
                        ss = ee = TIMELESS; /*auto*/
                    }
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

        TruthProjection tp = truthProjection(forceProject);
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
        TruthProjection tp = truthProjection(true);
        if (tp != null) {
            assert (!ditherTruth); //assert (eviMin() <= NAL.truth.EVI_MIN);

            return tp.truth(NAL.truth.EVI_MIN, false, false, nar);
        }

        return null;
    }

    @Nullable
    public final TruthProjection truthProjection(boolean forceProject) {
        int numTasks = tasks.size();
        if (numTasks == 0) return null;

        long s, e;
        if (forceProject) {
            s = start; e = end;
        } else { s = e = ETERNAL; /* auto */ }
        return nar.newProjection(s, e, dur).init(this.tasks.items, numTasks);
    }


    public final Answer match(TaskTable t) {
        t.match(this);
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
        //assert (t != null);
        int remain = --ttl;
        if (remain >= 0) {
            if (filter == null || filter.test(t)) {
                tasks.add(t);
            }
        }
        return remain > 0;
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
        this.start = start;
        this.end = end;
        return this;
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
