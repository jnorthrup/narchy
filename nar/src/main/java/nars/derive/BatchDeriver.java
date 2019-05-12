package nars.derive;

import jcog.math.IntRange;
import nars.attention.What;
import nars.derive.model.Derivation;
import nars.derive.rule.PremiseRuleSet;

import java.util.function.BooleanSupplier;


/** default deriver implementation */
public class BatchDeriver extends Deriver {

    public final IntRange premisesPerIteration = new IntRange(2, 1, 32);
    public final IntRange termLinksPerTaskLink = new IntRange(3, 1, 4);

    public BatchDeriver(PremiseRuleSet rules) {
        super(rules, rules.nar);
    }

    @Override
    protected final void derive(Derivation d, BooleanSupplier kontinue) {

        int matchTTL = matchTTL();
        int deriveTTL = d.nar().deriveBranchTTL.intValue();
        int premisesPerIteration = this.premisesPerIteration.intValue();
        int termLinksPerTaskLink = this.termLinksPerTaskLink.intValue();
        What w = d.what;

        do {
            w.derive(premisesPerIteration, termLinksPerTaskLink, matchTTL, deriveTTL, d);
        } while (kontinue.getAsBoolean());

    }

}
