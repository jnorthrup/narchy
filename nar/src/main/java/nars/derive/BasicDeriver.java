package nars.derive;

import jcog.func.TriFunction;
import nars.NAR;
import nars.Task;
import nars.attention.TaskLinkWhat;
import nars.attention.What;
import nars.derive.model.Derivation;
import nars.derive.premise.PremiseSource;
import nars.derive.rule.PremiseRuleSet;
import nars.derive.timing.NonEternalTaskOccurenceOrPresentDeriverTiming;
import nars.link.TaskLinks;
import nars.term.Term;
import nars.time.When;
import nars.time.event.WhenTimeIs;

import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;


/** default deriver implementation */
public class BasicDeriver extends Deriver {

    public BasicDeriver(PremiseRuleSet rules) {
        super(rules, rules.nar);
    }

    public BasicDeriver(PremiseRuleSet rules, PremiseSource premises) {
        this(rules, new NonEternalTaskOccurenceOrPresentDeriverTiming(), premises);
    }

    public BasicDeriver(PremiseRuleSet rules, TriFunction<What, Task, Term, long[]> timing, PremiseSource premises) {
        super(rules, timing, premises);
    }

    @Override
    protected final void derive(Derivation d, BooleanSupplier kontinue) {

        When<NAR> now = null;
        int matchTTL = matchTTL();
        int deriveTTL = d.nar().deriveBranchTTL.intValue();
        TaskLinks links = ((TaskLinkWhat) d.what).links;

        LongSupplier clock = d::time;

        do {

            premises.premises(
                p -> { p.derive(d, matchTTL, deriveTTL); return true; },
                now = now == null ?
                    WhenTimeIs.now(d) : now.update(clock),
                links, d);

        } while (kontinue.getAsBoolean());

    }


}
