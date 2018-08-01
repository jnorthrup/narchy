package nars;

import jcog.Util;
import jcog.bloom.StableBloomFilter;
import jcog.bloom.hash.BytesHashProvider;
import jcog.data.list.FasterList;
import jcog.math.Longerval;
import jcog.pri.Priority;
import nars.concept.Concept;
import nars.concept.Operator;
import nars.control.proto.Remember;
import nars.op.stm.ConjClustering;
import nars.subterm.Subterms;
import nars.task.*;
import nars.task.proxy.TaskWithNegatedTruth;
import nars.task.proxy.TaskWithTruthAndOccurrence;
import nars.task.util.InvalidTaskException;
import nars.task.util.TaskRegion;
import nars.term.*;
import nars.term.atom.Bool;
import nars.term.var.VarIndep;
import nars.time.Tense;
import nars.truth.PreciseTruth;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.Truthed;
import nars.truth.func.NALTruth;
import nars.truth.polation.TruthIntegration;
import nars.util.TimeAware;
import org.eclipse.collections.api.PrimitiveIterable;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.eclipse.collections.impl.map.mutable.primitive.ByteByteHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.ByteObjectHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;

import static nars.Op.*;
import static nars.time.Tense.XTERNAL;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

/**
 * NAL Task to be processed, consists of a Sentence, stamp, time, and budget.
 */
public interface Task extends Truthed, Stamp, Termed, ITask, TaskRegion, Priority {


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

        if (a.punc() != b.punc())
            return false;

        long[] evidence = a.stamp();
        if ((!Arrays.equals(evidence, b.stamp())))
            return false;


        Truth at = a.truth();
        Truth bt = b.truth();
        if (at == null) {
            if (bt != null) return false;
        } else {
            if (!at.equals(bt)) return false;
        }

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

        if (stamp.length == 1) {
            h = Util.hashCombine(h, Long.hashCode(stamp[0]));
        } else {

            if (truth != null)
                h = Util.hashCombine(h, truth.hashCode());

            if (start != ETERNAL) {
                h = Util.hashCombine(h,
                        Long.hashCode(start),
                        Long.hashCode(end)
                );
            }

            h = Util.hashCombine(h, Arrays.hashCode(stamp));

        }

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

    static StableBloomFilter<Task> newBloomFilter(int cap, Random rng) {
        return new StableBloomFilter<>(
                cap, 1, 0.0005f, rng,
                new BytesHashProvider<>(IO::taskToBytes));
    }

    static boolean taskConceptTerm(@Nullable Term t) {
        return taskConceptTerm(t, (byte) 0, true);
    }

    static boolean taskConceptTerm(@Nullable Term t, byte punc, boolean safe) {

        if (t == null)
            return fail(t, "null content", false /* FORCE */);

        if (t instanceof Bool || t instanceof Variable)
            return fail(t, "bool or variable", safe);

        if (punc != COMMAND) {


            if (!t.isNormalized()) {

                @Nullable Term n = t.normalize();
                if (!n.equals(t))
                    return fail(t, "task term not a normalized Compound", safe);
            }
        }

        Op o = t.op();

        if (o == NEG || !o.conceptualizable)
            return fail(t, "not conceptualizable", safe);

        if (t.hasAny(Op.VAR_PATTERN))
            return fail(t, "term has pattern variables", safe);

        if (!t.hasAny(Op.ATOM.bit | Op.INT.bit | Op.varBits))
            return fail(t, "term has no substance", safe);

        if (punc == Op.BELIEF || punc == Op.GOAL) {
            if (t.hasVarQuery())
                return fail(t, "belief or goal with query variable", safe);
            if (t.hasXternal())
                return fail(t, "belief/goal content with dt=XTERNAL", safe);
        }


        if ((punc == Op.GOAL || punc == Op.QUEST) && !goalable(t))
            return fail(t, "Goal/Quest task term may not be Implication", safe);

        return o.atomic || validTaskCompound(t, safe);
    }

    /**
     * call this directly instead of taskContentValid if the level, volume, and normalization have already been tested.
     * these can all be tested prenormalization, because normalization will not affect the result
     */
    static boolean validTaskCompound(Term x, boolean safe) {
        Op xo = x.op();
        return xo.atomic ? xo.conceptualizable : validIndep(x, safe);
    }

    static boolean validIndep(Term x, boolean safe) {
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

    @Nullable
    static boolean validIndepBalance(Term t, boolean safe) {


        FasterList</* length, */ ByteList> statements = new FasterList<>(4);
        ByteObjectHashMap<List<ByteList>> indepVarPaths = new ByteObjectHashMap<>(4);

        t.pathsTo(
                x -> {
                    Op xo = x.op();
                    return (xo.statement && x.varIndep() > 0) || (xo == VAR_INDEP) ? x : null;
                },
                x -> x.hasAny(Op.StatementBits | Op.VAR_INDEP.bit),
                (ByteList path, Term indepVarOrStatement) -> {
                    if (path.isEmpty())
                        return true;

                    if (indepVarOrStatement.op() == VAR_INDEP) {
                        indepVarPaths.getIfAbsentPut(((VarIndep) indepVarOrStatement).anonNum(),
                                () -> new FasterList<>(2))
                                .add(path.toImmutable());
                    } else {
                        statements.add(path.toImmutable());
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

            for (ByteList p: varPaths) {


                if (rootIsStatement) {
                    byte branch = p.get(0);
                    if (Util.branchOr((byte) -1, count, branch) == 3)
                        return true;
                }

                int pSize = p.size();
                byte statementNum = -1;


                nextStatement:
                for (ByteList statement: statements) {
                    statementNum++;
                    int statementPathLength = statement.size();
                    if (statementPathLength > pSize)
                        break;

                    for (int i = 0; i < statementPathLength; i++) {
                        if (p.get(i) != statement.get(i))
                            break nextStatement;
                    }

                    byte lastBranch = p.get(statementPathLength);
                    assert (lastBranch == 0 || lastBranch == 1) : lastBranch + " for path " + p + " while validating term: " + t;


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
            throw new InvalidTaskException(t, reason);
    }

    @Nullable
    static Task clone(Task x, Term newContent) {
        return clone(x, newContent, x.truth(), x.punc());
    }

    @Nullable
    static Task clone(Task x, byte newPunc) {
        return clone(x, x.term(), x.truth(), newPunc);
    }

    @Nullable
    static Task clone(Task x) {
        return clone(x, x.punc());
    }

    @Nullable
    static Task clone(Task x, Term newContent, Truth newTruth, byte newPunc) {
        return clone(x, newContent, newTruth, newPunc, x.start(), x.end());
    }

    @Nullable
    static Task clone(Task x, Term newContent, Truth newTruth, byte newPunc, long start, long end) {
        return clone(x, newContent, newTruth, newPunc, (c, t) ->
                new NALTask(c, newPunc, t,
                        x.creation(), start, end,
                        x.stamp()
                ));
    }

    @Nullable
    static Task clone(Task x, Term newContent, Truth newTruth, byte newPunc, BiFunction<Term, Truth, Task> taskBuilder) {

        Task y = Task.tryTask(newContent, newPunc, newTruth, taskBuilder);
        if (y == null)
            return null;

        float xp = x.priElseZero();
        y.pri(xp);

        ((NALTask) y).cause(x.cause().clone());

        return y;
    }

    static Task tryTask(Term t, byte punc, Truth tr, BiFunction<Term, Truth, ? extends Task> withResult) {
        return tryTask(t, punc, tr, withResult, !Param.DEBUG_EXTRA);
    }

    @Nullable
    static Task tryTask(Term t, byte punc, Truth tr, BiFunction<Term, Truth, ? extends Task> withResult, boolean safe) {
        if ((punc == BELIEF || punc == GOAL) && tr.evi() < Float.MIN_NORMAL /*Truth.EVI_MIN*/)
            throw new InvalidTaskException(t, "insufficient evidence");

        ObjectBooleanPair<Term> x = tryContent(t, punc, safe);
        return x != null ? withResult.apply(x.getOne(), tr != null ? tr.negIf(x.getTwo()) : null) : null;
    }

    /**
     * attempts to prepare a term for use as a Task content.
     *
     * @return null if unsuccessful, otherwise the resulting compound term and a
     * boolean indicating whether a truth negation occurred,
     * necessitating an inversion of truth when constructing a Task with the input term
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

        return Task.taskConceptTerm(t/*.the()*/, punc, safe) ? pair(t, negated) : null;
    }

    /**
     * creates lazily computing proxy task which facades the task to the target time range
     */
    static Task project(boolean flexible, @Nullable Task t, long subStart, long subEnd, NAR n, boolean negated) {

        if (!flexible && !(t.isEternal() || t.containedBy(subStart, subEnd))) {
            @Nullable Longerval intersection = Longerval.intersect(subStart, subEnd, t.start(), t.end());
            if (intersection != null) {

                subStart = intersection.a;
                subEnd = intersection.b;
            }

            subStart = Tense.dither(subStart, n);
            subEnd = Tense.dither(subEnd, n);
            Truth tt = t.truth(subStart, subEnd, n.dur());

            return (tt != null) ?
                    new TaskWithTruthAndOccurrence(t, subStart, subEnd, negated, tt.negIf(negated)) : null;
        }

        return negated ? Task.negated(t) : t;
    }

    /**
     * creates negated proxy of a task
     */
    static Task negated(@Nullable Task t) {
        return new TaskWithNegatedTruth(t);
    }

//    static Task eternalized(Task tx) {
//        return eternalized(tx, 1);
//    }

    static Task eternalized(Task x, float eviFactor, float eviMin, NAR n) {
        Truth tt = x.truth().eternalized(eviFactor, eviMin, n);
        if (tt == null)
            return null;
        return Task.clone(x, x.term(),
                tt,
                x.punc(),
                /* TODO current time, from NAR */
                (c, t) ->
                        new UnevaluatedTask(c, x.punc(), t,
                                x.creation(), ETERNAL, ETERNAL,
                                x.stamp()
                        )
        );
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

    /**
     * POINT EVIDENCE
     * <p>
     * amount of evidence measured at a given point in time with a given duration window
     * <p>
     * WARNING check that you arent calling this with (start,end) values
     *
     * @param when time
     * @param dur  duration period across which evidence can decay before and after its defined start/stop time.
     *             if (dur <= 0) then no extrapolation is computed
     * @return value >= 0 indicating the evidence
     */
    default float evi(long when, final long dur) {

        long s = start();
        if (s == ETERNAL) {
            return evi();
        } else if (when == ETERNAL) {
            return eviEternalized();
        } else {


            long dist = minTimeTo(when);
            if (dist == 0) {
                return evi();
            } else {
                if (dur == 0) {
                    return 0;
                } else {
                    return (float) Param.evi(evi(), dist, dur);
                }
            }

        }

    }


    @Override
    @NotNull
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

    @Nullable
    default Concept concept(/*@NotNull*/NAR n, boolean conceptualize) {
        return n.concept(term(), conceptualize);


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
    default Task onAnswered(/*@NotNull*/Task answer, /*@NotNull*/TimeAware timeAware) {
        return answer;
    }


    default @Nullable StringBuilder appendTo(@Nullable StringBuilder sb ) {
        return appendTo(sb, false);
    }

    @NotNull
    @Deprecated
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


        float eve = TruthIntegration.eviAvg(this, targetStart, targetEnd, dur);

        if (eve > Param.TRUTH_MIN_EVI) {
            return PreciseTruth.byEvi(
                    freq() /* TODO interpolate frequency wave */,
                    eve);


        }
        return null;
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
        StringBuilder sb = new StringBuilder(512);
        return proof(sb).toString();
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
        if (t == null) return Float.NaN;
        return t.expectation();
    }

    default ITask next(NAR n) {


        Collection<ITask> yy;

        Term x = term();

        boolean needEval = Evaluation.possiblyNeedsEval(x);
        if (!needEval) {
            yy = new FasterList<>(1);
            preProcess(n, x, yy);
        } else {
            yy = new LinkedHashSet<>(4);
            Evaluation.solve(null, x, false, n::functor,
                    z -> {
                        if (z.volume() < n.termVolumeMax.intValue()) {
                            preProcess(n, z, yy);
                        }
                        return true;
                    });

        }
        return postProcess(yy, needEval);
    }

    @Nullable
    default ITask postProcess(Collection<ITask> yy, boolean needEval) {
        switch (yy.size()) {
            case 0:
                return null;
            case 1:
                return needEval ? yy.iterator().next() : ((List<ITask>)yy).get(0); /* avoid creating iterator */
            default:
                return new AbstractTask.NARTask((nn) -> yy.forEach(z -> z.run(nn)));
        }
    }

    default void preProcess(NAR n, Term y, Collection<ITask> queue) {


        boolean cmd = isCommand();


        Term x = term();
        if (!x.equals(y)) {


//            if (Operator.func(y).equals(TRUE)) {
//                throw new UnsupportedOperationException();
//                if (isQuestionOrQuest()) {
//
//                    y = Operator.arg(y, 0);
//
//
//                    byte p = isQuestion() ? BELIEF : GOAL;
//
//                    @Nullable Task result = clone(this, y, $.t(1f, n.confDefault(p)), p);
//
//
//                    if (result != null) {
//
//                        float pBeforeDrain = priElseZero();
//                        pri(0);
//                        queue.add(inputStrategy(result));
//
//                        queue.add(new NativeTask.NARTask(nn -> {
//                            Tasklinks.linkTask(
//                                    new TaskLink.GeneralTaskLink(result, nn, pBeforeDrain / 2f),
//                                    concept(nn, true).tasklinks(),
//                                    null);
//
//                        }));
//                    } else {
//
//                    }
//
//                } else {
//
//                    y = Operator.arg(y, 0);
//                    if (!y.equals(x)) {
//                        Task tc = clone(this, y, truth(), punc());
//                        if (!equals(tc)) {
//                            //queue.add(inputStrategy(tc));
//                            queue.add(tc);
//                        }
//                    }
//
//                }
          /*  } else*/
            {
                @Nullable ObjectBooleanPair<Term> yy = tryContent(y, punc(),

                        !isInput() || !Param.DEBUG_EXTRA
                );
                        /* the evaluated result here acts as a memoization of possibly many results
                           depending on whether the functor is purely static in which case
                           it would be the only one.
                         */

                if (yy != null) {
                    Term yyz = yy.getOne();
                    @Nullable Task result;


                    result = clone(this, yyz.negIf(yy.getTwo()));


                    if (result != null) {
                        //queue.add(inputStrategy(result));
                        //pri(0);

                        queue.add(result);
                        return; //transformed
                    }
                }
            }
        }

        if (!cmd) {
            queue.add(inputStrategy(this, n));
        }


        if (cmd || (isGoal() && !isEternal())) {


            Pair<Operator, Term> o = Functor.ifFunc(y, (i) -> {
                Concept operation = n.concept(i);
                return operation instanceof Operator ? (Operator) operation : null;
            });
            if (o != null) {
                try {

                    Task yy = o.getOne().model.apply(this, n);
                    if (yy != null && !this.equals(yy)) {
                        queue.add(yy);
                    }
                } catch (Throwable xtt) {

                    queue.add(Operator.error(this, xtt, n.time()));
                    return;
                }
                if (cmd) {
                    n.eventTask.emit(this);
                    return;
                }

            }

            if (cmd) {

                n.out(term());
            }
        }

        if (Param.AUTO_DECOMPOSE_CONJ && op()==CONJ && (isBelief() || isGoal())) {
            if (!(this instanceof ConjClustering.STMClusterTask)) { //HACK
                Truth reducedTruth = NALTruth.StructuralDeduction.apply(truth(), null, n, n.confMin.floatValue());
                if (reducedTruth != null) {
                    long s = start();
                    long range = end() - s;
                    List<Task> subTasks = new FasterList(2);
                    term().eventsWhile((when, what) -> {
                        assert(!what.equals(term()));

                        Task t = Task.clone(this, what, reducedTruth, punc(), when, when + range);

                        if (t!=null)
                            subTasks.add(t);
                        return true;
                    }, s, true, true, false, 0);
                    if (!subTasks.isEmpty()) {
                        int ns = subTasks.size();
                        float p = priElseZero() / ns;
                        for (Task ss : subTasks) {
                            ss.pri(0);
                            ss.take(this, p, true, true);
                            ss.setCyclic(true);
                        }
                        queue.addAll(subTasks);
                    }
                }
            }
        }

    }

    default ITask inputStrategy(Task result, NAR n) {
        return Remember.the(result, n);
    }


    /**
     * TODO cause should be merged if possible when merging tasks in belief table or otherwise
     */
    short[] cause();


    /**
     * evaluate the midpoint value of every pair of times, and then multiply by x area between them
     */
    default float eviIntegRectMid(long dur, long... times) {
        float e = 0;
        for (int i = 1, timesLength = times.length; i < timesLength; i++) {
            long a = times[i - 1];
            long b = times[i];
            assert (b > a);
            long ab = (a + b) / 2L;
            e += (b - a) * evi(ab, dur);
        }
        return e;
    }


    /** maybe */


    /**
     * https:
     * long[] points needs to be sorted, unique, and not contain any ETERNALs
     */
    default float eviIntegTrapezoidal(long dur, long... times) {


        int n = times.length; assert(n > 1);
        long last = times[n - 1];
        long first = times[0];

        assert (first < last
                && first != ETERNAL && first != XTERNAL
                /*&& last != ETERNAL */ && last != XTERNAL);

        float X = 1 + (last - first);
        float dx = X / n;


        float e = 0;
        e += evi(first, dur) / 2;
        e += evi(last, dur) / 2;
        for (int i = 1, timesLength = times.length - 1; i < timesLength; i++) {
            long ti = times[i];
            if (!(ti != ETERNAL && ti != XTERNAL && ti > times[i - 1] && ti < times[i + 1]))
                throw new RuntimeException("invalid time point for evi integration");
            e += evi(ti, dur);
        }

        return dx * e; /* area */
    }

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


}