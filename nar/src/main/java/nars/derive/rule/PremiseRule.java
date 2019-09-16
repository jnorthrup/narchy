package nars.derive.rule;

import nars.NAR;
import nars.derive.Derivation;
import nars.derive.DeriveAction;
import nars.term.ProxyTerm;
import nars.term.Term;
import nars.term.control.PREDICATE;

import java.util.function.Function;

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
public class PremiseRule extends ProxyTerm  {

    final PREDICATE<Derivation>[] condition;
    final Function<NAR,DeriveAction> action;

    PremiseRule(Term id, PREDICATE<Derivation>[] condition, Function<NAR, DeriveAction> action) {
        super(id);
        this.condition = condition;
        this.action = action;
    }

}
