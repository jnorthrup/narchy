package nars.derive.rule;

import nars.NAR;
import nars.derive.PreDerivation;
import nars.derive.action.How;
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

    final PREDICATE<PreDerivation>[] condition;
    final Function<NAR, How> action;

    public PremiseRule(Term id, PREDICATE<PreDerivation>[] condition, Function<NAR, How> action) {
        super(id);

        this.condition = condition;
        this.action = action;
    }

}
