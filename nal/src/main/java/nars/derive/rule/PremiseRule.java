package nars.derive.rule;

import com.google.common.collect.Sets;
import jcog.list.FasterList;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.The;
import nars.control.Derivation;
import nars.derive.*;
import nars.derive.constraint.*;
import nars.derive.op.*;
import nars.index.term.PatternIndex;
import nars.index.term.TermIndex;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.Terms;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.container.Subterms;
import nars.term.transform.CompoundTransform;
import nars.truth.func.BeliefFunction;
import nars.truth.func.GoalFunction;
import nars.truth.func.TruthOperator;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static java.lang.Math.max;
import static java.util.Collections.addAll;
import static nars.$.*;
import static nars.Op.CONJ;
import static nars.Op.PROD;
import static nars.term.Terms.concat;
import static nars.term.Terms.maxLevel;
import static org.eclipse.collections.impl.tuple.Tuples.pair;

/**
 * A rule which matches a Premise and produces a Task
 * contains: preconditions, predicates, postconditions, post-evaluations and metainfo
 */
public class PremiseRule /*extends GenericCompound*/ {


    public static final Atomic Task = Atomic.the("task");
    public static final Atomic Belief = Atomic.the("belief");
    private static final Term TaskAny = $.func("task", Atomic.the("any"));
    private static final Term QUESTION_PUNCTUATION = $.inh(Atomic.the("Question"), Atomic.the("Punctuation"));
    public final Term id;


    public boolean permuteBackward;
    public boolean permuteForward;


    /**
     * conditions which can be tested before unification
     */
    private PrediTerm[] PRE;

    /**
     * consequences applied after unification
     */
    public PostCondition[] POST;


    /**
     * maximum of the minimum NAL levels involved in the postconditions of this rule
     */
    public int minNAL;

    public String source;


    final SortedSet<MatchConstraint> constraints = new TreeSet(PrediTerm.sortByCost);
    final List<PrediTerm<Derivation>> pre = $.newArrayList();
    final List<PrediTerm<Derivation>> post = $.newArrayList();

    PremiseRule(Pair<PremiseRule, String> x) {
        this((Subterms)(x.getOne().id));
        withSource(x.getTwo());
    }

    @Override
    public String toString() {
        return id.toString();
    }


    /**
     * for printing complex terms as a recursive tree
     */
    public static void printRecursive(@NotNull Term x) {
        Terms.printRecursive(System.out, x);
    }

    @NotNull
    private Compound match() {
        return (Compound) id.sub(0);
    }

    @NotNull
    public Compound conclusion() {
        return (Compound) id.sub(1);
    }

    public PremiseRule(Subterms premiseAndResult) {
        this.id = $.p(premiseAndResult);
    }


    /**
     * compiles the conditions which are necessary to activate this rule
     */
    public Pair<Set<Term>, PrediTerm<Derivation>> build(PostCondition post) {

        byte puncOverride = post.puncOverride;

        TruthOperator belief = BeliefFunction.get(post.beliefTruth);
        if ((post.beliefTruth != null) && !post.beliefTruth.equals(TruthOperator.NONE) && (belief == null)) {
            throw new RuntimeException("unknown BeliefFunction: " + post.beliefTruth);
        }
        TruthOperator goal = GoalFunction.get(post.goalTruth);
        if ((post.goalTruth != null) && !post.goalTruth.equals(TruthOperator.NONE) && (goal == null)) {
            throw new RuntimeException("unknown GoalFunction: " + post.goalTruth);
        }


        String beliefLabel = belief != null ? belief.toString() : "_";
        String goalLabel = goal != null ? goal.toString() : "_";

        FasterList<Term> args = new FasterList();
        args.add($.the(beliefLabel));
        args.add($.the(goalLabel));
        if (puncOverride != 0)
            args.add($.quote(((char) puncOverride)));

        Compound ii = (Compound) $.func("truth", args.toArrayRecycled(Term[]::new));


        Solve truth = (puncOverride == 0) ?
                new Solve.SolvePuncFromTask(ii, belief, goal) :
                new Solve.SolvePuncOverride(ii, puncOverride, belief, goal);

        //PREFIX
        Set<Term> precon = newHashSet(4); //for ensuring uniqueness / no duplicates

        addAll(precon, PRE);

        precon.addAll(this.pre);


        ////-------------------
        //below here are predicates which affect the derivation


        //SUFFIX (order already determined for matching)
        int n = 1 + this.constraints.size() + this.post.size();

        PrediTerm[] suff = new PrediTerm[n];
        int k = 0;
        suff[k++] = truth;
        for (PrediTerm p : this.constraints) {
            suff[k++] = p;
        }
        for (PrediTerm p : this.post) {
            suff[k++] = p;
        }

        return pair(precon, (PrediTerm<Derivation>) AndCondition.the(suff));
    }


    public PremiseRule withSource(String source) {
        this.source = source;
        return this;
    }


    /**
     * source string that generated this rule (for debugging)
     */
    public String getSource() {
        return source;
    }

    /**
     * the task-term pattern
     */
    public final Term getTask() {
        return (match().sub(0));
    }

    /**
     * the belief-term pattern
     */
    public final Term getBelief() {
        return (match().sub(1));
    }

    private Term getConclusionTermPattern() {
        return conclusion().sub(0);
    }


    /**
     * deduplicate and generate match-optimized compounds for rules
     */
    private void compile(TermIndex index) {
        Term[] premisePattern = ((Subterms) id.sub(0)).arrayClone();
        premisePattern[0] = index.get(premisePattern[0], true).term(); //task pattern
        premisePattern[1] = index.get(premisePattern[1], true).term(); //belief pattern
    }

    private static final CompoundTransform UppercaseAtomsToPatternVariables = new CompoundTransform() {
        @Override @NotNull public Termed apply(Term v) {
            if (v instanceof Atom) {
                if (!PostCondition.reservedMetaInfoCategories.contains(v)) { //do not alter keywords
                    String name = v.toString();
                    if (name.length() == 1 && Character.isUpperCase(name.charAt(0))) {
                        return $.v(Op.VAR_PATTERN, v.toString());
                    }
                }
            }
            return v;
        }


    };


    public final PremiseRule normalize(PatternIndex index) {
        Compound t = index.pattern((Compound) id.transform(UppercaseAtomsToPatternVariables));
        if (t != this)
            return new PremiseRule(t);
        else
            return this;
    }


    @NotNull
    final PremiseRule setup(PatternIndex index, NAR nar) /* throws PremiseRuleException */ {

        compile(index);

        //1. construct precondition term array
        //Term[] terms = terms();

        Term[] precon = ((Subterms) id.sub(0)).arrayClone();
        Term[] postcons = ((Subterms) id.sub(1)).arrayClone();


        Set<PrediTerm> pres =
                //Global.newArrayList(precon.length);
                new TreeSet(); //for consistent ordering to maximize folding


        Term taskPattern = getTask();
        Term beliefPattern = getBelief();

        if (beliefPattern.op() == Op.ATOM) {
            throw new RuntimeException("belief term must contain no atoms: " + beliefPattern);
        }

        //if it contains an atom term, this means it is a modifier,
        //and not a belief term pattern
        //(which will not reference any particular atoms)

        //pattern = PatternCompound.make(p(taskTermPattern, beliefTermPattern));

        char taskPunc = 0;


        //additional modifiers: either preConditionsList or beforeConcs, classify them here
        for (int i = 2; i < precon.length; i++) {

            Compound predicate = (Compound) precon[i];
            Term predicate_name = predicate.sub(1);

            String predicateNameStr = predicate_name.toString();

            Term[] args;
            Term X, Y;

            //if (predicate.getSubject() instanceof SetExt) {
            //decode precondition predicate arguments
            args = ((Subterms) (predicate.sub(0))).arrayClone();
            X = (args.length > 0) ? args[0] : null;
            Y = (args.length > 1) ? args[1] : null;
//            Z = (args.length > 2) ? args[2] : null;
            //..

            /*} else {
                throw new RuntimeException("invalid arguments");*/
                /*args = null;
                arg1 = arg2 = null;*/
            //}

            String XString = X.toString();
            switch (predicateNameStr) {


                case "neq":
                    //neqPrefilter(pres, taskTermPattern, beliefTermPattern, X, Y, neq);
                    neq(constraints, X, Y); //should the constraints be ommited in this case?
                    break;


                case "neqAndCom":
                    //neqPrefilter(pres, taskTermPattern, beliefTermPattern, X, Y, neq);
                    neq(constraints, X, Y);
                    constraints.add(new CommonSubtermConstraint(X, Y));
                    constraints.add(new CommonSubtermConstraint(Y, X));
                    break;

                case "neqCom":
                    //neqPrefilter(pres, taskTermPattern, beliefTermPattern, X, Y, neqCom);
                    constraints.add(new NoCommonSubtermConstraint(X, Y, false));
                    constraints.add(new NoCommonSubtermConstraint(Y, X, false));
                    break;
                case "neqRCom":
                    //neqPrefilter(pres, taskTermPattern, beliefTermPattern, X, Y, neqRCom);
                    constraints.add(new NoCommonSubtermConstraint(X, Y, true));
                    constraints.add(new NoCommonSubtermConstraint(Y, X, true));
                    break;

                case "notSet":
                    termIsNot(pres, taskPattern, beliefPattern, constraints, X, Op.SetBits);
                    break;

//                case "set":
//                    if (taskPattern.equals(X) || beliefPattern.equals(X))
//                        pres.add(new TaskBeliefHas(Op.SetBits, taskPattern.equals(X), beliefPattern.equals(X)));
//                    constraints.add(new OpInConstraint(X, Op.SETi, Op.SETe));
//                    break;
//
//
                case "opSECTe":
                    termIs(pres, taskPattern, beliefPattern, constraints, X, Op.SECTe);
                    break;
                case "opSECTi":
                    termIs(pres, taskPattern, beliefPattern, constraints, X, Op.SECTi);
                    break;

//
//                case "notEqui":
//                    opNotContaining(taskTermPattern, beliefTermPattern, pres, constraints, X, Op.EQUI.bit);
//                    break;

                case "notImpl":
                    termIsNot(pres, taskPattern, beliefPattern, constraints, X, Op.IMPL.bit);
                    break;

                 case "subOf":
                    //X subOf Y : X is subterm of Y
                    constraints.add(new SubOfConstraint(X, Y, false));
                    constraints.add(new SubOfConstraint(Y, X, true));
                    break;

                 case "isAny":

                     int struct = 0;
                     for (int k = 1; k < args.length; k++) {
                         struct |= Op.the($.unquote(args[k])).bit;
                     }
                     assert(struct!=0);
                     termIsAny(pres, taskPattern, beliefPattern, constraints, X, struct);
                     break;

                 case "is":
                    //TODO make var arg version of this
                     Op o = Op.the($.unquote(Y));
                     assert (o != null);
                     termIs(pres, taskPattern, beliefPattern, constraints, X, o);
                    break;

                case "has":
                    //TODO make var arg version of this
                    Op oh = Op.the($.unquote(Y));
                    assert (oh != null);
                    termHasAny(taskPattern, beliefPattern, pres, constraints, X, oh);
                    break;

                case "time":
                    switch (XString) {
                        case "dtEvents":
                            pres.add(TaskBeliefOccurrence.bothEvents);
                            minNAL = 7;
                            break;

                        //NOTE THIS SHOULD ACTUALLY BE CALLED dtBeforeAfterOrEternal or something
                        case "dtEventsOrEternals":
                            pres.add(TaskBeliefOccurrence.eventsOrEternals);
                            break;
                        case "dtAfter":
                            pres.add(TaskBeliefOccurrence.after);
                            break;
                        case "dtAfterOrEternals":
                            pres.add(TaskBeliefOccurrence.afterOrEternals);
                            break;


                        default:
                            throw new UnsupportedOperationException("time(" + XString + ") unknown");
                            //TODO warn about missing ones
                    }
                    break;

//                case "temporal":
//                    pres.add( Temporality.either;
//                    break;

//                case "occurr":
////                    pres.add( new occurr(arg1,arg2);
//                    break;

//                case "after":
//                    switch (arg1.toString()) {
//                        case "forward":
//                            pres.add( Event.After.forward;
//                            break;
//                        case "reverseStart":
//                            pres.add( Event.After.reverseStart;
//                            break;
//                        case "reverseEnd":
//                            pres.add( Event.After.reverseEnd;
//                            break;
//                        default:
//                            throw new RuntimeException("invalid after() argument: " + arg1);
//                    }
//                    break;

//                case "dt":
////                    switch (arg1.toString()) {
////                        case "avg":
////                            pres.add( dt.avg; break;
////                        case "task":
////                            pres.add( dt.task; break;
////                        case "belief":
////                            pres.add( dt.belief; break;
////                        case "exact":
////                            pres.add( dt.exact; break;
////                        case "sum":
////                            pres.add( dt.sum; break;
////                        case "sumNeg":
////                            pres.add( dt.sumNeg; break;
////                        case "bmint":
////                            pres.add( dt.bmint; break;
////                        case "tminb":
////                            pres.add( dt.tminb; break;
////
////                        case "occ":
////                            pres.add( dt.occ; break;
////
////                        default:
////                            throw new RuntimeException("invalid dt() argument: " + arg1);
////                    }
//                    break;

                case "belief":
                    switch (XString) {
                          case "containsTask":
                            pres.add(TaskPolarity.beliefContainsTask);
                            break;
                        case "negative":
                            pres.add(TaskPolarity.beliefNeg);
                            break;
                        case "positive":
                            pres.add(TaskPolarity.beliefPos);
                            break;

                        //HACK do somethign other than duplciate this with the "task" select below, and also generalize to all ops
                        case "\"*\"":
                            pres.add(new TaskBeliefOp(PROD, false, true));
                            break;
                        case "\"&&\"":
                            pres.add(new TaskBeliefOp(CONJ, false, true));
                            break;
                        case "\"&&+\"": //sequence
                            pres.add(new TaskBeliefOp.TaskBeliefConjSeq(false, true));
                            break;
                        case "\"&&|\"": //parallel or eternal
                            pres.add(new TaskBeliefOp.TaskBeliefConjComm(false, true));
                            break;
                        default:
                            throw new UnsupportedOperationException();
                    }
                    break;

                case "task":
                    switch (XString) {
                        case "containsBelief":
                            pres.add(TaskPolarity.taskContainsBelief);
                            break;

                        case "containsBeliefRecursively":
                            pres.add(TaskPolarity.taskContainsBeliefRecursively);
                            break;

                        case "negative":
                            pres.add(TaskPolarity.taskNeg);
                            break;
                        case "positive":
                            pres.add(TaskPolarity.taskPos);
                            break;
                        case "\"?\"":
                            pres.add(TaskPunctuation.Question);
                            taskPunc = '?';
                            break;
                        case "\"?@\"":
                            pres.add(TaskPunctuation.QuestionOrQuest);
                            taskPunc = '?'; //this will choose quest as punctuation type when necessary, according to the task
                            break;
                        case "\"@\"":
                            pres.add(TaskPunctuation.Quest);
                            taskPunc = '@';
                            break;
                        case "\".\"":
                            pres.add(TaskPunctuation.Belief);
                            taskPunc = '.';
                            break;
                        case "\"!\"":
                            pres.add(TaskPunctuation.Goal);
                            taskPunc = '!';
                            break;

                        case "\"*\"":
                            pres.add(new TaskBeliefOp(PROD, true, false));
                            break;
                        case "\"&&\"":
                            pres.add(new TaskBeliefOp(CONJ, true, false));
                            break;
                        case "\"&&|\"": //parallel or eternal
                            pres.add(new TaskBeliefOp.TaskBeliefConjComm(true, false));
                            break;
                        case "any":
                            taskPunc = ' ';
                            break;

                        default:
                            throw new RuntimeException("Unknown task punctuation type: " + XString);
                    }
                    break;


                default:
                    throw new RuntimeException("unhandled postcondition: " + predicateNameStr + " in " + this);

            }
        }

        Conclude.match(
                this,
                pre, post,
                constraints, index, nar);

        List<PostCondition> postConditions = newArrayList(postcons.length);

        for (int i = 0; i < postcons.length; ) {
            Term t = postcons[i++];
            if (i >= postcons.length)
                throw new RuntimeException("invalid rule: missing meta term for postcondition involving " + t);

            postConditions.add(PostCondition.make(this, t, Terms.sorted(((Subterms) postcons[i++]).arrayShared())));
        }


        int pcs = postConditions.size();
        assert (pcs > 0) : "no postconditions";
        assert (Sets.newHashSet(postConditions).size() == pcs) :
                "postcondition duplicates:\n\t" + postConditions;

        POST = postConditions.toArray(new PostCondition[pcs]);

        if (taskPunc == 0) {
            //default: add explicit no-questions rule
            // TODO restrict this further somehow


            boolean b = false, g = true;
            for (PostCondition x : POST) {
                if (x.puncOverride != 0) {
                    throw new RuntimeException("puncOverride with no input punc specifier");
                } else {
                    b |= (x.beliefTruth != null);
                    g |= (x.goalTruth != null);
                }
            }

            if (!b && !g) {
                throw new RuntimeException("can not assume this applies only to questions");
            } else if (b && g) {
                pres.add(TaskPunctuation.BeliefOrGoal);
            } else if (b) {
                pres.add(TaskPunctuation.Belief);
            } else {
                pres.add(TaskPunctuation.Goal);
            }

        } else if (taskPunc == ' ') {
            //any task type
            taskPunc = 0;
        }

        //store to arrays
        this.PRE = pres.toArray(new PrediTerm[pres.size()]);

        //TODO add modifiers to affect minNAL (ex: anything temporal set to 7)
        //this will be raised by conclusion postconditions of higher NAL level
        minNAL = max(minNAL,
                max(maxLevel(getConclusionTermPattern()),
                        max(maxLevel(getTask()),
                                maxLevel(getBelief())
                        )));


        //        if (getConclusionTermPattern().containsTemporal()) {
//            if ((!getTaskTermPattern().containsTemporal())
//                    &&
//                    (!getBeliefTermPattern().containsTemporal())) {
//                //if conclusion is temporal term but the premise has none:
//
//                String s = toString();
//                if ((!s.contains("after")) && (!s.contains("concurrent") && (!s.contains("measure")))) {
//                    //System.err.println
//                  throw new RuntimeException
//                            ("Possibly invalid temporal rule from atemporal premise: " + this);
//
//                }
//            }
//        }
//
//        if (!getTask().hasVarPattern())
//            throw new RuntimeException("rule's task term pattern has no pattern variable");
//        if (!getBelief().hasVarPattern())
//            throw new RuntimeException("rule's task belief pattern has no pattern variable");
//        if (!getConclusionTermPattern().hasVarPattern())
//            throw new RuntimeException("rule's conclusion belief pattern has no pattern variable");

        return this;
    }

    private static void termIs(Set<PrediTerm> pres, Term taskPattern, Term beliefPattern, SortedSet<MatchConstraint> constraints, Term x, Op v) {
        constraints.add(new OpIs(x, v));
        includesOp(pres, taskPattern, beliefPattern, x, v);
    }
    private static void termIsAny(Set<PrediTerm> pres, Term taskPattern, Term beliefPattern, SortedSet<MatchConstraint> constraints, Term x, int struct) {
        constraints.add(new OpIsAny(x, struct));
        includesOp(pres, taskPattern, beliefPattern, x, struct, true);
    }

    private static void includesOp(Set<PrediTerm> pres, Term taskPattern, Term beliefPattern, Term x, Op o) {
        includesOp(pres, taskPattern, beliefPattern, x, o.bit, true);
    }

    private static void includesOp(Set<PrediTerm> pres, Term taskPattern, Term beliefPattern, Term x, int struct, boolean includeExclude) {
        boolean inTask = taskPattern.equals(x) || taskPattern.containsRecursively(x);
        boolean inBelief = beliefPattern.equals(x) || beliefPattern.containsRecursively(x);
        if (inTask || inBelief)
            pres.add(new TaskBeliefHasOrHasnt(struct, inTask, inBelief, includeExclude));
    }


    private static void termIsNot(Set<PrediTerm> pres, Term taskPattern, Term beliefPattern, @NotNull SortedSet<MatchConstraint> constraints, @NotNull Term x, int struct) {
        constraints.add(new OpIsNot(x, struct));
        includesOp(pres, taskPattern, beliefPattern, x, struct, false);
    }

    private static void termHasAny(Term task, Term belief, @NotNull Set<PrediTerm> pres, @NotNull SortedSet<MatchConstraint> constraints, @NotNull Term x, Op o) {
        constraints.add(new StructureHasAny(x, o.bit));

        includesOp(pres, task, belief, x, o);
    }

    private static void termHasNot(Term task, Term belief, @NotNull Set<PrediTerm> pres, @NotNull SortedSet<MatchConstraint> constraints, @NotNull Term t, int structure) {
        constraints.add(new StructureHasNone(t, structure));
    }

    private static void neq(@NotNull SortedSet<MatchConstraint> constraints, @NotNull Term x, @NotNull Term y) {
        constraints.add(new NotEqualConstraint(x, y));
        constraints.add(new NotEqualConstraint(y, x));
    }


//    /**
//     * for each calculable "question reverse" rule,
//     * supply to the consumer
//     * <p>
//     * ex:
//     * (A --> B), (B --> C), not_equal(A,C) |- (A --> C), (Truth:Deduction, Goal:Strong, Derive:AllowBackward)
//     * 1. Deriving of backward inference rules, since Derive:AllowBackward it allows deriving:
//     * (A --> B), (A --> C), not_equal(A,C), task("?") |- (B --> C), (Truth:Deduction, Goal:Strong, Derive:AllowBackward)
//     * (A --> C), (B --> C), not_equal(A,C), task("?") |- (A --> B), (Truth:Deduction, Goal:Strong, Derive:AllowBackward)
//     * so each premise gets exchanged with the conclusion in order to form a own rule,
//     * additionally task("?") is added to ensure that the derived rule is only used in backward inference.
//     */
//    public final void backwardPermutation(@NotNull PatternIndex index, @NotNull BiConsumer<PremiseRule, String> w) {
//
//        Term T = getTask(); //Task
//        Term B = getBelief(); //Belief
//        Term C = getConclusionTermPattern(); //Conclusion
//
//        // C, B, [pre], task_is_question() |- T, [post]
//        PremiseRule clone1 = clonePermutation(C, B, T, true, index);
//        if (clone1 != null)
//            w.accept(clone1, "C,B,question |- T");
//
//        // T, C, [pre], task_is_question() |- B, [post]
//        PremiseRule clone3 = clonePermutation(T, C, B, true, index);
//        if (clone3 != null)
//            w.accept(clone3, "T,C,question |- B");
//
//        //if needed, use Swap which would be applied before this recursively,
////        // T, C, [pre], task_is_question() |- B, [post]
////        PremiseRule clone2 = clonePermutation(C, T, B, true, index);
////        if (clone2 != null)
////            w.accept(clone2, "C,T,question |- B");
//
//
//    }


//    /**
//     * for each calculable "question reverse" rule,
//     * supply to the consumer
//     * <p>
//     * 2. Deriving of forward inference rule by swapping the premises since !s.contains("task(") && !s.contains("after(") && !s.contains("measure_time(") && !s.contains("Structural") && !s.contains("Identity") && !s.contains("Negation"):
//     * (B --> C), (A --> B), not_equal(A,C) |- (A --> C), (Truth:Deduction, Goal:Strong, Derive:AllowBackward)
//     * <p>
//     * after generating, these are then backward permuted
//     */
//    @Nullable
//    public final PremiseRule swapPermutation(@NotNull PatternIndex index) {
//
//        // T, B, [pre] |- C, [post] ||--
//        Term T = getTask();
//        Term B = getBelief();
//
//        if (T.equals(B)) {
//            //no change, ignore the permutation
//            return null;
//        } else {
//            Term C = getConclusionTermPattern();
//            return clonePermutation(B, T, C, false, index);
//        }
//    }


    @NotNull
    private PremiseRule clonePermutation(Term newT, Term newB, Term newR, boolean question, @NotNull PatternIndex index, NAR nar) {


        Map<Term, Term> m = new HashMap(3);
        m.put(getTask(), newT);
        m.put(getBelief(), newB); //index.retemporalize(?

        //boolean swapTruth = (!question && getTask().equals(newB) && getBelief().equals(newT));

        m.put(getConclusionTermPattern(), newR);


        Compound remapped = (Compound) id.replace(m);

        //Append taskQuestion
        Compound pc = (Compound) remapped.sub(0);
        Term[] pp = pc.arrayClone(); //premise component
        Compound newPremise;

        Compound newConclusion = (Compound) remapped.sub(1);

        if (question) {

            newPremise = (Compound) $.p(concat(pp, TaskAny));
            //newPremise = pc; //same


            //remove truth values and add '?' punct
//            TermContainer ss = ((Compound) newConclusion.sub(1)).subterms();
//            newConclusion = p(
//
//                    newConclusion.sub(0), $.p(ss.asFiltered((x) -> {
//                        Compound cx = (Compound) x;
//                        return !(cx.op() == Op.INH && (
//                                cx.sub(1).equals(BELIEF)
//                                        || cx.sub(1).equals(GOAL)));
//                    }).append(QUESTION_PUNCTUATION))
//            );
            newConclusion = (Compound) $.p(newConclusion.sub(0), p(QUESTION_PUNCTUATION));

        } else {
//            if (swapTruth) {
//                newConclusion = (Compound) index.transform(newConclusion, truthSwap);
//            }


            newPremise = pc; //same
        }

        return PremiseRuleSet.normalize(
                new PremiseRule(The.subterms(newPremise, newConclusion)), index, nar);

    }


}





