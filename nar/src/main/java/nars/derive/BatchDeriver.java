package nars.derive;

import jcog.math.IntRange;
import nars.attention.TaskLinkWhat;
import nars.derive.model.Derivation;
import nars.derive.rule.PremiseRuleSet;
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

        int matchTTL = matchTTL();
        int deriveTTL = d.nar().deriveBranchTTL.intValue();
        int premisesPerIteration = this.premisesPerIteration.intValue();
        int termLinksPerTaskLink = this.termLinksPerTaskLink.intValue();

        d.premises.commit();
        do {
            derive(premisesPerIteration, termLinksPerTaskLink, matchTTL, deriveTTL, d);
        } while (kontinue.getAsBoolean());

    }

    /**
     * samples premises
     * thread-safe, for use by multiple threads
     */
    public final void derive(int premisesPerIteration, int termlinksPerTaskLink, int matchTTL, int deriveTTL, Derivation d) {
        d.premises.derive(
                WhenTimeIs.now(d),
                premisesPerIteration,
                termlinksPerTaskLink,
                matchTTL, deriveTTL,
                ((TaskLinkWhat)d.what).links, d);
    }

}
