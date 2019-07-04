package nars.derive;

import jcog.math.IntRange;
import nars.NAR;
import nars.attention.TaskLinkWhat;
import nars.derive.model.Derivation;
import nars.derive.premise.PremiseSource;
import nars.derive.rule.PremiseRuleSet;
import nars.link.TaskLinks;
import nars.time.When;
import nars.time.event.WhenTimeIs;

import java.util.function.BooleanSupplier;


/** default deriver implementation */
public class BatchDeriver extends Deriver {

    public final IntRange premisesPerIteration = new IntRange(2, 1, 32);

    public final IntRange termLinksPerTaskLink = new IntRange(1, 1, 8);

    public BatchDeriver(PremiseRuleSet rules) {
        super(rules, rules.nar);
    }

    @Override
    protected final void derive(Derivation d, BooleanSupplier kontinue) {

        PremiseSource premises = d.premises;
        premises.commit();

        When<NAR> now = WhenTimeIs.now(d);
        int matchTTL = matchTTL();
        int deriveTTL = d.nar().deriveBranchTTL.intValue();
        int premisesPerIteration = this.premisesPerIteration.intValue();
        int termLinksPerTaskLink = this.termLinksPerTaskLink.intValue();
        TaskLinks links = ((TaskLinkWhat) d.what).links;

        do {

            premises.derive(
                    now,
                    premisesPerIteration,
                    termLinksPerTaskLink,
                    matchTTL, deriveTTL,
                    links, d);

//            try {
//                for (PostDerivation pd : d.post) {
//                    pd.accept(d);
//                    if (!kontinue.getAsBoolean())
//                        return;
//                }
//            } finally {
//                d.post.clear();
//            }
        } while (kontinue.getAsBoolean());

    }


}
