package nars.derive.rule;

import jcog.data.list.FasterList;
import nars.NAR;
import nars.derive.PreDerivation;
import nars.derive.action.How;
import nars.derive.util.Forkable;
import nars.term.ProxyTerm;
import nars.term.Term;
import nars.term.control.PREDICATE;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
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
    final Function<RuleCause, How> action;


    @Nullable
    public final String tag;

    public PremiseRule(Term id, String tag, PREDICATE<PreDerivation>[] condition, Function<RuleCause, How> action) {
        super(id);

        this.tag = tag;
        this.condition = condition;
        this.action = action;
    }

    /** instance a list of conditions */
    FasterList<PREDICATE<PreDerivation>> conditions(short i) {
        var pre = new FasterList<PREDICATE<PreDerivation>>(this.condition.length + 1);
        pre.addAll(this.condition);
        pre.add(new Forkable(/* branch ID */  i));
        return pre;
    }

    How action(NAR n, Map<String, RuleCause> tags) {
        return action.apply(
            //tag==null ? cause(n, ref) : tags.computeIfAbsent(tag, t -> cause(n, Atomic.the(t))) //GROUPED
            cause(n, ref) //UNIQUE
        );
    }

    private static RuleCause cause(NAR n, Term ref) {
        return n.newCause(ruleInstanceID -> new RuleCause(ref, ruleInstanceID));
    }

}
