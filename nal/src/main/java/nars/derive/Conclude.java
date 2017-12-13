package nars.derive;

import nars.$;
import nars.NAR;
import nars.Op;
import nars.control.Cause;
import nars.control.Derivation;
import nars.derive.constraint.MatchConstraint;
import nars.derive.match.Ellipsis;
import nars.derive.op.AbstractPatternOp;
import nars.derive.op.SubTermStructure;
import nars.derive.op.UnifyTerm;
import nars.derive.rule.PremiseRule;
import nars.index.term.PatternIndex;
import nars.term.Term;
import nars.term.Termed;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.SortedSet;

/**
 * Conclusion builder
 */
public final class Conclude {

    private static final Term VAR_INTRO = $.the("varIntro");


    static public PrediTerm<Derivation> the(PremiseRule rule, PatternIndex index, NAR nar) {

        Term pattern = rule.conclusion().sub(0);

        //TODO may interfere with constraints, functors, etc or other features, ie.
        // if the pattern is a product for example?
        //            pattern = pattern.replace(ta, Derivation._taskTerm);
        // determine if any cases where a shortcut like this can work (ie. no constraints, not a product etc)

        //        //substitute compound occurrences of the exact task and belief terms with the short-cut
//        Term ta = rule.getTask();
//        if (!ta.op().var) {
//            if (pattern.equals(ta))
//                pattern = Derivation.TaskTerm;
//        }
//        Term tb = rule.getBelief();
//        if (!tb.op().var) {
//            //pattern = pattern.replace(tb, Derivation._beliefTerm);
//            if (pattern.equals(tb))
//                pattern = Derivation.BeliefTerm;
//        }

        //HACK unwrap varIntro so we can apply it at the end of the derivation process, not before like other functors
        boolean introVars;
        Pair<Termed, Term> outerFunctor = Op.functor(pattern, (i)->i.equals(VAR_INTRO) ? VAR_INTRO : null);
        if (outerFunctor != null) {
            introVars = true;
            pattern = outerFunctor.getTwo().sub(0);
        } else {
            introVars = false;
        }

        pattern = index.get(pattern, true).term(); //get(pattern, true).term();

        Taskify taskify = new Taskify( nar.newCause((s)->new RuleCause(rule, s)));

        Term concID = $.func("derive", /*$.the(cid), */pattern/* prod args */);
        Conclusion conc = new Conclusion(concID, pattern, rule);
        return AndCondition.the(
                conc,
                introVars ? //Fork.fork(
                        AndCondition.the(new IntroVars(), taskify)
                        //makeTask)
                        : taskify
        );

    }

    public static void match(final PremiseRule rule, List<PrediTerm<Derivation>> pre, List<PrediTerm<Derivation>> post, @NotNull SortedSet<MatchConstraint> constraints, PatternIndex index, NAR nar) {

        PrediTerm<Derivation> conc = the(rule, index, nar);

        final Term taskPattern = rule.getTask();
        final Term beliefPattern = rule.getBelief();

        boolean taskIsPatVar = taskPattern.op() == Op.VAR_PATTERN;
        boolean belIsPatVar = beliefPattern.op() == Op.VAR_PATTERN;

        if (!taskIsPatVar)
            pre.add(new AbstractPatternOp.PatternOp(0, taskPattern.op()));
        if (!belIsPatVar)
            pre.add(new AbstractPatternOp.PatternOp(1, beliefPattern.op()));

        if (!taskIsPatVar)
            pre.addAll(SubTermStructure.get(0, taskPattern.structure()));

        if (!belIsPatVar)
            pre.addAll(SubTermStructure.get(1, beliefPattern.structure()));

        //        } else {
        //            if (x0.containsTermRecursively(x1)) {
        //                //pre.add(new TermContainsRecursively(x0, x1));
        //            }
        //        }

        //@Nullable ListMultimap<Term, MatchConstraint> c){


        //ImmutableMap<Term, MatchConstraint> cc = compact(constraints);


        //match both
        //code.add(new MatchTerm.MatchTaskBeliefPair(pattern, initConstraints(constraints)));

        if (taskPattern.equals(beliefPattern)) {
            post.add(new UnifyTerm.UnifySubtermThenConclude(0, taskPattern, conc));
        } if (taskFirst(taskPattern, beliefPattern)) {
            //task first
            post.add(new UnifyTerm.UnifySubterm(0, taskPattern));
            post.add(new UnifyTerm.UnifySubtermThenConclude(1, beliefPattern, conc));
        } else {
            //belief first
            post.add(new UnifyTerm.UnifySubterm(1, beliefPattern));
            post.add(new UnifyTerm.UnifySubtermThenConclude(0, taskPattern, conc));
        }

        //Term beliefPattern = pattern.term(1);

        //if (Global.DEBUG) {
//            if (beliefPattern.structure() == 0) {

        // if nothing else in the rule involves this term
        // which will be a singular VAR_PATTERN variable
        // then allow null
//                if (beliefPattern.op() != Op.VAR_PATTERN)
//                    throw new RuntimeException("not what was expected");

//            }
        //}

        /*System.out.println( Long.toBinaryString(
                        pStructure) + " " + pattern
        );*/


    }

    private static boolean taskFirst(Term task, Term belief) {


        Ellipsis taskEllipsis = Ellipsis.firstEllipsisRecursive(task);
//        if (taskEllipsis instanceof EllipsisTransform) {
//            //belief must be matched first especially for EllipsisTransform
//            return false;
//        }

        if (belief.subs() == 0) {
            return false;
        }
        if (task.subs() == 0) {
            return true;
        }

        //prefer non-ellipsis matches first
        Ellipsis beliefEllipsis = Ellipsis.firstEllipsisRecursive(belief);
        if (beliefEllipsis != null) {
            return true;
        }
        if (taskEllipsis != null) {
            return false;
        }


        //return task.volume() >= belief.volume();

        return task.volume() <= belief.volume(); //might fold better

        //return task.varPattern() <= belief.varPattern();
    }

    /** just a cause, not an input channel.
     * derivation inputs are batched for input by another method
     * holds the deriver id also that it can be applied at the end of a derivation.
     */
    public static class RuleCause extends Cause {
        public final PremiseRule rule;
        public final String ruleString;

        public RuleCause(PremiseRule rule, short id) {
            super(id);
            this.rule = rule;
            this.ruleString = rule.toString();
        }

        @Override
        public String toString() {
            return $.p(rule.id, $.the(id)).toString();
        }
    }


    //    public static class RuleFeedbackDerivedTask extends DerivedTask.DefaultDerivedTask {
//
//        private final @NotNull PremiseRule rule;
//
//        public RuleFeedbackDerivedTask(@NotNull Termed<Compound> tc, @Nullable Truth truth, byte punct, long[] evidence, @NotNull Derivation premise, @NotNull PremiseRule rule, long now, long[] occ) {
//            super(tc, truth, punct, evidence, premise, now, occ);
//            this.rule = rule;
//        }
//
//        @Override
//        public void feedback(TruthDelta delta, float deltaConfidence, float deltaSatisfaction, NAR nar) {
//            if (!isDeleted())
//                Conclude.feedback(premise, rule, this, delta, deltaConfidence, deltaSatisfaction, nar);
//            super.feedback(delta, deltaConfidence, deltaSatisfaction, nar);
//
//        }
//    }
//
//    static class RuleStats {
//        final SummaryStatistics pri = new SummaryStatistics();
//        final SummaryStatistics dSat = new SummaryStatistics();
//        final SummaryStatistics dConf = new SummaryStatistics();
//
//        public long count() {
//            return dSat.getN();
//        }
//
//    }
//
//    static final Map<NAR, Map<PremiseRule, RuleStats>> stats = new ConcurrentHashMap();
//
//    private static void feedback(Premise premise, @NotNull PremiseRule rule, @NotNull RuleFeedbackDerivedTask t, @Nullable TruthDelta delta, float deltaConfidence, float deltaSatisfaction, NAR nar) {
//        Map<PremiseRule, RuleStats> x = stats.computeIfAbsent(nar, n -> new ConcurrentHashMap<>());
//
//        RuleStats s = x.computeIfAbsent(rule, d -> new RuleStats());
//
//        s.pri.addValue(t.pri());
//
//        if (delta != null) {
//            s.dSat.addValue(Math.abs(deltaSatisfaction));
//            s.dConf.addValue(Math.abs(deltaConfidence));
//        }
//
//    }
//
//    static public void printStats(NAR nar) {
//        stats.get(nar).forEach((r, s) -> {
//            long n = s.count();
//
//            System.out.println(
//                    r + "\t" +
//                            Texts.n4(s.pri.getSum()) + '\t' +
//                            Texts.n4(s.dConf.getSum()) + '\t' +
//                            Texts.n4(s.dSat.getSum()) + '\t' +
//                            n
//                    //" \t " + mean +
//            );
//        });
//    }


//    final static HashBag<PremiseRule> posGoal = new HashBag();
//    final static HashBag<PremiseRule> negGoal = new HashBag();
//    static {
//
//        Runtime.getRuntime().addShutdownHook(new Thread(()-> {
//            System.out.println("POS GOAL:\n" + print(posGoal));
//            System.out.println("NEG GOAL:\n" + print(negGoal));
//        }));
//    }
//
//    private static String print(HashBag<PremiseRule> h) {
//        return Joiner.on("\n").join(h.topOccurrences(h.size())) + "\n" + h.size() + " total";
//    }

}
