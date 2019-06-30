package nars.derive.rule;

import jcog.time.UsageNS;
import nars.$;
import nars.NAL;
import nars.NAR;
import nars.control.Why;
import nars.derive.model.Derivation;
import nars.derive.op.DirectPremisify;
import nars.derive.op.Taskify;
import nars.derive.op.Truthify;
import nars.term.Term;
import nars.term.control.AND;
import nars.term.control.PREDICATE;
import nars.term.var.ellipsis.Ellipsislike;
import org.HdrHistogram.AtomicHistogram;
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
public class PremiseRuleProto extends PremiseRule {

    public final Pair<PREDICATE<Derivation>[], DeriveAction> rule;

    PremiseRuleProto(PremiseRule raw, NAR nar) {
        super(raw);

        int k = 0;
        PREDICATE<Derivation>[] y = new PREDICATE[1 + CONSTRAINTS.size() ];

        for (PREDICATE p : CONSTRAINTS)
            y[k++] = p;

        RuleWhy cause = nar.newCause(s -> new RuleWhy(this, s));
        Taskify taskify = new Taskify(termify, cause);
        y[k++] =
            new DirectPremisify
            //new CachingPremisify //<- not ready yet
                (taskPattern, beliefPattern, isFwd(), taskify);

        this.rule = pair(PRE, action(y, cause));
    }

    private DeriveAction action(PREDICATE<Derivation>[] y, RuleWhy cause) {
        PREDICATE<Derivation> yy = AND.the(y);
        if (NAL.DEBUG)
            return new DeriveActionProfiled(cause, truthify, yy);
        else
            return new DeriveAction(cause, truthify, yy);

    }

    /** task,belief or belief,task ordering heuristic */
    private boolean isFwd() {

        int dir = 0; //-1 !fwd, 0=undecided yet, +1 = fwd

        if (taskPattern.equals(beliefPattern)) {
            dir = +1; //equal, so use convention
        }

        if (dir == 0) {
            //match ellipsis first. everything else will basically depend on this
            boolean te = hasEllipsis(taskPattern), be = hasEllipsis(beliefPattern);
            if (te || be) {
                if (te && !be) dir = +1;
                else if (be && !te) dir = -1;
            }
        }
        if (dir == 0) {
            boolean Tb = taskPattern.containsRecursively(beliefPattern);
            boolean Bt = beliefPattern.containsRecursively(taskPattern);
            if (Tb && !Bt) dir = -1; //belief first as it is a part of Task
            if (Bt && !Tb) dir = +1; //task first as it is a part of Belief
        }
        if (dir == 0) {
            if (taskPattern.volume() > beliefPattern.volume()) dir = -1; //belief first, since it is smaller
            if (beliefPattern.volume() > taskPattern.volume()) dir = +1; //task first, since it is smaller
        }
        if (dir == 0) {
            if (taskPattern.varPattern() > beliefPattern.varPattern()) dir = -1; //belief first, since it has fewer variables
            if (beliefPattern.varPattern() > taskPattern.varPattern()) dir = +1; //task first, since it has fewer variables
        }
        if (dir == 0) {
            int taskBits = Integer.bitCount(taskPattern.structure());
            int belfBits = Integer.bitCount(beliefPattern.structure());
            if (belfBits > taskBits) dir = -1; //belief first since it is more specific in its structure (easier to match)
            if (taskBits > belfBits) dir = +1; //belief first since it is more specific in its structure (easier to match)
        }

        return dir >= 0; //0 or +1 = fwd (task first), else !fwd (belief first)
    }

    private static boolean hasEllipsis(Term x) {
        return x.ORrecurse(t -> t instanceof Ellipsislike);
    }


    /**
     * just a cause, not an input channel.
     * derivation inputs are batched for input by another method
     * holds the deriver id also that it can be applied at the end of a derivation.
     */
    public static final class RuleWhy extends Why {

        public final PremiseRule rule;
        public final String ruleString;
        public final Term term;

        RuleWhy(PremiseRule rule, short id) {
            super(id);
            this.rule = rule;
            this.ruleString = rule.source;
            this.term = $.pFast(rule.ref, $.the(id));
        }

        @Override
        public String toString() {
            return term().toString();
        }

        @Override public Term term() {
            return term;
        }

    }


    private static class DeriveActionProfiled  extends DeriveAction {

        static final UsageNS<DeriveActionProfiled> usage = new UsageNS();
        final AtomicHistogram meter;

        public DeriveActionProfiled(RuleWhy cause, Truthify truthify, PREDICATE<Derivation> yy) {
            super(cause, truthify, yy);
            meter = usage.the(this);
        }

        @Override
        public boolean test(Derivation d) {
            long start = System.nanoTime();
            boolean r = super.test(d);
            long end = System.nanoTime();
            meter.recordValue(end-start);
            return r;
        }
    }
}
