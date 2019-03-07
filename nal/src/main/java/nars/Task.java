package nars;

import jcog.TODO;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.math.Longerval;
import jcog.pri.UnitPrioritizable;
import nars.control.op.Perceive;
import nars.subterm.Subterms;
import nars.task.DerivedTask;
import nars.task.ITask;
import nars.task.NALTask;
import nars.task.UnevaluatedTask;
import nars.task.proxy.SpecialNegatedTermTask;
import nars.task.proxy.SpecialTruthAndOccurrenceTask;
import nars.task.util.TaskException;
import nars.task.util.TaskRegion;
import nars.term.Variable;
import nars.term.*;
import nars.term.atom.Bool;
import nars.term.util.transform.VariableTransform;
import nars.term.var.VarIndep;
import nars.truth.PreciseTruth;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.Truthed;
import nars.truth.polation.TruthIntegration;
import nars.truth.util.EvidenceEvaluator;
import org.eclipse.collections.api.PrimitiveIterable;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.eclipse.collections.impl.map.mutable.primitive.ByteByteHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.ByteObjectHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import static nars.Op.*;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

/**
 * NAL Task to be processed, consists of a Sentence, stamp, time, and budget.
 */
@SuppressWarnings("ALL")
public interface Task extends Truthed, Stamp, TermedDelegate, ITask, TaskRegion, UnitPrioritizable {


    Task[] EmptyArray = new Task[0];

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
            Truth at = a.truth();
            Truth bt = b.truth();
            if (!at.equals(bt)) return false;
        }

        long[] evidence = a.stamp();
        if ((!Arrays.equals(evidence, b.stamp())))
            return false;


        if ((a.start() != b.start()) || (a.end() != b.end()))
            return false;


        return a.term().equals(b.term());
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

        if (start != ETERNAL) {
            h = Util.hashCombine(h, start);
            if (end != start)
                h = Util.hashCombine(h, end);
        }
        //}

        if (stamp.length > 0)
            h = Util.hashCombine(h, stamp);

        return h;
    }


    static void proof(/*@NotNull*/Task task, int indent, /*@NotNull*/StringBuilder sb) {


        for (int i = 0; i < indent; i++)
            sb.append("  ");
        task.appendTo(sb, true);
        sb.append("\n  ");


        if (task instanceof DerivedTask) {
            Task pt = ((DerivedTask) task).getParentTask();
            if (pt != null) {

                proof(pt, indent + 1, sb);
            }

            Task pb = ((DerivedTask) task).getParentBelief();
            if (pb != null) {

                proof(pb, indent + 1, sb);
            }
        }
    }

//    static StableBloomFilter<Task> newBloomFilter(int cap, Random rng) {
//        return new StableBloomFilter<>(
//                cap, 1, 0.0005f, rng,
//                new BytesHashProvider<>(IO::taskToBytes));
//    }

    static boolean validTaskTerm(@Nullable Term t) {
        return validTaskTerm(t, (byte) 0, true);
    }

    static boolean validTaskTerm(@Nullable Term t, byte punc, boolean safe) {

        if (t == null)
            return fail(t, "null content", false /* FORCE */);

        if (t instanceof Bool || t instanceof Variable)
            return fail(t, "bool or variable", safe);

        if (punc != COMMAND) {


            if (!t.isNormalized()) {

                @Nullable Term n = t.normalize();
                if (!n.equals(t))
                    return fail(t, "task target not a normalized Compound", safe);
            }
        }

        Op o = t.op();

        if (!o.taskable)
            return fail(t, "not taskable", safe);

        if (t.hasAny(Op.VAR_PATTERN))
            return fail(t, "target has pattern variables", safe);

        if (!t.hasAny(Op.ATOM.bit | Op.INT.bit | Op.varBits))
            return fail(t, "target has no substance", safe);

        if (punc == Op.BELIEF || punc == Op.GOAL) {
            if (t.hasVarQuery())
                return fail(t, "belief or goal with query variable", safe);
            if (t.hasXternal())
                return fail(t, "belief/goal content with dt=XTERNAL", safe);
        }


        if ((punc == Op.GOAL || punc == Op.QUEST) && !!t.hasAny(IMPL))
            return fail(t, "Goal/Quest task target may not be Implication", safe);

        return !(t instanceof Compound) || validTaskCompound((Compound) t, safe);
    }

    /**
     * call this directly instead of taskContentValid if the level, volume, and normalization have already been tested.
     * these can all be tested prenormalization, because normalization will not affect the result
     */
    static boolean validTaskCompound(Compound x, boolean safe) {
        return validIndep(x, safe);
    }

    private static boolean validIndep(Term x, boolean safe) {
        /* A statement sentence is not allowed to have a independent variable as subj or pred"); */
        switch (x.varIndep()) {
            case 0:
                return true;
            case 1:
                return fail(x, "singular independent variable", safe);
            default:
                if (!x.hasAny(Op.StatementBits)) {
                    return fail(x, "InDep variables must be subterms of statements", safe);
                } else {
                    Subterms xx = x.subterms();
                    if (x.op().statement && xx.AND(Termlike::hasVarIndep)) {
                        return validIndepBalance(x, safe); //indep appearing in both, test for balance
                    } else {
                        return xx.AND(s -> validIndep(s, safe));
                    }
                }
        }

    }

    static boolean validIndepBalance(Term t, boolean safe) {


        FasterList</* length, */ ByteList> statements = new FasterList<>(4);
        ByteObjectHashMap<List<ByteList>> indepVarPaths = new ByteObjectHashMap<>(4);

        t.pathsTo(
                (Term x) -> {
                    Op xo = x.op();
                    return (xo.statement && x.varIndep() > 0) || (xo == VAR_INDEP);
                },
                x -> x.hasAny(Op.StatementBits | Op.VAR_INDEP.bit),
                (ByteList path, Term indepVarOrStatement) -> {
                    if (!path.isEmpty()) {
                        if (indepVarOrStatement.op() == VAR_INDEP) {
                            indepVarPaths.getIfAbsentPut(((VarIndep) indepVarOrStatement).id,
                                    () -> new FasterList<>(2))
                                    .add(path.toImmutable());
                        } else {
                            statements.add(path.toImmutable());
                        }
                    }
                    return true;
                });

        if (indepVarPaths.anySatisfy(p -> p.size() < 2))
            return false;

        if (statements.size() > 1) {
            statements.sortThisByInt(PrimitiveIterable::size);

        }


        boolean rootIsStatement = t.op().statement;
        if (!indepVarPaths.allSatisfy((varPaths) -> {

            ByteByteHashMap count = new ByteByteHashMap();

            for (ByteList p : varPaths) {


                if (rootIsStatement) {
                    byte branch = p.get(0);
                    if (Util.branchOr((byte) -1, count, branch) == 3)
                        return true;
                }

                int pSize = p.size();
                byte statementNum = -1;


                nextStatement:
                for (ByteList statement : statements) {
                    statementNum++;
                    int statementPathLength = statement.size();
                    if (statementPathLength > pSize)
                        break;

                    for (int i = 0; i < statementPathLength; i++) {
                        if (p.get(i) != statement.get(i))
                            break nextStatement;
                    }

                    byte lastBranch = p.get(statementPathLength);
                    assert (lastBranch == 0 || lastBranch == 1) : lastBranch + " for path " + p + " while validating target: " + t;


                    if (Util.branchOr(statementNum, count, lastBranch) == 3) {
                        return true;
                    }
                }
            }
            return false;
        })) {
            return fail(t, "InDep variables must be balanced across a statement", safe);
        }
        return true;
    }

    private static boolean fail(@Nullable Term t, String reason, boolean safe) {
        if (safe)
            return false;
        else
            throw new TaskException(t, reason);
    }

    @Nullable
    static NALTask clone(Task x, Term newContent) {
        return clone(x, newContent, x.truth(), x.punc());
    }

    @Nullable
    static NALTask clone(Task x, byte newPunc) {
        return clone(x, x.term(), x.truth(), newPunc);
    }

    @Nullable
    static NALTask clone(Task x) {
        return clone(x, x.punc());
    }

    @Nullable
    static NALTask clone(Task x, Term newContent, Truth newTruth, byte newPunc) {
        return clone(x, newContent, newTruth, newPunc, x.start(), x.end());
    }

    @Nullable
    static NALTask clone(Task x, Term newContent, Truth newTruth, byte newPunc, long start, long end) {
        return clone(x, newContent, newTruth, newPunc, (c, t) -> NALTask.the(c, newPunc, t, x.creation(), start, end, x.stamp()));
    }

    @Nullable
    static <T extends Task> T clone(Task x, Term newContent, BiFunction<Term, Truth, T> taskBuilder) {
        return clone(x, newContent, x.truth(), x.punc(), taskBuilder);
    }

    @Nullable
    static <T extends Task> T clone(Task x, Term newContent, Truth newTruth, byte newPunc, BiFunction<Term, Truth, T> taskBuilder) {

        T y = Task.tryTask(newContent, newPunc, newTruth, taskBuilder);
        if (y == null)
            return null;

        float xp = x.priElseZero();
        y.pri(xp);

        ((NALTask) y).cause(x.cause()/*.clone()*/);

//        if (x.target().equals(y.target()) && x.isCyclic())
//            y.setCyclic(true);

        return y;
    }

    @Nullable
    static <T extends Task> T tryTask(Term t, byte punc, Truth tr, BiFunction<Term, Truth, T> withResult) {
        return tryTask(t, punc, tr, withResult, !Param.DEBUG_EXTRA);
    }

    @Nullable
    static <T extends Task> T tryTask(Term t, byte punc, Truth tr, BiFunction<Term, Truth, T> withResult, boolean safe) {
        if (punc == BELIEF || punc == GOAL) {
            if (tr == null)
                throw new TaskException(t, "null truth but required for belief or goal");
            if (tr.evi() < Float.MIN_NORMAL /*Truth.EVI_MIN*/)
                throw new TaskException(t, "insufficient evidence");
        }

        ObjectBooleanPair<Term> x = tryContent(t, punc, safe);
        return x != null ? withResult.apply(x.getOne(), tr != null ? tr.negIf(x.getTwo()) : null) : null;
    }

    /**
     * attempts to prepare a target for use as a Task content.
     *
     * @return null if unsuccessful, otherwise the resulting compound target and a
     * boolean indicating whether a truth negation occurred,
     * necessitating an inversion of truth when constructing a Task with the input target
     */
    @Nullable
    static ObjectBooleanPair<Term> tryContent(/*@NotNull*/Term t, byte punc, boolean safe) {

        t = t.normalize();

        Op o = t.op();

        boolean negated;
        if (o == NEG) {
            t = t.unneg();
            negated = true;
        } else {
            negated = false;
        }

        return Task.validTaskTerm(t/*.the()*/, punc, safe) ? pair(t, negated) : null;
    }


    @Nullable static Task project(Task t, long start, long end, NAR n) {
        return project(t, start, end, false, false, false, n);
    }

    @Nullable static Task project(Task t, long start, long end, boolean trimIfIntersects, boolean ditherTruth, boolean negated, NAR n) {
        return project(t, start, end, trimIfIntersects, ditherTruth, negated, 0, n);
    }

    @Nullable static Task project(Task t, long start, long end, boolean trimIfIntersects, boolean ditherTruth, boolean negated, float eviMin, NAR n) {
        if (!negated && t.start() == start && t.end() == end || (t.isBeliefOrGoal() && t.isEternal()))
            return t;

        if (trimIfIntersects) {
            @Nullable long[] intersection = Longerval.intersectionArray(start, end, t.start(), t.end());
            if (intersection != null) {

                start = intersection[0];
                end = intersection[1];
            }

//            start = Tense.dither(start, n);
//            end = Tense.dither(end, n);
        }


        Truth tt;
        if (t.isBeliefOrGoal()) {
            tt = t.truth(start, end, n.dur());
            if (tt == null)
                return null;

            if (ditherTruth) {
                tt = tt.dither(n);
                if (tt == null)
                    return null;
            }
            if (eviMin > 0 && tt.evi() < eviMin)
                return null;
            tt = tt.negIf(negated);
        } else {
            tt = null;
        }

        return new SpecialTruthAndOccurrenceTask(t, start, end, negated, tt);
    }


    /**
     * creates negated proxy of a task
     */
    static Task negated(@Nullable Task t) {
        if (t instanceof SpecialNegatedTermTask) {
            return ((SpecialNegatedTermTask) t).task;
        }
        return new SpecialNegatedTermTask(t);
    }

//    static Task eternalized(Task tx) {
//        return eternalized(tx, 1);
//    }

    /**
     * leave n null to avoid dithering
     */
    static Task eternalized(Task x, float eviFactor, float eviMin, @Nullable NAR n) {
        boolean isEternal = x.isEternal();
        boolean hasTruth = x.isBeliefOrGoal();
        if (isEternal) {
            if (eviFactor != 1)
                throw new TODO();
            if (hasTruth) {
                if (x.evi() < eviMin)
                    return null;
            }
            return x;
        }

        Truth tt;

        if (hasTruth) {
            tt = x.truth().eternalized(eviFactor, eviMin, n);
            if (tt == null)
                return null;
        } else {
            tt = null;
        }

        byte punc = x.punc();

        return Task.clone(x, x.term(),
                tt,
                punc,
                /* TODO current time, from NAR */
                (c, t) ->
                        new UnevaluatedTask(c, punc, t,
                                x.creation(), ETERNAL, ETERNAL,
                                x.stamp()
                        )
        );
    }

    static Term forceNormalizeForBelief(Term x) {
        x = x.normalize();

        if (x instanceof Compound && !validTaskCompound((Compound) x, true)) {
            x = VariableTransform.indepToDepVar(x).normalize();
        }

        return x;
    }

    static int i(byte p) {
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


    @Override
    default float freqMin() {
        return freq();
    }

    @Override
    default float freqMean() {
        return freq();
    }

    @Override
    default float freqMax() {
        return freq();
    }

    @Override
    default float confMin() {
        return conf();
    }

    @Override
    default float confMax() {
        return conf();
    }


    default EvidenceEvaluator eviEvaluator() {
        return EvidenceEvaluator.the(this);
    }

    /**
     * POINT EVIDENCE
     * <p>
     * amount of evidence measured at a given point in time with a given duration window
     * <p>
     * WARNING check that you arent calling this with (start,end) values
     *
     * @param when   time
     * @param dur    duration period across which evidence can decay before and after its defined start/stop time.
     *               if (dur <= 0) then no extrapolation is computed
     * @param minEvi used to fast fail if the result will not exceed the value
     * @return value >= 0 indicating the evidence
     */
    default float evi(long when, final int dur) {
        return EvidenceEvaluator.the(this).evi(when, dur);
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
    default Task onAnswered(/*@NotNull*/Task answer, NAR n) {

        Task question = this;

        answer.take(question, answer.priElseZero() * question.priElseZero(), true, false);

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

        String finalLog;
        if (showLog) {
            Object ll = lastLogged();

            finalLog = (ll != null ? ll.toString() : null);
            if (finalLog != null)
                stringLength += finalLog.length() + 1;
            else
                showLog = false;
        } else
            finalLog = null;


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

        if (showLog) {
            buffer.append(' ').append(finalLog);
        }

        return buffer;
    }

    /**
     * Check if a Task is a direct input,
     * or if its origin has been forgotten or never known
     */
    default boolean isInput() {
        return stamp().length <= 1 && !isCyclic();
    }

    default boolean isEternal() {
        return start() == ETERNAL;
    }

    default int dt() {
        return term().dt();
    }


    @Nullable
    default Truth truth(long targetStart, long targetEnd, int dur) {

        if (isEternal())
            return truth();
        else {

            float eve = TruthIntegration.eviAvg(this, targetStart, targetEnd, dur);

            if (eve > Param.TRUTH_EVI_MIN) {
                return PreciseTruth.byEvi(
                        freq() /* TODO interpolate frequency wave */,
                        eve);

            }
            return null;
        }
    }

    @Nullable
    default Truth truth(long when, int dur) {
        return truth(when, when, dur);
    }

    /**
     * append an entry to this task's log history
     * useful for debugging but can also be applied to meta-analysis
     * ex: an entry might be a String describing a change in the story/history
     * of the Task and the reason for it.
     */
    default Task log(Object entry) {
        if (!Param.DEBUG_TASK_LOG)
            return this;

        List ll = log(true);
        if (ll != null)
            ll.add(entry);

        return this;
    }

    @Nullable
    default List log(boolean createIfMissing) {
        return null;
    }

    @Nullable
    default Object lastLogged() {
        List log = log(false);
        if (log == null) return null;
        int s = log.size();
        if (s == 0) return null;
        else return log.get(s - 1);
    }

    default String proof() {
        StringBuilder sb = new StringBuilder(4096);
        return proof(sb).toString().trim();
    }

    default StringBuilder proof(/*@NotNull*/StringBuilder temporary) {
        temporary.setLength(0);
        proof(this, 0, temporary);
        return temporary;
    }

    /**
     * auto budget by truth (if belief/goal, or punctuation if question/quest)
     */
    default Task budget(NAR nar) {
        return budget(1f, nar);
    }

    default Task budget(float factor, NAR nar) {
        priMax(factor * nar.priDefault(punc()));
        return this;
    }

    @Override
    default float expectation() {
        return Truthed.super.expectation();
    }

    default float expectation(long when, int dur) {
        return expectation(when, when, dur);
    }

    default float expectation(long start, long end, int dur) {
        Truth t = truth(start, end, dur);
        return t == null ? Float.NaN : t.expectation();
    }

    default ITask next(NAR n) {
        return Perceive.perceive(this, n);
    }


    /**
     * TODO cause should be merged if possible when merging tasks in belief table or otherwise
     */
    short[] cause();


//    /**
//     * evaluate the midpoint value of every pair of times, and then multiply by x area between them
//     */
//    default float eviIntegRectMid(long dur, long... times) {
//        float e = 0;
//        for (int i = 1, timesLength = times.length; i < timesLength; i++) {
//            long a = times[i - 1];
//            long b = times[i];
//            assert (b > a);
//            long ab = (a + b) / 2L;
//            e += (b - a) * evi(ab, dur);
//        }
//        return e;
//    }


    /**
     * maybe
     */


    byte punc();

    /**
     * fluent form of pri(x) which returns this class
     */
    default Task priSet(float p) {
        this.pri(p);
        return this;
    }

    default Task pri(NAR defaultPrioritizer) {
        return priSet(defaultPrioritizer.priDefault(punc()));
    }

    /**
     * computes the average frequency during the given interval
     */
    float freq(long start, long end);


    default boolean isBeliefOrGoal(boolean beliefOrGoal) {
        return beliefOrGoal ? isBelief() : isGoal();
    }


}