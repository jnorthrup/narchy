package nars;

import jcog.Util;
import jcog.bloom.StableBloomFilter;
import jcog.bloom.hash.BytesHashProvider;
import jcog.list.FasterList;
import jcog.math.Longerval;
import jcog.pri.Pri;
import jcog.pri.Priority;
import nars.concept.Concept;
import nars.concept.Operator;
import nars.control.proto.TaskAddTask;
import nars.derive.Premise;
import nars.link.TaskLink;
import nars.link.Tasklinks;
import nars.task.*;
import nars.task.proxy.TaskWithNegatedTruth;
import nars.task.proxy.TaskWithTruthAndOccurrence;
import nars.task.util.InvalidTaskException;
import nars.task.util.TaskRegion;
import nars.term.Evaluation;
import nars.term.Term;
import nars.term.Termed;
import nars.term.var.VarIndep;
import nars.truth.PreciseTruth;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.Truthed;
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


    static boolean equal(Task thiz, Object that) {
        return (thiz == that) ||
                ((that instanceof Task && thiz.hashCode() == that.hashCode() && Task.equal(thiz, (Task)that)));
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

//        if (evidence.length > 1) {
            Truth at = a.truth();
            Truth bt = b.truth();
            if (at == null) {
                if (bt != null) return false;
            } else {
                if (bt == null || !at.equals(bt)) return false;
            }

            if ((a.start() != b.start()) || (a.end() != b.end()))
                return false;
//        }

        return a.term().equals(b.term());
    }

//    static @Nullable Task eviMax(@Nullable Task a, @Nullable Task b, long start, long end) {
//        if (b == null) {
//            return a;
//        } else {
//            if (a == null) {
//                return b;
//            } else {
//                boolean ae = a.isEternal();
//                boolean be = b.isEternal();
//                if (!ae && !be) {
//                    return (Revision.eviInteg(a, start, end, 1) >=
//                            Revision.eviInteg(b, start, end, 1)) ?
//                            a : b;
//                } else if (ae && be) {
//                    return a.evi() >= b.evi() ? a : b;
//                } else {
//                    //compare eternal vs. temporal
//                    if (start == ETERNAL) {
//                        //prefer the eternal result
//                        return ae ? a : b;
//                    } else {
//                        //prefer the temporal result
//                        return ae ? b : a;
//                    }
//
//                }
//            }
//        }
//    }

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
        //TODO StringBuilder

        for (int i = 0; i < indent; i++)
            sb.append("  ");
        task.appendTo(sb, true);
        sb.append("\n  ");


        if (task instanceof DerivedTask) {
            Task pt = ((DerivedTask) task).getParentTask();
            if (pt != null) {
                //sb.append("  PARENT ");
                proof(pt, indent + 1, sb);
            }

            Task pb = ((DerivedTask) task).getParentBelief();
            if (pb != null) {
                //sb.append("  BELIEF ");
                proof(pb, indent + 1, sb);
            }
        }
    }

    static StableBloomFilter<Task> newBloomFilter(int cap, Random rng) {
        return new StableBloomFilter<>(
                cap, 1, 0.0005f, rng,
                new BytesHashProvider<>(IO::taskToBytes));
    }

    static boolean validTaskTerm(@Nullable Term t) {
        return validTaskTerm(t, (byte) 0, true);
    }

    static boolean validTaskTerm(@Nullable Term t, byte punc, boolean safe) {

        if (t == null)
            return fail(t, "null content", false /* FORCE */);

        if (punc != COMMAND) {

//            if ((t = t.normalize()) == null)
//                return fail(t, "not normalizable", safe);

            if (!t.isNormalized()) {
                //HACK
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

//        if (nar != null) {
//            int maxVol = nar.termVolumeMax.intValue();
//            if (t.volume() > maxVol)
//                return fail(t, "task term exceeds maximum volume", safe);
//            int nalLevel = nar.nal();
//            if (!t.levelValid(nalLevel))
//                return fail(t, "task term exceeds maximum NAL level", safe);
//        }

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
        if (xo.atomic) {
            if (xo.conceptualizable)
                return true;
            return false; //var or else
        }

        /* A statement sentence is not allowed to have a independent variable as subj or pred"); */
        switch (x.varIndep()) {
            case 0:
                return true;  //OK
            case 1:
                return fail(x, "singular independent variable", safe);
            default:
                if (!x.hasAny(Op.StatementBits)) {
                    return fail(x, "InDep variables must be subterms of statements", safe);
                } else {
                    if (xo.statement)
                        return validIndepBalanced(x, safe);
                    else
                        return x.subterms().AND(s -> validTaskCompound(s, safe));
                }
        }

    }

    @Nullable
    static boolean validIndepBalanced(Term t, boolean safe) {

        //Trie<ByteList, ByteSet> m = new Trie(Tries.TRIE_SEQUENCER_BYTE_LIST);
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
                        return true; //skip the input term

                    if (indepVarOrStatement.op() == VAR_INDEP) {
                        indepVarPaths.getIfAbsentPut(((VarIndep) indepVarOrStatement).anonNum(),
                                () -> new FasterList<>(2))
                                .add(path.toImmutable());
                    } else {
                        statements.add(path.toImmutable());
                    }
//
//                        Term t = null; //root
//                        int pathLength = path.size();
//                        for (int i = -1; i < pathLength - 1 /* dont include the selected term itself */; i++) {
//                            t = (i == -1) ? comp : t.sub(path.get(i));
//
//                            if (t.op().indepVarParent) {
//                                byte branch = path.get(i + 1); //either 0 or 1
//                                byte branchBit = (byte) (1 << branch);
//                                m.addToValue(path.toImmutable(), branchBit); //would be nice: orToValue(..)
//                                //m.updateValue( , (previous) -> (byte) (previous | branchBit));
//                            }
//                        }

                    return true;
                });

        if (indepVarPaths.anySatisfy(p -> p.size() < 2))
            return false; //there is an indep variable that appears only once

        if (statements.size() > 1) {
            statements.sortThisByInt(PrimitiveIterable::size);
            //Comparator.comparingInt(PrimitiveIterable::size));
        }


        boolean rootIsStatement = t.op().statement;
        if (!indepVarPaths.allSatisfy((varPaths) -> {

            ByteByteHashMap count = new ByteByteHashMap();


            //byte 1 = which statement path, byte 2 = length down it
            int numVarPaths = varPaths.size();
            for (byte varPath = 0; varPath < numVarPaths; varPath++) {


                ByteList p = varPaths.get(varPath);
                if (rootIsStatement) {
                    byte branch = p.get(0);
                    if (Util.branchOr((byte) -1, count, branch) == 3)
                        return true; //valid
                }

                int pSize = p.size();
                byte statementNum = -1;


                nextStatement:
                for (int i1 = 0, statementsSize = statements.size(); i1 < statementsSize; i1++) {
                    ByteList statement = statements.get(i1);
                    statementNum++;
                    int statementPathLength = statement.size();
                    if (statementPathLength > pSize)
                        break; //since its sorted we know we dont have to try the remaining paths that go to deeper siblings

                    for (int i = 0; i < statementPathLength; i++) {
                        if (p.get(i) != statement.get(i))
                            break nextStatement; //mismatch
                    }

                    byte lastBranch = p.get(statementPathLength);
                    assert (lastBranch == 0 || lastBranch == 1) : lastBranch + " for path " + p + " while validating term: " + t;

                    //match
                    if (Util.branchOr(statementNum, count, lastBranch) == 3) {
                        return true; //VALID
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

    @Nullable static Task clone(Task x) {
        return clone(x, x.punc());
    }

    @Nullable
    static Task clone(Task x, Term newContent, Truth newTruth, byte newPunc) {
        return clone(x, newContent, newTruth, newPunc, x.creation(), x.start(), x.end());
    }

    @Nullable
    static Task clone(Task x, Term newContent, Truth newTruth, byte newPunc, long creation, long start, long end) {

        Task y = Task.tryTask(newContent, newPunc, newTruth, (c, t) ->
                new NALTask(c, newPunc, t,
                        creation, start, end,
                        x.stamp())
                        .cause(x.cause().clone()));
        if (y == null)
            return null;

        float xp = x.pri();
        if (xp == xp)
            y.priSet(xp); //otherwise leave zero

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
        if (x != null)
            return withResult.apply(x.getOne(), tr != null ? tr.negIf(x.getTwo()) : null);
        else
            return null;
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


        Op o = t.op();

        boolean negated = false;
//        boolean reduced, negated;
//        do {
//            reduced = true;


        if (o == NEG) {
            t = t.unneg();
            o = t.op();
            negated = !negated;
        }

        if (o.statement && t.hasAny(BOOL)) {
            fail(t, "statement term containing boolean", safe);
            return null;
        }

//            if (!t.hasAny(ConstantAtomics)) {
//                fail(t, "contains no constant atomics (ATOM | INT)", safe);
//                return null;
//            }

//            if (o == INH && t.hasAny(BOOL)) {
//                Term pred = t.sub(1);
//                if (pred.op() == BOOL) {
//                    if (pred == Null)
//                        return null;
//                    else {
//                        t = t.sub(0); //reduce to the subject
//                        o = t.op();
//
//                        if (pred == False)
//                            negated = !negated; //invert truth
//
//                        if (t.op()==NEG)
//                            reduced = false; //repeat to handle the possible contained reductions
//                    }
//
//                }
//            }
//
//        } while (!reduced);

        t = t.normalize().the();

        return Task.validTaskTerm(t, punc, safe) ? pair(t, negated) : null;
    }

    /**
     * creates lazily computing proxy task which facades the task to the target time range
     */
    static Task project(boolean flexible, @Nullable Task t, long subStart, long subEnd, TimeAware n, boolean negated) {

        if (!flexible && !(t.isEternal() || t.containedBy(subStart, subEnd))) {
            @Nullable Longerval intersection = Longerval.intersect(subStart, subEnd, t.start(), t.end());
            if (intersection != null) {
                //narrow to the intersecting region
                subStart = intersection.a;
                subEnd = intersection.b;
            }

            Truth ttt = t.truth(subStart, subEnd, n.dur());
            return (ttt != null) ?
                    new TaskWithTruthAndOccurrence(t, subStart, subEnd, negated, ttt.negIf(negated)) : null;
        }

        return negated ? Task.negated(t) : t; //dont project, but apply negate if necessary
    }

    /**
     * creates negated proxy of a task
     */
    static Task negated(@Nullable Task t) {
        return new TaskWithNegatedTruth(t);
    }

    static Task eternalized(Task tx) {
        return eternalized(tx, 1);
    }

    static Task eternalized(Task x, float eviFactor) {
        if (x == null)
            return null;

        //  non-proxy immutable impl
        @Nullable Task ete = Task.clone(x, x.term(),
                x.truth().eternalized(eviFactor),
                x.punc(),
                /* TODO current time, from NAR */ x.creation(),
                ETERNAL, ETERNAL
        );
        if (ete.isDeleted())
            ete.pri(0); //x was deleted

        return ete;

//        return new TaskProxy.WithTruthAndTime(
//                tx,
//                ETERNAL, ETERNAL,
//                false,
//                ttx -> ttx.truth().eternalized(eviFactor)
//        );

    }



    @Override
    default float freqMin() {
        return freq();
    }


    //    @Nullable
//    static boolean taskStatementValid(/*@NotNull*/Compound t, boolean safe) {
//        return taskStatementValid(t, (byte) 0, safe); //ignore the punctuation-specific conditions
//    }

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
     *
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


            long dist = minDistanceTo(when);
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

//    /**
//     * called if this task is entered into a concept's belief tables
//     * TODO what about for questions/quests
//     */
//    void feedback(TruthDelta delta, float deltaConfidence, float deltaSatisfaction, NAR nar);
//

    default boolean isBelief() {
        return (punc() == BELIEF);
    }

    default boolean isGoal() {
        return (punc() == GOAL);
    }

//    /** allows for budget feedback that occurrs on revision */
//    default boolean onRevision(Task conclusion) {
//        return true;
//    }

    default boolean isQuest() {
        return (punc() == QUEST);
    }

//    @Nullable
//    default Appendable appendTo(Appendable sb) throws IOException {
//        sb.append(appendTo(null));
//        return sb;
//    }

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
//        if (!(c instanceof TaskConcept)) {
//            throw new InvalidTaskException
//                    //System.err.println
//                    (this, "unconceptualized");
//        }
    }

    Task[] EmptyArray = new Task[0];


    /** sloppy pre-sort of premises by task/task_term,
     *  to maximize sequential repeat of derived task term */
    Comparator<? super Premise> sortByTaskSloppy =
            Comparator
                    .comparingInt((Premise a) -> a.task.hashCode())
                    .thenComparingInt((Premise a) -> a.task.term().hashCode())
                    .thenComparingInt((Premise a) -> System.identityHashCode(a.task));


//    @NotNull
//    default Task projectTask(long when, long now) {
//        Truth adjustedTruth = projectTruth(when, now, false);
//        long occ = occurrence();
//        long projOcc = (adjustedTruth instanceof ProjectedTruth) ? ((ProjectedTruth)adjustedTruth).when : occ;
//        return /*occ == projOcc &&*/ adjustedTruth.equals(truth()) ? this :
//                MutableTask.project(this, adjustedTruth, now, projOcc);
//
//    }


//    /** get the absolute time of an event subterm, if present, TIMELESS otherwise */
//    default long subtermTimeAbs(Term x) {
//        long t = subtermTime(x);
//        if (t == TIMELESS) return TIMELESS;
//        return t + occurrence();
//    }

//    /** relevant time of an event subterm (or self), if present, TIMELESS otherwise */
//    default long subtermTime(Term x) {
//        return term().subtermTime(x, x.t());
//    }


//    default float projectionConfidence(long when, long now) {
//        //TODO avoid creating Truth Values by calculating the confidence directly. then use this in projection's original usage as well
//
//        float factor = TruthFunctions.temporalProjection(getOccurrenceTime(), when, now);
//
//        return factor * getConfidence();
//
//        //return projection(when, now).getConfidence();
//    }


//    final class Solution extends AtomicReference<Task> {
//        Solution(Task referent) {
//            super(referent);
//        }
//
//        @NotNull
//        @Override
//        public String toString() {
//            return "Solved: " + get();
//        }
//    }

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

//        if (isInput()) {
//
//            Concept concept = concept(nar, true);
//            if (concept != null) {
//                //shared by both questions and quests per concept
//                PLinkArrayBag<Twin<Task>> answers = concept.meta("?", (x) ->
//                        new PLinkArrayBag<Twin<Task>>(Param.taskMerge, Param.ANSWER_BAG_CAPACITY)
//                );
//
//                Twin<Task> qa = twin(this, answer);
//                PLink<Twin<Task>> p = new PLink<>(qa,
//                        (this.priElseZero()) * (answer.conf()));
//                PriReference<Twin<Task>> r = answers.commit().put(p);
//                if (Param.DEBUG_REPORT_ANSWERS && r == p) {
//                    //added
//                    nar.input(Operator.log(nar.time(), qa.getOne() + "  " + qa.getTwo()));
//                }
//
//            }
//        }

//        Task forward = meta("@");
//        long s, e;
//        int dur = nar.dur();
//        if (forward == null || (forward != answer && forward.conf(s = start(), e = end(), dur) < answer.conf(s, e, dur))) {
//            meta("@", answer); //forward to the top answer if this ever gets deleted
//        }

//        return answer;
//    }

    default @Nullable StringBuilder appendTo(@Nullable StringBuilder sb /**@Nullable*/) {
        return appendTo(sb, false);
    }

    @NotNull
    @Deprecated
    default String toStringWithoutBudget() {
        return appendTo(new StringBuilder(64), true, false,
                false, //budget
                false//log
        ).toString();

    }

    @NotNull
    @Deprecated
    default StringBuilder appendTo(StringBuilder buffer, /**@Nullable*/boolean showStamp) {
        boolean notCommand = punc() != Op.COMMAND;
        return appendTo(buffer, true, showStamp && notCommand,
                notCommand, //budget
                showStamp //log
        );
    }

    @NotNull
    default StringBuilder appendTo(@Nullable StringBuilder buffer, /**@Nullable*/boolean term, boolean showStamp, boolean showBudget, boolean showLog) {

        String contentName = term ? term().toString() : "";

        CharSequence tenseString;
//        if (memory != null) {
//            tenseString = getTense(memory.time());
//        } else {
        //TODO dont bother craeting new StringBuilder and calculating the entire length etc.. just append it to a reusable StringReader?
        appendOccurrenceTime(
                (StringBuilder) (tenseString = new StringBuilder()));
//        }


        CharSequence stampString = showStamp ? stampAsStringBuilder() : null;

        int stringLength = contentName.length() + tenseString.length() + 1 + 1;

        boolean hasTruth = isBeliefOrGoal();
        if (hasTruth)
            stringLength += 11;

        if (showStamp)
            stringLength += stampString.length() + 1;

        /*if (showBudget)*/
        //"$0.8069;0.0117;0.6643$ "
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

        //return evidence().length <= 1;
        //return (getParentTask() == null);
        //return (evidence().length <= 1) && ;
    }

    default boolean isEternal() {
        return start() == ETERNAL;
    }

    default int dt() {
        return term().dt();
    }


//    /**
//     * prints this task as a TSV/CSV line.  fields:
//     * Compound
//     * Punc
//     * Freq (blank space if quest/question)
//     * Conf (blank space if quest/question)
//     * Start
//     * End
//     */
//    default void appendTSV(Appendable a) throws IOException {
//
//        char sep = '\t'; //','
//
//        a
//                .append(term().toString()).append(sep).append("\"").append(String.valueOf(punc())).append("\"").append(sep)
//                .append(truth() != null ? Texts.n2(truth().freq()) : " ").append(sep)
//                .append(truth() != null ? Texts.n2(truth().conf()) : " ").append(sep)
//                .append(!isEternal() ? Long.toString(start()) : " ").append(sep)
//                .append(!isEternal() ? Long.toString(end()) : " ").append(sep)
//                .append(proof().replace("\n", "  ")).append(sep)
//                .append('\n');
//
//    }

    @Nullable
    default Truth truth(long targetStart, long targetEnd, int dur) {



        float eve = Revision.eviAvg(this, targetStart, targetEnd, dur);

        if (eve > Param.TRUTH_MIN_EVI) {
            return new PreciseTruth(
                freq() /* TODO interpolate frequency wave */,
                eve, false);

            //quantum entropy uncertainty:
//                float ff = freq();
//                ff = (float) Util.unitize(
//                        (ThreadLocalRandom.current().nextFloat() - 0.5f) *
//                                2f * Math.pow((1f-conf),4) + ff);
//                return $.t(ff, conf);
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
        if (ll!=null)
            ll.add(entry);

        return this;
    }

    @Nullable
    default List log(boolean createIfMissing) {
        return null; //default: no log ability
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

        if (Param.CAUSE_MULTIPLY_EVERY_TASK) {
            float amp = n.amp(this);
            priMult(amp, Pri.EPSILON);
        }


        Term x = term();

        //invoke dynamic functors and apply aliases
        //Term y = x.eval(n.concepts.functors);

        //this might be overkill
        Set<ITask> yy = new LinkedHashSet<>(4);
        Evaluation.solve(x, n.functors,
                _y -> {
                    preProcess(n, _y, yy);
                    return true;
                });

        switch (yy.size()) {
            case 0:
                return null;
            case 1:
                return yy.iterator().next();
            default:
                //HACK use some kind of iterator
                return new NativeTask.NARTask((nn) -> yy.forEach(z -> z.run(nn)));
        }

    }

    @Nullable
    default void preProcess(NAR n, Term y, Collection<ITask> queue) {


        boolean cmd = isCommand();


        Term x = term();
        if (!x.equals(y)) {

            //clone a new task because it has changed


            if (Operator.func(y).equals(Evaluation.TRUE)) {
                if (isQuestionOrQuest()) {

                    y = Operator.arg(y, 0); //unwrap

                    //convert to final implicit answer
                    byte p = isQuestion() ? BELIEF : GOAL;

                    @Nullable Task result = clone(this, y, $.t(1f, n.confDefault(p)), p);


                    if (result != null) {
                        //delete();
                        float pBeforeDrain = priElseZero();
                        pri(0); //drain pri from here
                        queue.add(inputStrategy(result));
                        //HACK tasklink question to answer
                        queue.add(new NativeTask.NARTask(nn -> {
                            Tasklinks.linkTask(
                                    new TaskLink.GeneralTaskLink(result, nn, pBeforeDrain / 2f),
                                    concept(nn, true).tasklinks(),
                                    null);
                            //TaskLinkTask( result, pri(), concept(n, true))
                        }));
                    } else {
                        //TODO maybe print error, at least in debug mode
                    }

                } else{
                    //belief or goal boolean verified truth
                    y = Operator.arg(y, 0);
                    if (!y.equals(x)) {
                        Task tc = clone(this, y, truth(), punc());
                        if (!equals(tc)) {
                            queue.add(inputStrategy(tc));
                        }
                    }
                    return;
                }
            } else {
                @Nullable ObjectBooleanPair<Term> yy = tryContent(y, punc(),
                        //false
                        !isInput() || !Param.DEBUG_EXTRA
                );
                        /* the evaluated result here acts as a memoization of possibly many results
                           depending on whether the functor is purely static in which case
                           it would be the only one.
                         */
                //TODO see if a TaskProxy would work here
                if (yy!=null) {
                    Term yyz = yy.getOne();
                    @Nullable Task result;
//                    if (yyz.subterms().OR(qx -> qx.op().var)) {
                        //still seem to be variables, continue as a revised question
                        result = clone(this, yyz.negIf(yy.getTwo()));
//                    } else {
//                        //top-level variables seem eliminated. convert to belief with default truth
//                        byte p = isQuestion() ? BELIEF : GOAL;
//                        result = clone(this, yyz.negIf(yy.getTwo()), $.t(1,n.confDefault(p)), p);
//                    }

                    if (result!=null) {
                        pri(0); //drain pri from here
                        queue.add(inputStrategy(result));

//                        //HACK tasklink question to answer
//                        queue.add(new NativeTask.NARTask(nn -> {
//                            Tasklinks.linkTask(
//                                    new TaskLink.GeneralTaskLink(result, nn, priElseZero()),
//                                    concept(nn, true).tasklinks(),
//                                    null);
//                            //TaskLinkTask( result, pri(), concept(n, true))
//                        }));
                    }
                }
            }
        }

        if (!cmd) {
            queue.add(inputStrategy(this)); //probably should be added first
        }

        //invoke possible Operation

        if (cmd || (isGoal() && !isEternal())) {
            //resolve possible functor in goal or command
            //TODO question functors
            //the eval step producing 'y' above will have a reference to any resolved functor concept
            Pair<Operator, Term> o = Op.functor(y, (i) -> {
                Concept operation = n.concept(i);
                return operation instanceof Operator ? (Operator) operation : null;
            });
            if (o != null) {
                try {
                    //TODO add a pre-test guard here to avoid executing a task which will be inconsequential anyway
                    Task yy = o.getOne().execute.apply(this, n);
                    if (yy != null && !this.equals(yy)) {
                        queue.add(yy);
                    }
                } catch (Throwable xtt) {
                    //n.logger.error("{} {}", this, t);
                    queue.add(Operator.error(this, xtt, n.time()));
                    return;
                }
                if (cmd) {
                    n.eventTask.emit(this);
                    return;
                }
                //otherwise: allow processing goal
            }
        }

        if (cmd) {
            //default: Echo
            n.out(term());
        }

    }

    default ITask inputStrategy(Task result) {
        return new TaskAddTask(result);
    }

//    @Override
//    default boolean intersectsConf(float cMin, float cMax) {
//        float c = conf();
//        return c >= cMin && c <= cMax;
//    }
//
//    @Override
//    default boolean containsConf(float cMin, float cMax) {
//        float c = conf();
//        return c >= cMin && c <= cMax;
//    }



    /**
     * TODO cause should be merged if possible when merging tasks in belief table or otherwise
     */
    short[] cause();




    /** evaluate the midpoint value of every pair of times, and then multiply by x area between them */
    default float eviIntegRectMid(long dur, long... times) {
        float e = 0;
        for (int i = 1, timesLength = times.length; i < timesLength; i++) {
            long a = times[i-1];
            long b = times[i];
            assert(b>a);
            long ab = (a+b)/2L;
            e += (b-a) * evi(ab, dur);
        }
        return e;
    }


    /** maybe */
//    default float eviIntegPieceWise(long dur, long... times) {
////        if (times.length == 2) {
////            return evi(times[0], dur)
////        }
//        throw new TODO();
//    }

    /**
     * https://www.intmath.com/integration/5-trapezoidal-rule.php
     * long[] points needs to be sorted, unique, and not contain any ETERNALs
     */
    default float eviIntegTrapezoidal(long dur, long... times) {


        int n = times.length;
        long last = times[n - 1];
        long first = times[0];

        assert (first < last
                && first != ETERNAL && first != XTERNAL
                /*&& last != ETERNAL */&& last != XTERNAL);

        float X = 1 + (last - first);
        float dx = X / n;

        //area = dx * (y0/2 + y1 + y2 ... + yn/2)
        float e = 0; //evidence sum
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
     * fluent form of setPri which returns this class
     */
    default Task pri(float p) {
        priSet(p);
        return this;
    }

    default Task pri(NAR defaultPrioritizer) {
        return pri(defaultPrioritizer.priDefault(punc()));
    }

    /** computes the average frequency during the given interval */
    float freq(long start, long end);

//    default float freqMean(int dur, long... when) {
//
//        assert (when.length > 1);
//
//        float fSum = 0;
//        int num = 0;
//        for (long w : when) {
//            float tf = freq(w, dur);
//            if (tf == tf) {
//                fSum += tf;
//                num++;
//            }
//        }
//        if (num == 0)
//            return Float.NaN;
//        return fSum / num;
//    }

}