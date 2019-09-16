package nars.derive.rule;

import nars.NAR;
import nars.derive.Derivation;
import nars.derive.DeriveAction;
import nars.derive.op.DirectPremisify;
import nars.derive.op.Taskify;
import nars.derive.op.Truthify;
import nars.term.Term;
import nars.term.Terms;
import nars.term.control.AND;
import nars.term.control.PREDICATE;

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
public class PremiseRuleProto implements Comparable<PremiseRuleProto> {

    final PREDICATE<Derivation>[] condition;
    final DeriveAction action;

    private final PremiseRule rule;

    PremiseRuleProto(PremiseRule rule, NAR nar) {

        this.rule = rule;

        int k = 0;
        PREDICATE<Derivation>[] y = new PREDICATE[1 + rule.CONSTRAINTS.size() ];
        for (PREDICATE p : rule.CONSTRAINTS)
            y[k++] = p;

        RuleWhy cause = nar.newCause(s -> new RuleWhy(this.rule, s));
        Taskify taskify = new Taskify(rule.termify, cause);
        y[k++] =
            new DirectPremisify
            //new CachingPremisify //<- not ready yet
                (rule.taskPattern, rule.beliefPattern, isFwd(), taskify);

        this.condition = rule.PRE.clone(); //clone because it gets modified per instantiation
        this.action = action(y, cause);
    }

    private DeriveAction action(PREDICATE<Derivation>[] y, RuleWhy cause) {
        PREDICATE<Derivation> yy = AND.the(y);

        Truthify truthify = rule.truthify;
//        if (NAL.DEBUG)
//            return new DeriveActionProfiled(cause, truthify, yy);
//        else
            return new DeriveAction(cause, truthify, yy);

    }

    /** task,belief or belief,task ordering heuristic */
    private boolean isFwd() {

        int dir = 0; //-1 !fwd, 0=undecided yet, +1 = fwd

        Term T = rule.taskPattern;
        Term B = rule.beliefPattern;
        if (T.equals(B)) {
            dir = +1; //equal, so use convention
        }

        if (dir == 0) {
            //match ellipsis first. everything else will basically depend on this
            boolean te = Terms.hasEllipsisRecurse(T), be = Terms.hasEllipsisRecurse(B);
            if (te || be) {
                if (te && !be) dir = +1;
                else if (!te && be) dir = -1;
            }
        }
        if (dir == 0) {
            boolean Tb = T.containsRecursively(B);
            boolean Bt = B.containsRecursively(T);
            if (Tb && !Bt) dir = -1; //belief first as it is a part of Task
            if (Bt && !Tb) dir = +1; //task first as it is a part of Belief
        }
        if (dir == 0) {
            if (T.volume() > B.volume()) dir = -1; //belief first, since it is smaller
            if (B.volume() > T.volume()) dir = +1; //task first, since it is smaller
        }
        if (dir == 0) {
            if (T.varPattern() > B.varPattern()) dir = -1; //belief first, since it has fewer variables
            if (B.varPattern() > T.varPattern()) dir = +1; //task first, since it has fewer variables
        }
        if (dir == 0) {
            int taskBits = Integer.bitCount(T.structure());
            int belfBits = Integer.bitCount(B.structure());
            if (belfBits > taskBits) dir = -1; //belief first since it is more specific in its structure (easier to match)
            if (taskBits > belfBits) dir = +1; //belief first since it is more specific in its structure (easier to match)
        }

        return dir >= 0; //0 or +1 = fwd (task first), else !fwd (belief first)
    }


    @Override
    public int compareTo(PremiseRuleProto p) {
        return this==p ? 0 : rule.ref.compareTo(p.rule.ref);
    }
}
