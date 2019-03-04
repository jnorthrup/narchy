package nars.derive.premise;

import nars.$;
import nars.NAR;
import nars.control.Cause;
import nars.derive.Derivation;
import nars.derive.op.Premisify;
import nars.derive.op.Taskify;
import nars.term.Term;
import nars.term.control.AND;
import nars.term.control.PREDICATE;
import nars.unify.ellipsis.Ellipsislike;
import org.eclipse.collections.api.tuple.Pair;

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

    public PremiseRuleProto(PremiseRuleSource raw, NAR nar) {
        super(raw);

        RuleCause cause = nar.newCause(s -> new RuleCause(this, s));

        Taskify taskify = !raw.varIntro ?
                new Taskify(termify, cause) :
                new Taskify.VarTaskify(termify, cause);

        boolean fwd = ((!hasEllipsis(taskPattern) && hasEllipsis(beliefPattern)) || taskPattern.vars() <= beliefPattern.vars());
        Premisify premisify =
            new Premisify
            //new Premisify.CachingPremisify
                (taskPattern, beliefPattern, fwd, taskify);


        PREDICATE<Derivation>[] y = new PREDICATE[3 + CONSTRAINTS.size() ];

        int k = 0;

        y[k++] = this.truthify;
        y[k++] = Premisify.preUnify;

        for (PREDICATE p : CONSTRAINTS)
            y[k++] = p;

        y[k++] = premisify;

        this.rule = pair(PRE, DeriveAction.action(cause, AND.the(y)));
    }

    private static boolean hasEllipsis(Term x) {
        return x.ORrecurse(t -> t instanceof Ellipsislike);
    }


    /**
     * just a cause, not an input channel.
     * derivation inputs are batched for input by another method
     * holds the deriver id also that it can be applied at the end of a derivation.
     */
    public static final class RuleCause extends Cause {
        public final PremiseRuleSource rule;
        public final String ruleString;

        RuleCause(PremiseRuleSource rule, short id) {
            super(id);
            this.rule = rule;
            this.ruleString = rule.source;
        }

        @Override
        public String toString() {
            return $.pFast(rule.ref, $.the(id)).toString();
        }

    }


}
