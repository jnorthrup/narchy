package nars.task.util;

import jcog.sort.FloatRank;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.table.TaskTable;
import nars.task.Revision;
import nars.task.proxy.SpecialTruthAndOccurrenceTask;
import nars.term.Term;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.dynamic.DynTruth;
import nars.truth.polation.TruthIntegration;
import nars.truth.polation.TruthPolation;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.set.primitive.ImmutableLongSet;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static nars.Op.*;
import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.TIMELESS;
import static nars.truth.polation.TruthIntegration.valueInEternity;

/** heuristic task ranking for matching of evidence-aware truth values may be computed in various ways.
 */
public class Answer extends TaskRank {

    public final static int TASK_LIMIT = Param.STAMP_CAPACITY-1;

    public final NAR nar;

    @Deprecated public static Answer belief(long start, long end, @Nullable Term template, @Nullable Predicate<Task> filter, NAR nar) {
        return belief(TASK_LIMIT, start, end, template, filter, nar);
    }

    /** for belief or goals (not questions / quests */
    @Deprecated public static Answer belief(int limit, long start, long _end, @Nullable Term template, @Nullable Predicate<Task> filter, NAR nar) {

        long end;
        if (start == ETERNAL && _end == ETERNAL)
            end = TIMELESS; //to cover the whole range of values when ETERNAL,ETERNAL is provided
        else
            end = _end;

        int dur = nar.dur();
        FloatRank<Task> r;
        if (template == null || !template.hasAny(Temporal)) {
            if (start == ETERNAL) {
                r = (t, m) -> {
                    if (filter!=null && !filter.test(t))
                        return Float.NaN;
                    return t.isEternal() ? t.evi() : valueInEternity(t);
                };
            } else {
                r = (t, m) -> {
                    if (filter!=null && !filter.test(t))
                        return Float.NaN;
                    return TruthIntegration.eviInteg(t, start, end, dur);
                };
            }
        } else {
            FloatRank<Task> rr = ComplexTaskStrength(template, start, end, dur);
            r = (t, m) -> {
                if (filter!=null && !filter.test(t))
                    return Float.NaN;
                return rr.rank(t, m);
            };
        }
        Answer b = belief(limit, new TimeRangeFilter(start, end, true), r, nar);
        b.template = template;
        return b;
    }



    public static Answer belief(int limit, @Nullable TimeRangeFilter  timeFilter, FloatRank<Task> rank, NAR nar) {
        return new Answer(limit, rank, timeFilter, nar);
    }

    public Answer(int limit, FloatRank<Task> rank, @Nullable TimeRangeFilter timeFilter, NAR nar) {
        super(limit, rank, timeFilter);
        this.nar = nar;
    }

    public static FloatFunction<TaskRegion> mergeability(Task x, float tableDur) {
        ImmutableLongSet xStamp = Stamp.toSet(x);

        long xStart = x.start();
        long xEnd = x.end();

        return (TaskRegion _y) -> {
            Task y = (Task) _y;

            if (Stamp.overlapsAny(xStamp, y.stamp()))
                return Float.NaN;


            return
                    (1f / (1f +
                            (Math.abs(y.start() - xStart) + Math.abs(y.end() - xEnd)) / tableDur))

                    ;
        };
    }

    public static float costDtDiff(Term template, Term x, int dur) {
        return Revision.dtDiff(template, x) / (dur /* * dur*/);
    }

    public static FloatRank<Task> ComplexTaskStrength(@Nullable Term template, long start, long end, int dur) {
        FloatFunction<Task> f = taskStrength(start, end, dur);
        if (template != null && template.hasAny(Temporal)) {
            return (x,min) -> {
                float base = (1 + f.floatValueOf(x));
                if (base < min)
                    return Float.NaN;

                return base * (1 + 1/ costDtDiff(template, x.term(), dur));
            };
        } else {
            return (x,min) -> f.floatValueOf(x);
                    //  / x.volume(); //prefer lower complexity variants
        }
    }

    public static FloatFunction<Task> taskStrength(long start, long end, int dur) {
        if (start == ETERNAL) {
            return TruthIntegration::valueInEternity;
        } else {
            return x -> TruthIntegration.eviInteg(x, start, end, dur);
        }
    }


    boolean forceProject;
//    float confMin = Float.MIN_NORMAL;
    boolean ditherTruth = false;

    public Answer forceProjection(boolean forceProject) {
        this.forceProject = forceProject;
        return this;
    }

//    public Answer confMin(float confMin) {
//        this.confMin = confMin;
//        return this;
//    }

    public Answer ditherTruth(boolean ditherTruth) {
        this.ditherTruth = ditherTruth;
        return this;
    }

    /**
     * matches, and projects to the specified time-range if necessary
     * note: if forceProject, the result may be null if projection doesnt succeed.
     *   only useful for precise value summarization for a specific time.
     */
    @Nullable public Task task() {
        int s = tasks.size();
        Task t;
        switch (s) {
            case 0:
                return null;
            case 1:
                t = tasks.first();
                break;
            default: {
                @Nullable Task tf = tasks.first();
                switch (tf.punc()) {
                    case BELIEF:
                    case GOAL:
                        t = Truth.stronger(dynTask(tf.isBelief()), tf);
                        break;
                    case QUESTION:
                    case QUEST:
                        t = tasks.first();
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
                break;
            }
        }
        if (this.forceProject && t!=null) {
            long ss = time.start;
            if (ss != ETERNAL) { //dont eternalize here
                long ee = time.end;
                if (!t.intersects(ss, ee)) {
                    @Nullable SpecialTruthAndOccurrenceTask p = Task.project(t, ss, ee, nar);
                    return p;
                }
            }
        }

        return t;

//        Task m = match(start, end, template, filter, nar);
//        if (m == null)
//            return null;
//        if (m.containedBy(start, end))
//            return m;
//        Task t = Task.project(false, m, start, end, nar, false);
//        if (t instanceof TaskProxy) {
//            //dither truth
//            @Nullable PreciseTruth tt = t.truth().dither(nar);
//            if (tt != null) {
//                t = Task.clone(t, t.term(), tt, t.punc());
//            } else {
//                t = null;
//            }
//        }
//        return t;
    }

    private Task dynTask(boolean beliefOrGoal) {

        @Nullable DynTruth d = dynTruth();
        TruthPolation tp = Param.truth(time.start, time.end, nar.dur()); tp.ensureCapacity(d.size());
        d.forEach(r -> tp.add(r.task()));
        tp.filterCyclic(false);
        if (tp.size()==1) {
            return tp.getTask(0);
        }
        @Nullable Truth tt = tp.truth(nar);
        if (tt==null)
            return null;
        return d.task(template, tt, beliefOrGoal, ditherTruth, nar);
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
        DynTruth d = dynTruth();
        return d != null ? truth(d, nar) : null;
    }

    @Nullable protected Truth truth(DynTruth d, NAR n) {
        TruthPolation tp = Param.truth(time.start, time.end, n.dur()); tp.ensureCapacity(d.size());
        d.forEach(r -> tp.add(r.task()));
        tp.filterCyclic(false);
        return tp.truth(n);
    }

    public Answer match(TaskTable t) {
        t.match(this);
        return this;
    }
}
