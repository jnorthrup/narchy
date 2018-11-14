package nars.budget;

import nars.NAR;

/** abstract attention economy model */
public abstract class Budget {

    public final DeriverBudget deriving;

    public final Activator linking;

    public final Forgetting forgetting;

    protected Budget(DeriverBudget deriving, Activator linking, Forgetting forgetting) {
        this.deriving = deriving;
        this.linking = linking;
        this.forgetting = forgetting;
    }

    /**
     * updated each cycle
     */
    public void update(NAR nar) {

        forgetting.update(nar);

        deriving.update(nar);

        linking.update(nar);

    }

}
