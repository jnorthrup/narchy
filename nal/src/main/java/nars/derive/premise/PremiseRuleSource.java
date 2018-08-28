package nars.derive.premise;

import jcog.data.map.CustomConcurrentHashMap;
import nars.$;
import nars.Narsese;
import nars.Op;
import nars.concept.Operator;
import nars.derive.Derivation;
import nars.derive.op.IntroVars;
import nars.derive.op.Occurrify;
import nars.derive.op.Termify;
import nars.derive.op.Truthify;
import nars.op.SubIfUnify;
import nars.subterm.BiSubterm;
import nars.subterm.Subterms;
import nars.term.*;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;
import nars.term.control.PREDICATE;
import nars.term.util.Image;
import nars.term.util.transform.TermTransform;
import nars.truth.func.NALTruth;
import nars.truth.func.TruthFunc;
import nars.unify.constraint.CommonSubEventConstraint;
import nars.unify.constraint.MatchConstraint;
import nars.unify.constraint.NotEqualConstraint;
import nars.unify.constraint.SubOfConstraint;
import nars.unify.match.Ellipsislike;
import nars.unify.op.TaskPunctuation;
import nars.unify.op.TermMatch;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static jcog.data.map.CustomConcurrentHashMap.*;
import static nars.Op.*;
import static nars.derive.Derivation.Task;
import static nars.subterm.util.SubtermCondition.*;
import static nars.unify.op.TaskPunctuation.Belief;
import static nars.unify.op.TaskPunctuation.Goal;

/**
 * A rule which matches a Premise and produces a Task
 * contains: preconditions, predicates, postconditions, post-evaluations and metainfo
 */
public class PremiseRuleSource extends ProxyTerm implements Function<PatternIndex, PremiseRuleProto> {

    private static final Pattern ruleImpl = Pattern.compile("\\|\\-");
    private final String source;
    public final Truthify truthify;
    /**
     * return this to being a inline evaluable functor
     */
    static final IntroVars introVars = new IntroVars();
    /**
     * conditions which can be tested before unification
     */

    private final MutableSet<MatchConstraint> constraints;
    protected final ImmutableSet<MatchConstraint> CONSTRAINTS;
    private final MutableSet<PREDICATE> pre;
    protected final PREDICATE[] PRE;
    protected final Occurrify.TaskTimeMerge time;
    protected final boolean varIntro;
    protected final byte taskPunc;
    protected final Term postcons;
    protected final byte puncOverride;
    protected final Term beliefTruth;
    protected final Term goalTruth;

    protected final Term taskPattern;
    protected final Term beliefPattern;
    protected final Term concPattern;


    private static final PatternIndex INDEX = new PatternIndex();
    protected final Termify termify;

    private PremiseRuleSource(String ruleSrc) throws Narsese.NarseseException {
        super(
                INDEX.rule(new UppercaseAtomsToPatternVariables().transform($.pFast(parseRuleComponents(ruleSrc))))
        );

        this.source = ruleSrc;

        constraints = new UnifiedSet(8);
        pre = new UnifiedSet(8);

        /**
         * deduplicate and generate match-optimized compounds for rules
         */


        Term[] precon = ref.sub(0).arrayShared();
        Term[] postcon = ref.sub(1).arrayShared();


        this.taskPattern = PatternIndex.patternify(precon[0]);
        this.beliefPattern = PatternIndex.patternify(precon[1]);
        if (beliefPattern.op() == Op.ATOM) {
            throw new RuntimeException("belief term must contain no atoms: " + beliefPattern);
        }

        this.concPattern = PatternIndex.patternify(postcon[0]);


        byte taskPunc = 0;


        for (int i = 2; i < precon.length; i++) {

            Term p = precon[i];

            boolean negated = p.op() == NEG;
            boolean negationApplied = false; //safety check to make sure semantic of negation was applied by the handler
            if (negated)
                p = p.unneg();

            Term name = Functor.func(p);
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
                case "neqRoot":
                    neqRoot(constraints, X, Y);
                    break;

//                case "neqTaskBelief":
//                    pre.add(neqTaskBelief);
//                    break;

//                case "neqUnneg":
//                    constraints.add(new NotEqualConstraint.NotEqualUnnegConstraint(X, Y));
//                    constraints.add(new NotEqualConstraint.NotEqualUnnegConstraint(Y, X));
//                    break;

//                case "neqAndCom":
//                    neqRoot(constraints, X, Y);
//                    constraints.add(new CommonSubtermConstraint(X, Y));
//                    constraints.add(new CommonSubtermConstraint(Y, X));
//                    break;


                case "neqRCom":
                    neqRoot(constraints, X, Y);
                    constraints.add(new NotEqualConstraint.NeqRootAndNotRecursiveSubtermOf(X, Y));
                    constraints.add(new NotEqualConstraint.NeqRootAndNotRecursiveSubtermOf(Y, X));
                    break;

//                case "opSECTe":
//                    termIs(pre, taskPattern, beliefPattern, constraints, X, Op.SECTe);
//                    break;
//                case "opSECTi":
//                    termIs(pre, taskPattern, beliefPattern, constraints, X, Op.SECTi);
//                    break;


                case "subOf": {

                    if (!negated)
                        neq(constraints, X, Y);

                    constraints.add(new SubOfConstraint(X, Y, false, Subterm).negIf(negated));
                    constraints.add(new SubOfConstraint(Y, X, true, Subterm).negIf(negated));

                    if (negated)
                        negationApplied = true;
                    break;
                }

//                case "subOfNeg":
//
//                    neq(constraints, X, Y);
//                    constraints.add(new SubOfConstraint(X, Y, false, false, Subterm, -1));
//                    constraints.add(new SubOfConstraint(Y, X, true, false, Subterm, -1));
//                    break;

                case "subPosOrNeg":
                    neq(constraints, X, Y);
                    constraints.add(new SubOfConstraint(X, Y, false, Subterm, 0));
                    constraints.add(new SubOfConstraint(Y, X, true, Subterm, 0));
                    break;

                case "in":
                    neq(constraints, X, Y);
                    constraints.add(new SubOfConstraint(X, Y, false, Recursive));
                    constraints.add(new SubOfConstraint(Y, X, true, Recursive));
                    break;

//                case "inNeg":
//
//                    neq(constraints, X, Y);
//                    constraints.add(new SubOfConstraint(X, Y, false, false, Recursive, -1));
//                    constraints.add(new SubOfConstraint(Y, X, true, false, Recursive, -1));
//                    break;

                case "eventOf":
                    neq(constraints, X, Y);


                    match(X, new TermMatch.Is(CONJ));

                    constraints.add(new SubOfConstraint(X, Y, false, Event));
                    constraints.add(new SubOfConstraint(Y, X, true, Event));
                    break;

                case "eventOfNeg":
                    neq(constraints, X, Y);


                    match(X, new TermMatch.Is(CONJ));

                    constraints.add(new SubOfConstraint(X, Y, false, Event, -1));
                    constraints.add(new SubOfConstraint(Y, X, true, Event, -1));
                    break;

                case "eventOfPosOrNeg":
                    neq(constraints, X, Y);


                    match(X, new TermMatch.Is(CONJ));

                    constraints.add(new SubOfConstraint(X, Y, false, Event, 0));
                    constraints.add(new SubOfConstraint(Y, X, true, Event, 0));
                    break;

                case "eventsOf":
                    neq(constraints, X, Y);


                    match(X, new TermMatch.Is(CONJ));

                    constraints.add(new SubOfConstraint(X, Y, false, Events, 1));
                    constraints.add(new SubOfConstraint(Y, X, true, Events, 1));
                    break;

                case "eventCommon":


                    neq(constraints, X, Y);
                    match(X, new TermMatch.Is(CONJ));
                    match(Y, new TermMatch.Is(CONJ));
                    constraints.add(new CommonSubEventConstraint(X, Y));
                    constraints.add(new CommonSubEventConstraint(Y, X));
                    break;

//                case "eqOrIn":
//                    constraints.add(new SubOfConstraint(X, Y, false, true, Recursive));
//                    constraints.add(new SubOfConstraint(Y, X, true, true, Recursive));
//                    break;


                case "subsMin":
                    match(X, new TermMatch.SubsMin((short) $.intValue(Y)));
                    break;

//                case "imaged": {
//                    //@Deprecated use subOf and --subOf for both / and \
//                    if (!taskPattern.containsRecursively(X) && !taskPattern.equals(X))
//                        throw new TODO("expected/tested occurrence in task concPattern ");
//
//                    final byte[] pp = Terms.constantPath(taskPattern, X);
//                    assert pp != null;
//                    pre.add(new Imaged(X, !negated, pp));
//                    if (negated) negationApplied = true;
//                    break;
//                }


                case "notSet":
                    /** deprecated soon */
                    matchNot(X, new TermMatch.Is(Op.Set));
                    break;


                case "is": {
                    match(X, new TermMatch.Is(Op.the($.unquote(Y))), !negated);
                    if (negated)
                        negationApplied = true;
                    break;
                }

                case "has": {
                    //hasAny
                    match(X, new TermMatch.Has(Op.the($.unquote(Y)), true), !negated);
                    if (negated)
                        negationApplied = true;
                    break;
                }


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

        Term[] modifiers = postcon != null && postcon.length > 1 ? Terms.sorted(postcon[1].arrayShared()) : Op.EmptyTermArray;
        boolean varIntro = false;
        for (Term m : modifiers) {
            if (m.op() != Op.INH)
                throw new RuntimeException("Unknown postcondition format: " + m);

            Term type = m.sub(1);
            Term which = m.sub(0);

            switch (type.toString()) {

                case "Also":
                    switch (which.toString()) {
                        case "VarIntro":
                            varIntro = true;
                            break;
                    }
                    break;
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

        Op to = taskPattern.op();
        boolean taskIsPatVar = to == Op.VAR_PATTERN;
        Op bo = beliefPattern.op();
        boolean belIsPatVar = bo == Op.VAR_PATTERN;


        TruthFunc beliefTruthOp = NALTruth.get(beliefTruth);
        if (beliefTruth != null && beliefTruthOp == null)
            throw new RuntimeException("unknown BeliefFunction: " + beliefTruth);

        TruthFunc goalTruthOp = NALTruth.get(goalTruth);
        if (goalTruth != null && goalTruthOp == null)
            throw new RuntimeException("unknown GoalFunction: " + goalTruth);


        Term finalConcPattern = this.concPattern;


        {
            //subIfUnify prefilter
            Term concFunc = Functor.func(finalConcPattern);
            if (concFunc.equals(SubIfUnify.SubIfUnify)) {

                Subterms a = Operator.args(finalConcPattern);
                UnifyPreFilter.tryAdd(a.sub(1), a.sub(2),
                        taskPattern, beliefPattern,
                        a, pre);

            }
        }

        if (!taskIsPatVar) {
            pre.add(new TermMatchPred(new TermMatch.Is(to), true, true, TaskTerm));

            int ts = taskPattern.structure() & (~Op.VAR_PATTERN.bit);
            if (Integer.bitCount(ts) > 1) {
                //if there are additional bits that the structure can filter, include the hasAll predicate
                pre.add(new TermMatchPred<>(new TermMatch.Has(
                        ts, false /* all */, taskPattern.complexity()),
                        true, true, TaskTerm));
            }
        }

        if (!belIsPatVar) {
            pre.add(new TermMatchPred<>(new TermMatch.Is(bo), true, true, BeliefTerm));
            int bs = beliefPattern.structure() & (~Op.VAR_PATTERN.bit);
            if (Integer.bitCount(bs) > 1) {
                //if there are additional bits that the structure can filter, include the hasAll predicate
                pre.add(new TermMatchPred<>(new TermMatch.Has(
                        bs, false /* all */, beliefPattern.complexity()),
                        true, true, BeliefTerm));
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
            } else if (puncOverride == GOAL && (taskPunc == QUESTION || taskPunc == QUEST)) {
                pre.add(new DoublePremiseRequired(false, false, true));
            }
        }

        /*System.out.println( Long.toBinaryString(
                        pStructure) + " " + concPattern
        );*/


        constraints.forEach(c -> {
            PREDICATE<Derivation> p = c.preFilter(taskPattern, beliefPattern);
            if (p != null) {
                pre.add(p);
            }
        });

        {


            assert (puncOverride > 0) || !taskPattern.equals(concPattern) :
                    "punctuation not modified yet rule task equals pattern: " + this;


            if (taskPunc == 0) {
                //no override, determine automaticaly by presence of belief or truth

                boolean b = false, g = false;
                //for (PostCondition x : POST) {
                if (puncOverride != 0) {
                    throw new RuntimeException("puncOverride with no input punc specifier");
                } else {
                    b |= beliefTruth != null;
                    g |= goalTruth != null;
                }
                //}

                if (!b && !g) {
                    throw new RuntimeException(ruleSrc + "\n^\tcan not assume this applies only to questions");
                } else if (b && g) {
                    pre.add(TaskPunctuation.BeliefOrGoal);
                } else if (b) {
                    pre.add(Belief);
                } else /* if (g) */ {
                    pre.add(Goal);
                }
            }
        }

        this.truthify = Truthify.the(puncOverride, beliefTruthOp, goalTruthOp, time);
        this.time = time;
        this.varIntro = varIntro;
        this.taskPunc = taskPunc;

        this.postcons = postcon[0];
        this.puncOverride = puncOverride;
        this.beliefTruth = beliefTruth;
        this.goalTruth = goalTruth;
        this.CONSTRAINTS = Sets.immutable.of(theInterned(constraints));


        int rules = pre.size();
        PREDICATE[] PRE = pre.toArray(new PREDICATE[rules + 1 /* extra to be filled in later stage */]);
        ArrayUtils.sort(PRE, 0, rules - 1, (x) -> -x.cost());
        assert (PRE[PRE.length - 1] == null);
        //Arrays.sort(PRE, 0, rules, sortByCostIncreasing);
        assert rules <= 1 || (PRE[0].cost() <= PRE[rules - 2].cost()); //increasing cost

        //not working yet:
//        for (int i = 0, preLength = PRE.length; i < preLength; i++) {
//            PRE[i] = INDEX.intern(PRE[i]);
//        }
        this.termify = new Termify(concPattern, truthify, time);

        this.PRE = PRE;

    }


    private static final Map<Term, MatchConstraint> constra =
            new CustomConcurrentHashMap<>(STRONG, EQUALS, WEAK, EQUALS, 1024);

    private static MatchConstraint intern(MatchConstraint x) {
        MatchConstraint y = constra.putIfAbsent(x.term(), x);
        return y != null ? y : x;
    }

    private static MatchConstraint[] theInterned(MutableSet<MatchConstraint> constraints) {
        if (constraints.isEmpty())
            return MatchConstraint.EmptyMatchConstraints;

        MatchConstraint[] mc = MatchConstraint.the(constraints);
        for (int i = 0, mcLength = mc.length; i < mcLength; i++)
            mc[i] = intern(mc[i]);
        return mc;
    }

    protected PremiseRuleSource(PremiseRuleSource raw, PatternIndex index) {
        super((index.rule(raw.ref)));

        this.termify = raw.termify;
        this.PRE = raw.PRE.clone(); //because it gets modified when adding Branchify suffix
        this.CONSTRAINTS = null;
        this.source = raw.source;
        this.truthify = raw.truthify;
        this.constraints = raw.constraints;
        this.pre = raw.pre;
        this.time = raw.time;
        this.varIntro = raw.varIntro;
        this.taskPunc = raw.taskPunc;
        this.postcons = raw.postcons;
        this.puncOverride = raw.puncOverride;
        this.beliefTruth = raw.beliefTruth;
        this.goalTruth = raw.goalTruth;
        this.taskPattern = raw.taskPattern;
        this.beliefPattern = raw.beliefPattern;
        this.concPattern = raw.concPattern;

    }

    public static PremiseRuleSource parse(String ruleSrc) throws Narsese.NarseseException {
        return new PremiseRuleSource(ruleSrc);
    }

    public static Stream<PremiseRuleSource> parse(String... rawRules) {
        return parse(Stream.of(rawRules));
    }

    public static Stream<PremiseRuleSource> parse(Stream<String> rawRules) {
        return rawRules.map(src -> {
            try {
                return parse(src);
            } catch (Exception e) {
                throw new RuntimeException("rule parse: " + e.getCause() + "\n\t" + src);
            }
        });

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

    private void matchSuper(boolean taskOrBelief, TermMatch m, boolean trueOrFalse) {
        pre.add(new TermMatchPred(m, trueOrFalse, false, TaskOrBelief(taskOrBelief)));
    }


    private void match(boolean taskOrBelief, byte[] path, TermMatch m, boolean isOrIsnt) {
        if (path.length == 0) {
            //root
            pre.add(new TermMatchPred<>(m, isOrIsnt, true, TaskOrBelief(taskOrBelief)));
        } else {
            //subterm
            pre.add(new TermMatchPred.Subterm(path, m, isOrIsnt, TaskOrBelief(taskOrBelief)));
        }
    }

    @Override
    public PremiseRuleProto apply(PatternIndex i) {
        return new PremiseRuleProto(this, i);
    }


    private void match(Term x,
                       BiConsumer<byte[], byte[]> preDerivationExactFilter,
                       BiConsumer<Boolean, Boolean> preDerivationSuperFilter /* task,belief*/
    ) {


        boolean checkedTask = false, checkedBelief = false;

        final byte[] pt = (taskPattern.equals(x) || !taskPattern.ORrecurse(s -> s instanceof Ellipsislike)) ?
                Terms.constantPath(taskPattern, x) : null;
        final byte[] pb = (beliefPattern.equals(x) || !beliefPattern.ORrecurse(s -> s instanceof Ellipsislike)) ?
                Terms.constantPath(beliefPattern, x) : null;
        if (pt != null || pb != null) {
            if (pt != null)
                checkedTask = true;
            if (pb != null)
                checkedBelief = true;

            preDerivationExactFilter.accept(pt, pb);
        }

        if (!checkedTask && !checkedBelief) {
            //non-exact filter
            boolean inTask = (taskPattern.equals(x) || taskPattern.containsRecursively(x));
            boolean inBelief = (beliefPattern.equals(x) || beliefPattern.containsRecursively(x));
            preDerivationSuperFilter.accept(inTask, inBelief);
        }
    }


    private void match(Term x, TermMatch m) {
        match(x, m, true);
    }

    private void matchNot(Term x, TermMatch m) {
        match(x, m, false);
    }

    private void match(Term x, TermMatch m, boolean trueOrFalse) {
        match(x, (pathInTask, pathInBelief) -> {

                    if (pathInTask != null)
                        match(true, pathInTask, m, trueOrFalse);
                    if (pathInBelief != null)
                        match(false, pathInBelief, m, trueOrFalse);

                }, (inTask, inBelief) -> {

                    if (inTask)
                        matchSuper(true, m, trueOrFalse);
                    if (inBelief)
                        matchSuper(false, m, trueOrFalse);

                    constraints.add(m.constraint(x, trueOrFalse));
                }
        );
    }


//    void subsMin(Term X, int min) {
//        if (taskPattern.equals(X)) {
//            pre.add(new SubsMin.SubsMinProto(true, min));
//        } else if (beliefPattern.equals(X)) {
//            pre.add(new SubsMin.SubsMinProto(false, min));
//        } else {
//            constraints.add(new SubsMin(X, min));
//        }
//
//        //TODO
//        //filter(x, (pt, pb)->
//
//    }


//    private void termHasNot(Term taskPattern, Term beliefPattern, Collection<PrediTerm> pre, Set<MatchConstraint> constraints, Term t, int structure) {
//
////        //TODO: filter(x, )
////
////        boolean inTask = taskPattern.equals(t) || taskPattern.containsRecursively(t);
////        boolean inBelief = beliefPattern.equals(t) || beliefPattern.containsRecursively(t);
////        if (inTask || inBelief) {
////            if (inTask)
////                pre.addAll(TaskBeliefHas.get(true, structure, false));
////            if (inBelief)
////                pre.addAll(TaskBeliefHas.get(false, structure, false));
////        } else {
////
////            throw new TODO();
////        }
//
//    }

    private static void neq(Set<MatchConstraint> constraints, Term x, Term y) {
        constraints.add(new NotEqualConstraint(x, y));
        constraints.add(new NotEqualConstraint(y, x));
    }

    private static void neqRoot(Set<MatchConstraint> constraints, Term x, Term y) {
        constraints.add(new NotEqualConstraint.NotEqualRootConstraint(x, y));
        constraints.add(new NotEqualConstraint.NotEqualRootConstraint(y, x));
    }

    public static Term pp(byte[] b) {
        return b == null ? Op.EmptyProduct : $.p(b);
    }

    static class MatchFilter extends AbstractPred<Derivation> {

        private static final Atom MATCH_FILTER = (Atom) Atomic.the(MatchFilter.class.getSimpleName());
        private final Term pattern;
        private final boolean isTaskOrBelief;

        protected MatchFilter(Term pattern, boolean isTaskOrBelief) {
            super($.func(MATCH_FILTER, pattern, isTaskOrBelief ? Task : Belief));
            this.isTaskOrBelief = isTaskOrBelief;
            this.pattern = pattern;
        }

        @Override
        public boolean test(Derivation derivation) {
            return false;
        }
    }

    static class UnifyPreFilter extends AbstractPred<Derivation> {

        private final byte[] xpInT, xpInB, ypInT, ypInB;
        private final boolean isStrict;

        private static final Atomic UnifyPreFilter = Atomic.the("unifyPreFilter");
        private final int varBits;

        UnifyPreFilter(byte[] xpInT, byte[] xpInB, byte[] ypInT, byte[] ypInB, int varBits, boolean isStrict) {
            super($.func(UnifyPreFilter, pp(xpInT), pp(xpInB), pp(ypInT), pp(ypInB)));
            this.xpInT = xpInT;
            this.xpInB = xpInB;
            this.ypInT = ypInT;
            this.ypInB = ypInB;
            this.varBits = varBits;
            this.isStrict = isStrict;
        }

        static void tryAdd(Term x, Term y, Term taskPattern, Term beliefPattern, Subterms a, MutableSet<PREDICATE> pre) {
            //some structure exists that can be used to prefilter
            byte[] xpInT = Terms.constantPath(taskPattern, x);
            byte[] xpInB = Terms.constantPath(beliefPattern, x); //try the belief
            if (xpInT != null || xpInB != null) {
                byte[] ypInT = Terms.constantPath(taskPattern, y);
                byte[] ypInB = Terms.constantPath(beliefPattern, y); //try the belief
                if (ypInT != null || ypInB != null) {

                    //the unifying terms are deterministicaly extractable from the task or belief:

                    int varBits = (a.contains(SubIfUnify.DEP_VAR)) ? Op.VAR_DEP.bit : (Op.VAR_INDEP.bit | Op.VAR_DEP.bit);

                    boolean strict = a.contains(SubIfUnify.STRICT);

                    pre.add(new UnifyPreFilter(xpInT, xpInB, ypInT, ypInB, varBits, strict));
                }
            }
        }


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

            return possibleUnification(x, y, varBits, 0);
        }

        public boolean possibleUnification(Term x, Term y, int varExcluded, int level) {
            boolean xEqY = x.equals(y);
            if (xEqY) {
                return level > 0 || !isStrict;
            }

            Op xo = x.op();
            if ((xo.bit & ~varExcluded) == 0)
                return true; //unifies, allow

            Op yo = y.op();
            if ((yo.bit & ~varExcluded) == 0)
                return true; //unifies, allow

            if (xo != yo)
                return false;


            x = Image.imageNormalize(x);
            y = Image.imageNormalize(y);

            Subterms xx = x.subterms(), yy = y.subterms();
            int xxs = xx.subs();
            if (xxs != yy.subs())
                return false;

            if (!Subterms.possiblyUnifiable(xx, yy, varExcluded))
                return false;

            if (xo.commutative)
                return true;

            //if (mustUnify(xx) || mustUnify(yy)) {
            //if (!xo.commutative) {

            int nextLevel = level + 1;
            for (int i = 0; i < xxs; i++) {
                Term x0 = xx.sub(i), y0 = yy.sub(i);
                if (!possibleUnification(x0, y0, varExcluded, nextLevel))
                    return false;
            }
            return true;
        }


        @Override
        public float cost() {
            return 0.3f;
        }
    }

    static class DoublePremiseRequired extends AbstractPred<Derivation> {

        final static Atomic key = (Atomic) $.the("DoublePremise");
        final boolean ifBelief, ifGoal, ifQuestionOrQuest;

        DoublePremiseRequired(boolean ifBelief, boolean ifGoal, boolean ifQuestionOrQuest) {
            super($.func(key,
                    ifBelief ? Op.Belief : Op.EmptyProduct,
                    ifGoal ? Op.Goal : Op.EmptyProduct,
                    ifQuestionOrQuest ? Op.Que : Op.EmptyProduct));
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

//    static final class SubOf extends AbstractPred<Derivation> {
//        private final Term y;
//
//        boolean task;
//        boolean belief;
//
//        SubOf(Term y, Term taskPattern, Term x, Term beliefPattern) {
//            super($.func("subOf", $.quote(y.toString())));
//            this.y = y;
//
//            task = taskPattern.containsRecursively(x);
//            belief = beliefPattern.containsRecursively(y);
//            assert task || belief;
//        }
//
//        @Override
//        public boolean test(Derivation preDerivation) {
//            if (task && !preDerivation.taskTerm.containsRecursively(y))
//                return false;
//            return !belief || preDerivation.beliefTerm.containsRecursively(y);
//        }
//
//        @Override
//        public float cost() {
//            return 0.5f;
//        }
//    }

//    static final class Imaged extends AbstractPred<Derivation> {
//        private final byte[] pp;
//        private final boolean isOrIsnt;
//
//        Imaged(Term x, boolean hasOrHasnt, byte[] pp) {
//            super($.func("imaged", x));
//            this.pp = pp;
//            this.isOrIsnt = hasOrHasnt;
//        }
//
//        @Override
//        public float cost() {
//            return 0.1f;
//        }
//
//        @Override
//        public boolean test(Derivation o) {
//            Term prod = o.taskTerm.subPath(pp);
//            return prod.op() == PROD && (isOrIsnt==Image.imaged(prod));
//        }
//    }


    private static Function<PreDerivation, Term> TaskOrBelief(boolean taskOrBelief) {
        return taskOrBelief ? TaskTerm : BeliefTerm;
    }

    final static Function<PreDerivation, Term> TaskTerm = new Function<>() {

        @Override
        public String toString() {
            return "taskTerm";
        }

        @Override
        public Term apply(PreDerivation d) {
            return d.taskTerm;
        }
    };

    final static Function<PreDerivation, Term> BeliefTerm = new Function<>() {

        @Override
        public String toString() {
            return "beliefTerm";
        }

        @Override
        public Term apply(PreDerivation d) {
            return d.beliefTerm;
        }
    };


    static class UppercaseAtomsToPatternVariables extends UnifiedMap<String, Term> implements TermTransform.NegObliviousTermTransform {

        static final ImmutableSet<Atomic> reservedMetaInfoCategories = Sets.immutable.of(
                Atomic.the("Belief"),
                Atomic.the("Goal"),
                Atomic.the("Punctuation"),
                Atomic.the("Time")
        );


        UppercaseAtomsToPatternVariables() {
            super(8);
        }

        @Override
        public Term transformAtomic(Atomic atomic) {
            if (atomic instanceof Atom) {
                if (!reservedMetaInfoCategories.contains(atomic)) {
                    String name = atomic.toString();
                    if (name.length() == 1 && Character.isUpperCase(name.charAt(0))) {
                        return this.computeIfAbsent(name, (n) -> $.varPattern(1 + this.size()));

                    }
                }
            }
            return atomic;
        }

    }

}





