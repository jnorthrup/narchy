package nars;

import jcog.Util;
import jcog.math.LongInterval;
import jcog.pri.Prioritizable;
import jcog.pri.UnitPrioritizable;
import jcog.pri.op.PriMerge;
import jcog.pri.op.PriReturn;
import jcog.tree.rtree.HyperRegion;
import nars.control.Caused;
import nars.control.Why;
import nars.task.AbstractTask;
import nars.task.DerivedTask;
import nars.task.NALTask;
import nars.task.proxy.SpecialNegatedTask;
import nars.task.proxy.SpecialTruthAndOccurrenceTask;
import nars.task.util.TaskException;
import nars.task.util.TaskRegion;
import nars.task.util.TasksRegion;
import nars.term.Variable;
import nars.term.*;
import nars.term.anon.Intrin;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.util.TermedDelegate;
import nars.term.util.transform.RecursiveTermTransform;
import nars.term.var.NormalizedVariable;
import nars.term.var.VarIndep;
import nars.time.Tense;
import nars.truth.PreciseTruth;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.Truthed;
import nars.truth.proj.TruthIntegration;
import org.eclipse.collections.api.tuple.primitive.ShortBytePair;
import org.eclipse.collections.impl.map.mutable.primitive.ShortByteHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.lang.Math.min;
import static nars.Op.*;

/**
 * NAL Task to be processed, consists of a Sentence, stamp, time, and budget.
 */
public interface Task extends Truthed, Stamp, TermedDelegate, TaskRegion, UnitPrioritizable, Caused {

    Task[] EmptyArray = new Task[0];
    Atom BeliefAtom = $.quote(String.valueOf((char)BELIEF));
    Atom GoalAtom =  $.quote(String.valueOf((char)GOAL));
    Atom QuestionAtom =  $.quote(String.valueOf((char)QUESTION));
    Atom QuestAtom =  $.quote(String.valueOf((char)QUEST));
    Atom CommandAtom =  $.quote(String.valueOf((char)COMMAND));

    Atom Que = Atomic.atom(String.valueOf((char) QUESTION) + (char) QUEST);

    Term VAR_DEP_1 = $.varDep(1);
    Term VAR_DEP_2 = $.varDep(2);


    /**
     * fast, imprecise sort.  for cache locality and concurrency purposes
     */
    Comparator<Task> sloppySorter = Comparator
            .comparingInt((Task x) ->
                    //x.term()
                    x.term().concept()
                            .hashCode())
            .thenComparing((Task x) -> -x.priElseZero());

    static boolean equal(Task thiz, Object that) {
        return (thiz == that) ||
                ((that instanceof Task && thiz.hashCode() == that.hashCode() && Task.equal(thiz, (Task) that)));
    }

    /**
     * assumes identity and hash have been tested already.
     * <p>
     * if evidence is of length 1 (such as input or signal tasks,), the system
     * assumes that its ID is unique (or most likely unique)
     * and this becomes the only identity condition.
     * (start/stop and truth are not considered for equality)
     * this allows these values to mutate dynamically
     * while the system runs without causing hash or equality
     * inconsistency.  see hash()
     */
    static boolean equal(Task a, Task b) {

        byte p = a.punc();
        if (p != b.punc())
            return false;

        if (p == BELIEF || p == GOAL) {
            if (!a.truth().equals(b.truth())) return false;
        }

        long[] evidence = a.stamp();
        if ((!Arrays.equals(evidence, b.stamp())))
            return false;


        if ((a.start() != b.start()) || (a.end() != b.end()))
            return false;


        return a.term().equals(b.term());
    }

    static void fund(Task y, Task x, float factor, boolean copyOrMove) {
        Task.fund(y, new Task[] { x }, copyOrMove);
        y.priMult(factor);

//        //discount pri by increase in target complexity
//        float xc = xx.voluplexity(), yc = yy.voluplexity();
//        float priSharePct =
//                1f - (yc / (xc + yc));
//        yy.pri(0);
//        yy.take(xx, priSharePct * factor, false, copyOrMove);
    }

    static Task negIf(Task answer, boolean negate) {
        return negate ? Task.negated(answer) : answer;
    }

    /**
     * with most common defaults
     */
    static void merge(Task pp, Task tt) {
        merge(pp, tt, PriMerge.max);
    }
    static void merge(Task pp, Task tt, PriMerge merge) {
        merge(pp, tt, merge, null, true);
    }

    static float merge(final Task e, final Task i, PriMerge merge, @Nullable PriReturn returning, boolean updateCreationTime) {

        if (e == i)
            return 0;

        float y = merge.merge(e, i.pri(), returning);

        mergeWhy(e, i);

        if (e != null) {
            if (updateCreationTime) {
                long inCreation = i.creation();
                if (inCreation > e.creation())
                    e.setCreation(inCreation);
            }

            if (e.isCyclic() && !i.isCyclic())
                e.setCyclic(false);
        }

        return y;
    }

    static void mergeWhy(Task e, Task i) {
        if (e instanceof AbstractTask)
            ((AbstractTask) e).why(i.why());
        //else TODO
    }

    /**
     * see equals()
     */
    static int hash(Term term, Truth truth, byte punc, long start, long end, long[] stamp) {
        int h = Util.hashCombine(
                term.hashCode(),
                punc
        );

        //if (stamp.length > 1) {

        if (truth != null)
            h = Util.hashCombine(h, truth.hashCode());

        if (start != LongInterval.ETERNAL) {
            h = Util.hashCombine(h, start);
            if (end != start)
                h = Util.hashCombine(h, end);
        }
        //}

        if (stamp.length > 0)
            h = Util.hashCombine(h, stamp);

        if (h == 0)
            h = 1; //keep 0 as a special non-hashed indicator value

        return h;
    }

//    static StableBloomFilter<Task> newBloomFilter(int cap, Random rng) {
//        return new StableBloomFilter<>(
//                cap, 1, 0.0005f, rng,
//                new BytesHashProvider<>(IO::taskToBytes));
//    }

    static void proof(/*@NotNull*/Task task, int indent, /*@NotNull*/StringBuilder sb) {


        sb.append("  ".repeat(Math.max(0, indent)));
        task.appendTo(sb, true);
        sb.append("\n  ");


        if (task instanceof DerivedTask) {
            Task pt = ((DerivedTask) task).parentTask();
            if (pt != null) {

                proof(pt, indent + 1, sb);
            }

            Task pb = ((DerivedTask) task).parentBelief();
            if (pb != null) {

                proof(pb, indent + 1, sb);
            }
        }
    }

    static boolean validTaskTerm(@Nullable Term t) {
        return validTaskTerm(t, (byte) 0, true);
    }

    static boolean validTaskTerm(@Nullable Term t, byte punc, boolean safe) {

        if (t == null)
            return fail(null, "null content", false /* FORCE */);

        if (t instanceof Bool || t instanceof Variable)
            return fail(t, "bool or variable", safe);

        if (punc != COMMAND) {


//            if (!t.isNormalized()) {
//
//                @Nullable Term n = t.normalize();
//                if (!n.equals(t))
//                    return fail(t, "task target not a normalized Compound", safe);
//                else
//                    t = n;
//            }
        }

        Op o = t.op();

        if (!o.taskable)
            return fail(t, "not taskable", safe);

        if (t.hasAny(Op.VAR_PATTERN))
            return fail(t, "target has pattern variables", safe);

        if (!t.hasAny(Op.ATOM.bit | Op.INT.bit))
            return fail(t, "target has no substance", safe);

        if (punc == Op.BELIEF || punc == Op.GOAL) {
            if (t.hasVarQuery())
                return fail(t, "belief or goal with query variable", safe);
            if (t.hasXternal())
                return fail(t, "belief/goal content with dt=XTERNAL", safe);
        }


        if ((punc == Op.GOAL || punc == Op.QUEST) && t.hasAny(IMPL))
            return fail(t, "Goal/Quest task target may not be Implication", safe);

        return !(t instanceof Compound) || validTaskCompound((Compound) t, safe);
    }

    /**
     * call this directly instead of taskContentValid if the level, volume, and normalization have already been tested.
     * these can all be tested prenormalization, because normalization will not affect the result
     */
    static boolean validTaskCompound(Compound x, boolean safe) {
        if (!VarIndep.validIndep(x, safe)) {
            return fail(x, "invalid independent variable usage", safe);
        }
        return true;
    }

    static boolean fail(@Nullable Termlike t, String reason, boolean safe) {
        if (safe)
            return false;
        else
            throw new TaskException(reason, t);
    }

    @Nullable
    static Task clone(Task x, Term newContent) {
        return x.term().equals(newContent) ? x :
            clone(x, newContent, x.truth(), x.punc());
    }

    @Nullable
    static AbstractTask clone(Task x, byte newPunc) {
        return clone(x, x.term(), x.truth(), newPunc);
    }

    @Nullable
    static AbstractTask clone(Task x) {
        return clone(x, x.punc());
    }

    @Nullable
    static AbstractTask clone(Task x, Term newContent, Truth newTruth, byte newPunc) {
        return clone(x, newContent, newTruth, newPunc, x.start(), x.end());
    }

    @Nullable
    static AbstractTask clone(Task x, Term newContent, Truth newTruth, byte newPunc, long start, long end) {
        return clone(x, newContent, newTruth, newPunc, start, end, x.stamp());
    }
    @Nullable
    static AbstractTask clone(Task x, Term newContent, Truth newTruth, byte newPunc, long start, long end, long[] stamp) {

        Term c = Task.taskValid(newContent, newPunc, newTruth, false);
        AbstractTask y = NALTask.the(c, newPunc, newTruth, x.creation(), start, end, stamp);

        y.pri(x.pri());

        y.why(x.why());

//        if (x.target().equals(y.target()) && x.isCyclic())
//            y.setCyclic(true);

        return y;
    }

    @Deprecated @Nullable
    static <T extends Task> T tryTask(Term t, byte punc, Truth tr, BiFunction<Term, Truth, T> withResult) {
        return tryTask(t, punc, tr, withResult, !NAL.test.DEBUG_EXTRA);
    }

    @Deprecated @Nullable
    static <T extends Task> T tryTask(Term t, byte punc, Truth tr, BiFunction<Term, Truth, T> withResult, boolean safe) {
        if (punc == BELIEF || punc == GOAL) {
            if (tr == null)
                throw new TaskException("non-null truth required for belief or goal", t);
            if (tr.evi() < NAL.truth.EVI_MIN)
                throw new TaskException("insufficient evidence", t);
        } else {
            if (tr!=null)
                throw new TaskException("null truth required for questions or quests", t);
        }

        Term x = taskTerm(t, punc, safe);
        return x != null ? withResult.apply(x.unneg(), tr != null ? tr.negIf(x instanceof Neg) : null) : null;
    }

    @Nullable static Term taskValid(Term t, byte punc, Truth tr, boolean safe) {
        if (punc == BELIEF || punc == GOAL) {
            if (tr == null)
                throw new TaskException("non-null truth required for belief or goal", t);
            if (tr.evi() < NAL.truth.EVI_MIN)
                throw new TaskException("insufficient evidence", t);
        } else {
            if (tr!=null)
                throw new TaskException("null truth required for questions or quests", t);
        }

        return taskTerm(t, punc, safe);
    }
    /** validates and prepares a term for use as a task's content */
    static Term taskTerm(/*@NotNull*/Term t, byte punc, boolean safe) {

        boolean negated = (t instanceof Neg);
        if (negated)
            t = t.unneg();

        t = t.normalize();

        if (t instanceof Compound && NAL.TASK_COMPOUND_POST_NORMALIZE)
            t = Task.postNormalize((Compound)t);

        return Task.validTaskTerm(t/*.the()*/, punc, safe) ? Util.maybeEqual(t, t.negIf(negated)) : null;
    }

    static Compound postNormalize(Compound t) {

        int v = t.vars();
        //TODO VAR_QUERY and VAR_INDEP, including non-0th variable id
        if (v > 0 && t.hasAny(NEG.bit)) {
            ShortByteHashMap counts = new ShortByteHashMap(v);
            t.recurseTermsOrdered(Termlike::hasVars, x -> {
                if (x instanceof Neg) {
                    Term xu = x.unneg();
                    if (xu instanceof Variable) {
                        counts.addToValue(((NormalizedVariable)xu).i, (byte)-2); //-2 because +1 will be added when recursing inside it leaving net -1
                    }
                } else if (x instanceof Variable) {
                    counts.addToValue(((NormalizedVariable)x).i, (byte)+1);
                }
                return true;
            }, null);
            int cs = counts.size();
            if (cs == 1) {
                ShortBytePair ee = counts.keyValuesView().getOnly();
                if (ee.getTwo() < 0) {
                    Term vv = Intrin.term(ee.getOne());
                    return (Compound) t.replace(vv, vv.neg());
                }
            } else if (cs > 1) {
                counts.values().removeIf(c -> c >= 0); //keep only entries where more neg than positive. these will be flipped
                cs = counts.size();
                if (cs > 0) {
                    return (Compound) t.transform(new InvertVariableTransform(counts));
                }
            }
        }
        return t;
    }

    /**
     * start!=ETERNAL
     * TODO boolean eternalize=false
     */
    @Nullable
    static Task project(Task t, long start, long end, double eviMin, boolean ditherTruth, int dtDither, float dur, boolean eternalize, NAL n) {

        if (dtDither > 1) {
            start = Tense.dither(start, dtDither, -1);
            end = Tense.dither(end, dtDither, +1);
        }

        long ts = t.start();
        if ((ts == ETERNAL) || (ts == start && t.end() == end))
            return t;

        Truth tt;
        if (t.isBeliefOrGoal()) {

            //tt = t.truthRelative((start+end)/2, n.time(), eviMin);
            //if (tt == null) return null;

            tt = t.truth(start, end, dur, eternalize);

            if (tt == null || tt.evi() < eviMin) return null;

            if (ditherTruth) {
                Truth ttd = tt.dither(n);
                if (ttd == null || (ttd != tt && ttd.evi() < eviMin))
                    return null;
                tt = ttd;
            }

//            if (NAL.PROJECTION_EVIDENCE_INFLATION_DETECT) {
//                if (tt.evi() > t.evi()) {
//                    double inflationPct = (tt.evi() - t.evi()) / (t.evi());
//                    if (inflationPct >= NAL.PROJECTION_EVIDENCE_INFLATION_PCT_TOLERANCE) {
//                        throw new Truth.TruthException("inflation, %=", inflationPct);
//                    } /* else {
//                    //allow
//                }*/
//                }
//            }
        } else {
            tt = null;
        }

        return SpecialTruthAndOccurrenceTask.the(t, tt, start, end);
    }

    /**
     * creates negated proxy of a task
     */
    static Task negated(Task t) {
        if (t instanceof SpecialNegatedTask)
            return ((SpecialNegatedTask) t).task;
        else
            return new SpecialNegatedTask(t);
    }

    //    @Deprecated
//    @Nullable
//    static Task project(Task t, long start, long end, boolean ditherTruth, boolean negated, float eviMin, NAR n) {
//        if (!negated && t.start() == start && t.end() == end || (t.isBeliefOrGoal() && t.isEternal()))
//            return t;
//
////        if (trimIfIntersects) {
////            @Nullable long[] intersection = Longerval.intersectionArray(start, end, t.start(), t.end());
////            if (intersection != null) {
////
////                start = intersection[0];
////                end = intersection[1];
////            }
////
//////            start = Tense.dither(start, n);
//////            end = Tense.dither(end, n);
////        }
//
//
//        Truth tt;
//        if (t.isBeliefOrGoal()) {
//            tt = t.truth(start, end, n.dur());
//            if (tt == null || tt.evi() < eviMin)
//                return null;
//
//            if (ditherTruth) {
//                Truth ttd = tt.dither(n);
//                if (ttd == null || ttd.evi() < eviMin)
//                    return null;
//                tt = ttd;
//            }
//
//            tt = tt.negIf(negated);
//        } else {
//            tt = null;
//        }
//
//        return new SpecialTruthAndOccurrenceTask(t, start, end, negated, tt);
//    }

    static byte i(byte p) {
        switch (p) {
            case BELIEF:
                return 0;
            case QUESTION:
                return 1;
            case GOAL:
                return 2;
            case QUEST:
                return 3;
            default:
                return -1;
        }
    }

//    static Task eternalized(Task tx) {
//        return eternalized(tx, 1);
//    }

    static byte p(int index) {
        switch (index) {
            case 0:
                return BELIEF;
            case 1:
                return QUESTION;
            case 2:
                return GOAL;
            case 3:
                return QUEST;
            default:
                return -1;
        }
    }

    /**
     * TODO to be refined
     * TODO make Iterable<Task> x version so that callee's avoid constructing Task[] only for this */
    static void fund(Task y, Task[] x, boolean priCopyOrMove) {
        int volSum = Util.sum(TermedDelegate::volume, x);
        double volFactor =
                min(1, ((double)volSum) / y.volume() );

        double confFactor;
        boolean xHasTruth = x[0].isBeliefOrGoal();
        if (y.isBeliefOrGoal() && xHasTruth) {
            double yConf = y.truth().confDouble();
            //double xConfMax = Util.max(Task::conf, x);
            double xConfMean = Util.mean(Task::conf, x);
            confFactor = min(1, (yConf / xConfMean));
        } else {
            if (xHasTruth) {
                //question formation
                double xConfAvg = Util.mean(Task::conf, x);
                confFactor = Math.pow(1 - xConfAvg, 2);
                //confFactor = Math.pow(1 - xConfAvg, x.length);
            } else {
                confFactor = 1;
            }
        }

        double rangeFactor;
        if (y.isEternal())
            rangeFactor = 1;
        else {
            long xRangeMax = Util.max((Task t) -> t.rangeIfNotEternalElse(1), x);
            long yRange = y.range();
            rangeFactor = min(1, ((double) yRange) / xRangeMax);
        }

        //int Xn = x.length;
        //double priSum = Util.sumDouble(Task::priElseZero, x);
        //double priAvg = priSum / Xn;
        double priMean = Util.sum(Task::priElseZero, x)/x.length;
        float p = (float)(priMean * volFactor * confFactor * rangeFactor);

        float yp = Prioritizable.fund(p, priCopyOrMove, x);

        merge(y, x, yp);
    }

    static void merge(Task y, Task[] x, float yp) {
        y.pri(yp);


        if (y instanceof AbstractTask) {
            ((AbstractTask)y).why(Why.why(x, NAL.causeCapacity.intValue()));
        }

        if (Util.and(Task::isCyclic, x))
            y.setCyclic(true);

        assert(!y.isDeleted());
    }




    Truth truth();

    @Override default float freq() { return truth().freq(); }
    @Override default double evi() { return truth().evi(); }
    @Override default double confDouble() { return truth().confDouble(); }

    @Override
    default <X extends HyperRegion> Function<X, HyperRegion> mbrBuilder() {
        long s = start(), e = end();
        float tf = freq(), tc = conf();
        return y ->
                TasksRegion.mbr((TaskRegion) y, s, e, tf, tc);
//        {
//
//            if (y instanceof Task) {
//
//                Task ty = (Task) y;
//                float ef = ty.freq(), ec = ty.conf();
//                long es = ty.start(), ee = ty.end();
//
//                float f0, f1;
//                if (tf <= ef) {
//                    f0 = tf;
//                    f1 = ef;
//                } else {
//                    f0 = ef;
//                    f1 = tf;
//                }
//                float c0;
//                float c1;
//                if (tc <= ec) {
//                    c0 = tc;
//                    c1 = ec;
//                } else {
//                    c0 = ec;
//                    c1 = tc;
//                }
//                return new TasksRegion(Math.min(s, es), Math.max(e, ee),
//                        f0, f1, c0, c1
//                );
//
//            } else {
//                return TaskRegion.mbr((TaskRegion)y, this);
//            }
//
//        };
    }

    @Override
    default TaskRegion mbr(HyperRegion y) {
        if (this == y) return this;
        return TaskRegion.mbr((TaskRegion) y, this);
    }

    @Override
    default float freqMean() {
        return freq();
    }

    @Override
    default float freqMin() {
        return freqMean();
    }

    @Override
    default float freqMax() {
        return freqMean();
    }

    @Override
    default float confMean() {
        return conf();
    }

    @Override
    default float confMin() {
        return confMean();
    }

    @Override
    default float confMax() {
        return confMean();
    }

    @Override
    default Task task() {
        return this;
    }

    default boolean isQuestion() {
        return (punc() == QUESTION);
    }

    default boolean isBelief() {
        return (punc() == BELIEF);
    }

    default boolean isGoal() {
        return (punc() == GOAL);
    }

    default boolean isQuest() {
        return (punc() == QUEST);
    }

    default boolean isCommand() {
        return (punc() == COMMAND);
    }

    @Nullable
    default Appendable toString(boolean showStamp) {
        return appendTo(new StringBuilder(128), showStamp);
    }

    default boolean isQuestionOrQuest() {
        byte c = punc();
        return c == Op.QUESTION || c == Op.QUEST;
    }

    default boolean isBeliefOrGoal() {
        byte c = punc();
        return c == Op.BELIEF || c == Op.GOAL;
    }

    /**
     * for question tasks: when an answer appears.
     * <p>
     * <p>
     * return the input task, or a modification of it to use a customized matched premise belief. or null to
     * to cancel any matched premise belief.
     */
    @Nullable
    default Task onAnswered(/*@NotNull*/Task answer) {

        Task question = this;

//        if (!(question.isInput() && question.isEternal()) && !(answer.isInput() && answer.isEternal()) && !Stamp.overlap(question, answer)) {
//            answer.take(question, answer.priElseZero() * question.priElseZero(), true, false);
//        }

//        if(answer instanceof NALTask) {
//            mergeCause(answer, question, CauseMerge.AppendUnique);
//        }

//        n.emotion.onAnswer(this, answer);

        return answer;
    }

    default @Nullable StringBuilder appendTo(@Nullable StringBuilder sb) {
        return appendTo(sb, false);
    }

    default String toStringWithoutBudget() {
        return appendTo(new StringBuilder(64), true, false,
                false,
                false
        ).toString();

    }

    @Deprecated
    default StringBuilder appendTo(StringBuilder buffer, boolean showStamp) {
        boolean notCommand = punc() != Op.COMMAND;
        return appendTo(buffer, true, showStamp && notCommand,
                notCommand,
                showStamp
        );
    }

    default StringBuilder appendTo(@Nullable StringBuilder buffer, boolean term, boolean showStamp, boolean showBudget, boolean showLog) {

        String contentName = term ? term().toString() : "";

        CharSequence tenseString;


        appendOccurrenceTime(
                (StringBuilder) (tenseString = new StringBuilder()));


        CharSequence stampString = showStamp ? stampAsStringBuilder() : null;

        int stringLength = contentName.length() + tenseString.length() + 1 + 1;

        boolean hasTruth = isBeliefOrGoal();
        if (hasTruth)
            stringLength += 11;

        if (showStamp)
            stringLength += stampString.length() + 1;

        /*if (showBudget)*/

        stringLength += 1 + 6 + 1 + 6 + 1 + 6 + 1 + 1;


        if (buffer == null)
            buffer = new StringBuilder(stringLength);
        else {
            buffer.ensureCapacity(stringLength);
        }


        if (showBudget) {
            toBudgetStringExternal(buffer).append(' ');
        }

        buffer.append(contentName).append((char) punc());

        if (tenseString.length() > 0)
            buffer.append(' ').append(tenseString);

        if (hasTruth) {
            buffer.append(' ');
            Truth.appendString(buffer, 2, freq(), conf());

        }

        if (showStamp) {
            buffer.append(' ').append(stampString);
        }
        return buffer;
    }

    /**
     * Check if a Task is a direct input,
     * or if its origin has been forgotten or never known
     */
    default boolean isInput() {
        return !isCyclic() && stamp().length <= 1;
    }

    default boolean isEternal() {
        return start() == LongInterval.ETERNAL;
    }

    default int dt() {
        return term().dt();
    }


    @Nullable
    @Deprecated default Truth truth(long targetStart, long targetEnd, float dur) {
        return truth(targetStart, targetEnd, dur, false);
    }

    @Nullable default Truth truth(long qStart, long qEnd, float dur, boolean eternalize) {

        if (isEternal())
            return truth();
        else {

            double e = eviAvg(qStart, qEnd, dur, eternalize);

            return (e < NAL.truth.EVI_MIN) ?
                null :
                PreciseTruth.byEvi(
                        freq() /* TODO interpolate frequency wave */,
                        e);
        }
    }

    default double eviAvg(long qStart, long qEnd, float dur, boolean eternalize) {
        assert(qStart!=ETERNAL);
        long range = 1 + (qEnd - qStart);
        return TruthIntegration.eviAbsolute(this, qStart, qEnd, dur, eternalize) / range;
    }


    default double eviMean() {
        return evi();
    }

    default String proof() {
        StringBuilder sb = new StringBuilder(1024);
        return proof(sb).toString().trim();
    }

    //    default String proof(NAR n) {
//    }
    default StringBuilder proof(/*@NotNull*/StringBuilder temporary) {
        temporary.setLength(0);
        proof(this, 0, temporary);
        return temporary;
    }

    /**
     * auto budget by truth (if belief/goal, or punctuation if question/quest)
     */
    default Task budget(NAL nar) {
        return budget(1f, nar);
    }

    default Task budget(float factor, NAL nar) {
        priMax(factor * nar.priDefault(punc()));
        return this;
    }


    byte punc();


    default Task pri(NAL defaultPrioritizer) {
        return pri(defaultPrioritizer.priDefault(punc()));
    }


    /**
     * computes the average frequency during the given interval
     */
    default float freq(long start, long end) {
        return freq();
    }

    default boolean isBeliefOrGoal(boolean beliefOrGoal) {
        return beliefOrGoal ? isBelief() : isGoal();
    }


    final class InvertVariableTransform implements RecursiveTermTransform {
        private final ShortByteHashMap counts;

        public InvertVariableTransform(ShortByteHashMap counts) {
            this.counts = counts;
        }

        @Override
        public Term applyCompound(Compound c) {
            if (!c.hasVars()) return c; //TODO test by structure

            if (c instanceof Neg) {
                Term d = c.unneg();
                if (d instanceof Variable && invert((Variable)d))
                    return d;
            }

            return RecursiveTermTransform.super.applyCompound(c);
        }

        @Override
        public Term applyAtomic(Atomic a) {
            return a instanceof Variable && invert((Variable)a) ? a.neg() : a;
        }

        boolean invert(Variable d) {
            return counts.get(((NormalizedVariable) d).i) < 0;
        }
    }
}