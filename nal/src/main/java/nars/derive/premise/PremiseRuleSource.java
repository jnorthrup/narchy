package nars.derive.premise;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import jcog.TODO;
import jcog.data.list.FasterList;
import jcog.util.ArrayUtils;
import nars.$;
import nars.Narsese;
import nars.Op;
import nars.derive.Derivation;
import nars.derive.PreDerivation;
import nars.derive.filter.CommutativeConstantPreFilter;
import nars.derive.filter.DoublePremiseRequired;
import nars.derive.filter.Unifiable;
import nars.derive.op.ConjParallel;
import nars.derive.op.Occurrify;
import nars.derive.op.Termify;
import nars.derive.op.Truthify;
import nars.derive.premise.op.TaskPunc;
import nars.derive.premise.op.TermMatch;
import nars.subterm.BiSubterm;
import nars.subterm.Subterms;
import nars.term.Variable;
import nars.term.*;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.control.PREDICATE;
import nars.term.util.transform.AbstractTermTransform;
import nars.term.util.transform.DirectTermTransform;
import nars.term.util.transform.TermTransform;
import nars.truth.func.NALTruth;
import nars.truth.func.TruthFunc;
import nars.unify.Unify;
import nars.unify.constraint.*;
import org.eclipse.collections.api.block.function.primitive.ByteToByteFunction;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static nars.Op.*;
import static nars.subterm.util.SubtermCondition.*;
import static nars.term.control.PREDICATE.sortByCostIncreasing;

/**
 * A rule which matches a Premise and produces a Task
 * contains: preconditions, predicates, postconditions, post-evaluations and metainfo
 */
public class PremiseRuleSource extends ProxyTerm {

    private static final Pattern ruleImpl = Pattern.compile("\\|-");
    private static final String BIDI_modifier = "bidi:";
    public final String source;
    final Truthify truthify;

    /**
     * conditions which can be tested before unification
     */

    private final MutableSet<UnifyConstraint> constraints;
    final ImmutableSet<UnifyConstraint> CONSTRAINTS;
    private final MutableSet<PREDICATE<? extends Unify>> pre;
    final PREDICATE[] PRE;
    protected final Occurrify.OccurrenceSolver time;


    protected final Term beliefTruth;
    protected final Term goalTruth;

    public final Term taskPattern;
    public final Term beliefPattern;



    private static final PatternTermBuilder INDEX = new PatternTermBuilder();
    final Termify termify;

    private final BytePredicate taskPunc;


    public PremiseRuleSource(String ruleSrc) throws Narsese.NarseseException {
        this(ruleSrc, false);
    }

    private PremiseRuleSource(String ruleSrc, boolean swap) throws Narsese.NarseseException {
        super(
                INDEX.rule(new UppercaseAtomsToPatternVariables().apply($.pFast(parseRuleComponents(ruleSrc))))
        );

        this.source = ruleSrc;

        constraints = new UnifiedSet(8);
        pre = new UnifiedSet(8);


        Term[] precon = ref.sub(0).arrayShared();
        Term[] postcon = ref.sub(1).arrayShared();



        Term a = PatternTermBuilder.patternify(precon[0]);
        Term b = PatternTermBuilder.patternify(precon[1]);
        if (swap) {
            String preconStr = ref.sub(0).toString().toLowerCase();
            //if (preconStr.contains("task") || preconStr.contains("belief"))
              //  throw new TODO();
            String postconStr = ref.sub(1).toString()/*.toLowerCase()*/;
            if (postconStr.contains("task") || postconStr.contains("belief"))
                throw new TODO();
            Term c = a;
            a = b;
            b = c;
        }
        this.taskPattern = a;
        this.beliefPattern = b;
        if (beliefPattern.op() == Op.ATOM) {
            throw new RuntimeException("belief target must contain no atoms: " + beliefPattern);
        }


        ByteToByteFunction concPunc = null;
        boolean concBelief = false, concQuestion = false, concGoal = false, concQuest = false;

        /** TODO just use one punc->punc transfer function, and its return of 0 to deny */
        @Deprecated BytePredicate taskPunc = null;

        for (int i = 2; i < precon.length; i++) {

            Term p = precon[i];

            boolean negated = p.op() == NEG;
            boolean negationApplied = false; //safety check to make sure semantic of negation was applied by the handler
            if (negated)
                p = p.unneg();

            Term name = Functor.func(p);
            if (name == Bool.Null)
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
                    neq(constraints, XX, Y);
                    break;
                case "neqRoot":
                    neqRoot(XX, YY);
                    break;
                case "eqRoot":
                    match(XX, new TermMatcher.EqualsRoot(YY));
                    break;
                case "subCountEqual":
                    constraints.add(new NotEqualConstraint.SubCountEqual(XX, YY));
                    break;
                case "neqOrInhCommon":
                    neqRoot(XX, YY);
                    constraints.add(new NotEqualConstraint.NoCommonInh(XX, YY));
                    break;

                case "eqPN":
                    constraints.add(new NotEqualConstraint.EqualPosOrNeg(XX, YY));
                    break;

                case "eqNeg":
                    neq(constraints, XX, YY);
                    constraints.add(new NotEqualConstraint.EqualNegConstraint(XX, YY));
                    break;


                case "neqRCom":
                    neqRoot(XX, YY);
                    constraints.add(new NotEqualConstraint.NotEqualAndNotRecursiveSubtermOf(XX, YY));
                    break;

                case "notSetsOrDifferentSets":
                    constraints.add(new NotEqualConstraint.NotSetsOrDifferentSets(XX, YY));
                    break;

                case "subOf": {
                    if (!negated)
                        neq(constraints, XX, Y);

                    if (Y.unneg() instanceof Variable) {
                        constraints.add(new SubOfConstraint(XX, ((Variable) (Y.unneg())),
                                Subterm, Y.op() == NEG ? -1 : +1).negIf(negated));
                    } else {
                        match(XX, new TermMatcher.Contains(Y), true);
                    }

                    if (negated)
                        negationApplied = true;
                    break;
                }

                case "subOfPosOrNeg":
                    //TODO handle negation
                    neq(constraints, XX, YY);
                    constraints.add(new SubOfConstraint(XX, YY, Subterm, 0));
                    break;

                case "in":
                    neq(constraints, XX, Y.unneg());
                    constraints.add(new SubOfConstraint(XX, ((Variable) Y.unneg()), Recursive, Y.op() == NEG ? -1 : +1));
                    break;

                case "conjParallel":
                    match(X, ConjParallel.the);
                    break;

                case "eventOf":
                case "eventOfNeg": {
                    match(X, new TermMatcher.Is(CONJ));
                    neq(constraints, XX, YY);
                    boolean yNeg = pred.contains("Neg");
                    constraints.add(new SubOfConstraint(XX, YY, Event, yNeg ? -1 : +1).negIf(negated));
                    if (negated) {
                        negationApplied = true;
                    }
                    break;
                }

                case "eventFirstOf":
//                case "eventFirstOfNeg":
                case "eventLastOf":
//                case "eventLastOfNeg":
                {
                    match(X, new TermMatcher.Is(CONJ));
                    neq(constraints, XX, YY);
                    boolean yNeg = pred.contains("Neg");
                    constraints.add(new SubOfConstraint(XX, YY, pred.contains("First") ? EventFirst : EventLast, yNeg ? -1 : +1).negIf(negated));
                    if (negated) {
                        negationApplied = true;
                    }
                    break;
                }

                case "eventOfPN":
                    neq(constraints, XX, YY);
                    match(X, new TermMatcher.Is(CONJ));

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

                case "eventCommon":


                    neq(constraints, XX, YY);
                    match(X, new TermMatcher.Is(CONJ));
                    match(Y, new TermMatcher.Is(CONJ));
                    constraints.add(new CommonSubEventConstraint(XX, YY));

                    break;


                case "subsMin":
                    match(X, new TermMatcher.SubsMin((short) $.intValue(Y)));
                    break;


//                case "notSet":
//                    /** deprecated soon */
//                    matchNot(X, new TermMatch.Is(Op.Set));
//                    break;


                case "is": {
                    int struct;
                    if (Y.op()==SETe) {
                        struct = 0;
                        for (Term yy : Y.subterms()) {
                            struct |= Op.the($.unquote(yy)).bit;
                        }
                    } else {
                        struct = Op.the($.unquote(Y)).bit;
                    }
                    match(X, new TermMatcher.Is(struct), !negated);
                    if (negated)
                        negationApplied = true;
                    break;
                }
                case "isVar": {
                    match(X, new TermMatcher.Is(Op.Variable), !negated);
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
                    match(X, new TermMatcher.IsUnneg(Op.the($.unquote(Y))), !negated);
                    if (negated)
                        negationApplied = true;
                    break;
                }

                case "has": {
                    //hasAny
                    hasAny(X, Op.the($.unquote(Y)), negated);
                    if (negated)
                        negationApplied = true;
                    break;
                }


                case "task":
                    String XString = X.toString();
                    switch (XString) {


                        case "\"?\"":
                            BytePredicate QUESTION_QUESTION = t -> t == QUESTION; //TODO toString() all of these
                            taskPunc = QUESTION_QUESTION;
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

                        case "\"?@\"":
                            taskPunc = t -> t == QUESTION || t == QUEST;
                            concPunc = c -> c;  //default
                            break;

                        case "\".!\"":
                            taskPunc = t -> t == BELIEF || t == GOAL;
                            concPunc = c -> c; //default
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

                case "hasBelief":
                    pre.add(new DoublePremiseRequired(true, true, true));
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
                        case "Ask":
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

        if (swap) {
            if (beliefTruthOp!=null)
                beliefTruthOp = new TruthFunc.SwappedTruth(beliefTruthOp); //use interned
            if (goalTruthOp!=null)
                goalTruthOp = new TruthFunc.SwappedTruth(goalTruthOp); //use interned
        }

        {
            CommutativeConstantPreFilter.tryFilter(true, taskPattern, beliefPattern, pre);
            CommutativeConstantPreFilter.tryFilter(false, taskPattern, beliefPattern, pre);
        }


        tryGuard(TaskTerm, taskPattern);
        tryGuard(BeliefTerm, beliefPattern);


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

        if (beliefTruthOp != null) {
            assert (concPunc.valueOf(BELIEF) == BELIEF || concPunc.valueOf(GOAL) == BELIEF || concPunc.valueOf(QUESTION) == BELIEF || concPunc.valueOf(QUEST) == BELIEF);
            if (!beliefTruthOp.single())
                pre.add(new DoublePremiseRequired(true, false, false));
        }
        if (goalTruthOp != null) {
            assert (concPunc.valueOf(BELIEF) == GOAL || concPunc.valueOf(GOAL) == GOAL || concPunc.valueOf(QUESTION) == GOAL || concPunc.valueOf(QUEST) == GOAL);
            if (!goalTruthOp.single())
                pre.add(new DoublePremiseRequired(false, true, false));
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
        List<RelationConstraint> mirrors = new FasterList(4);
        constraints.removeIf(cc -> {
//            PREDICATE<Derivation> post = cc.postFilter();
//            if (post!=null) {
//            }

            PREDICATE<Unify> p = cc.preFilter(taskPattern, beliefPattern);
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

        if (taskPunc == null)
            throw new UnsupportedOperationException("no taskPunc specified");

        if (concPunc == null)
            throw new UnsupportedOperationException("no concPunc specified");

        pre.add(new TaskPunc(taskPunc));

        this.truthify = Truthify.the(concPunc, beliefTruthOp, goalTruthOp, time);
        this.time = time;

        this.taskPunc = taskPunc;
        this.beliefTruth = beliefTruth;
        this.goalTruth = goalTruth;
        this.CONSTRAINTS = Sets.immutable.of(theInterned(constraints));


        this.termify = new Termify(conclusion(postcon[0]), truthify, time);


        this.PRE = preconditions();
//        this.constraintSet = CONSTRAINTS.toSet();

    }

    private void hasAny(Term x, Op o) {
        hasAny(x, o, false);
    }

    private void hasAny(Term x, Op o, boolean negated) {
        match(x, new TermMatcher.Has(o, true), !negated);
    }

    public static Term volMin(int volMin) {
        return $.func("volMin", $.the(volMin));
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

    private Term conclusion(Term x) {
        if (!x.unneg().op().var)
            return conclusionOptimize(ConcTransform.apply(PatternTermBuilder.patternify(x)));
        else
            return x;
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


        if (y0 != y) {
//                System.out.println(y + " -> " + y0);
        }
        return y;
    }

    private void tryGuard(PremiseTermAccessor r, Term root) {
        tryGuard(r, root,
                new ByteArrayList(2));
    }

    private void tryGuard(PremiseTermAccessor r, Term root, ByteArrayList p) {


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
                    tryGuard(r, root, p);
                }
                p.popByte();
            }
        }
    }


    private static final Map<Term, UnifyConstraint> constra =
            new ConcurrentHashMap<>();
            //new CustomConcurrentHashMap<>(STRONG, EQUALS, WEAK, EQUALS, 1024);

    private static UnifyConstraint intern(UnifyConstraint x) {
        UnifyConstraint y = constra.putIfAbsent(x.term(), x);
        return y != null ? y : x;
    }

    private static UnifyConstraint[] theInterned(MutableSet<UnifyConstraint> constraints) {
        if (constraints.isEmpty())
            return UnifyConstraint.EmptyUnifyConstraints;

        UnifyConstraint[] mc = UnifyConstraint.the(constraints);
        for (int i = 0, mcLength = mc.length; i < mcLength; i++)
            mc[i] = intern(mc[i]);
        return mc;
    }

    PremiseRuleSource(PremiseRuleSource raw) {
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


    public static Stream<PremiseRuleSource> parse(String src) {
        try {
            //bidi-rectional: swap premise components, and swap truth func param
            if (src.startsWith(BIDI_modifier)) {
                src = src.substring(BIDI_modifier.length());
                PremiseRuleSource a = new PremiseRuleSource(src, false);
                PremiseRuleSource b = new PremiseRuleSource(src, true);
                return a.equals(b) ? Stream.of(a) : Stream.of(a, b);
            } else {
                return Stream.of(new PremiseRuleSource(src));
            }
        } catch (Exception e) {
            throw new RuntimeException("rule parse:\n\t" + src, e);
        }
    }

    public static Stream<PremiseRuleSource> parse(String... rawRules) {
        return parse(Stream.of(rawRules));
    }

    public static Stream<PremiseRuleSource> parse(Stream<String> rawRules) {
        return rawRules.flatMap(PremiseRuleSource::parse);
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

    private void matchSuper(boolean taskOrBelief, TermMatcher m, boolean trueOrFalse) {
        pre.add(new TermMatch(m, trueOrFalse, false, taskOrBelief, ArrayUtils.EMPTY_BYTE_ARRAY));
    }


    private void match(boolean taskOrBelief, byte[] path, TermMatcher m, boolean isOrIsnt) {
        pre.add(new TermMatch(m, isOrIsnt, true, taskOrBelief, path));
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

    private void matchNot(Term x, TermMatcher m) {
        match(x, m, false);
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

    private void neq(Set<UnifyConstraint> constraints, Variable x, Term y) {

        if (y.op() == NEG && y.unneg() instanceof Variable) {
            constraints.add(new NotEqualConstraint.EqualNegConstraint(x, (Variable) (y.unneg())).neg());
        } else if (y instanceof Variable) {
            constraints.add(new NotEqualConstraint(x, (Variable) y));
        } else {
            match(x, new TermMatcher.Equals(y), false);
        }
    }

    private void neqRoot(Variable x, Variable y) {
        match(x, new TermMatcher.EqualsRoot(y), false);
        //constraints.addAt(new NotEqualConstraint.NotEqualRootConstraint(x, y));
    }

    public static Term pp(@Nullable byte[] b) {
        return b == null ? $.the(-1) /* null */ : $.p(b);
    }


    public final static PremiseTermAccessor TaskTerm = new PremiseTermAccessor(0, "taskTerm") {
        @Override
        public Term apply(PreDerivation d) {
            return d.taskTerm;
        }
    };

    public final static PremiseTermAccessor BeliefTerm = new PremiseTermAccessor(1, "beliefTerm") {

        @Override
        public Term apply(PreDerivation d) {
            return d.beliefTerm;
        }
    };


    static class UppercaseAtomsToPatternVariables extends DirectTermTransform {

        final UnifiedMap<String, Term> map = new UnifiedMap<>(4);

        static final ImmutableSet<Atomic> reservedMetaInfoCategories = Sets.immutable.of(
                Atomic.the("Belief"),
                Atomic.the("Goal"),
                Atomic.the("Punctuation"),
                Atomic.the("Time")
        );


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

    /** conclusion post-processing */
    private final TermTransform ConcTransform = new AbstractTermTransform() {
        @Override
        public Term applyCompound(Compound c) {

            c = Unifiable.transform(c, PremiseRuleSource.this, pre);

            return AbstractTermTransform.super.applyCompound(c);
        }
    };
}





