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
import java.util.function.BiFunction;
import java.util.function.Predicate;

import static nars.Op.*;
import static nars.time.Tense.ETERNAL;
import static nars.truth.func.TruthFunctions.c2wSafe;

/**
 * heuristic task ranking for matching of evidence-aware truth values may be computed in various ways.
 * designed to be reusable
 */
public final class Answer implements Timed, Predicate<Task> {

    public final NAR nar;


	private @Nullable Term term = null;

    public final RankedN<Task> tasks;

    public long start = ETERNAL;
    public long end = ETERNAL;

    public final Predicate<Task> filter;

    public boolean ditherTruth = false;

    /**
     * time to live, # of tries remain
     */
    public int ttl;

    /**
     * truthpolation duration in result evidence projection
     */
    public float dur = (float) 0;


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
    public static FloatFunction<TaskRegion> beliefStrength(long targetStart, long targetEnd) {
        return t -> beliefStrength(t, targetStart, targetEnd);
    }


    private static float beliefStrength(TaskRegion t, long qStart, long qEnd) {
        return (float)(c2wSafe((double) t.confMean()) * (double) t.range() / (1.0 + (double) t.minTimeTo(qStart, qEnd)));
    }

//    /** @param confPerTime rate at which confidence compensates for temporal distance (proportional to dur) */
//    public static FloatRank<TaskRegion> regionNearness(long qStart, long qEnd, float confPerTime) {
//        //TODO special impl for confPerTime==0
//        return qStart == qEnd ?
//            (x,min) -> -(float)(((double)x.minTimeTo(qStart)) + x.confMax() * confPerTime) :
//            (x,min) -> -(float)(((double)x.minTimeTo(qStart,qEnd))+ x.confMax() * confPerTime) ;
//    }
    public static FloatRank<TaskRegion> regionNearness(long qStart, long qEnd) {
        //TODO special impl for confPerTime==0
        return qStart == qEnd ?
            (x,min) -> (float) -x.minTimeTo(qStart)
            :
            (x,min) -> (float) -x.minTimeTo(qStart, qEnd);
//            (x,min) -> -(float)(((double)x.minTimeTo(qStart)) + x.confMax() * confPerTime) :
            //return (x,min) -> (float)(x.confMax() / (1.0 + x.maxTimeTo(qStart,qEnd)/dur));
        //return (x,min) -> (float)((1+x.confMin()) / (1.0 + x.maxTimeTo(qStart,qEnd)/dur));

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
                .clear((int) Math.ceil((double) ((beliefOrQuestion ? NAL.ANSWER_TRYING : 1.0F) * (float) capacity)));
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
            throw new TaskException("not Answerable", template);

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

            float d = 1.0F / (1.0F + dtDiff);
            return str * d;
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
                                return (float) (pri / (float) (1L + t.maxTimeTo(start, end)));
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

    public final @Nullable Task task(boolean topOrSample, boolean forceProject, boolean ditherTruth) {
        assert(!forceProject || topOrSample);
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
    private @Nullable Task task(boolean topOrSample, boolean forceProject, boolean ditherTruth, boolean ditherTime) {

        int s = tasks.size();
        if (s == 0)
            return null;

        ditherTruth(ditherTruth); //enable/disable truth dithering


        Task t = topOrSample ? taskTop(forceProject) : taskSample();
        if (t == null)
            return null; //why?  intermpolate?

//        if (start!=ETERNAL && tasks.items[0].isEternal() && !t.isEternal())
//            assert(t.range() >= end-start);


        double eviMin = eviMin();
        if (t.evi() < eviMin)
            return null;

        //dont bother sub-projecting eternal here.
        if (forceProject) {
            long ss = start;
            if (ss != ETERNAL) {
                long ee = end;
                t = Task.project(t, ss, ee, eviMin, ditherTruth, ditherTime ? nar.dtDither() : 1, dur, false, nar);
            }
        }

        return (filter==null || filter.test(t)) ? t :
            null;
    }

    private Task taskTop(boolean forceProject) {
        Task root = tasks.first();
        int s = tasks.size();
        if (s == 1 && (!forceProject || start==ETERNAL || (root.start() == start && root.end() == end)))
            return root;

        //compare alternate roots, as they might match better with tasks below
        switch (root.punc()) {
            case BELIEF:
            case GOAL:
                return truthProjection().task(eviMin(), ditherTruth, root.isBelief(), forceProject, nar);

            case QUESTION:
            case QUEST:
            default:
                throw new UnsupportedOperationException();
        }
    }

    private Task taskSample() {
        return tasks.getRoulette(random());
    }

    private double eviMin() {
        return ditherTruth ? nar.confMin.evi() : NAL.truth.EVI_MIN;
    }

    /**
     * clears the cache and tasks before returning
     */
    public @Nullable Truth truth(float perceptualDur) {
        assert (!ditherTruth); //assert (eviMin() <= NAL.truth.EVI_MIN);

        TruthProjection tp = truthProjection();

        if (tp != null) {
            tp.dur(perceptualDur);
            return tp.truth(start, end, NAL.truth.EVI_MIN, false, false, nar);
        } else
            return null;
    }

    public final @Nullable Truth truth() {
        return truth(dur);
    }

    public final @Nullable TruthProjection truthProjection() {
        int numTasks = tasks.size();
        if (numTasks == 0)
            return null;

        //        if (start!=ETERNAL && start!=TIMELESS && Util.or((Task t) -> t.intersects(start, end), 0, tasks.size(), tasks.items)) {
        long s = start;
        long e = end;
//        } else {
//            s = e = TIMELESS;
//        }
        return NAL.newProjection(
            s,e
            //start, end
            //TIMELESS, TIMELESS //auto
        ).dur(dur)./*eternalizeComponents(true).*/with(this.tasks.items, numTasks);
    }


    public final Answer match(TaskTable t) {
        t.match(this);
        return this;
    }

    public final boolean isEmpty() {
        return tasks.isEmpty();
    }


    /**
     * consume a limited 'tries' iteration. also applies the filter.
     * a false return value should signal a stop to any iteration supplying results
     */
    public final boolean test(Task t) {
        int remain = --ttl;
        if (remain >= 0 && (filter == null || filter.test(t)))
            tasks.add(t);

        return remain > 0;
    }

    public final void test(Task t, BiFunction<Task,Answer,Task> ifSuccessfulInsert) {
        int remain = --ttl;
        if (remain >= 0 && (filter == null || filter.test(t)))
            tasks.add(ifSuccessfulInsert.apply(t,this));
        // a tiny curry:            ifSuccessfulInsert.andThen(tasks::add).apply(t, this);
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
        return time(start, end, false);
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
    public @Nullable Term term() {
        return term;
    }

    public @Nullable Task sample() {
        switch (tasks.size()) {
            case 0: return null;
            case 1: return tasks.get(0);
            default: return tasks.getRoulette(random());
        }
    }
}
