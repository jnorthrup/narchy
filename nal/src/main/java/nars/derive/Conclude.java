package nars.derive;

import nars.$;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.control.Cause;
import nars.derive.constraint.MatchConstraint;
import nars.derive.op.AbstractPatternOp;
import nars.derive.op.SubTermStructure;
import nars.derive.op.TaskBeliefOp;
import nars.derive.op.UnifyTerm;
import nars.derive.rule.DeriveRuleProto;
import nars.derive.rule.DeriveRuleSource;
import nars.index.term.PatternIndex;
import nars.op.DepIndepVarIntroduction;
import nars.term.Term;
import nars.term.Termed;
import nars.term.pred.AbstractPred;
import nars.term.pred.AndCondition;
import nars.term.pred.PrediTerm;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import static nars.derive.Conclude.IntroVars.VAR_INTRO;

/**
 * Conclusion builder
 */
public final class Conclude {

    public static final IntroVars introVars = new IntroVars();


    public static void match(final DeriveRuleProto rule, List<PrediTerm<PreDerivation>> pre, List<PrediTerm<Derivation>> post, @NotNull SortedSet<MatchConstraint> constraints, PatternIndex index, NAR nar) {

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
        boolean introVars1;
        Pair<Termed, Term> outerFunctor = Op.functor(pattern, (i) -> i.equals(VAR_INTRO) ? VAR_INTRO : null);
        if (outerFunctor != null) {
            introVars1 = true;
            pattern = outerFunctor.getTwo().sub(0);
        } else {
            introVars1 = false;
        }

        pattern = index.get(pattern, true).term();

        Taskify taskify = new Taskify(nar.newCause((s) -> new RuleCause(rule, s)));

        PrediTerm<Derivation> conc = AndCondition.the(
                new Termify($.func("derive", pattern), pattern, rule),
                introVars1 ?
                        AndCondition.the(introVars, taskify)
                        :
                        taskify
        );

        final Term taskPattern = rule.getTask();
        final Term beliefPattern = rule.getBelief();

        Op to = taskPattern.op();
        boolean taskIsPatVar = to == Op.VAR_PATTERN;
        Op bo = beliefPattern.op();
        boolean belIsPatVar = bo == Op.VAR_PATTERN;

        if (!taskIsPatVar) {
            pre.add(new TaskBeliefOp(to, true, false));
            pre.addAll(SubTermStructure.get(0, taskPattern.structure()));
        }
        if (!belIsPatVar) {
            if (to == bo) {
                pre.add(new AbstractPatternOp.TaskBeliefOpEqual());
            } else {
                pre.add(new TaskBeliefOp(bo, false, true));
                pre.addAll(SubTermStructure.get(1, beliefPattern.structure()));
            }
        }


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
        }
        if (taskFirst(taskPattern, beliefPattern)) {
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
        return true;
    }

//    private static boolean taskFirst0(Term task, Term belief) {
//
//        if (belief.subs() == 0) {
//            return false;
//        }
//        if (task.subs() == 0) {
//            return true;
//        }
//
//        //prefer non-ellipsis matches first
//        Ellipsis taskEllipsis = Ellipsis.firstEllipsisRecursive(task);
//        if (taskEllipsis != null) {
//            return false; //match belief first
//        }
//        Ellipsis beliefEllipsis = Ellipsis.firstEllipsisRecursive(belief);
//        if (beliefEllipsis != null) {
//            return true; //match task first
//        }
//
//        //return task.volume() >= belief.volume();
//
//        int tv = task.volume();
//        int bv = belief.volume();
//        return (tv < bv);
//
//        //return task.varPattern() <= belief.varPattern();
//    }

    /**
     * just a cause, not an input channel.
     * derivation inputs are batched for input by another method
     * holds the deriver id also that it can be applied at the end of a derivation.
     */
    static class RuleCause extends Cause {
        public final DeriveRuleSource rule;
        public final String ruleString;

        RuleCause(DeriveRuleSource rule, short id) {
            super(id);
            this.rule = rule;
            this.ruleString = rule.toString();
        }

        @Override
        public String toString() {
            return $.p(rule.term(), $.the(id)).toString();
        }
    }


    static class IntroVars extends AbstractPred<Derivation> {

        static final Term VAR_INTRO = $.the("varIntro");

        private IntroVars() {
            super(VAR_INTRO);
        }

        @Override
        public boolean test(Derivation p) {
            final Term x = p.derivedTerm.get();


            @Nullable Pair<Term, Map<Term, Term>> xy = DepIndepVarIntroduction.the.apply(x, p.random);
            if (xy == null)
                return false;

            final Term y = xy.getOne();

            if (!y.unneg().op().conceptualizable ||
                y.equals(x) || /* keep only if it differs */
                //!y.hasAny(Op.ConstantAtomics) ||  //entirely variablized
                !Task.validTaskTerm(y, p.concPunc, true)
            )
                return false;


            Map<Term, Term> changes = xy.getTwo();
            changes.forEach(p::replaceXY);
            p.derivedTerm.set(y);

//            //reduce evidence by a factor proportional to the number of variables introduced
//            p.concEviFactor *= (((float)(1+y.complexity()))/(1+x.complexity()));

            return true;
        }
    }
}
