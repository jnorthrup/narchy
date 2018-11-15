package nars.attention;

import nars.NAR;
import nars.control.DurService;

/** abstract attention economy model */
public abstract class Attention extends DurService {

    public DerivePri deriving;

    public Activator linking;

    public Forgetting forgetting;

    public final ActiveConcepts concepts;

    protected Attention(DerivePri deriving, Activator linking, Forgetting forgetting, ActiveConcepts concepts) {
        super((NAR)null);
        this.deriving = deriving;
        this.linking = linking;
        this.forgetting = forgetting;
        this.concepts = concepts;
    }

    @Override
    protected void starting(NAR nar) {
        super.starting(nar);

        concepts.starting(nar);
        on(
            nar.eventClear.on(this.concepts::clear)
        );
    }

    @Override
    protected void stopping(NAR nar) {
        concepts.stopping(nar);
        super.stopping(nar);
    }

    @Override
    protected void run(NAR n, long dt) {

        forgetting.update(nar);

        forgetting.updateConcepts(concepts.active, dt, n);


        deriving.update(nar);

        linking.update(nar);
    }
}
