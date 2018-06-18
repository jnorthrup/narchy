package nars.derive.premise;

import jcog.list.FasterList;
import nars.$;
import nars.control.Cause;
import nars.derive.Derivation;
import nars.derive.step.Taskify;
import nars.derive.step.Termify;
import nars.term.Compound;
import nars.term.control.AndCondition;
import nars.term.control.PrediTerm;
import nars.unify.constraint.MatchConstraint;
import nars.unify.op.TaskPunctuation;
import nars.unify.op.UnifyTerm;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;

import java.util.List;

import static nars.unify.op.TaskPunctuation.Belief;
import static nars.unify.op.TaskPunctuation.Goal;
import static org.eclipse.collections.impl.tuple.Tuples.pair;

/**
 * an intermediate representation of a premise rule
 * with fully expanded opcodes
 *
 * instantiated for each NAR, because it binds the conclusion steps to it
 *
 * anything non-NAR specific (static) is done in PremiseDeriverSource as a
 * ready-made template to make constructing this as fast as possible
 * in potentially multiple NAR instances later
 */
public class PremiseDeriverProto extends PremiseDeriverSource {


    public final Pair<PrediTerm<Derivation>[], DeriveAction> rule;

    public PremiseDeriverProto(PremiseDeriverSource raw, PremisePatternIndex index) {
        super(raw, index);

        final List<PrediTerm<Derivation>> post = new FasterList<>(8);

        MutableSet<MatchConstraint> constraints = raw.CONSTRAINTS.toSet();
        MutableSet<PrediTerm> pre = raw.PRE.toSet();

        Taskify taskify = new Taskify(index.nar.newCause(s -> new RuleCause(this, s)));

        PrediTerm<Derivation> conc = AndCondition.the(
                new Termify(concPattern, this, truthify, time),
                doIntroVars ?
                        AndCondition.the(introVars, taskify)
                        :
                        taskify
        );

        if (taskPattern.equals(beliefPattern)) {
            post.add(new UnifyTerm.UnifySubtermThenConclude(0, taskPattern, conc));
        } else { //if (taskFirst(taskPattern1, beliefPattern1)) {

            post.add(new UnifyTerm.UnifySubterm(0, taskPattern));
            post.add(new UnifyTerm.UnifySubtermThenConclude(1, beliefPattern, conc));
        }


        {
            PostCondition POST = new PostCondition(postcons, beliefTruth, goalTruth, puncOverride);

            assert POST.modifiesPunctuation() || !(postcons instanceof Compound) || !taskPattern.equals(postcons) :
                    "punctuation not modified yet rule task equals pattern: " + this;


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
        }


        int rules = pre.size();
        PrediTerm[] PRE = pre.toArray(new PrediTerm[rules + 1 /* extra to be filled in later stage */]);
        ArrayUtils.sort(PRE, 0, rules-1, (x)-> -x.cost());
        //Arrays.sort(PRE, 0, rules, sortByCostIncreasing);
        assert rules <= 1 || (PRE[0].cost() <= PRE[rules - 2].cost()); //increasing cost

        PrediTerm<Derivation>[] postpost = new PrediTerm[
                1 + constraints.size() + post.size()
        ];

        int k = 0;

        postpost[k++] = this.truthify;

        for (PrediTerm p : constraints)
            postpost[k++] = p;

        for (PrediTerm p : post)
            postpost[k++] = p;

        this.rule = pair(PRE,
                DeriveAction.action((AndCondition)AndCondition.the(postpost)));
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


}
