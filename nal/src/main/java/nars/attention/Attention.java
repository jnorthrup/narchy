package nars.attention;

import nars.NAR;
import nars.attention.derive.DefaultDerivePri;
import nars.control.DurService;

/** abstract attention economy model */
public abstract class Attention extends DurService {


    public Activator activating;

    public Forgetting forgetting;

    /** default derivePri for derivers */
    public DerivePri derivePri =
            new DefaultDerivePri();
            //new DefaultPuncWeightedDerivePri();


    protected Attention(Activator activating, Forgetting forgetting) {
        super((NAR)null);
        this.activating = activating;
        this.forgetting = forgetting;
    }

    @Override
    protected void starting(NAR nar) {

        super.starting(nar);
//        on(
//            nar.onCycle(this::cycle)
//        );
    }

    @Override
    protected void run(NAR n, long dt) {

        forgetting.update(n);
        activating.update(n);
        derivePri.update(n);

    }

//    private void cycle(NAR n) {
//
//    }
}
