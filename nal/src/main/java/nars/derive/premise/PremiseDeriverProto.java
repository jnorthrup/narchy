package nars.derive.premise;

import com.google.common.collect.ImmutableSet;
import jcog.TODO;
import jcog.list.FasterList;
import nars.$;
import nars.Op;
import nars.control.Cause;
import nars.derive.Derivation;
import nars.derive.step.*;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.Terms;
import nars.term.atom.Atomic;
import nars.term.compound.util.Image;
import nars.term.control.AbstractPred;
import nars.term.control.AndCondition;
import nars.term.control.PrediTerm;
import nars.truth.func.NALTruth;
import nars.truth.func.TruthFunc;
import nars.unify.constraint.*;
import nars.unify.match.Ellipsislike;
import nars.unify.op.*;
import org.eclipse.collections.api.tuple.Pair;

import java.util.*;

import static java.util.Collections.addAll;
import static nars.$.newHashSet;
import static nars.Op.*;
import static nars.derive.step.IntroVars.VAR_INTRO;
import static nars.subterm.util.Contains.*;
import static nars.unify.op.TaskPunctuation.Belief;
import static nars.unify.op.TaskPunctuation.Goal;
import static org.eclipse.collections.impl.tuple.Tuples.pair;

/**
 * an intermediate representation of a premise rule
 * with fully expanded opcodes
 */
public class PremiseDeriverProto extends PremiseDeriverSource {

    static final IntroVars introVars = new IntroVars();
    /**
     * requires double premise belief task evidence if deriving
     */
//    private static final AbstractPred<PreDerivation> DoublePremise = new DoublePremise((byte) 0);
//    private static final AbstractPred<PreDerivation> neqTaskBelief = new AbstractPred<PreDerivation>($.the("neqTaskBelief")) {
//
//        @Override
//        public float cost() {
//            return 0.1f;
//        }
//
//        @Override
//        public boolean test(PreDerivation preDerivation) {
//            return !preDerivation.taskTerm.equals(preDerivation.beliefTerm);
//        }
//    };
    private static final Atomic TRUTH = Atomic.the("truth");
    private static final Atomic BELIEF_AT = Atomic.the("beliefAt");
    final SortedSet<MatchConstraint> constraints = new TreeSet<>(PrediTerm.sortByCostIncreasing);
    final SortedSet<PrediTerm<PreDerivation>> pre = new TreeSet<>(PrediTerm.sortByCostIncreasing);
    final List<PrediTerm<Derivation>> post = new FasterList<>(8);
    private final PrediTerm<Derivation> truthify;

    /**
     * consequences applied after unification
     */
    PostCondition POST;
    /**
     * conditions which can be tested before unification
     */
    private final PrediTerm<PreDerivation>[] PRE;

    public PremiseDeriverProto(PremiseDeriverSource raw, PremisePatternIndex index) {
        super(raw, index);


        /**
         * deduplicate and generate match-optimized compounds for rules
         */


        Term[] precon = ref.sub(0).arrayShared();
        Term[] postcons = ref.sub(1).arrayShared();


        Term taskPattern = getTask();
        Term beliefPattern = getBelief();

        if (beliefPattern.op() == Op.ATOM) {
            throw new RuntimeException("belief term must contain no atoms: " + beliefPattern);
        }


        byte taskPunc = 0;


        for (int i = 2; i < precon.length; i++) {

            Compound predicate = (Compound) precon[i];
            Term predicate_name = predicate.sub(1);

            String predicateNameStr = predicate_name.toString();


            Term X, Y;


            Term[] args = predicate.sub(0).arrayShared();
            X = args.length > 0 ? args[0] : null;
            Y = args.length > 1 ? args[1] : null;



        /*} else {
            throw new RuntimeException("invalid arguments");*/
            /*args = null;
            arg1 = arg2 = null;*/


            switch (predicateNameStr) {


                case "neq":
                    neq(constraints, X, Y);
                    break;

//                case "neqTaskBelief":
//                    pre.add(neqTaskBelief);
//                    break;

//                case "neqUnneg":
//                    constraints.add(new NotEqualConstraint.NotEqualUnnegConstraint(X, Y));
//                    constraints.add(new NotEqualConstraint.NotEqualUnnegConstraint(Y, X));
//                    break;

                case "neqAndCom":
                    neq(constraints, X, Y);
                    constraints.add(new CommonSubtermConstraint(X, Y));
                    constraints.add(new CommonSubtermConstraint(Y, X));
                    break;


                case "neqRCom":
                    neq(constraints, X, Y);
                    constraints.add(new NoCommonSubtermConstraint(X, Y, true));
                    constraints.add(new NoCommonSubtermConstraint(Y, X, true));
                    break;

//                case "opSECTe":
//                    termIs(pre, taskPattern, beliefPattern, constraints, X, Op.SECTe);
//                    break;
//                case "opSECTi":
//                    termIs(pre, taskPattern, beliefPattern, constraints, X, Op.SECTi);
//                    break;


                case "subOf":

                    if (Y == Op.imExt || Y == Op.imInt) {

                        pre.add(new SubOf(Y, taskPattern, X, beliefPattern));

                    } else {
                        neq(constraints, X, Y);
                        constraints.add(new SubOfConstraint(X, Y, false, false, Subterm));
                        constraints.add(new SubOfConstraint(Y, X, true, false, Subterm));
                    }
                    break;

//                case "subOfNeg":
//
//                    neq(constraints, X, Y);
//                    constraints.add(new SubOfConstraint(X, Y, false, false, Subterm, -1));
//                    constraints.add(new SubOfConstraint(Y, X, true, false, Subterm, -1));
//                    break;

                case "subPosOrNeg":

                    neq(constraints, X, Y);
                    constraints.add(new SubOfConstraint(X, Y, false, false, Subterm, 0));
                    constraints.add(new SubOfConstraint(Y, X, true, false, Subterm, 0));
                    break;

                case "in":

                    neq(constraints, X, Y);
                    constraints.add(new SubOfConstraint(X, Y, false, false, Recursive));
                    constraints.add(new SubOfConstraint(Y, X, true, false, Recursive));
                    break;

//                case "inNeg":
//
//                    neq(constraints, X, Y);
//                    constraints.add(new SubOfConstraint(X, Y, false, false, Recursive, -1));
//                    constraints.add(new SubOfConstraint(Y, X, true, false, Recursive, -1));
//                    break;

                case "eventOf":
                    neq(constraints, X, Y);
                    eventPrefilter(pre, X, taskPattern, beliefPattern);
                    constraints.add(new SubOfConstraint(X, Y, false, false, Event));
                    constraints.add(new SubOfConstraint(Y, X, true, false, Event));
                    break;

                case "eventOfNeg":
                    neq(constraints, X, Y);
                    eventPrefilter(pre, X, taskPattern, beliefPattern);
                    constraints.add(new SubOfConstraint(X, Y, false, false, Event, -1));
                    constraints.add(new SubOfConstraint(Y, X, true, false, Event, -1));
                    break;

                case "eventOfPosOrNeg":
                    neq(constraints, X, Y);
                    eventPrefilter(pre, X, taskPattern, beliefPattern);
                    constraints.add(new SubOfConstraint(X, Y, false, false, Event, 0));
                    constraints.add(new SubOfConstraint(Y, X, true, false, Event, 0));
                    break;

                case "eventsOf":
                    neq(constraints, X, Y);
                    eventPrefilter(pre, X, taskPattern, beliefPattern);

                    constraints.add(new SubOfConstraint(X, Y, false, false, Events, 1));
                    constraints.add(new SubOfConstraint(Y, X, true, false, Events, 1));
                    break;

                case "eventCommon":
                    eventPrefilter(pre, X, taskPattern, beliefPattern);
                    eventPrefilter(pre, Y, taskPattern, beliefPattern);
                    constraints.add(new CommonSubEventConstraint(X, Y));
                    constraints.add(new CommonSubEventConstraint(Y, X));
                    break;

//                case "eqOrIn":
//                    constraints.add(new SubOfConstraint(X, Y, false, true, Recursive));
//                    constraints.add(new SubOfConstraint(Y, X, true, true, Recursive));
//                    break;


                case "subsMin":
                    int min = $.intValue(Y);
                    constraints.add(new SubsMin(X, min));
                    if (taskPattern.equals(X)) {
                        pre.add(SubsMin.proto(true, min));
                    }
                    if (beliefPattern.equals(X)) {
                        pre.add(SubsMin.proto(false, min));
                    }
                    break;

                case "notImaged":
                    termIsNotImaged(pre, taskPattern, X);
                    break;

                case "notSet":
                    termIsNot(pre, taskPattern, beliefPattern, constraints, X, Op.SetBits);
                    break;

                case "notImpl":
                    termIsNot(pre, taskPattern, beliefPattern, constraints, X, Op.IMPL.bit);
                    break;

//                case "notImplConj":
//                    termIsNot(pre, taskPattern, beliefPattern, constraints, X, Op.IMPL.bit | Op.CONJ.bit);
//                    break;

                case "isNot": {
                    Op o = Op.the($.unquote(Y));
                    termIsNot(pre, taskPattern, beliefPattern, constraints, X, o.bit);
                    break;
                }

                case "is": {

                    Op o = Op.the($.unquote(Y));
                    termIs(pre, taskPattern, beliefPattern, constraints, X, o);
                    break;
                }


                case "hasNoDiffed":

                    termHasNot(taskPattern, beliefPattern, pre, constraints, X, Op.DiffBits);
                    termIsNot(pre, taskPattern, beliefPattern, constraints, X, Op.INH.bit);
                    break;


                case "task":
                    String XString = X.toString();
                    switch (XString) {


                        case "\"?\"":
                            pre.add(TaskPunctuation.Question);
                            taskPunc = '?';
                            break;


                        case "\"@\"":
                            pre.add(TaskPunctuation.Quest);
                            taskPunc = '@';
                            break;
                        case "\".\"":
                            pre.add(Belief);
                            taskPunc = '.';
                            break;
                        case "\"!\"":
                            pre.add(Goal);
                            taskPunc = '!';
                            break;

//                        case "\"*\"":
//                            pre.add(new TaskBeliefOp(PROD, true, false));
//                            break;
//                        case "\"&&\"":
//                            pre.add(new TaskBeliefOp(CONJ, true, false));
//                            break;


                        default:
                            throw new RuntimeException("Unknown task punctuation type: " + XString);
                    }
                    break;


                default:
                    throw new RuntimeException("unhandled postcondition: " + predicateNameStr + " in " + this);

            }
        }

        Term beliefTruth = null, goalTruth = null;
        byte puncOverride = 0;
        Occurrify.TaskTimeMerge time = Occurrify.mergeDefault;

        Term[] modifiers = Terms.sorted(postcons[1].arrayShared());
        for (Term m : modifiers) {
            if (m.op() != Op.INH)
                throw new RuntimeException("Unknown postcondition format: " + m);

            Term type = m.sub(1);
            Term which = m.sub(0);

            switch (type.toString()) {

                case "Punctuation":
                    switch (which.toString()) {
                        case "Question":
                            puncOverride = QUESTION;
                            break;
                        case "Goal":
                            puncOverride = Op.GOAL;
                            break;
                        case "Belief":
                            puncOverride = Op.BELIEF;
                            break;
                        case "Quest":
                            puncOverride = QUEST;
                            break;

                        default:
                            throw new RuntimeException("unknown punctuation: " + which);
                    }
                    break;

                case "Time":
                    time = Occurrify.merge.get(which);
                    if (time == null)
                        throw new RuntimeException("unknown Time modifier:" + which);
                    break;


                case "Belief":
                    beliefTruth = which;
                    break;

                case "Goal":
                    goalTruth = which;
                    break;


                default:
                    throw new RuntimeException("Unhandled postcondition: " + type + ':' + which);
            }

        }

        Term pattern = intern(conclusion().sub(0), index);

        final Term taskPattern1 = getTask();
        final Term beliefPattern1 = getBelief();

        Op to = taskPattern1.op();
        boolean taskIsPatVar = to == Op.VAR_PATTERN;
        Op bo = beliefPattern1.op();
        boolean belIsPatVar = bo == Op.VAR_PATTERN;





        TruthFunc beliefTruthOp = NALTruth.get(beliefTruth);
        if (beliefTruth != null && beliefTruthOp == null) {
            throw new RuntimeException("unknown BeliefFunction: " + beliefTruth);
        }
        TruthFunc goalTruthOp = NALTruth.get(goalTruth);
        if (goalTruth != null && goalTruthOp == null) {
            throw new RuntimeException("unknown GoalFunction: " + goalTruth);
        }
        String beliefLabel = beliefTruthOp != null ? beliefTruthOp.toString() : null;
        String goalLabel = goalTruthOp != null ? goalTruthOp.toString() : null;




        Occurrify.BeliefProjection projection = time.projection();

        Term truthMode;
        if (beliefLabel!=null || goalLabel!=null) {
            FasterList<Term> args = new FasterList(4);
            if (puncOverride != 0)
                args.add($.quote((char) puncOverride));            args.add(beliefLabel != null ? Atomic.the(beliefLabel) : Op.EmptyProduct);
            args.add(goalLabel != null ? Atomic.the(goalLabel) : Op.EmptyProduct);
            args.add($.func(BELIEF_AT, Atomic.the(projection.name())));

            truthMode = $.func(TRUTH, args.toArrayRecycled(Term[]::new));
        } else {
            if (puncOverride != 0) {
                truthMode = $.func(TRUTH, $.quote((char) puncOverride));
            } else {
                //truthMode = Op.EmptyProduct; //auto
                throw new UnsupportedOperationException("ambiguous truth/punctuation");
            }
        }

        truthMode = intern(truthMode, index);

        Truthify truthify = puncOverride == 0 ?
                new Truthify.TruthifyPuncFromTask(truthMode, beliefTruthOp, goalTruthOp, projection) :
                new Truthify.TruthifyPuncOverride(truthMode, puncOverride, beliefTruthOp, goalTruthOp, projection);


        RuleCause cause = index.nar.newCause(s -> new RuleCause(this, s));
        Taskify taskify = new Taskify(cause);

        boolean introVars1;
        Pair<Termed, Term> outerFunctor = Op.functor(pattern, i -> i.equals(VAR_INTRO) ? VAR_INTRO : null);
        if (outerFunctor != null) {
            introVars1 = true;
            pattern = outerFunctor.getTwo().sub(0);
        } else {
            introVars1 = false;
        }
        PrediTerm<Derivation> conc = AndCondition.the(
                new Termify(pattern, this, truthify, time),
                introVars1 ?
                        AndCondition.the(introVars, taskify)
                        :
                        taskify
        );


        PrediTerm<Derivation> timeFilter = time.filter();
        if (timeFilter != null) {
            this.truthify = AndCondition.the(timeFilter, truthify);
        } else {
            this.truthify = truthify;
        }


        if (taskPattern1.equals(beliefPattern1)) {
            post.add(new UnifyTerm.UnifySubtermThenConclude(0, taskPattern1, conc));
        }
        //if (taskFirst(taskPattern1, beliefPattern1)) {

        post.add(new UnifyTerm.UnifySubterm(0, taskPattern1));
        post.add(new UnifyTerm.UnifySubtermThenConclude(1, beliefPattern1, conc));
//        } else {
//
//            post.add(new UnifyTerm.UnifySubterm(1, beliefPattern1));
//            post.add(new UnifyTerm.UnifySubtermThenConclude(0, taskPattern1, conc));
//        }


        if (!taskIsPatVar) {
            pre.add(new TaskBeliefOp(to, true, false));
            pre.addAll(SubTermStructure.get(0, taskPattern1.structure()));
        }
        if (!belIsPatVar) {
            if (to == bo) {
                pre.add(AbstractPatternOp.TaskBeliefOpEqual);
            } else {
                pre.add(new TaskBeliefOp(bo, false, true));
                pre.addAll(SubTermStructure.get(1, beliefPattern1.structure()));
            }
        }



        if (beliefTruthOp != null && !beliefTruthOp.single()) {
            if ((taskPunc == 0 || taskPunc == BELIEF)) {
                pre.add(new DoublePremiseRequired(true, false, false));
            } else if (puncOverride == BELIEF && (taskPunc == QUESTION || taskPunc == QUEST)) {
                pre.add(new DoublePremiseRequired(false, false, true));
            }
        }
        if (goalTruthOp != null && !goalTruthOp.single()) {
            if ((taskPunc == 0 || taskPunc == GOAL)) {
                pre.add(new DoublePremiseRequired(false, true, false));
            }else if (puncOverride == GOAL && (taskPunc == QUESTION || taskPunc == QUEST)) {
                pre.add(new DoublePremiseRequired(false, false, true));
            }
        }


































        /*System.out.println( Long.toBinaryString(
                        pStructure) + " " + pattern
        );*/


        constraints.forEach(c -> {
            PrediTerm<PreDerivation> p = c.preFilter(taskPattern, beliefPattern);
            if (p != null) {
                pre.add(p);
            }
        });


        POST = PostCondition.the(this, postcons[0], puncOverride, beliefTruth, goalTruth);


        //this.POST = postConditions.toArray(new PostCondition[pcs]);

        if (taskPunc == 0) {
            //no override, determine automaticaly by presence of belief or truth

            boolean b = false, g = false;
            //for (PostCondition x : POST) {
            if (POST.puncOverride != 0) {
                throw new RuntimeException("puncOverride with no input punc specifier");
            } else {
                b |= POST.beliefTruth != null;
                g |= POST.goalTruth != null;
            }
            //}

            if (!b && !g) {
                throw new RuntimeException("can not assume this applies only to questions");
            } else if (b && g) {
                pre.add(TaskPunctuation.BeliefOrGoal);
            } else if (b) {
                pre.add(Belief);
            } else /* if (g) */ {
                pre.add(Goal);
            }

        }

        this.PRE = pre.toArray(new PrediTerm[0]);
    }

    static private Term intern(Term pattern, PremisePatternIndex index) {
        return index.get(pattern, true).term();
    }

//    private static boolean taskFirst(Term task, Term belief) {
//        return true;
//    }

    static void eventPrefilter(Set<PrediTerm<PreDerivation>> pres, Term conj, Term taskPattern, Term beliefPattern) {


        boolean isTask = taskPattern.equals(conj);
        boolean isBelief = beliefPattern.equals(conj);
        if (isTask || isBelief)
            pres.add(new TaskBeliefOp(CONJ, isTask, isBelief));
        boolean inTask = !isTask && taskPattern.containsRecursively(conj);
        boolean inBelief = !isBelief && beliefPattern.containsRecursively(conj);
        if (inTask || inBelief) {
            pres.add(new TaskBeliefHasOrHasnt(true, CONJ.bit, isTask || inTask, isBelief || inBelief));
        }
    }

    private static void termIs(Set<PrediTerm<PreDerivation>> pres, Term taskPattern, Term beliefPattern, SortedSet<MatchConstraint> constraints, Term x, Op struct) {


        boolean checkedTask = false, checkedBelief = false;

        final byte[] pt = !checkedTask && (taskPattern.equals(x) || !taskPattern.ORrecurse(s -> s instanceof Ellipsislike)) ? onlyPathTo(taskPattern, x) : null;
        final byte[] pb = !checkedBelief && (beliefPattern.equals(x) || !beliefPattern.ORrecurse(s -> s instanceof Ellipsislike)) ? onlyPathTo(beliefPattern, x) : null;
        if (pt != null || pb != null) {
            if (pt != null)
                checkedTask = true;
            if (pb != null)
                checkedBelief = true;
            TaskBeliefOp.add(pres, true, struct.bit, pt, pb);

        }

        if (!checkedTask && !checkedBelief) {
            //non-exact filter
            boolean inTask = (taskPattern.equals(x) || taskPattern.containsRecursively(x));
            boolean inBelief = (beliefPattern.equals(x) || beliefPattern.containsRecursively(x));
            if (inTask || inBelief) {
                pres.add(new TaskBeliefHasOrHasnt(true, struct, inTask, inBelief));
            }


            constraints.add(new OpIs(x, struct));
        }
    }


    private static void termIsNot(Set<PrediTerm<PreDerivation>> pres, Term taskPattern, Term beliefPattern, SortedSet<MatchConstraint> constraints, Term x, int struct) {


        boolean checkedTask = false, checkedBelief = false;

        final byte[] pt = !checkedTask && (taskPattern.equals(x) || !taskPattern.ORrecurse(s -> s instanceof Ellipsislike)) ? onlyPathTo(taskPattern, x) : null;
        final byte[] pb = !checkedBelief && (beliefPattern.equals(x) || !beliefPattern.ORrecurse(s -> s instanceof Ellipsislike)) ? onlyPathTo(beliefPattern, x) : null;
        if (pt != null || pb != null) {
            if (pt != null)
                checkedTask = true;
            if (pb != null)
                checkedBelief = true;
            TaskBeliefOp.add(pres,false, struct, pt, pb);
        }

        if (!checkedTask && !checkedBelief) {
            //non-exact filter

            boolean inTask = !checkedTask && (taskPattern.equals(x) || taskPattern.containsRecursively(x));
            boolean inBelief = !checkedBelief && (beliefPattern.equals(x) || beliefPattern.containsRecursively(x));
            if (inTask || inBelief) {
                pres.add(new TaskBeliefHasOrHasnt(false, struct, inTask, inBelief));
            }

            constraints.add(new OpIsNot(x, struct));

        }
    }

    private static void termIsNotImaged(Set<PrediTerm<PreDerivation>> pres, Term taskPattern, Term x) {
        if (!taskPattern.containsRecursively(x) && !taskPattern.equals(x))
            throw new TODO("expected/tested occurrence in task pattern ");

        final byte[] pp = onlyPathTo(taskPattern, x);
        assert pp != null;
        pres.add(new NotImaged(x, pp));
    }

    private static byte[] onlyPathTo(Term taskPattern, Term x) {
        final byte[][] p = new byte[1][];
        taskPattern.pathsTo(x, (path, xx) -> {
            //assert (p[0] == null) : "only one";
            if (p[0] != null) {
                p[0] = null;
                return false;
            }
            p[0] = path.toArray();
            return true;
        });
        return p[0];
    }


    private static void termHasNot(Term task, Term belief, Set<PrediTerm<PreDerivation>> pres, SortedSet<MatchConstraint> constraints, Term t, int structure) {
        boolean inTask = task.equals(t) || task.containsRecursively(t);
        boolean inBelief = belief.equals(t) || belief.containsRecursively(t);
        if (inTask || inBelief) {
            pres.add(new TaskBeliefHasOrHasnt(false, structure, inTask, inBelief));
        } else {

            throw new TODO();
        }

    }

    private static void neq(SortedSet<MatchConstraint> constraints, Term x, Term y) {
        constraints.add(new NotEqualConstraint(x, y));
        constraints.add(new NotEqualConstraint(y, x));
    }

    /**
     * the task-term pattern
     */
    public final Term getTask() {
        return build().sub(0);
    }

    public Compound build() {
        return (Compound) ref.sub(0);
    }

    public Compound conclusion() {
        return (Compound) ref.sub(1);
    }

    /**
     * the belief-term pattern
     */
    public final Term getBelief() {
        return build().sub(1);
    }

    /**
     * compiles the conditions which are necessary to activate this rule
     */
    public Pair<Set<PrediTerm<PreDerivation>>, PrediTerm<Derivation>> build(PostCondition post) {


        Set<PrediTerm<PreDerivation>> precon = newHashSet(4);
        addAll(precon, PRE);

        precon.addAll(this.pre);


        int n = 1 + this.constraints.size() + this.post.size();


        PrediTerm[] suff = new PrediTerm[n];
        int k = 0;
        suff[k++] = this.truthify;
        for (PrediTerm p : this.constraints) {
            suff[k++] = p;
        }
        for (PrediTerm p : this.post) {
            suff[k++] = p;
        }

        return pair(ImmutableSet.copyOf(precon),
                AndCondition.<PrediTerm<PreDerivation>>the(suff));
    }

    /**
     * just a cause, not an input channel.
     * derivation inputs are batched for input by another method
     * holds the deriver id also that it can be applied at the end of a derivation.
     */
    public static class RuleCause extends Cause {
        public final PremiseDeriverSource rule;
        public final String ruleString;

        RuleCause(PremiseDeriverSource rule, short id) {
            super(id);
            this.rule = rule;
            this.ruleString = rule.toString();
        }

        @Override
        public String toString() {
            return $.pFast(rule.ref, $.the(id)).toString();
        }
    }

    private static class DoublePremiseRequired extends AbstractPred<PreDerivation> {

        final static Atomic key = (Atomic) $.the("DoublePremise");
        final boolean ifBelief, ifGoal,ifQuestionOrQuest;

        DoublePremiseRequired(boolean ifBelief, boolean ifGoal, boolean ifQuestionOrQuest) {
            super($.func(key,
                    ifBelief ? Op.BELIEF_TERM : Op.EmptyProduct,
                    ifGoal ? Op.GOAL_TERM : Op.EmptyProduct,
                    ifQuestionOrQuest? Op.QUE_TERM : Op.EmptyProduct));
            this.ifBelief = ifBelief;
            this.ifGoal = ifGoal;
            this.ifQuestionOrQuest = ifQuestionOrQuest;
        }

        @Override
        public boolean test(PreDerivation preDerivation) {
            byte x = preDerivation.taskPunc;
            boolean requireDouble;
            switch (x) {
                case BELIEF: requireDouble = ifBelief; break;
                case GOAL: requireDouble = ifGoal; break;
                case QUESTION:
                case QUEST:
                        requireDouble = ifQuestionOrQuest; break;
                default:
                    throw new UnsupportedOperationException();
            }
            return !requireDouble || preDerivation.hasBeliefTruth();
        }

        @Override
        public float cost() {
            return 0.1f;
        }
    }

    private static final class SubOf extends AbstractPred<PreDerivation> {
        private final Term y;

        boolean task;
        boolean belief;

        SubOf(Term y, Term taskPattern, Term x, Term beliefPattern) {
            super($.func("subOf", $.quote(y.toString())));
            this.y = y;

            task = taskPattern.containsRecursively(x);
            belief = beliefPattern.containsRecursively(y);
            assert task || belief;
        }

        @Override
        public boolean test(PreDerivation preDerivation) {
            if (task && !preDerivation.taskTerm.containsRecursively(y))
                return false;
            return !belief || preDerivation.beliefTerm.containsRecursively(y);
        }

        @Override
        public float cost() {
            return 0.5f;
        }
    }


    private static class NotImaged extends AbstractPred<PreDerivation> {

        private final byte[] pp;

        public NotImaged(Term x, byte[] pp) {
            super($.func("notImaged", x));
            this.pp = pp;
        }

        @Override
        public float cost() {
            return 0.15f;
        }

        @Override
        public boolean test(PreDerivation o) {
            Term prod = o.taskTerm.subPath(pp);
            return prod.op() == PROD && !Image.imaged(prod);
        }
    }

}
