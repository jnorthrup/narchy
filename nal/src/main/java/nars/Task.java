package nars;

import jcog.Util;
import jcog.bag.impl.PLinkArrayBag;
import jcog.bloom.StableBloomFilter;
import jcog.bloom.hash.BytesHashProvider;
import jcog.list.FasterList;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import jcog.pri.Priority;
import nars.concept.Concept;
import nars.control.proto.TaskAdd;
import nars.concept.Operator;
import nars.task.DerivedTask;
import nars.task.ITask;
import nars.task.NALTask;
import nars.task.TaskProxy;
import nars.task.util.InvalidTaskException;
import nars.task.util.TaskRegion;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Bool;
import nars.term.var.VarIndep;
import nars.truth.PreciseTruth;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.Truthed;
import org.eclipse.collections.api.PrimitiveIterable;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.eclipse.collections.impl.map.mutable.primitive.ByteByteHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.ByteObjectHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;

import static nars.Op.*;
import static nars.truth.TruthFunctions.w2cSafe;
import static org.eclipse.collections.impl.tuple.Tuples.twin;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

/**
 * NAL Task to be processed, consists of a Sentence, stamp, time, and budget.
 */
public interface Task extends Truthed, Stamp, Termed, ITask, TaskRegion, jcog.data.map.MetaMap, Priority {


    Task[] EmptyArray = new Task[0];

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

        if (evidence.length > 1) {
            Truth at = a.truth();
            Truth bt = b.truth();
            if (at == null) {
                if (bt != null) return false;
            } else {
                if (bt == null || !at.equals(bt)) return false;
            }

            if ((a.start() != b.start()) || (a.end() != b.end()))
                return false;
        }

        if (a.term().equals(b.term())) {

//            //clear cyclic state if either is not cyclic
//            boolean ac = a.isCyclic();
//            boolean bc = b.isCyclic();
//            if (!ac && bc)
//                b.setCyclic(false);
//            else if (ac && !bc)
//                a.setCyclic(false);

            return true;
        }
        return false;
    }

    static @Nullable Task eviMax(@Nullable Task a, @Nullable Task b, long start, long end) {
        if (b == null) {
            return a;
        } else {
            if (a == null) {
                return b;
            } else {
                return (a.evi() > b.evi(start, end, 1)) ?
                        a : b;
            }
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
    static boolean validTaskCompound(Term t, boolean safe) {
        /* A statement sentence is not allowed to have a independent variable as subj or pred"); */

//        if (t.varDep()==1) {
//            return fail(t, "singular dependent variable", safe);
//        }

        switch (t.varIndep()) {
            case 0:
                break;  //OK
            case 1:
                return fail(t, "singular independent variable", safe);
            default:
                if (!t.hasAny(Op.StatementBits)) {
                    return fail(t, "InDep variables must be subterms of statements", safe);
                } else {


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
                                        .add( path.toImmutable() );
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

                    if (indepVarPaths.anySatisfy(p->p.size() < 2))
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


                }
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
    static NALTask clone(Task x, Term newContent) {
        return clone(x, newContent, x.truth(), x.punc());
    }

    @Nullable
    static NALTask clone(Task x, byte newPunc) {
        return clone(x, x.term(), x.truth(), newPunc);
    }

    @Nullable
    static NALTask clone(Task x, Term newContent, Truth newTruth, byte newPunc) {
        return clone(x, newContent, newTruth, newPunc, x.creation(), x.start(), x.end());
    }

    @Nullable
    static NALTask clone(Task x, Term newContent, Truth newTruth, byte newPunc, long creation, long start, long end) {

        NALTask y = (NALTask) Task.tryTask(newContent, newPunc, newTruth, (c, t)->
            new NALTask(c, newPunc,
                t,
                creation, start, end,
                x.stamp()));
        if (y == null)
            return null;

        float xp = x.pri();
        if (xp == xp)
            y.priSet(xp); //otherwise leave zero

        short[] xc = x.cause();
        if (xc.length > 0)
            y.cause = xc.clone(); //clone necessary?

        //y.meta.putAll(x.meta());
        return y;
    }


    //    @Nullable
//    static boolean taskStatementValid(/*@NotNull*/Compound t, boolean safe) {
//        return taskStatementValid(t, (byte) 0, safe); //ignore the punctuation-specific conditions
//    }


    static Task tryTask(Term t, byte punc, Truth tr, BiFunction<Term, Truth, ? extends Task> withResult) {
        return tryTask(t, punc, tr, withResult, !Param.DEBUG_EXTRA);
    }

    @Nullable
    static Task tryTask(Term t, byte punc, Truth tr, BiFunction<Term, Truth, ? extends Task> withResult, boolean safe) {
        if ((punc == BELIEF || punc == GOAL) && tr.evi() < Truth.EVI_MIN)
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
     * amount of evidence measured at a given time with a given duration window
     * <p>
     * WARNING check that you arent calling this with (start,end) values
     *
     * @param when time
     * @param dur  duration period across which evidence can decay before and after its defined start/stop time.
     *             if (dur <= 0) then no extrapolation is computed
     *
     * @return value >= 0 indicating the evidence
     */
    default float evi(long when, final long dur) {


        long s = start();


        if (s == ETERNAL) {
            return evi();
        } else if (when == ETERNAL) {
            return eternalizability() * eviEternalized();
        } else {

            float cw = evi();

            long dist = minDistanceTo(when);
            if (dist > 0) {
                float ete = eternalizability();
                float ecw = ete > 0 ? eviEternalized() * ete : 0;
                cw = ecw + dur > 0 ?
                        (float) Param.evi(
                            cw - ecw /* delta to eternalization, >= 0 */,
                                dist, dur)
                        :
                        0;
            }

            return cw;
        }

    }


    default float eternalizability() {

        return 1f; //always
        //return 0.5f; //some
        //return punc()==BELIEF ? 1f: 0f; //always if belief
        //return 0f; //never

//        Term t = term();
//        return t.op().temporal || t.vars() > 0 ? 1f : 0f;

        //return term().vars() > 0 ? 1f : 0f;
        //return term().vars() > 0 ? 1f : 0.5f;
        //return term().varIndep() > 0 ? 1f: 0f;

//        Term t = term();
//        return t.varIndep() > 0 || t.op() == IMPL ?
//                //0.5f + 0.5f * polarity()
//                polarity()
//                    : 0f;

        //return true;
        //return op().temporal;


        //Op op = term.op();
        //return op ==IMPL || op ==EQUI || term.vars() > 0;
        //return op.statement || term.vars() > 0;
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

//    /**
//     * called if this task is entered into a concept's belief tables
//     * TODO what about for questions/quests
//     */
//    void feedback(TruthDelta delta, float deltaConfidence, float deltaSatisfaction, NAR nar);
//

    default boolean isGoal() {
        return (punc() == GOAL);
    }

    default boolean isQuest() {
        return (punc() == QUEST);
    }

//    /** allows for budget feedback that occurrs on revision */
//    default boolean onRevision(Task conclusion) {
//        return true;
//    }

    default boolean isCommand() {
        return (punc() == COMMAND);
    }

//    @Nullable
//    default Appendable appendTo(Appendable sb) throws IOException {
//        sb.append(appendTo(null));
//        return sb;
//    }

    @Nullable
    default Appendable toString(boolean showStamp) {
        return appendTo(new StringBuilder(32), showStamp);
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


    default boolean isQuestOrQuestion() {
        byte c = punc();
        return c == Op.QUESTION || c == Op.QUEST;
    }


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
    default Task onAnswered(/*@NotNull*/Task answer, /*@NotNull*/NAR nar) {
        if (isInput()) {

            Concept concept = concept(nar, true);
            if (concept != null) {
                //shared by both questions and quests per concept
                PLinkArrayBag<Twin<Task>> answers = concept.meta("?", (x) ->
                        new PLinkArrayBag<Twin<Task>>(Param.taskMerge, Param.ANSWER_BAG_CAPACITY)
                );

                Twin<Task> qa = twin(this, answer);
                PLink<Twin<Task>> p = new PLink<>(qa,
                        (this.priElseZero()) * (answer.conf()));
                PriReference<Twin<Task>> r = answers.commit().put(p);
                if (Param.DEBUG_REPORT_ANSWERS && r == p) {
                    //added
                    nar.input(Operator.log(nar.time(), qa.getOne() + "  " + qa.getTwo()));
                }

            }

        }

        Task forward = meta("@");
        long s, e;
        int dur = nar.dur();
        if (forward == null || (forward != answer && forward.conf(s = start(), e = end(), dur) < answer.conf(s, e, dur))) {
            meta("@", answer); //forward to the top answer if this ever gets deleted
        }

        return answer;
    }


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
        return stamp().length <=1 && !isCyclic();

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

    default float conf(long start, long end, long dur) {
        return w2cSafe(evi(start, end, dur));
//        float cw = evi(start, end, dur);
//        assert (cw == cw);
//        return cw > 0 ? w2cSafe(cw) : Float.NaN;
    }

    default float conf(long when, long dur) {
        return conf(when, when, dur);
    }


    @Nullable
    default Truth truth(long when, long dur, NAR nar) {
        Truth t = truth(when, dur, nar.confMin.floatValue());
        if (t == null)
            return t;
        return t.dither(nar);
    }

    @Nullable
    default Truth truth(long targetStart, long targetEnd, long dur, float minConf) {
        return truth(nearestPointExternal(targetStart, targetEnd), dur, minConf);
    }

    default float evi(long targetStart, long targetEnd, final long dur) {
        return evi(nearestPointExternal(targetStart, targetEnd), dur);
    }

    @Nullable
    default Truth truth(long when, long dur, float minConf) {
        float eve = evi(when, dur);
        if (eve == eve && w2cSafe(eve) >= minConf) {

            return new PreciseTruth(freq(), eve, false);

            //quantum entropy uncertainty:
//                float ff = freq();
//                ff = (float) Util.unitize(
//                        (ThreadLocalRandom.current().nextFloat() - 0.5f) *
//                                2f * Math.pow((1f-conf),4) + ff);
//                return $.t(ff, conf);
        }
        return null;
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

    /**
     * append an entry to this task's log history
     * useful for debugging but can also be applied to meta-analysis
     * ex: an entry might be a String describing a change in the story/history
     * of the Task and the reason for it.
     */
    @NotNull
    default Task log(Object entry) {
        if (!Param.DEBUG_TASK_LOG)
            return this;

        log(true).add(entry);
        return this;
    }

    @Nullable
    default List log(boolean createIfMissing) {
        if (createIfMissing)
            return meta("!", (x) -> new FasterList(1));
        else
            return meta("!");
    }

    @Nullable
    default Object lastLogged() {
        List log = log(false);
        return log == null || log.isEmpty() ? null : log.get(log.size() - 1);
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

    default ITask run(NAR n) {


        n.emotion.onInput(this, n);


        Term x = term();

        //invoke dynamic functors and apply aliases
        Term y = x.eval(n.concepts.functors);

        if (!x.equals(y)) {

            //clone a new task because it has changed

            Task result;
            if (y instanceof Bool && isQuestOrQuestion()) {
                //convert to final implicit answer
                byte p = isQuestion() ? BELIEF : GOAL;

                @Nullable NALTask finalResult = clone(this, x, $.t(y == True ? 1f : 0f, n.confDefault(p)), p);

                delete();

                if (finalResult != null) {
                    return new TaskAdd(finalResult);
                } else {
                    //TODO maybe print error, at least in debug mode
                    return null;
                }

            } else {
                if (y.op() == Op.BOOL)
                    return null;

                @Nullable ObjectBooleanPair<Term> yy = tryContent(y, punc(),
                        false
                        //!isInput() || !Param.DEBUG_EXTRA
                );
                        /* the evaluated result here acts as a memoization of possibly many results
                           depending on whether the functor is purely static in which case
                           it would be the only one.
                         */
                //TODO see if a TaskProxy would work here
                result = yy != null ? clone(this, yy.getOne().negIf(yy.getTwo())) : null;
            }

            delete(); //delete intermediate

            return result;
        }

        //invoke possible Operation

        boolean cmd = isCommand();
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
                        return yy;
                    }
                } catch (Throwable xtt) {
                    //n.logger.error("{} {}", this, t);
                    return Operator.error(this, xtt, n.time());
                }
                if (cmd) {
                    n.eventTask.emit(this);
                    return null;
                }
                //otherwise: allow processing goal
            }
        }

        if (!cmd) {
            return new TaskAdd(this);
        } else {
            //default: Echo
            n.out(term());
        }

        return null;
    }

    /**
     * projected truth value
     */
    @Nullable
    default Truth truth(long when, long dur) {
        float e = evi(when, dur);
        if (e <= Float.MIN_NORMAL)
            return null;
        return new PreciseTruth(freq(), e, false);
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

    /**
     * creates lazily computing proxy task which facades the task to the target time range
     */
    static Task project(@Nullable Task t, long subStart, long subEnd, NAR n, boolean negated) {
        if (!negated && t.contains(subStart, subEnd))
            return t;

        int dur = n.dur();


        return new TaskProxy.WithTruthAndTime(t, subStart, subEnd, negated, () ->
                t.truth(subStart, subEnd, dur, Float.MIN_NORMAL)
        );
    }

    default float eviInteg() {
        return isEternal() ? Float.POSITIVE_INFINITY : range() * evi();
    }
    default float confInteg() {
        return isEternal() ? Float.POSITIVE_INFINITY : range() * conf();
    }

    byte punc();

    /**
     * fluent form of setPri which returns this class
     */
    default ITask pri(float p) {
        priSet(p);
        return this;
    }

    default float freq(long w, int dur) {
        Truth x = truth(w, dur);
        if (x != null)
            return x.freq();
        else
            return Float.NaN;
    }

    default float freqMean(int dur, long... when) {

        assert (when.length > 1);

        float fSum = 0;
        int num = 0;
        for (long w : when) {
            float tf = freq(w, dur);
            if (tf == tf) {
                fSum += tf;
                num++;
            }
        }
        if (num == 0)
            return Float.NaN;
        return fSum / num;
    }

}