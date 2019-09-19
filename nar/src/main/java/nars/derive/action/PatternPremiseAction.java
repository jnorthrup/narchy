package nars.derive.action;

import jcog.data.list.FasterList;
import jcog.data.set.ArrayHashSet;
import nars.$;
import nars.Builtin;
import nars.Narsese;
import nars.Op;
import nars.derive.Derivation;
import nars.derive.op.*;
import nars.derive.premise.PremiseRuleNormalization;
import nars.derive.rule.ConditionalPremiseRuleBuilder;
import nars.derive.rule.PremiseRule;
import nars.derive.rule.PremiseRuleBuilder;
import nars.derive.rule.RuleWhy;
import nars.op.UniSubst;
import nars.subterm.BiSubterm;
import nars.subterm.Subterms;
import nars.term.Variable;
import nars.term.*;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.control.AND;
import nars.term.control.PREDICATE;
import nars.term.util.TermException;
import nars.term.util.conj.ConjMatch;
import nars.term.util.transform.AbstractTermTransform;
import nars.truth.func.NALTruth;
import nars.truth.func.TruthFunction;
import nars.truth.func.TruthModel;
import org.eclipse.collections.api.block.function.primitive.ByteToByteFunction;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static nars.Op.*;
import static nars.derive.premise.PatternTermBuilder.patternify;
import static nars.term.atom.Bool.Null;
import static nars.time.Tense.DTERNAL;

/**
 * A rule which matches a Premise and produces a Task
 * contains: preconditions, predicates, postconditions, post-evaluations and metainfo
 */
public class PatternPremiseAction extends ConditionalPremiseRuleBuilder {

    private final TruthModel truthModel;

    protected Occurrify.OccurrenceSolver time;
    protected Term beliefTruth, goalTruth;
    Truthify truthify;
    public Termify termify;


    private Term concTerm;
    /** TODO just use one punc->punc transfer function, and its return of 0 to deny */
    protected BytePredicate taskPunc = null;
    @Deprecated protected ByteToByteFunction concPunc = null;

    @Deprecated transient private boolean concBelief = false, concQuestion = false, concGoal = false, concQuest = false;



    public PatternPremiseAction(TruthModel truthModel) {
        this.truthModel = truthModel;
    }


    public static PremiseRuleBuilder parseSafe(String ruleSrc)  {
        try {
            return parse(ruleSrc);
        } catch (Narsese.NarseseException e) {
            throw new RuntimeException("rule parse failure:\n" + ruleSrc, e);
        }
    }

    public static PatternPremiseAction parse(String ruleSrc) throws Narsese.NarseseException {
        return parse(ruleSrc, NALTruth.the);
    }

    public static PatternPremiseAction parse(String ruleSrc, TruthModel truthModel) throws Narsese.NarseseException {
        PatternPremiseAction r = new PatternPremiseAction(truthModel);
        r._parse(ruleSrc);
        return r;
    }

    protected void _parse(String ruleSrc) throws Narsese.NarseseException {
        this.source = ruleSrc;
        this.id = new MyPremiseRuleNormalization().apply(
            new UppercaseAtomsToPatternVariables().apply(
                $.pFast(parseRuleComponents(ruleSrc))
            )
        );

        Term[] precon = id.sub(0).arrayShared();

        taskPattern(precon[0]);
        beliefPattern(precon[1]);









        Term[] postcon = id.sub(1).arrayShared();
        concTerm = postcon[0];


        for (int i = 2; i < precon.length; i++) {
            cond(precon[i]);
        }

        time = null;

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
                            assert (beliefTruth != null && goalTruth != null);
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
                    time = Occurrify.solvers.get(which);
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


    }

    @Override protected void cond(Term o, boolean negated, boolean[] _negationApplied, String pred, Term x, Term y, nars.term.Variable Xv, Variable Yv, Variable Xvs, Variable Yvs) {
        super.cond(o, negated, _negationApplied, pred, x, y, Xv, Yv, Xvs, Yvs);
        switch (pred) {
            case "task":
                String XString = x.toString();
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
                    case "all":
                        taskPunc = t -> true;
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

        }
    }


    private static Truthify intern(Truthify x) {
        Truthify y = truthifies.putIfAbsent(x.term(), x);
        return y != null ? y : x;
    }

    public static Stream<PremiseRule> parse(String... rules) {
        return parse(Stream.of(rules));
    }

    public static Stream<PremiseRule> parse(Stream<String> rules) {
        return rules.map(PatternPremiseAction::parseSafe).map(PremiseRuleBuilder::get).distinct();
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


    //    private void matchSuper(boolean taskOrBelief, TermMatcher m, boolean trueOrFalse) {
//        byte[] path = ArrayUtil.EMPTY_BYTE_ARRAY;
//        pre.add(new TermMatch(m, trueOrFalse, false, TaskOrBelief(taskOrBelief).path(path), cost(path.length)));
//    }


    private PremiseAction action(PREDICATE<Derivation>[] y, RuleWhy cause) {
        return new TruthifyDeriveAction(cause, truthify, AND.the(y));
    }

    protected void commit() {

        super.commit();

        TruthFunction beliefTruthOp = truthModel.get(beliefTruth);
        if (beliefTruth != null && beliefTruthOp == null)
            throw new RuntimeException("unknown BeliefFunction: " + beliefTruth);

        TruthFunction goalTruthOp = truthModel.get(goalTruth);
        if (goalTruth != null && goalTruthOp == null)
            throw new RuntimeException("unknown GoalFunction: " + goalTruth);







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

            assert (concBelief || concQuest || concQuestion || concGoal);
        }



        /** infer necessary double premise for derived belief  */

        boolean doubleBelief = false, doubleGoal = false;
        if (beliefTruthOp != null) {
            assert (concPunc.valueOf(BELIEF) == BELIEF || concPunc.valueOf(GOAL) == BELIEF || concPunc.valueOf(QUESTION) == BELIEF || concPunc.valueOf(QUEST) == BELIEF);
            if (!beliefTruthOp.single()) {
                doubleBelief = true;
            }
        }
        /** infer necessary double premise for derived goal  */
        if (goalTruthOp != null) {
            assert (concPunc.valueOf(BELIEF) == GOAL || concPunc.valueOf(GOAL) == GOAL || concPunc.valueOf(QUESTION) == GOAL || concPunc.valueOf(QUEST) == GOAL);
            if (!goalTruthOp.single()) {
                doubleGoal = true;
            }
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



        if (concPunc == null)
            throw new UnsupportedOperationException("no concPunc specified");


        if (time == null) {
            if (!doubleBelief && !doubleGoal)
                time = Occurrify.solverDefaultSingle;
            else
                time = Occurrify.solverDefaultDouble;
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

        PuncMap tp = new PuncMap(
            PuncMap.p(taskPunc, concPunc, BELIEF),
            PuncMap.p(taskPunc, concPunc, GOAL),
            PuncMap.p(taskPunc, concPunc, QUESTION),
            PuncMap.p(taskPunc, concPunc, QUEST),
            (byte) 0 //COMMAND
        );
        if (!tp.all())
            pre.add(tp); //add filter to allow only the mapped types

        if (doubleBelief || doubleGoal) {
            if (beliefPattern.op() != VAR_PATTERN && !beliefPattern.op().taskable)
                throw new TermException("double premise may be required and belief pattern is not taskable", beliefPattern);

            boolean forBelief = doubleBelief && tp.get(BELIEF)==BELIEF;
            boolean forGoal = doubleGoal && tp.get(GOAL)==GOAL;
            boolean forQ = (doubleBelief && (tp.get(QUESTION)==BELIEF || tp.get(QUEST)==BELIEF))
                ||
                (doubleGoal && (tp.get(QUESTION)==GOAL || tp.get(QUEST)==GOAL));
            if (forBelief || forGoal || forQ) {
                //pre.add(new S(forBelief, forGoal, forQ));
                pre.add(new SingleOrDoublePremise(new PuncMap(forBelief, forGoal, forQ, forQ), false));
            }
        }

        this.truthify = intern(Truthify.the(tp, beliefTruthOp, goalTruthOp, time));

        this.termify = new Termify(conclusion(concTerm), truthify, time);

    }


    @Override protected PremiseAction action(RuleWhy cause) {
        Taskify taskify = new Taskify(termify, cause);

        int numConstraints = CONSTRAINTS.length;
        PREDICATE<Derivation>[] y = new PREDICATE[1 + numConstraints];

        System.arraycopy(CONSTRAINTS, 0, y, 0, numConstraints);

        y[numConstraints] = new DirectPremiseUnify(taskPattern, beliefPattern, taskify)
                //new CachingPremisify //<- not ready yet
                ;

        return action(y, cause);
    }


    private static final Pattern ruleImpl = Pattern.compile("\\|-");

    private static final Term eteConj = $.the("eteConj");

    private static final Map<Term, Truthify> truthifies = new ConcurrentHashMap<>();


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

    /**
     * conclusion post-processing
     */
    private final AbstractTermTransform.NegObliviousTermTransform ConcTransform = new AbstractTermTransform.NegObliviousTermTransform() {
        @Override
        public Term applyPosCompound(Compound c) {

            Term f = Functor.func(c);
            if (f != Null) {
                Subterms a = Functor.args(c);
                if (f.equals(UniSubst.unisubst)) {
                    Unifiable.constrainUnifiable(a, PatternPremiseAction.this);
                } else if (f.equals(ConjMatch.BEFORE) || f.equals(ConjMatch.AFTER)) {
                    Unifiable.constraintEvent(a, PatternPremiseAction.this, true);
                } else if (f.equals(Derivation.SUBSTITUTE)) {
                    Unifiable.constrainSubstitute(a, PatternPremiseAction.this);
                } else if (f.equals(Derivation.CONJ_WITHOUT)) {
                    Unifiable.constraintEvent(a, PatternPremiseAction.this, false);
                }
            }
            return super.applyPosCompound(c);
        }
    };

    public static class TruthifyDeriveAction extends PremiseAction {

        /** 2nd stage filter and evaluator/ranker */
        public final Truthify truth;

        public final PREDICATE<Derivation> action;

        public TruthifyDeriveAction(RuleWhy cause, Truthify pre, PREDICATE<Derivation> post) {
            super(cause);
            this.action = post;
            this.truth = pre;
        }

        /**
         * compute probabilistic throttle value, in consideration of the premise's task and the punctuation outcome
         * with respect to the deriver's punctuation equalization
         *
         * returning 0 invalidates this action for the provided derivation
         *
         * TO BE REFINED
         */
        @Override
        public final float pri(Derivation d) {

            float causeValue = why.amp();
            if (causeValue < Float.MIN_NORMAL)
                return 0f; //disabled

            byte punc = truth.preFilter(d);
            if (punc == 0)
                return 0f; //disabled or not applicable to the premise

            float puncFactor = d.preAmp(d.punc);
            if (puncFactor < Float.MIN_NORMAL)
                return 0f; //entirely disabled by deriver

            if (!truth.test(d))
                return 0;

            return causeValue * puncFactor * d.what.derivePri.prePri(d);
        }

        @Override
        public String toString() {
            return why.rule.toString();
        }

        @Override
        public final void run(Derivation d) {
            action.test(d);
        }

    }
}





