package nars.attention;

import nars.NAR;
import nars.control.DurService;

/** abstract attention economy model */
public abstract class Attention extends DurService {

    public DerivePri deriving;

    public Activator activating;

    public Forgetting forgetting;


    protected Attention(DerivePri deriving, Activator activating, Forgetting forgetting) {
        super((NAR)null);
        this.deriving = deriving;
        this.activating = activating;
        this.forgetting = forgetting;
    }

    @Override
    protected void starting(NAR nar) {
        super.starting(nar);
        on(
            nar.onCycle(this::cycle)
        );
    }

    @Override
    protected void run(NAR n, long dt) {

        forgetting.update(nar);

    }

    private void cycle() {
        deriving.update(nar);

        activating.update(nar);
    }
}
