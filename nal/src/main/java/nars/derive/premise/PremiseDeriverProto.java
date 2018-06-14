package nars.derive.premise;

import jcog.TODO;
import jcog.list.FasterList;
import nars.$;
import nars.Op;
import nars.concept.Operator;
import nars.control.Cause;
import nars.derive.Derivation;
import nars.derive.step.*;
import nars.op.SubIfUnify;
import nars.op.Subst;
import nars.subterm.Subterms;
import nars.term.*;
import nars.term.atom.Atomic;
import nars.term.compound.util.Image;
import nars.term.control.AbstractPred;
import nars.term.control.AndCondition;
import nars.term.control.PrediTerm;
import nars.truth.func.NALTruth;
import nars.truth.func.TruthFunc;
import nars.unify.constraint.*;
import nars.unify.match.Ellipsislike;
import nars.unify.op.TaskBeliefHas;
import nars.unify.op.TaskBeliefIs;
import nars.unify.op.TaskPunctuation;
import nars.unify.op.UnifyTerm;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import org.eclipse.collections.api.tuple.Pair;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static nars.Op.*;
import static nars.subterm.util.Contains.*;
import static nars.unify.op.TaskPunctuation.Belief;
import static nars.unify.op.TaskPunctuation.Goal;
import static org.eclipse.collections.impl.tuple.Tuples.pair;

/**
 * an intermediate representation of a premise rule
 * with fully expanded opcodes
 */
public class PremiseDeriverProto extends PremiseDeriverSource {

    /**
     * return this to being a inline evaluable functor
     */
    @Deprecated
    static final IntroVars introVars = new IntroVars();
    /**
     * conditions which can be tested before unification
     */
    private final PrediTerm<Derivation>[] PRE;
    private final Set<MatchConstraint> constraints = new HashSet<>();
    private final List<PrediTerm<Derivation>> post = new FasterList<>(8);
    private final Truthify truthify;
    /**
     * consequences applied after unification
     */
    PostCondition POST;

    public PremiseDeriverProto(PremiseDeriverSource raw, PremisePatternIndex index) {
        super(raw, index);

        final Set<PrediTerm> pre = new HashSet(8);

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

            Term p = precon[i];

            boolean negated = p.op() == NEG;
            boolean negationApplied = false; //safety check to make sure semantic of negation was applied by the handler
            if (negated)
                p = p.unneg();

            Term name = Functor.funcName(p);
            if (name == Null)
                throw new RuntimeException("invalid precondition: " + p);

            String predicateNameStr = name.toString();
            Term[] args = Functor.funcArgsArray(p);
            Term X = args.length > 0 ? args[0] : null;
            Term Y = args.length > 1 ? args[1] : null;



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
                    eventPrefilter(pre, X, taskPattern, beliefPattern, constraints);
                    constraints.add(new SubOfConstraint(X, Y, false, false, Event));
                    constraints.add(new SubOfConstraint(Y, X, true, false, Event));
                    break;

                case "eventOfNeg":
                    neq(constraints, X, Y);
                    eventPrefilter(pre, X, taskPattern, beliefPattern, constraints);
                    constraints.add(new SubOfConstraint(X, Y, false, false, Event, -1));
                    constraints.add(new SubOfConstraint(Y, X, true, false, Event, -1));
                    break;

                case "eventOfPosOrNeg":
                    neq(constraints, X, Y);
                    eventPrefilter(pre, X, taskPattern, beliefPattern, constraints);
                    constraints.add(new SubOfConstraint(X, Y, false, false, Event, 0));
                    constraints.add(new SubOfConstraint(Y, X, true, false, Event, 0));
                    break;

                case "eventsOf":
                    neq(constraints, X, Y);
                    eventPrefilter(pre, X, taskPattern, beliefPattern, constraints);

                    constraints.add(new SubOfConstraint(X, Y, false, false, Events, 1));
                    constraints.add(new SubOfConstraint(Y, X, true, false, Events, 1));
                    break;

                case "eventCommon":
                    eventPrefilter(pre, X, taskPattern, beliefPattern, constraints);
                    eventPrefilter(pre, Y, taskPattern, beliefPattern, constraints);
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
                    /** deprecated soon */
                    termIsNot(pre, taskPattern, beliefPattern, constraints, X, Op.SetBits);
                    break;


                case "is": {
                    Op o = Op.the($.unquote(Y));
                    if (!negated) {
                        termIs(pre, taskPattern, beliefPattern, constraints, X, o);
                    } else {
                        termIsNot(pre, taskPattern, beliefPattern, constraints, X, o.bit);
                        negationApplied = true;
                    }

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

            if (negationApplied != negated)
                throw new RuntimeException("unhandled negation: " + p);
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

        Term pattern = index.intern(conclusion().sub(0));

        final Term taskPattern1 = getTask();
        final Term beliefPattern1 = getBelief();

        Op to = taskPattern1.op();
        boolean taskIsPatVar = to == Op.VAR_PATTERN;
        Op bo = beliefPattern1.op();
        boolean belIsPatVar = bo == Op.VAR_PATTERN;


        TruthFunc beliefTruthOp = NALTruth.get(beliefTruth);
        if (beliefTruth != null && beliefTruthOp == null)
            throw new RuntimeException("unknown BeliefFunction: " + beliefTruth);

        TruthFunc goalTruthOp = NALTruth.get(goalTruth);
        if (goalTruth != null && goalTruthOp == null)
            throw new RuntimeException("unknown GoalFunction: " + goalTruth);

        Occurrify.BeliefProjection projection = time.projection();


        RuleCause cause = index.nar.newCause(s -> new RuleCause(this, s));
        Taskify taskify = new Taskify(cause);


        boolean doIntroVars;
        {
            Pair<Termed, Term> varIntroPattern = Functor.ifFunc(pattern, i -> i.equals(introVars.ref) ? introVars.ref : null);
            if (varIntroPattern != null) {
                doIntroVars = true;
                pattern = varIntroPattern.getTwo().sub(0);
            } else {
                doIntroVars = false;
            }
        }

        {
            //add subIfUnify prefilter
            if (Functor.funcName(pattern).equals(SubIfUnify.SubIfUnify)) {
                Subterms args = Operator.args(pattern);
                Term x = args.sub(1);
                Term y = args.sub(2);
                ;
                boolean isStrict = args.contains(Subst.STRICT);

                //some structure exists that can be used to prefilter
                byte[] xpInT = Terms.extractFixedPath(taskPattern, x);
                byte[] xpInB = Terms.extractFixedPath(beliefPattern, x); //try the belief
                if (xpInT != null || xpInB != null) {
                    byte[] ypInT = Terms.extractFixedPath(taskPattern, y);
                    byte[] ypInB = Terms.extractFixedPath(beliefPattern, y); //try the belief
                    if (ypInT != null || ypInB != null) {
                        //the unifying terms are deterministicaly extractable from the task or belief
                        pre.add(new AbstractPred<Derivation>($.func("unifyPreFilter", $.p(xpInT), $.p(xpInB), $.p(ypInT), $.p(ypInB))) {

                            @Override
                            public boolean test(Derivation d) {
                                Term x = xpInT != null ? d.taskTerm.subPath(xpInT) : d.beliefTerm.subPath(xpInB);
                                assert (x != Null);
                                if (x == null)
                                    return false; //ex: seeking a negation but wasnt negated
                                Term y = ypInT != null ? d.taskTerm.subPath(ypInT) : d.beliefTerm.subPath(ypInB);
                                assert (y != Null);
                                if (y == null)
                                    return false; //ex: seeking a negation but wasnt negated

//                                x = Image.imageNormalize(x);
//                                y = Image.imageNormalize(y);

                                boolean xEqY = x.equals(y);
                                if (xEqY) {
                                    if (isStrict)
                                        return false;
                                } else {
                                    Op xo = x.op();
                                    if (xo.var)
                                        return true; //allow
                                    Op yo = y.op();
                                    if (yo.var)
                                        return true; //alow

                                    if (xo != yo)
                                        return false;
                                    if ((x.subterms().structure() & y.subterms().structure()) == 0)
                                        return false; //no common structure

                                    //TODO other exclusion cases
                                }


                                return true;
                            }

                            @Override
                            public float cost() {
                                return 0.3f;
                            }
                        });
                    }
                }


                //TODO match constraints

            }
        }

        {
            truthify = Truthify.the(index, puncOverride, beliefTruthOp, goalTruthOp, projection, time);
        }

        PrediTerm<Derivation> conc = AndCondition.the(
                new Termify(pattern, this, truthify, time),
                doIntroVars ?
                        AndCondition.the(introVars, taskify)
                        :
                        taskify
        );

        if (taskPattern1.equals(beliefPattern1)) {
            post.add(new UnifyTerm.UnifySubtermThenConclude(0, taskPattern1, conc));
        } else { //if (taskFirst(taskPattern1, beliefPattern1)) {

            post.add(new UnifyTerm.UnifySubterm(0, taskPattern1));
            post.add(new UnifyTerm.UnifySubtermThenConclude(1, beliefPattern1, conc));
        }
//        } else {
//
//            post.add(new UnifyTerm.UnifySubterm(1, beliefPattern1));
//            post.add(new UnifyTerm.UnifySubtermThenConclude(0, taskPattern1, conc));
//        }


        if (!taskIsPatVar) {
            pre.add(new TaskBeliefIs(to, true, false));
            pre.addAll(TaskBeliefHas.get(true, taskPattern1.structure(), true));
        }
        if (!belIsPatVar) {
//            if (to == bo) {
//                //pre.add(AbstractPatternOp.TaskBeliefOpEqual); //<- probably not helpful and just misaligns the trie
//            } else {
                pre.add(new TaskBeliefIs(bo, false, true));
                pre.addAll(TaskBeliefHas.get(false, beliefPattern1.structure(), true));
//            }
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
            } else if (puncOverride == GOAL && (taskPunc == QUESTION || taskPunc == QUEST)) {
                pre.add(new DoublePremiseRequired(false, false, true));
            }
        }


































        /*System.out.println( Long.toBinaryString(
                        pStructure) + " " + pattern
        );*/


        constraints.forEach(c -> {
            PrediTerm<Derivation> p = c.preFilter(taskPattern, beliefPattern);
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


        int rules = pre.size();
        this.PRE = pre.toArray(new PrediTerm[rules + 1 /* extra to be filled in later stage */]);
        ArrayUtils.sort(PRE, 0, rules-1, (x)-> -x.cost());
        //Arrays.sort(PRE, 0, rules, sortByCostIncreasing);
        if (rules > 1)
            assert(PRE[0].cost() <= PRE[rules-2].cost()); //increasing cost
    }


    void eventPrefilter(Collection<PrediTerm> pres, Term conj, Term taskPattern, Term beliefPattern, Set<MatchConstraint> constraints) {


        termIs(pres, taskPattern, beliefPattern, constraints, conj, CONJ);

//        boolean isTask = taskPattern.equals(conj);
//        boolean isBelief = beliefPattern.equals(conj);
//        if (isTask || isBelief)
//            pres.add(new TaskBeliefOp(CONJ, isTask, isBelief));
//        boolean inTask = !isTask && taskPattern.containsRecursively(conj);
//        boolean inBelief = !isBelief && beliefPattern.containsRecursively(conj);
//        if (inTask || inBelief) {
//            pres.add(new TaskBeliefHasOrHasnt(true, CONJ.bit, isTask || inTask, isBelief || inBelief));
//        }
    }

    private static void termIs(Collection<PrediTerm> pres, Term taskPattern, Term beliefPattern, Set<MatchConstraint> constraints, Term x, Op struct) {


        boolean checkedTask = false, checkedBelief = false;

        final byte[] pt = !checkedTask && (taskPattern.equals(x) || !taskPattern.ORrecurse(s -> s instanceof Ellipsislike)) ? Terms.extractFixedPath(taskPattern, x) : null;
        final byte[] pb = !checkedBelief && (beliefPattern.equals(x) || !beliefPattern.ORrecurse(s -> s instanceof Ellipsislike)) ? Terms.extractFixedPath(beliefPattern, x) : null;
        if (pt != null || pb != null) {
            if (pt != null)
                checkedTask = true;
            if (pb != null)
                checkedBelief = true;

            TaskBeliefIs.add(pres, true, struct.bit, pt, pb);
        }

        if (!checkedTask && !checkedBelief) {
            //non-exact filter
            boolean inTask = (taskPattern.equals(x) || taskPattern.containsRecursively(x));
            boolean inBelief = (beliefPattern.equals(x) || beliefPattern.containsRecursively(x));
            if (inTask)
                pres.addAll(TaskBeliefHas.get(true, struct.bit, true));
            if (inBelief)
                pres.addAll(TaskBeliefHas.get(false, struct.bit, true));


            constraints.add(new OpIs(x, struct));
        }
    }


    private static void termIsNot(Set<PrediTerm> pres, Term taskPattern, Term beliefPattern, Set<MatchConstraint> constraints, Term x, int struct) {


        boolean checkedTask = false, checkedBelief = false;

        final byte[] pt = !checkedTask && (taskPattern.equals(x) || !taskPattern.ORrecurse(s -> s instanceof Ellipsislike)) ? Terms.extractFixedPath(taskPattern, x) : null;
        final byte[] pb = !checkedBelief && (beliefPattern.equals(x) || !beliefPattern.ORrecurse(s -> s instanceof Ellipsislike)) ? Terms.extractFixedPath(beliefPattern, x) : null;
        if (pt != null || pb != null) {
            if (pt != null)
                checkedTask = true;
            if (pb != null)
                checkedBelief = true;
            TaskBeliefIs.add(pres, false, struct, pt, pb);
        }

        if (!checkedTask && !checkedBelief) {
            //non-exact filter
            throw new TODO();
//
//            boolean inTask = !checkedTask && (taskPattern.equals(x) || taskPattern.containsRecursively(x));
//            boolean inBelief = !checkedBelief && (beliefPattern.equals(x) || beliefPattern.containsRecursively(x));
//            if (inTask || inBelief) {
//                pres.add(new TaskBeliefHasOrHasnt(false, struct, inTask, inBelief));
//            }
//
//            constraints.add(new OpIsNot(x, struct));

        }
    }

    private static void termIsNotImaged(Collection<PrediTerm> pres, Term taskPattern, Term x) {
        if (!taskPattern.containsRecursively(x) && !taskPattern.equals(x))
            throw new TODO("expected/tested occurrence in task pattern ");

        final byte[] pp = Terms.extractFixedPath(taskPattern, x);
        assert pp != null;
        pres.add(new NotImaged(x, pp));
    }


    private static void termHasNot(Term task, Term belief, Collection<PrediTerm> pres, Set<MatchConstraint> constraints, Term t, int structure) {
        boolean inTask = task.equals(t) || task.containsRecursively(t);
        boolean inBelief = belief.equals(t) || belief.containsRecursively(t);
        if (inTask || inBelief) {
            if (inTask)
                pres.addAll(TaskBeliefHas.get(true, structure, false));
            if (inBelief)
                pres.addAll(TaskBeliefHas.get(false, structure, false));
        } else {

            throw new TODO();
        }

    }

    private static void neq(Set<MatchConstraint> constraints, Term x, Term y) {
        constraints.add(new NotEqualConstraint(x, y));
        constraints.add(new NotEqualConstraint(y, x));
    }

    /**
     * the task-term pattern
     */
    public final Term getTask() {
        return task().sub(0);
    }

    public Compound task() {
        return (Compound) ref.sub(0);
    }

    public Compound conclusion() {
        return (Compound) ref.sub(1);
    }

    /**
     * the belief-term pattern
     */
    public final Term getBelief() {
        return task().sub(1);
    }

    /**
     * compiles the conditions which are necessary to activate this rule
     */
    public Pair<PrediTerm<Derivation>[], DeriveAction> build() {

        int n = 1 + this.constraints.size() + this.post.size();


        PrediTerm<Derivation>[] post = new PrediTerm[n];
        int k = 0;
        post[k++] = this.truthify;
        for (PrediTerm p : this.constraints) {
            post[k++] = p;
        }
        for (PrediTerm p : this.post) {
            post[k++] = p;
        }

        DeriveAction POST =
                DeriveAction.action((AndCondition<Derivation>)AndCondition.the(post));

        return pair(PRE, POST);
    }

    /**
     * just a cause, not an input channel.
     * derivation inputs are batched for input by another method
     * holds the deriver id also that it can be applied at the end of a derivation.
     */
    public static class RuleCause extends Cause implements IntToFloatFunction {
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

        /** throttle */
        @Override public float valueOf(int intParameter) {
            return amp();
        }
    }

    private static class DoublePremiseRequired extends AbstractPred<Derivation> {

        final static Atomic key = (Atomic) $.the("DoublePremise");
        final boolean ifBelief, ifGoal, ifQuestionOrQuest;

        DoublePremiseRequired(boolean ifBelief, boolean ifGoal, boolean ifQuestionOrQuest) {
            super($.func(key,
                    ifBelief ? Op.BELIEF_TERM : Op.EmptyProduct,
                    ifGoal ? Op.GOAL_TERM : Op.EmptyProduct,
                    ifQuestionOrQuest ? Op.QUE_TERM : Op.EmptyProduct));
            this.ifBelief = ifBelief;
            this.ifGoal = ifGoal;
            this.ifQuestionOrQuest = ifQuestionOrQuest;
        }

        @Override
        public boolean test(Derivation preDerivation) {
            byte x = preDerivation.taskPunc;
            boolean requireDouble;
            switch (x) {
                case BELIEF:
                    requireDouble = ifBelief;
                    break;
                case GOAL:
                    requireDouble = ifGoal;
                    break;
                case QUESTION:
                case QUEST:
                    requireDouble = ifQuestionOrQuest;
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
            return !requireDouble || preDerivation.hasBeliefTruth();
        }

        @Override
        public float cost() {
            return 0.09f;
        }
    }

    private static final class SubOf extends AbstractPred<Derivation> {
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
        public boolean test(Derivation preDerivation) {
            if (task && !preDerivation.taskTerm.containsRecursively(y))
                return false;
            return !belief || preDerivation.beliefTerm.containsRecursively(y);
        }

        @Override
        public float cost() {
            return 0.5f;
        }
    }


    private static class NotImaged extends AbstractPred<Derivation> {

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
        public boolean test(Derivation o) {
            Term prod = o.taskTerm.subPath(pp);
            return prod.op() == PROD && !Image.imaged(prod);
        }
    }

}
