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
import nars.unify.op.TaskPunctuation;
import nars.unify.op.UnifyTerm;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
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


    private final PrediTerm[] PRE;

    /**
     * consequences applied after unification
     */
    private final PostCondition POST;

    final List<PrediTerm<Derivation>> post;

    public PremiseDeriverProto(PremiseDeriverSource raw, PremisePatternIndex index) {
        super(raw, index);

        this.post = new FasterList<>(8);

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


        PostCondition pc = new PostCondition(postcons, beliefTruth, goalTruth, puncOverride);

        if (!pc.modifiesPunctuation() && postcons instanceof Compound) {
            assert !taskPattern.equals(postcons) :
                    "punctuation not modified yet rule task equals pattern: " + this;
//            assert !rule.getBelief().equals(pattern) :
//                    "punctuation not modified yet rule belief equals pattern: " + rule + "\n\t" + rule.getBelief() + "\n\t" + pattern;
        }

        POST = pc;


        //this.POST = postConditions.toArray(new PostCondition[pcs]);

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


        int rules = pre.size();
        this.PRE = pre.toArray(new PrediTerm[rules + 1 /* extra to be filled in later stage */]);
        ArrayUtils.sort(PRE, 0, rules-1, (x)-> -x.cost());
        //Arrays.sort(PRE, 0, rules, sortByCostIncreasing);
        if (rules > 1)
            assert(PRE[0].cost() <= PRE[rules-2].cost()); //increasing cost
    }



    /**
     * compiles the conditions which are necessary to activate this rule
     */
    public Pair<PrediTerm<Derivation>[], DeriveAction> build() {

        int n = 1 + this.constraints.size() + this.post.size();


        PrediTerm<Derivation>[] post = new PrediTerm[n];
        int k = 0;
        post[k++] = this.truthify;
        for (PrediTerm p : this.constraints) {
            post[k++] = p;
        }
        for (PrediTerm p : this.post) {
            post[k++] = p;
        }

        DeriveAction POST =
                DeriveAction.action((AndCondition<Derivation>)AndCondition.the(post));

        return pair(PRE, POST);
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
