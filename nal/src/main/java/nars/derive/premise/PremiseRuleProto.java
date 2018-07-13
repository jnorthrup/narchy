package nars.derive.premise;

import jcog.data.list.FasterList;
import nars.$;
import nars.control.Cause;
import nars.derive.Derivation;
import nars.derive.step.Taskify;
import nars.term.control.AND;
import nars.term.control.PREDICATE;
import nars.unify.constraint.MatchConstraint;
import nars.unify.op.UnifyTerm;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;

import java.util.List;

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
public class PremiseRuleProto extends PremiseRuleSource {


    public final Pair<PREDICATE<Derivation>[], DeriveAction> rule;

    public PremiseRuleProto(PremiseRuleSource raw, PremisePatternIndex index) {
        super(raw, index);


        Taskify taskify = new Taskify(index.nar.newCause(s -> new RuleCause(this, s)));

        PREDICATE<Derivation> conc = AND.the(
                this.termify,
                varIntro ?
                        AND.the(taskify, introVars, taskify)
                        :
                        taskify
        );

        final List<PREDICATE<Derivation>> post = new FasterList<>(8);

        if (taskPattern.equals(beliefPattern)) {
            post.add(new UnifyTerm.UnifySubtermThenConclude(0, taskPattern, conc));
        } else {
            post.add(new UnifyTerm.UnifySubterm(0, taskPattern));
            post.add(new UnifyTerm.UnifySubtermThenConclude(1, beliefPattern, conc));
        }

        MutableSet<MatchConstraint> constraints = raw.CONSTRAINTS.toSet();

        PREDICATE<Derivation>[] postpost = new PREDICATE[
                1 + constraints.size() + post.size()
        ];

        int k = 0;

        postpost[k++] = this.truthify;

        for (PREDICATE p : constraints)
            postpost[k++] = p;

        for (PREDICATE p : post)
            postpost[k++] = p;

        this.rule = pair(PRE,
                DeriveAction.action((AND) AND.the(postpost)));
    }



    /**
     * just a cause, not an input channel.
     * derivation inputs are batched for input by another method
     * holds the deriver id also that it can be applied at the end of a derivation.
     */
    public static class RuleCause extends Cause implements IntToFloatFunction {
        public final PremiseRuleSource rule;
        public final String ruleString;

        RuleCause(PremiseRuleSource rule, short id) {
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
