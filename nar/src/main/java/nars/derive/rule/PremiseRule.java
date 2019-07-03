package nars.derive.rule;

import com.google.common.collect.Iterables;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import jcog.TODO;
import jcog.data.list.FasterList;
import jcog.data.set.ArrayHashSet;
import jcog.util.ArrayUtil;
import nars.$;
import nars.Builtin;
import nars.Narsese;
import nars.Op;
import nars.derive.condition.*;
import nars.derive.model.Derivation;
import nars.derive.model.PreDerivation;
import nars.derive.op.Occurrify;
import nars.derive.op.Termify;
import nars.derive.op.Truthify;
import nars.derive.premise.PremiseRuleNormalization;
import nars.derive.premise.PremiseTermAccessor;
import nars.op.UniSubst;
import nars.subterm.BiSubterm;
import nars.subterm.Subterms;
import nars.subterm.util.SubtermCondition;
import nars.term.Variable;
import nars.term.*;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.control.PREDICATE;
import nars.term.control.TermMatch;
import nars.term.util.TermException;
import nars.term.util.conj.ConjMatch;
import nars.term.util.transform.AbstractTermTransform;
import nars.term.util.transform.TermTransform;
import nars.truth.func.NALTruth;
import nars.truth.func.TruthFunc;
import nars.unify.Unify;
import nars.unify.constraint.*;
import org.eclipse.collections.api.block.function.primitive.ByteToByteFunction;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static nars.Op.*;
import static nars.derive.premise.PatternTermBuilder.patternify;
import static nars.subterm.util.SubtermCondition.*;
import static nars.term.atom.Bool.Null;
import static nars.term.control.AbstractTermMatchPred.cost;
import static nars.term.control.PREDICATE.sortByCostIncreasing;
import static nars.time.Tense.DTERNAL;

/**
 * A rule which matches a Premise and produces a Task
 * contains: preconditions, predicates, postconditions, post-evaluations and metainfo
 */
public class PremiseRule extends ProxyTerm {

    private static final Pattern ruleImpl = Pattern.compile("\\|-");

    private static final Term eteConj = $.the("eteConj");
    private static final Map<Term, UnifyConstraint> constra = new ConcurrentHashMap<>();
    private static final Map<Term, Truthify> truthifies = new ConcurrentHashMap<>();
    private final static PremiseTermAccessor TaskTerm = new PremiseTermAccessor(0, Derivation.TaskTerm) {
        @Override
        public Term apply(PreDerivation d) {
            return d.taskTerm;
        }
    };
    private final static PremiseTermAccessor BeliefTerm = new PremiseTermAccessor(1, Derivation.BeliefTerm) {

        @Override
        public Term apply(PreDerivation d) {
            return d.beliefTerm;
        }
    };
    public final String source;
    public final Term taskPattern, beliefPattern;
    protected final Occurrify.OccurrenceSolver time;


    protected final Term beliefTruth, goalTruth;
    final Truthify truthify;
    final ImmutableSet<UnifyConstraint> CONSTRAINTS;
    final PREDICATE[] PRE;
    final Termify termify;
    /**
     * conditions which can be tested before unification
     */

    public final MutableSet<UnifyConstraint> constraints;
    public final MutableSet<PREDICATE<? extends Unify>> pre;
    private final BytePredicate taskPunc;
    /**
     * conclusion post-processing
     */
    private final TermTransform ConcTransform = new AbstractTermTransform.NegObliviousTermTransform() {
        @Override
        public Term applyPosCompound(Compound c) {

            Term f = Functor.func(c);
            if (f!=Null) {
                Subterms a = Functor.args(c);
                if (f.equals(UniSubst.unisubst)) {
                    Unifiable.constrainUnifiable(a, PremiseRule.this);
                } else if (f.equals(ConjMatch.BEFORE) || f.equals(ConjMatch.AFTER)) {
                    Unifiable.constraintEvent(a, PremiseRule.this, true);
                } else if (f.equals(Derivation.SUBSTITUTE)) {
                    Unifiable.constrainSubstitute(a, PremiseRule.this);
                } else if (f.equals(Derivation.CONJ_WITHOUT)) {
                    Unifiable.constraintEvent(a, PremiseRule.this, false);
                }
            }
            return super.applyPosCompound(c);
        }

    };



    public PremiseRule(String ruleSrc) throws Narsese.NarseseException {
        super(
                rule(ruleSrc)
        );

        this.source = ruleSrc;

        constraints = new UnifiedSet(8);
        pre = new UnifiedSet(8);


        Term[] precon = ref.sub(0).arrayShared();
        Term[] postcon = ref.sub(1).arrayShared();


        Term a = patternify(precon[0]);
        Term b = patternify(precon[1]);

        this.taskPattern = a;
        this.beliefPattern = b;

        if (taskPattern instanceof Neg)
            throw new TermException("task pattern can never be NEG", taskPattern);
        if (taskPattern.op() != VAR_PATTERN && !taskPattern.op().taskable)
            throw new TermException("task pattern is not taskable", taskPattern);
        if (beliefPattern instanceof Neg)
            throw new TermException("belief pattern can never be NEG", beliefPattern);
        if (beliefPattern.op() == Op.ATOM)
            throw new TermException("belief target must contain no atoms", beliefPattern);

        boolean questionSingle = true;
        ByteToByteFunction concPunc = null;
        boolean concBelief = false, concQuestion = false, concGoal = false, concQuest = false;

        /** TODO just use one punc->punc transfer function, and its return of 0 to deny */
        @Deprecated BytePredicate taskPunc = null;

        for (int i = 2; i < precon.length; i++) {

            Term p = precon[i];

            boolean negated = p instanceof Neg;
            boolean negationApplied = false; //safety check to make sure semantic of negation was applied by the handler
            if (negated)
                p = p.unneg();

            Term name = Functor.func(p);
            if (name == Null)
                throw new RuntimeException("invalid precondition: " + p);

            String pred = name.toString();
            Subterms args = Functor.args(p);
            int an = args.subs();
            Term X = an > 0 ? args.sub(0) : null;
            Term Y = an > 1 ? args.sub(1) : null;

            Variable XX = X instanceof Variable ? (Variable) X : null;
            Variable YY = Y instanceof Variable ? (Variable) Y : null;

            switch (pred) {


                case "neq":
                    neq(XX, Y);
                    break;
                case "neqRoot":
                    neqRoot(XX, YY);
                    break;

                case "subCountEqual":
                    constraints.add(new NotEqualConstraint.SubCountEqual(XX, YY));
                    break;
                case "neqOrInhCommon":
                    neqRoot(XX, YY);
                    constraints.add(new NotEqualConstraint.NoCommonInh(XX, YY));
                    break;

                case "eqPN":
                    constraints.add(new EqualPosOrNeg(XX, YY).negIf(negated));
                    if (negated) negationApplied = true;
                    break;

                case "eqNeg":
                    //TODO special predicate: either (but not both) is Neg
                    neq(XX, YY);
                    constraints.add(new EqualNegConstraint(XX, YY));
                    break;


                case "neqRCom":
                    neqRoot(XX, YY);
                    constraints.add(new NotEqualConstraint.NotEqualAndNotRecursiveSubtermOf(XX, YY));
                    break;

                case "setsIntersect":
                    constraints.add(new NotEqualConstraint.SetsIntersect(XX, YY));
                    break;

                case "notSetsOrDifferentSets":
                    constraints.add(new NotEqualConstraint.NotSetsOrDifferentSets(XX, YY));
                    break;

                case "subOf": case "subOfPN": {

                    SubtermCondition mode = null;

//                    if (pred.startsWith("sub"))

                        if (Y instanceof Neg) {
                            YY = (Variable) (Y = Y.unneg());
                            mode = SubtermNeg;
                        } else
                            mode = Subterm;
//                    else {
//
//                        if (Y instanceof Neg) {
//                            YY = (Variable) (Y = Y.unneg());
//                            mode = SubsectNeg;
//                        } else
//                            mode = Subsect;
//
//                        if (!negated)
//                            is(XX, Op.CONJ);
//                        else
//                            throw new TODO();
//
//                    }

                    if (!negated) {

                        neq(XX, Y);
                        if (Y instanceof Variable)
                            bigger(XX, YY);
                    }

                    int polarity;
                    if (pred.endsWith("PN")) {
                        polarity = 0;
                        assert (Y.op() != NEG);
                    } else {
                        polarity = 1; //negation already handled
                    }

                    if (Y instanceof Variable) {

                        SubOfConstraint c = new SubOfConstraint(XX, (nars.term.Variable) Y, mode, polarity);
                        constraints.add(c.negIf(negated));
                    } else {
                        if (polarity == 0)
                            throw new TODO(); //TODO contains(Y) || contains(--Y)
                        match(XX, new TermMatcher.Contains(Y), !negated);
                    }

                    if (negated)
                        negationApplied = true;
                    break;
                }


                case "in":
                    neq(XX, Y.unneg());
                    constraints.add(new SubOfConstraint(XX, ((Variable) Y.unneg()), Recursive, Y instanceof Neg ? -1 : +1));
                    break;

                case "hasBelief":
                    questionSingle = false;
                    DoublePremiseRequired dpr = new DoublePremiseRequired(true, true, true);
                    pre.add(negated ? dpr.neg() : dpr);
                    if (negated) negationApplied = true;
                    break;

                case "conjParallel":
                case "conjSequence":
                    if (!negated) is(X, CONJ);
                    match(X, pred.equals("conjSequence") ? TermMatcher.ConjSequence.the : TermMatcher.ConjParallel.the, !negated);
                    if (negated) negationApplied = true;
                    break;

                case "eventOf":
                case "eventOfNeg": {

                    boolean yNeg = pred.endsWith("Neg");

                    if (Y instanceof Neg) {
                        Y = Y.unneg();
                        YY = (Variable)Y;
                        yNeg = !yNeg;
                    }

                    constraints.add(new SubOfConstraint(XX, YY, Event, yNeg ? -1 : +1).negIf(negated));

                    if (!negated) {
                        bigger(XX, YY);
                        is(XX, CONJ);
//                        if (yNeg &&
//                                (YY.equals(taskPattern) ||
//                                        (YY.equals(beliefPattern) ||
//                                                (XX.containsRecursively(YY) && !XX.containsRecursively(YY.neg())
//                                                )))) {
//                            hasAny(XX, NEG); //taskPattern and beliefPattern themselves will always be unneg so it is safe to expect a negation
//                        }
                        eventable(YY);
                    }

                    if (negated) {
                        negationApplied = true;
                    }
                    break;
                }

//                case "eventFirstOf":
////                case "eventFirstOfNeg":
//                case "eventLastOf":
////                case "eventLastOfNeg":
//                {
//                    match(X, new TermMatcher.Is(CONJ));
//                    neq(constraints, XX, YY);
//                    boolean yNeg = pred.contains("Neg");
//                    constraints.add(new SubOfConstraint(XX, YY, pred.contains("First") ? EventFirst : EventLast, yNeg ? -1 : +1).negIf(negated));
//
//                    eventable(YY);
//
//                    if (negated) {
//                        negationApplied = true;
//                    }
//                    break;
//                }

                case "eventOfPN":
                    is(X, CONJ);
                    neq(XX, YY);
                    bigger(XX, YY);

                    eventable(YY);

                    constraints.add(new SubOfConstraint(XX, YY, Event, 0));
                    break;

//                /** one or more events contained */
//                case "eventsOf":
//                    neq(constraints, XX, YY);
//                    match(X, new TermMatch.Is(CONJ));
//
//                    constraints.addAt(new SubOfConstraint(XX, YY, EventsAny, 1));
//                    break;

                //case "eventsOfNeg":

//                case "eventCommon":
//
//
//                    neq(XX, YY);
//                    is(X, CONJ);
//                    is(Y, CONJ);
//                    constraints.add(new CommonSubEventConstraint(XX, YY));
//
//                    break;


                case "subsMin":
                    match(X, new TermMatcher.SubsMin((short) $.intValue(Y)));
                    break;


                case "equals": {
                    //is(X, Y.opBit(), !negated);
                    match(X, new TermMatcher.Equals(Y), !negated);
                    if (negated) negationApplied = true;
                    break;
                }

                case "is": {
                    int struct;
                    if (Y.op() == SETe) {
                        struct = 0;
                        for (Term yy : Y.subterms()) {
                            struct |= Op.the($.unquote(yy)).bit;
                        }
                    } else {
                        //TEMPORARY
                        if (Y.toString().equals("\"||\"")) {
                            isUnneg(X, CONJ, negated);
                            if (negated) negationApplied = true;
                            break;
                        }
                        struct = Op.the($.unquote(Y)).bit;
                    }
                    is(X, struct, negated);
                    if (negated) negationApplied = true;
                    break;
                }
                case "isVar": {
                    is(X, Op.Variable, negated);
                    if (negated)
                        negationApplied = true;
                    break;
                }

                case "hasVar": {
                    match(X, new TermMatcher.Has(Op.Variable, true, 2), !negated);
                    if (negated)
                        negationApplied = true;
                    break;
                }

                case "isUnneg": {
                    Op o = Op.the($.unquote(Y));
                    isUnneg(X, o, negated);
                    if (negated) negationApplied = true;
                    break;
                }


                case "has": {
                    //hasAny
                    hasAny(X, Op.the($.unquote(Y)), !negated);
                    if (negated) negationApplied = true;
                    break;
                }


                case "task":
                    String XString = X.toString();
                    switch (XString) {


                        case "\"?\"":
                            taskPunc = t -> t == QUESTION;
                            break;
                        case "\"@\"":
                            taskPunc = t -> t == QUEST;
                            break;
                        case "\".\"":
                            taskPunc = t -> t == BELIEF;
                            break;
                        case "\"!\"":
                            taskPunc = t -> t == GOAL;
                            break;

                        case "\".?\"":
                            assert (taskPunc == null);
                            taskPunc = t -> t == BELIEF || t == QUESTION;
                            break;

                        case "\"?@\"":
                            assert (taskPunc == null && concPunc == null);
                            taskPunc = t -> t == QUESTION || t == QUEST;
                            concPunc = c -> c;
                            break;

                        case "\".!\"":
                            assert (taskPunc == null && concPunc == null);
                            taskPunc = t -> t == BELIEF || t == GOAL;
                            concPunc = c -> c;
                            break;
//                        case "\"*\"":
//                            pre.addAt(new TaskBeliefOp(PROD, true, false));
//                            break;
//                        case "\"&&\"":
//                            pre.addAt(new TaskBeliefOp(CONJ, true, false));
//                            break;


                        default:
                            throw new RuntimeException("Unknown task punctuation type: " + XString);
                    }
                    break;



                default:
                    throw new RuntimeException("unhandled postcondition: " + pred + " in " + this);

            }

            if (negationApplied != negated)
                throw new RuntimeException("unhandled negation: " + p);
        }

        Term beliefTruth = null, goalTruth = null;
        Occurrify.OccurrenceSolver time = Occurrify.mergeDefault;

        Term[] modifiers = postcon != null && postcon.length > 1 ? postcon[1].arrayShared() : Op.EmptyTermArray;

        for (Term m : modifiers) {
            if (m.op() != Op.INH)
                throw new RuntimeException("Unknown postcondition format: " + m);

            Term type = m.sub(1);
            Term which = m.sub(0);

            switch (type.toString()) {

                case "Punctuation":
                    switch (which.toString()) {
                        case "Belief":
                            concBelief = true;
                            break;
                        case "Question":
                            concQuestion = true;
                            break;
                        case "Goal":
                            concGoal = true;
                            break;
                        case "Quest":
                            concQuest = true;
                            break;

                        /** belief -> question, goal -> quest */
                        case "Answer":
                            assert (taskPunc == null && concPunc == null);
                            assert(beliefTruth != null && goalTruth != null);
                            taskPunc = p -> p == QUESTION || p == QUEST;
                            concPunc = p -> {
                                switch (p) {
                                    case QUESTION:
                                        return BELIEF;
                                    case QUEST:
                                        return GOAL;
                                    default:
                                        return (byte) 0;
                                }
                            };
                            break;
                        /** belief -> question, goal -> quest */
                        case "Ask":
                            assert (taskPunc == null && concPunc == null);
                            taskPunc = p -> p == BELIEF || p == GOAL;
                            concPunc = p -> {
                                switch (p) {
                                    case BELIEF:
                                        return QUESTION;
                                    case GOAL:
                                        return QUEST;
                                    default:
                                        return (byte) 0;
                                }
                            };
                            break;

                        /** re-ask a new question/quest in response to question/quest */
                        case "AskAsk":
                            assert (taskPunc == null && concPunc == null);
                            taskPunc = p -> p == QUESTION || p == QUEST;
                            concPunc = p -> {
                                switch (p) {
                                    case QUESTION:
                                        return QUESTION;
                                    case QUEST:
                                        return QUEST;
                                    default:
                                        return (byte) 0;
                                }
                            };
                            break;

                        /** belief,question -> question, goal,quest -> quest */
                        case "AskAll":
                            assert (taskPunc == null && concPunc == null);
                            taskPunc = p -> true;
                            concPunc = p -> {
                                switch (p) {
                                    case BELIEF:
                                    case QUESTION:
                                        return QUESTION;
                                    case GOAL:
                                    case QUEST:
                                        return QUEST;
                                    default:
                                        throw new UnsupportedOperationException();
                                }
                            };
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



        TruthFunc beliefTruthOp = NALTruth.get(beliefTruth);
        if (beliefTruth != null && beliefTruthOp == null)
            throw new RuntimeException("unknown BeliefFunction: " + beliefTruth);

        TruthFunc goalTruthOp = NALTruth.get(goalTruth);
        if (goalTruth != null && goalTruthOp == null)
            throw new RuntimeException("unknown GoalFunction: " + goalTruth);



        {
            CommutativeConstantPreFilter.tryFilter(true, taskPattern, beliefPattern, pre);
            CommutativeConstantPreFilter.tryFilter(false, taskPattern, beliefPattern, pre);
        }


        structureAndVolumeGuards(TaskTerm, taskPattern);
        structureAndVolumeGuards(BeliefTerm, beliefPattern);


        /** infer missing conclusion punctuation */
        if (concPunc == null) {
            if (beliefTruth != null) concBelief = true;
            if (goalTruth != null) concGoal = true;

            if (!concQuest && !concQuestion) {
                if (beliefTruth != null && goalTruth == null) {
                    concPunc = (p) -> BELIEF;
                } else if (beliefTruth == null && goalTruth != null) {
                    concPunc = (p) -> GOAL;
                } else if (beliefTruth != null && goalTruth != null) {
                    concPunc = (p) -> p == BELIEF ? BELIEF : GOAL;
                }
            } else {
                if (concQuestion && !concQuest) {
                    concPunc = (p) -> QUESTION;
                } else if (concQuest && !concQuestion) {
                    concPunc = (p) -> QUEST;
                } else if (concQuestion && concQuest) {
                    concPunc = (p) -> (p == QUESTION || p == BELIEF) ? QUESTION : QUEST;
                }
            }

            //AUTO
            assert (concBelief || concQuest || concQuestion || concGoal);
//            boolean finalConcBelief = concBelief, finalConcGoal = concGoal, finalConcQuestion = concQuestion, finalConcQuest = concQuest;
//            concPunc = (p) -> {
//                switch (p) {
//                    case BELIEF:
//                        return finalConcBelief ? BELIEF : 0;
//                    case GOAL:
//                        return finalConcGoal ? GOAL : 0;
//                    case QUESTION:
//                        return finalConcQuestion ? QUESTION : 0;
//                    case QUEST:
//                        return finalConcQuest ? QUEST : 0;
//                    default:
//                        return (byte) 0;
//                }
//            };
        }

        /** infer necessary task punctuation */
        if (taskPunc == null) {
            //auto
            if (beliefTruth != null && goalTruth != null) {
                taskPunc = t -> t == BELIEF || t == GOAL; //accept belief and goal and map to those
            } else if (beliefTruth != null) {
                taskPunc = t -> t == BELIEF; //accept only belief -> belief
            } else if (goalTruth != null) {
                taskPunc = t -> t == GOAL; //accept only goal -> goal
            }
            //concPunc = t -> t;
        }

        /** infer necessary double premise for derived belief  */
        {
            boolean doublePremiseMaybe = false;
            if (beliefTruthOp != null) {
                assert (concPunc.valueOf(BELIEF) == BELIEF || concPunc.valueOf(GOAL) == BELIEF || concPunc.valueOf(QUESTION) == BELIEF || concPunc.valueOf(QUEST) == BELIEF);
                if (!beliefTruthOp.single()) {
                    pre.add(new DoublePremiseRequired(true, false, false));
                    doublePremiseMaybe = true;
                }
            }
            /** infer necessary double premise for derived goal  */
            if (goalTruthOp != null) {
                assert (concPunc.valueOf(BELIEF) == GOAL || concPunc.valueOf(GOAL) == GOAL || concPunc.valueOf(QUESTION) == GOAL || concPunc.valueOf(QUEST) == GOAL);
                if (!goalTruthOp.single()) {
                    pre.add(new DoublePremiseRequired(false, true, false));
                    doublePremiseMaybe = true;
                }
            }

            if (doublePremiseMaybe && (beliefPattern.op() != VAR_PATTERN && !beliefPattern.op().taskable))
                throw new TermException("double premise may be required and belief pattern is not taskable", beliefPattern);
        }

        /*System.out.println( Long.toBinaryString(
                        pStructure) + " " + concPattern
        );*/


//        constraints.forEach(cc -> {
//            PREDICATE<Derivation> p = cc.preFilter(taskPattern, beliefPattern);
//            if (p != null) {
//                pre.addAt(p);
//            }
//        });


        if (taskPunc == null)
            throw new UnsupportedOperationException("no taskPunc specified");

        if (concPunc == null)
            throw new UnsupportedOperationException("no concPunc specified");

        PuncMap tp = PuncMap.get(taskPunc, concPunc);
        if (!tp.all())
            pre.add(tp); //add filter to allow only the mapped types

        this.truthify = intern(Truthify.the(tp, beliefTruthOp, goalTruthOp, questionSingle, time));
        this.time = time;

        this.taskPunc = taskPunc;
        this.beliefTruth = beliefTruth;
        this.goalTruth = goalTruth;


        this.termify = new Termify(conclusion(postcon[0]), truthify, time);

        this.CONSTRAINTS = constraints(constraints);

        this.PRE = preconditions();

    }

    private ImmutableSet<UnifyConstraint> constraints(MutableSet<UnifyConstraint> constraints) {
        List<RelationConstraint> mirrors = new FasterList(4);
        constraints.removeIf(cc -> {
//            PREDICATE<Derivation> post = cc.postFilter();
//            if (post!=null) {
//            }

            PREDICATE<Unify> p = preFilter(cc, taskPattern, beliefPattern);
            if (p != null) {
                pre.add(p);
                return true;
            }
            if (cc instanceof RelationConstraint) {
                RelationConstraint m = ((RelationConstraint) cc).mirror();
                if (m != null)
                    mirrors.add(m);
            }
            return false;
        });

        constraints.addAll(mirrors);

        return theInterned(UnifyConstraint.the(constraints)); //AFTER .. constraints can be added to in conclusion()
    }

    PremiseRule(PremiseRuleBuilder b) {
        super(b.term());
        throw new TODO();
    }

    PremiseRule(PremiseRule raw) {
        super((/*index.rule*/(raw.ref)));

        this.termify = raw.termify;
        this.PRE = raw.PRE.clone(); //because it gets modified when adding Branchify suffix
        this.CONSTRAINTS = raw.CONSTRAINTS;
        this.source = raw.source;
        this.truthify = raw.truthify;
        this.constraints = raw.constraints;
        this.pre = raw.pre;
        this.time = raw.time;
        this.taskPunc = raw.taskPunc;
        this.beliefTruth = raw.beliefTruth;
        this.goalTruth = raw.goalTruth;
        this.taskPattern = raw.taskPattern;
        this.beliefPattern = raw.beliefPattern;
//        this.constraintSet = raw.constraintSet;

    }

    private static Term rule(String ruleSrc) throws Narsese.NarseseException {
        return new MyPremiseRuleNormalization().apply(
                new UppercaseAtomsToPatternVariables().apply(
                        $.pFast(
                                parseRuleComponents(ruleSrc)
                        )
                )
        );
    }

    private static <X extends Unify> UnifyConstraint<X> intern(UnifyConstraint<X> x) {
        UnifyConstraint<X> y = constra.putIfAbsent(x.term(), x);
        return y != null ? y : x;
    }
    private static Truthify intern(Truthify x) {
        Truthify y = truthifies.putIfAbsent(x.term(), x);
        return y != null ? y : x;
    }

    private static ImmutableSet<UnifyConstraint> theInterned(MutableCollection<UnifyConstraint> constraints) {
        if (constraints.isEmpty())
            return Sets.immutable.empty();
        else {
            return Sets.immutable.ofAll(Iterables.transform(constraints, PremiseRule::intern));
        }
    }

    public static Stream<PremiseRule> parse(String src) {
        try {
//            //bidi-rectional: swap premise components, and swap truth func param
//            if (src.startsWith(BIDI_modifier)) {
//                src = src.substring(BIDI_modifier.length());
//                PremiseRule a = new PremiseRule(src, false);
//                PremiseRule b = new PremiseRule(src, true);
//                return a.equals(b) ? Stream.of(a) : Stream.of(a, b);
//            } else {
                return Stream.of(new PremiseRule(src));
//            }
        } catch (Exception e) {
            throw new RuntimeException("rule parse:\n\t" + src + "\n\t" + e.getMessage(), e);
        }
    }

    public static Stream<PremiseRule> parse(String... rawRules) {
        return parse(Stream.of(rawRules));
    }

    public static Stream<PremiseRule> parse(Stream<String> rawRules) {
        return rawRules.flatMap(PremiseRule::parse);
    }

    private static Subterms parseRuleComponents(String src) throws Narsese.NarseseException {


        String[] ab = ruleImpl.split(src);
        if (ab.length != 2)
            throw new Narsese.NarseseException("Rule component must have arity=2, separated by \"|-\": " + src);

        String A = '(' + ab[0].trim() + ')';
        Term a = Narsese.term(A, false);
        if (!(a instanceof Compound))
            throw new Narsese.NarseseException("Left rule component must be compound: " + src);

        String B = '(' + ab[1].trim() + ')';
        Term b = Narsese.term(B, false);
        if (!(b instanceof Compound))
            throw new Narsese.NarseseException("Right rule component must be compound: " + src);

        return new BiSubterm(a, b);
    }

    private static PremiseTermAccessor TaskOrBelief(boolean taskOrBelief) {
        return taskOrBelief ? PremiseRule.TaskTerm : PremiseRule.BeliefTerm;
    }

    public static Term pathTerm(@Nullable byte[] path) {
        return path == null ? $.the(-1) /* null */ : $.p(path);
    }

    public final Term conclusion() {
        return termify.pattern;
    }

    private void isUnneg(Term x, Op o, boolean negated) {
        match(x, new TermMatcher.IsUnneg(o), !negated);
    }

    private PREDICATE<Unify> preFilter(UnifyConstraint cc, Term taskPattern, Term beliefPattern) {

        Variable x = cc.x;

        if (cc instanceof RelationConstraint.NegRelationConstraint) {
            PREDICATE p = preFilter(((RelationConstraint.NegRelationConstraint) cc).r, taskPattern, beliefPattern);
            return p != null ? p.neg() : null;
        } else if (cc instanceof RelationConstraint) {

            Variable y = ((RelationConstraint) cc).y;
            byte[] xInT = Terms.pathConstant(taskPattern, x);
            byte[] xInB = Terms.pathConstant(beliefPattern, x);
            if (xInT != null || xInB != null) {
                byte[] yInT = Terms.pathConstant(taskPattern, y);
                byte[] yInB = Terms.pathConstant(beliefPattern, y);
                if ((yInT != null || yInB != null)) {
                    if (xInT!=null && xInB!=null) {
                        if (xInB.length < xInT.length) xInT = null;
                        else xInB = null;
                    }
                    if (yInT!=null && yInB!=null) {
                        if (yInB.length < yInT.length) yInT = null;
                        else yInB = null;
                    }
                    return ConstraintAsPremisePredicate.the(cc, xInT, xInB, yInT, yInB);
                }
            }


        } else if (cc instanceof UnaryConstraint) {
            byte[] xInT = Terms.pathConstant(taskPattern, x);
            byte[] xInB = Terms.pathConstant(beliefPattern, x);
            if (xInT != null || xInB != null) {
                if (xInT!=null && xInB!=null) {
                    if (xInB.length < xInT.length) xInT = null;
                    else xInB = null;
                }
                return ConstraintAsPremisePredicate.the(cc, xInT, xInB, null, null);
            }

        }

        return null;
    }

    public void is(Term x, Op o) {
        is(x, o.bit);
    }

    private void is(Term x, int struct) {
        is(x, struct, false);
    }
    //new CustomConcurrentHashMap<>(STRONG, EQUALS, WEAK, EQUALS, 1024);

    private void is(Term x, int struct, boolean negated) {
        match(x, new TermMatcher.Is(struct), !negated);
        //constraints.add(new TermMatcher.Is(struct).constraint((Variable)x, !negated));
    }

    public void eventable(Variable YY) {
        constraints.add(TermMatcher.Eventable.the.constraint(YY, true));
    }

    public void hasAny(Term x, Op o) {
        hasAny(x, o, true);
    }

    private void hasAny(Term x, Op o, boolean trueOrFalse) {
        match(x, new TermMatcher.Has(o, true), trueOrFalse);
    }

    private PREDICATE[] preconditions() {
        int rules = pre.size();
        PREDICATE[] PRE = pre.toArray(new PREDICATE[rules + 1 /* extra to be filled in later stage */]);

        assert (PRE[PRE.length - 1] == null);

        if (rules > 1) {
            Arrays.sort(PRE, 0, rules, sortByCostIncreasing);
            assert (PRE[0].cost() <= PRE[rules - 2].cost()); //increasing cost
        }


        //not working yet:
//        for (int i = 0, preLength = PRE.length; i < preLength; i++) {
//            PRE[i] = INDEX.intern(PRE[i]);
//        }

        return PRE;
    }

    private Term conclusion(Term c) {
        //verify that all pattern variables in c are present in either taskTerm or beliefTerm
        assertConclusionVariablesPresent(c);

        if (!c.unneg().op().var) {

            List<Term> subbedConj;
            ArrayHashSet<Term> savedConj;

            if (c.hasAll(INH.bit | CONJ.bit)) {
                subbedConj = new FasterList(0);
                c = saveEteConj(c, subbedConj, savedConj = new ArrayHashSet(0));
            } else {
                savedConj = null;
                subbedConj = null;
            }

            c = conclusionOptimize(ConcTransform.apply(patternify(c)));

            if (savedConj != null && !savedConj.isEmpty())
                c = restoreEteConj(c, subbedConj, savedConj);

        }

        return c;
    }

    private Term restoreEteConj(Term c, List<Term> subbedConj, ArrayHashSet<Term> savedConj) {
        for (int i = 0, subbedConjSize = subbedConj.size(); i < subbedConjSize; i++) {
            Term y = subbedConj.get(i);
            Term x = savedConj.get(i);
            c = c.replace(y, x);
        }
        return c;
    }

    /**
     * HACK preserve any && occurring in --> by substituting them then replacing them
     */
    @Deprecated
    private Term saveEteConj(Term c, List<Term> subbedConj, ArrayHashSet<Term> savedConj) {

        c.recurseTerms(x -> x.hasAll(INH.bit | CONJ.bit), t -> {
            if (t.op() == INH) {
                Term s = t.sub(0);
                Term su = s.unneg();
                if (su.op() == CONJ && su.dt() == DTERNAL)
                    savedConj.add(patternify(s, false));
                Term p = t.sub(1);
                Term pu = p.unneg();
                if (pu.op() == CONJ && pu.dt() == DTERNAL)
                    savedConj.add(patternify(p, false));
            }
            return true;
        }, null);
        if (!savedConj.isEmpty()) {
            int i = 0;
            for (Term x : savedConj) {
                Term y = $.p(eteConj, $.the(i));
                subbedConj.add(y);
                c = c.replace(x, y);
                i++;
            }
        }
        return c;
    }

    private void assertConclusionVariablesPresent(Term c) {
        boolean tb = taskPattern.equals(beliefPattern);
        c.recurseTerms(Termlike::hasVarPattern, z -> {
            if (z.op() == VAR_PATTERN) {
                if (!(taskPattern.equals(z) || taskPattern.containsRecursively(z)) &&
                        (tb || !(beliefPattern.equals(z) || beliefPattern.containsRecursively(z)))) {
                    throw new RuntimeException("conclusion has pattern variable not contained in task or belief pattern: " + z);
                }
            }
            return true;
        }, null);
    }

    private Term conclusionOptimize(Term y) {

//            boolean taskEqY = taskPattern.equals(y);
//            boolean beliefEqualY = beliefPattern.equals(y);

        boolean taskObviouslyNotPastable = taskPattern.op().var;
        boolean beliefObviouslyNotPastable = beliefPattern.op().var;
        boolean taskTemporal = taskPattern.hasAny(Temporal);
        boolean beliefTemporal = beliefPattern.hasAny(Temporal);

        boolean atMostOneTemporal = !(taskTemporal && beliefTemporal);

        //decide when inline "paste" (macro substitution) of the premise task or belief terms is allowed.
        boolean taskPastable = false, beliefPastable = false;
        if (!taskObviouslyNotPastable && ((atMostOneTemporal || !y.hasAny(Op.Temporal))))
            taskPastable = true;
        if (!beliefObviouslyNotPastable && ((atMostOneTemporal || !y.hasAny(Op.Temporal))))
            beliefPastable = true;

        Term yT, yB;
        Term y0 = y;
        if (beliefPattern.volume() <= taskPattern.volume()) {
            //subst task first
            yT = taskPastable ? y0.replace(taskPattern, Derivation.TaskTerm) : y0;
            yB = beliefPastable ? yT.replace(beliefPattern, Derivation.BeliefTerm) : yT;
            y = yB;
        } else {
            //subst belief first
            yB = beliefPastable ? y0.replace(beliefPattern, Derivation.BeliefTerm) : y0;
            yT = taskPastable ? yB.replace(taskPattern, Derivation.TaskTerm) : yB;
            y = yT;
        }

//            boolean taskPasted = y0 != y1;
//            if (!taskObviouslyNotPastable && !taskPasted && !y.replace(taskPattern, Derivation.TaskTerm).equals(y))
//                System.out.println("task paste possible: " + y + " -> " + y0);
//
//            boolean beliefPasted = y1 != y2;
//            if (!beliefObviouslyNotPastable && !beliefPasted && !y.replace(beliefPattern, Derivation.BeliefTerm).equals(y))
//                System.out.println("belf paste possible: " + y + " -> " + y0);


//        if (y0 != y) {
//                System.out.println(y + " -> " + y0);
//        }
        return y;
    }

    /**
     * untested
     */
    private void structureAndVolumeGuards(PremiseTermAccessor r, Term root) {
        structureAndVolumeGuards(r, root, new ByteArrayList(6));
    }

    /**
     * untested
     */
    private void structureAndVolumeGuards(PremiseTermAccessor r, Term root, ByteArrayList p) {
        if (root.op() == VAR_PATTERN)
            return;

        byte[] pp = p.toByteArray(); //HACK
        int depth = pp.length;
        Term t = p.isEmpty() ? root : root.subPath(pp);

        Op to = t.op();
        if (to == Op.VAR_PATTERN)
            return;

        Function<PreDerivation, Term> rr = depth == 0 ? r : r.path(pp);
//        int ts = t.structure() & (~Op.VAR_PATTERN.bit);
//        pre.addAt(new TermMatchPred<>(new TermMatch.Is(to),  rr));
//        pre.addAt(new TermMatchPred<>(new TermMatch.Has(ts, false /* all */, t.complexity()), rr));
        pre.add(new TermMatch<>(TermMatcher.get(t, depth), rr, depth));

        int n = t.subs();
        if (!to.commutative || n == 1) {
            for (byte i = 0; i < n; i++) {
                p.add(i);
                {
                    structureAndVolumeGuards(r, root, p);
                }
                p.popByte();
            }
        }
    }

    private void matchSuper(boolean taskOrBelief, TermMatcher m, boolean trueOrFalse) {
        byte[] path = ArrayUtil.EMPTY_BYTE_ARRAY;
        pre.add(new TermMatch(m, trueOrFalse, false, TaskOrBelief(taskOrBelief).path(path), cost(path.length)));
    }

    private void match(boolean taskOrBelief, byte[] path, TermMatcher m, boolean trueOrFalse) {
        pre.add(new TermMatch(m, trueOrFalse, true, TaskOrBelief(taskOrBelief).path(path), cost(path.length)));
    }

    private void match(Term x,
                       BiConsumer<byte[], byte[]> preDerivationExactFilter,
                       BiConsumer<Boolean, Boolean> preDerivationSuperFilter /* task,belief*/
    ) {

        //boolean isTask = taskPattern.equals(x);
        //boolean isBelief = beliefPattern.equals(x);
        byte[] pt = //(isTask || !taskPattern.ORrecurse(s -> s instanceof Ellipsislike)) ?
                Terms.pathConstant(taskPattern, x);
        byte[] pb = //(isBelief || !beliefPattern.ORrecurse(s -> s instanceof Ellipsislike)) ?
                Terms.pathConstant(beliefPattern, x);// : null;
        if (pt != null || pb != null) {


            if ((pt != null) && (pb != null)) {
                //only need to test one. use shortest path
                if (pb.length < pt.length)
                    pt = null;
                else
                    pb = null;
            }

            preDerivationExactFilter.accept(pt, pb);
        } else {
            //assert(!isTask && !isBelief);
            //non-exact filter
            boolean inTask = taskPattern.containsRecursively(x);
            boolean inBelief = beliefPattern.containsRecursively(x);

            if (inTask && inBelief) {
                //only need to test one. use smallest volume
                if (beliefPattern.volume() < taskPattern.volume())
                    inTask = false;
                else
                    inBelief = false;
            }
            preDerivationSuperFilter.accept(inTask, inBelief);
        }
    }

    private void match(Term x, TermMatcher m) {
        match(x, m, true);
    }

    private void match(Term x, TermMatcher m, boolean trueOrFalse) {
        match(x, (pathInTask, pathInBelief) -> {


                    if (pathInTask != null)
                        match(true, pathInTask, m, trueOrFalse);
                    if (pathInBelief != null)
                        match(false, pathInBelief, m, trueOrFalse);

                }, (inTask, inBelief) -> {

                    if (trueOrFalse /*|| m instanceof TermMatch.TermMatchEliminatesFalseSuper*/) { //positive only (absence of evidence / evidence of absence)
                        if (inTask)
                            matchSuper(true, m, trueOrFalse);
                        if (inBelief)
                            matchSuper(false, m, trueOrFalse);
                    }

                    constraints.add(m.constraint((Variable) x, trueOrFalse));
                }
        );
    }

    public void neq(Variable x, Term y) {

        if (y instanceof Neg && y.unneg() instanceof Variable) {
            constraints.add(new EqualNegConstraint(x, (Variable) (y.unneg())).neg());
        } else if (y instanceof Variable) {
            constraints.add(new NotEqualConstraint(x, (Variable) y));
        } else {
            match(x, new TermMatcher.Equals(y), false);
        }
    }

    public void neqRoot(Variable x, Variable y) {
        constraints.add(new NotEqualConstraint.NotEqualRootConstraint(x, y));
    }

    public void bigger(Variable x, Variable y) {
        constraints.add(new Bigger(x, y));
    }

    static class UppercaseAtomsToPatternVariables extends AbstractTermTransform.NegObliviousTermTransform {

        static final ImmutableSet<Atomic> reservedMetaInfoCategories = Sets.immutable.of(
                Atomic.the("Belief"),
                Atomic.the("Goal"),
                Atomic.the("Punctuation"),
                Atomic.the("Time")
        );
        final UnifiedMap<String, Term> map = new UnifiedMap<>(8);

        @Override
        public Term applyAtomic(Atomic atomic) {
            if (atomic instanceof Atom) {
                if (!reservedMetaInfoCategories.contains(atomic)) {
                    String name = atomic.toString();
                    if (name.length() == 1 && Character.isUpperCase(name.charAt(0))) {
                        return map.computeIfAbsent(name, (n) -> $.varPattern(1 + map.size()));

                    }
                }
            }
            return atomic;
        }

    }

    private static class MyPremiseRuleNormalization extends PremiseRuleNormalization {
        @Override
        public Term applyAtomic(Atomic x) {
            if (x instanceof Atom) {
                Functor f = Builtin.functor(x);
                return f != null ? f : x;
            } else
                return super.applyAtomic(x);
        }

    }

}





