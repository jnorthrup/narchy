package nars.derive.rule;

import com.google.common.collect.Sets;
import jcog.TODO;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.derive.Conclude;
import nars.derive.PostCondition;
import nars.derive.ProtoDerivation;
import nars.derive.constraint.*;
import nars.derive.op.*;
import nars.index.term.PatternIndex;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.Terms;
import nars.term.atom.Atom;
import nars.term.pred.PrediTerm;
import nars.term.transform.TermTransform;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static nars.$.newArrayList;
import static nars.Op.CONJ;
import static nars.Op.PROD;
import static nars.subterm.util.Contains.Event;
import static nars.subterm.util.Contains.Recursive;
import static nars.subterm.util.Contains.Subterm;

/** an intermediate representation of a premise rule
 * with fully expanded opcodes
 */
public class CompileablePremiseRule extends PremiseRule {


    public CompileablePremiseRule(PremiseRule raw, PatternIndex index) {
        this(raw.term(), raw.source, index);
    }

    CompileablePremiseRule(Term rule, String src, PatternIndex index) {
        super(((Compound)index.pattern(rule.transform(UppercaseAtomsToPatternVariables))), src);

        NAR nar = index.nar;

        /**
         * deduplicate and generate match-optimized compounds for rules
         */
//        Term[] premisePattern = ((Subterms) term().sub(0)).arrayShared();
//        premisePattern[0] = index.get(premisePattern[0], true).term(); //task pattern
//        premisePattern[1] = index.get(premisePattern[1], true).term(); //belief pattern

        //1. construct precondition term array
        //Term[] terms = terms();

        Term[] precon = ((Subterms) term().sub(0)).arrayShared();
        Term[] postcons = ((Subterms) term().sub(1)).arrayShared();


        Set<PrediTerm<ProtoDerivation>> pres =
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


            Term X, Y;

            //if (predicate.getSubject() instanceof SetExt) {
            //decode precondition predicate arguments
            Term[] args = ((Subterms) (predicate.sub(0))).arrayShared();
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
                    neqPrefilter(pres, taskPattern, beliefPattern, X, Y);
                    neq(constraints, X, Y); //should the constraints be ommited in this case?
                    break;
                case "neqUnneg":
                    neqPrefilter(pres, taskPattern, beliefPattern, X, Y);
                    constraints.add(new NotEqualConstraint.NotEqualUnnegConstraint(X, Y));
                    constraints.add(new NotEqualConstraint.NotEqualUnnegConstraint(Y, X));
                    break;

                case "neqAndCom":
                    neqPrefilter(pres, taskPattern, beliefPattern, X, Y);
                    neq(constraints, X, Y);
                    constraints.add(new CommonSubtermConstraint(X, Y));
                    constraints.add(new CommonSubtermConstraint(Y, X));
                    break;


                case "neqRCom":
                    neqPrefilter(pres, taskPattern, beliefPattern, X, Y);
                    constraints.add(new NoCommonSubtermConstraint(X, Y, true));
                    constraints.add(new NoCommonSubtermConstraint(Y, X, true));
                    break;

                case "opSECTe":
                    termIs(pres, taskPattern, beliefPattern, constraints, X, Op.SECTe);
                    break;
                case "opSECTi":
                    termIs(pres, taskPattern, beliefPattern, constraints, X, Op.SECTi);
                    break;


                case "subOf": //non-recursive
                    //X subOf Y : X is subterm of Y
                    neqPrefilter(pres, taskPattern, beliefPattern, X, Y);
                    constraints.add(new SubOfConstraint(X, Y, false, false, Subterm));
                    constraints.add(new SubOfConstraint(Y, X, true, false, Subterm));
                    break;

                case "subOfNeg": //non-recursive
                    //X subOfNeg Y : --X is subterm of Y
                    neqPrefilter(pres, taskPattern, beliefPattern, X, Y);
                    constraints.add(new SubOfConstraint(X, Y, false, false, Subterm, -1));
                    constraints.add(new SubOfConstraint(Y, X, true, false, Subterm, -1));
                    break;

                case "subPosOrNeg": //non-recursive
                    //X subPosOrNeg Y : X or --X is subterm of Y
                    neqPrefilter(pres, taskPattern, beliefPattern, X, Y);
                    constraints.add(new SubOfConstraint(X, Y, false, false, Subterm, 0));
                    constraints.add(new SubOfConstraint(Y, X, true, false, Subterm, 0));
                    break;

                case "in": //recursive
                    //X in Y : X is recursive subterm of Y
                    neqPrefilter(pres, taskPattern, beliefPattern, X, Y);
                    constraints.add(new SubOfConstraint(X, Y, false, false, Recursive));
                    constraints.add(new SubOfConstraint(Y, X, true, false, Recursive));
                    break;

                case "inNeg": //recursive
                    //X inNeg Y : --X is recursive subterm of Y
                    neqPrefilter(pres, taskPattern, beliefPattern, X, Y);
                    constraints.add(new SubOfConstraint(X, Y, false, false, Recursive, -1));
                    constraints.add(new SubOfConstraint(Y, X, true, false, Recursive, -1));
                    break;

                case "inPosOrNeg": //recursive
                    //X inPosOrNeg Y : X or --X is recursive subterm of Y
                    neqPrefilter(pres, taskPattern, beliefPattern, X, Y);
                    constraints.add(new SubOfConstraint(X, Y, false, false, Recursive, 0));
                    constraints.add(new SubOfConstraint(Y, X, true, false, Recursive, 0));
                    break;


                case "eventOf": //recursive
                    //X in Y : X is recursive subterm of Y
                    neqPrefilter(pres, taskPattern, beliefPattern, X, Y);
                    eventPrefilter(pres, X, taskPattern, beliefPattern);
                    constraints.add(new SubOfConstraint(X, Y, false, false, Event));
                    constraints.add(new SubOfConstraint(Y, X, true, false, Event));
                    break;

                case "eventOfNeg": //recursive
                    //X inNeg Y : --X is recursive subterm of Y
                    neqPrefilter(pres, taskPattern, beliefPattern, X, Y);
                    eventPrefilter(pres, X, taskPattern, beliefPattern);
                    constraints.add(new SubOfConstraint(X, Y, false, false, Event, -1));
                    constraints.add(new SubOfConstraint(Y, X, true, false, Event, -1));
                    break;

                case "eventOfPosOrNeg": //recursive
                    //X inPosOrNeg Y : X or --X is recursive subterm of Y
                    neqPrefilter(pres, taskPattern, beliefPattern, X, Y);
                    eventPrefilter(pres, X, taskPattern, beliefPattern);
                    constraints.add(new SubOfConstraint(X, Y, false, false, Event, 0));
                    constraints.add(new SubOfConstraint(Y, X, true, false, Event, 0));
                    break;


                case "eqOrIn": //recursive
                    constraints.add(new SubOfConstraint(X, Y, false, true, Recursive));
                    constraints.add(new SubOfConstraint(Y, X, true, true, Recursive));
                    break;

//                 case "isAny":
//
//                     int struct = 0;
//                     for (int k = 1; k < args.length; k++) {
//
//                         Op o = Op.the($.unquote(args[k]));
//                         if (o.atomic)
//                             throw new TODO();
//                         struct |= o.bit;
//                     }
//                     assert(struct!=0);
//                     termIsAny(pres, taskPattern, beliefPattern, constraints, X, struct);
//                     break;

                case "subsMin":
                    int min = $.intValue(Y);
                    constraints.add(new SubsMin(X, min));
                    if (taskPattern.equals(X)) {
                        pres.add(SubsMin.proto(true, min));
                    }
                    if (!taskPattern.equals(beliefPattern) && beliefPattern.equals(X)) {
                        pres.add(SubsMin.proto(false, min));
                    }
                    break;

                case "notSet":
                    termIsNot(pres, taskPattern, beliefPattern, constraints, X, Op.SetBits);
                    break;

                case "notImpl":
                    termIsNot(pres, taskPattern, beliefPattern, constraints, X, Op.IMPL.bit);
                    break;

                case "notImplConj":
                    termIsNot(pres, taskPattern, beliefPattern, constraints, X, Op.IMPL.bit | Op.CONJ.bit);
                    break;

                case "isNot": {
                    Op o = Op.the($.unquote(Y));
                    assert (o != null);
                    termIsNot(pres, taskPattern, beliefPattern, constraints, X, o.bit);
                    break;
                }

                case "is": {
                    //TODO make var arg version of this
                    Op o = Op.the($.unquote(Y));
                    assert (o != null);
                    termIs(pres, taskPattern, beliefPattern, constraints, X, o);
                    break;
                }

//                case "has":
//                    //TODO make var arg version of this
//                    Op oh = Op.the($.unquote(Y));
//                    assert (oh != null);
//                    termHasAny(taskPattern, beliefPattern, pres, constraints, X, oh);
//                    break;

//                case "time":
//                    switch (XString) {
////                        case "dtEvents":
////                            pres.add(TaskBeliefOccurrence.bothEvents);
////                            minNAL = 7;
////                            break;
//
//                        //NOTE THIS SHOULD ACTUALLY BE CALLED dtBeforeAfterOrEternal or something
////                        case "dtEventsOrEternals":
////                            pres.add(TaskBeliefOccurrence.eventsOrEternals);
////                            break;
//
//                        case "dtAfter":
//                            pres.add(TaskBeliefOccurrence.after);
//                            break;
//                        case "dtAfterOrEternals":
//                            pres.add(TaskBeliefOccurrence.afterOrEternals);
//                            break;
//
//
//                        default:
//                            throw new UnsupportedOperationException("time(" + XString + ") unknown");
//                            //TODO warn about missing ones
//                    }
//                    break;

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
//                          case "containsTask":
//                            pres.add(TaskPolarity.beliefContainsTask);
//                            break;
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
//                        case "\"&&+\"": //sequence
//                            pres.add(new TaskBeliefOp.TaskBeliefConjSeq(false, true));
//                            break;
//                        case "\"&&|\"": //parallel or eternal
//                            pres.add(new TaskBeliefOp.TaskBeliefConjComm(false, true));
//                            break;
                        default:
                            throw new UnsupportedOperationException();
                    }
                    break;

                case "task":
                    switch (XString) {
//                        case "containsBelief":
//                            pres.add(TaskPolarity.taskContainsBelief);
//                            break;
//
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
//                        case "\"?@\"":
//                            pres.add(TaskPunctuation.QuestionOrQuest);
//                            taskPunc = '?'; //this will choose quest as punctuation type when necessary, according to the task
//                            break;
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
//                        case "\"&&|\"": //parallel or eternal
//                            pres.add(new TaskBeliefOp.TaskBeliefConjComm(true, false));
//                            break;
//                        case "any":
//                            taskPunc = ' ';
//                            break;

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


    }


    static void eventPrefilter(Set<PrediTerm<ProtoDerivation>> pres, Term conj, Term taskPattern, Term beliefPattern) {
        includesOp(pres, taskPattern, beliefPattern, conj, CONJ.bit, true, true);

        boolean isTask = taskPattern.equals(conj);
        boolean isBelief = beliefPattern.equals(conj);
        if (isTask || isBelief)
            pres.add(new TaskBeliefOp(CONJ, isTask, isBelief));
        boolean inTask = isTask || taskPattern.containsRecursively(conj);
        boolean inBelief = isBelief || beliefPattern.containsRecursively(conj);
        if (inTask || inBelief) {
            pres.add(new TaskBeliefHasOrHasnt(CONJ.bit, inTask, inBelief, true));
        }
    }

    static private void neqPrefilter(Set<PrediTerm<ProtoDerivation>> pres, Term taskPattern, Term beliefPattern, Term x, Term y) {
        //TODO maybe structure non-overlap test
        assert(!taskPattern.equals(beliefPattern));
        if ((taskPattern.equalsRoot(x) && beliefPattern.equalsRoot(y)) || (taskPattern.equalsRoot(y) && beliefPattern.equalsRoot(x))) {
            pres.add(TaskBeliefInequal.the);
        }
    }

    private static void termIs(Set<PrediTerm<ProtoDerivation>> pres, Term taskPattern, Term beliefPattern, SortedSet<MatchConstraint> constraints, Term x, Op v) {
        constraints.add(OpIs.the(x, v));
        includesOp(pres, taskPattern, beliefPattern, x, v);
    }
//    private static void termIsAny(Set<PrediTerm<ProtoDerivation>> pres, Term taskPattern, Term beliefPattern, SortedSet<MatchConstraint> constraints, Term x, int struct) {
//        constraints.add(new OpIsAny(x, struct));
//        includesOp(pres, taskPattern, beliefPattern, x, struct, true);
//    }

    private static void includesOp(Set<PrediTerm<ProtoDerivation>> pres, Term taskPattern, Term beliefPattern, Term x, Op o) {
        boolean isTask = taskPattern.equals(x);
        boolean isBelief = beliefPattern.equals(x);
        if (isTask || isBelief)
            pres.add(new TaskBeliefOp(o, isTask, isBelief));

        if (!o.atomic) // any atomic terms these will be Anon 'd and thus undetectable
            includesOp(pres, taskPattern, beliefPattern, x, o.bit, true, true);
        else
            throw new TODO("is this valid");
    }

    private static void includesOp(Set<PrediTerm<ProtoDerivation>> pres, Term taskPattern, Term beliefPattern, Term x, int struct, boolean includeExclude, boolean recurse) {
        //TODO test for presence of any atomic terms these will be Anon'd and thus undetectable

        boolean isTask = taskPattern.equals(x);
        boolean isBelief = beliefPattern.equals(x);


        boolean inTask = isTask || (recurse && taskPattern.containsRecursively(x));
        boolean inBelief = isBelief || (recurse && beliefPattern.containsRecursively(x));
        if (inTask || inBelief)
            pres.add(new TaskBeliefHasOrHasnt(struct, inTask, inBelief, includeExclude));
    }


    private static void termIsNot(Set<PrediTerm<ProtoDerivation>> pres, Term taskPattern, Term beliefPattern, @NotNull SortedSet<MatchConstraint> constraints, @NotNull Term x, int struct) {
        //TODO test for presence of any atomic terms these will be Anon'd and thus undetectable
        constraints.add(new OpIsNot(x, struct));
        includesOp(pres, taskPattern, beliefPattern, x, struct, false, false);
    }

//    private static void termHasAny(Term task, Term belief, @NotNull Set<PrediTerm> pres, @NotNull SortedSet<MatchConstraint> constraints, @NotNull Term x, Op o) {
//        constraints.add(new StructureHasAny(x, o.bit));
//
//        includesOp(pres, task, belief, x, o);
//    }
//
//    private static void termHasNot(Term task, Term belief, @NotNull Set<PrediTerm> pres, @NotNull SortedSet<MatchConstraint> constraints, @NotNull Term t, int structure) {
//        constraints.add(new StructureHasNone(t, structure));
//    }

    private static void neq(SortedSet<MatchConstraint> constraints, Term x, Term y) {
        constraints.add(new NotEqualConstraint(x, y));
        constraints.add(new NotEqualConstraint(y, x));
    }

    private static final TermTransform UppercaseAtomsToPatternVariables = new TermTransform() {
        @Override
        public Termed transformAtomic(Term atomic) {
            if (atomic instanceof Atom) {
                if (!PostCondition.reservedMetaInfoCategories.contains(atomic)) { //do not alter keywords
                    String name = atomic.toString();
                    if (name.length() == 1 && Character.isUpperCase(name.charAt(0))) {
                        return $.v(Op.VAR_PATTERN, atomic.toString());
                    }
                }
            }
            return atomic;
        }


    };


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


//    @NotNull
//    private PremiseRule clonePermutation(Term newT, Term newB, Term newR, boolean question, @NotNull PatternIndex index, NAR nar) {
//
//
//        Map<Term, Term> m = new HashMap(3);
//        m.put(getTask(), newT);
//        m.put(getBelief(), newB); //index.retemporalize(?
//
//        //boolean swapTruth = (!question && getTask().equals(newB) && getBelief().equals(newT));
//
//        m.put(getConclusionTermPattern(), newR);
//
//
//        Compound remapped = (Compound) term().replace(m);
//
//        //Append taskQuestion
//        Compound pc = (Compound) remapped.sub(0);
//
//        Compound newPremise;
//
//        Compound newConclusion = (Compound) remapped.sub(1);
//
//        if (question) {
//
//            newPremise = (Compound) $.p(concat(
//                    pc.arrayShared() /* premise component */,
//                    TaskAny));
//            //newPremise = pc; //same
//
//
//            //remove truth values and add '?' punct
////            TermContainer ss = ((Compound) newConclusion.sub(1)).subterms();
////            newConclusion = p(
////
////                    newConclusion.sub(0), $.p(ss.asFiltered((x) -> {
////                        Compound cx = (Compound) x;
////                        return !(cx.op() == Op.INH && (
////                                cx.sub(1).equals(BELIEF)
////                                        || cx.sub(1).equals(GOAL)));
////                    }).append(QUESTION_PUNCTUATION))
////            );
//            newConclusion = (Compound) $.p(newConclusion.sub(0), p(QUESTION_PUNCTUATION));
//
//        } else {
////            if (swapTruth) {
////                newConclusion = (Compound) index.transform(newConclusion, truthSwap);
////            }
//
//
//            newPremise = pc; //same
//        }
//
//        return PremiseRuleSet.normalize(
//                new PremiseRule(Subterms.subtermsInterned(newPremise, newConclusion)), index, nar);
//
//    }
//

}
