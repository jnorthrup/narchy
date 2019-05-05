package nars.derive.impl;

import jcog.math.IntRange;
import nars.derive.Derivation;
import nars.derive.Deriver;
import nars.derive.premise.PremiseDeriverRuleSet;

import java.util.function.BooleanSupplier;


/** buffers premises in batches*/
public class BatchDeriver extends Deriver {

    public final IntRange tasklinksPerIteration = new IntRange(1, 1, 32);
    public final IntRange termlinksPerTaskLink = new IntRange(1, 1, 4);



    public BatchDeriver(PremiseDeriverRuleSet rules) {
        super(rules, rules.nar);
    }

    @Override
    protected final void derive(Derivation d, BooleanSupplier kontinue) {

        int matchTTL = matchTTL(), deriveTTL = d.nar().deriveBranchTTL.intValue();

        do {

            d.derive(matchTTL, deriveTTL);

        } while (kontinue.getAsBoolean());

    }





}
