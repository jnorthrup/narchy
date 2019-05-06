package nars.derive.impl;

import jcog.math.IntRange;
import nars.attention.What;
import nars.derive.Derivation;
import nars.derive.Deriver;
import nars.derive.premise.PremiseDeriverRuleSet;

import java.util.function.BooleanSupplier;


/** buffers premises in batches*/
public class BatchDeriver extends Deriver {

    public final IntRange premisesPerIteration = new IntRange(1, 1, 32);
    public final IntRange termLinksPerTaskLink = new IntRange(1, 1, 4);

    public BatchDeriver(PremiseDeriverRuleSet rules) {
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
