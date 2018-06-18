package nars.derive.premise;

import com.google.common.collect.Iterables;
import jcog.TODO;
import nars.$;
import nars.Narsese;
import nars.Op;
import nars.concept.Operator;
import nars.derive.Derivation;
import nars.derive.step.IntroVars;
import nars.derive.step.Occurrify;
import nars.derive.step.Truthify;
import nars.op.SubIfUnify;
import nars.op.Subst;
import nars.subterm.Subterms;
import nars.term.*;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.compound.util.Image;
import nars.term.control.AbstractPred;
import nars.term.control.PrediTerm;
import nars.truth.func.NALTruth;
import nars.truth.func.TruthFunc;
import nars.unify.constraint.*;
import nars.unify.match.Ellipsislike;
import nars.unify.op.TaskBeliefHas;
import nars.unify.op.TaskBeliefIs;
import nars.unify.op.TaskPunctuation;
import nars.util.term.transform.TermTransform;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.Collection;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static nars.Op.*;
import static nars.subterm.util.Contains.*;
import static nars.unify.op.TaskPunctuation.Belief;
import static nars.unify.op.TaskPunctuation.Goal;

/**
 * A rule which matches a Premise and produces a Task
 * contains: preconditions, predicates, postconditions, post-evaluations and metainfo
 */
public class PremiseDeriverSource extends ProxyTerm implements Function<PremisePatternIndex, PremiseDeriverProto> {

    static final Pattern ruleImpl = Pattern.compile("\\|\\-");
    public final String source;
    public final Truthify truthify;
    /**
     * return this to being a inline evaluable functor
     */
    @Deprecated
    static final IntroVars introVars = new IntroVars();
    /**
     * conditions which can be tested before unification
     */

    private final MutableSet<MatchConstraint> constraints;
    protected final ImmutableSet<MatchConstraint> CONSTRAINTS;
    private final MutableSet<PrediTerm> pre;
    protected final ImmutableSet<PrediTerm> PRE;
    protected final Occurrify.TaskTimeMerge time;
    protected final boolean doIntroVars;
    protected final byte taskPunc;
    protected final Term postcons;
    protected final byte puncOverride;
    protected final Term beliefTruth;
    protected final Term goalTruth;

    protected final Term taskPattern;
    protected final Term beliefPattern;
    protected final Term concPattern;



    private static final PremisePatternIndex INDEX = new PremisePatternIndex();

    public PremiseDeriverSource(String ruleSrc) throws Narsese.NarseseException {
        super(
                INDEX.pattern($.pFast(parseRuleComponents(ruleSrc))
                        .transform(new UppercaseAtomsToPatternVariables()))
        );

        this.source = ruleSrc;

        constraints = new UnifiedSet(8);
        pre = new UnifiedSet(8);

        /**
         * deduplicate and generate match-optimized compounds for rules
         */


        Term[] precon = ref.sub(0).arrayShared();
        Term[] postcons = ref.sub(1).arrayShared();


        this.taskPattern = INDEX.intern(ref.sub(0).sub(0));
        this.beliefPattern = INDEX.intern(ref.sub(0).sub(1));
        if (beliefPattern.op() == Op.ATOM) {
            throw new RuntimeException("belief term must contain no atoms: " + beliefPattern);
        }

        this.concPattern = INDEX.intern(ref.sub(1).sub(0));



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
                    subsMin(X,$.intValue(Y));
                    break;

                case "notImaged":
                    termIsNotImaged(X);
                    break;

                case "notSet":
                    /** deprecated soon */
                    termIsNot(X, Op.SetBits);
                    break;


                case "is": {
                    Op o = Op.the($.unquote(Y));
                    if (!negated) {
                        termIs(X, o);
                    } else {
                        termIsNot(X, o.bit);
                        negationApplied = true;
                    }

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

        Term[] modifiers = Terms.sorted(postcons[1].arrayShared());
        for (Term m: modifiers) {
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

        final Term taskPattern1 = taskPattern;
        final Term beliefPattern1 = beliefPattern;

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


        Term finalConcPattern = this.concPattern;
        boolean doIntroVars;
        {
            Pair<Termed, Term> varIntroPattern = Functor.ifFunc(finalConcPattern, i -> i.equals(introVars.ref) ? introVars.ref : null);
            if (varIntroPattern != null) {
                doIntroVars = true;
                finalConcPattern = varIntroPattern.getTwo().sub(0);
            } else {
                doIntroVars = false;
            }
        }

        {
            //add subIfUnify prefilter
            if (Functor.funcName(finalConcPattern).equals(SubIfUnify.SubIfUnify)) {
                Subterms args = Operator.args(finalConcPattern);
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
                        pre.add(new UnifyPreFilter(xpInT, xpInB, ypInT, ypInB, isStrict));
                    }
                }


                //TODO match constraints

            }
        }
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
                        pStructure) + " " + concPattern
        );*/


        constraints.forEach(c -> {
            PrediTerm<Derivation> p = c.preFilter(taskPattern, beliefPattern);
            if (p != null) {
                pre.add(p);
            }
        });



        this.truthify = Truthify.the(puncOverride, beliefTruthOp, goalTruthOp, projection, time);
        this.time = time;
        this.doIntroVars = doIntroVars;
        this.taskPunc = taskPunc;

        this.postcons = postcons[0];
        this.puncOverride = puncOverride;
        this.beliefTruth = beliefTruth;
        this.goalTruth = goalTruth;
        this.CONSTRAINTS = Sets.immutable.of(theInterned(constraints));
        this.PRE = Sets.immutable.ofAll(
                //Iterables.transform(pre, INDEX::intern)
                pre //can not intern pre conditions apparently
        );

    }

    private static MatchConstraint[] theInterned(MutableSet<MatchConstraint> constraints) {
        if (constraints.isEmpty())
            return MatchConstraint.EmptyMatchConstraints;

        MatchConstraint[] mc = MatchConstraint.the(constraints);
        for (int i = 0, mcLength = mc.length; i < mcLength; i++)
            mc[i] = INDEX.intern(mc[i]);
        return mc;
    }

    protected PremiseDeriverSource(PremiseDeriverSource raw, PremisePatternIndex index) {
        super((index.pattern(raw.ref)));

        this.PRE = null;
        this.CONSTRAINTS = null;
        this.source = raw.source;
        this.truthify = raw.truthify;
        this.constraints = raw.constraints;
        this.pre = raw.pre;
        this.time = raw.time;
        this.doIntroVars = raw.doIntroVars;
        this.taskPunc = raw.taskPunc;
        this.postcons = raw.postcons;
        this.puncOverride = raw.puncOverride;
        this.beliefTruth = raw.beliefTruth;
        this.goalTruth = raw.goalTruth;
        this.taskPattern = raw.taskPattern;
        this.beliefPattern = raw.beliefPattern;
        this.concPattern = raw.concPattern;

    }

    public static PremiseDeriverSource parse(String ruleSrc) throws Narsese.NarseseException {
        return new PremiseDeriverSource(ruleSrc);
    }

    public static Stream<PremiseDeriverSource> parse(String... rawRules) {
        return parse(Stream.of(rawRules));
    }

    public static Stream<PremiseDeriverSource> parse(Stream<String> rawRules) {
        return rawRules.map(src -> {
            try {
                return parse(src);
            } catch (Narsese.NarseseException e) {
                throw new RuntimeException("rule parse: " + e + "\n\t" + src);
            }
        });

    }

    static Subterms parseRuleComponents(String src) throws Narsese.NarseseException {


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

        return Op.terms.subtermsInstance(a, b);
    }

    @Override
    public PremiseDeriverProto apply(PremisePatternIndex i) {
        return new PremiseDeriverProto(this, i);
    }

    static class UppercaseAtomsToPatternVariables extends UnifiedMap<String, Term> implements TermTransform {

        public UppercaseAtomsToPatternVariables() {
            super(8);
        }

        @Override
        public Term transformAtomic(Atomic atomic) {
            if (atomic instanceof Atom) {
                if (!PostCondition.reservedMetaInfoCategories.contains(atomic)) {
                    String name = atomic.toString();
                    if (name.length() == 1 && Character.isUpperCase(name.charAt(0))) {
                        return this.computeIfAbsent(name, (n) -> $.varPattern(1 + this.size()));

                    }
                }
            }
            return atomic;
        }

    }


    void eventPrefilter(Collection<PrediTerm> pres, Term conj, Term taskPattern, Term beliefPattern) {


        termIs(conj, CONJ);

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



    private void filter(Term x,
                               BiConsumer<byte[], byte[]> preDerivationExactFilter,
                               BiConsumer<Boolean, Boolean> preDerivationSuperFilter /* task,belief*/
    ) {


        boolean checkedTask = false, checkedBelief = false;

        final byte[] pt = !checkedTask && (taskPattern.equals(x) || !taskPattern.ORrecurse(s -> s instanceof Ellipsislike)) ? Terms.extractFixedPath(taskPattern, x) : null;
        final byte[] pb = !checkedBelief && (beliefPattern.equals(x) || !beliefPattern.ORrecurse(s -> s instanceof Ellipsislike)) ? Terms.extractFixedPath(beliefPattern, x) : null;
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


    void termIs(Term x, Op struct) {
        filter(x, (pt, pb)->
                TaskBeliefIs.preOrConstraint(pre, pt, pb, struct.bit, true),

                (inTask,inBelief)->{
                    if (inTask)
                        pre.addAll(TaskBeliefHas.get(true, struct.bit, true));
                    if (inBelief)
                        pre.addAll(TaskBeliefHas.get(false, struct.bit, true));
                    constraints.add(new OpIs(x, struct));
                }
        );
    }

    private void termIsNot(Term x, int struct) {
        filter(x, (pt, pb)->{
            TaskBeliefIs.preOrConstraint(pre, pt, pb, struct, false);
        }, (inTask, inBelief) -> {
            throw new TODO();
        });
    }

    void subsMin(Term X, int min) {
        if (taskPattern.equals(X)) {
            pre.add(new SubsMin.SubsMinProto(true, min));
        } else if (beliefPattern.equals(X)) {
            pre.add(new SubsMin.SubsMinProto(false, min));
        } else {
            constraints.add(new SubsMin(X, min));
        }

        //TODO
        //filter(x, (pt, pb)->

    }
    private void termIsNotImaged(Term x) {
        if (!taskPattern.containsRecursively(x) && !taskPattern.equals(x))
            throw new TODO("expected/tested occurrence in task concPattern ");

        final byte[] pp = Terms.extractFixedPath(taskPattern, x);
        assert pp != null;
        pre.add(new NotImaged(x, pp));
    }


    private void termHasNot(Term taskPattern, Term beliefPattern, Collection<PrediTerm> pre, Set<MatchConstraint> constraints, Term t, int structure) {

        //TODO: filter(x, )

        boolean inTask = taskPattern.equals(t) || taskPattern.containsRecursively(t);
        boolean inBelief = beliefPattern.equals(t) || beliefPattern.containsRecursively(t);
        if (inTask || inBelief) {
            if (inTask)
                pre.addAll(TaskBeliefHas.get(true, structure, false));
            if (inBelief)
                pre.addAll(TaskBeliefHas.get(false, structure, false));
        } else {

            throw new TODO();
        }

    }

    private static void neq(Set<MatchConstraint> constraints, Term x, Term y) {
        constraints.add(new NotEqualConstraint(x, y));
        constraints.add(new NotEqualConstraint(y, x));
    }

    static class UnifyPreFilter extends AbstractPred<Derivation> {

        private final byte[] xpInT;
        private final byte[] xpInB;
        private final byte[] ypInT;
        private final byte[] ypInB;
        private final boolean isStrict;

        public UnifyPreFilter(byte[] xpInT, byte[] xpInB, byte[] ypInT, byte[] ypInB, boolean isStrict) {
            super($.func("unifyPreFilter", $.p(xpInT), $.p(xpInB), $.p(ypInT), $.p(ypInB)));
            this.xpInT = xpInT;
            this.xpInB = xpInB;
            this.ypInT = ypInT;
            this.ypInB = ypInB;
            this.isStrict = isStrict;
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
    }

    protected static class DoublePremiseRequired extends AbstractPred<Derivation> {

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

    protected static final class SubOf extends AbstractPred<Derivation> {
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


    protected static class NotImaged extends AbstractPred<Derivation> {

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





