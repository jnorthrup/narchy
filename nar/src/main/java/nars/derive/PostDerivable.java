package nars.derive;

import nars.NAL;
import nars.derive.action.PremiseAction;
import nars.truth.MutableTruth;
import nars.truth.func.TruthFunction;

public class PostDerivable {

    public final PreDerivation d;
    public final MutableTruth truth = new MutableTruth();

    public float pri;
    private byte punc;

    private TruthFunction truthFunction;
    private boolean single;
    private PremiseAction action;

    PostDerivable(PreDerivation d) {
        this.d = d;
    }



    public float can(PremiseAction a, Derivation d) {
        float p = a.pri(d);
        if (p > Float.MIN_NORMAL) {
            this.action = a;
            this.truth.set( d.truth );
            this.punc = d.punc;
            this.single = d.single;
            this.truthFunction = d.truthFunction;
            return this.pri = p;
        } else {
            this.action = null;
            this.pri = 0;
            return 0;
        }
    }


    public final boolean run() {
        Derivation d = (Derivation) this.d;
        d.clear();
        d.retransform.clear();
        d.forEachMatch = null;

        d.truth.set(truth);
        d.punc = punc;
        d.single = single;
        d.truthFunction = truthFunction;

        System.out.println(d + " " + action);
        action.run(d);

        return d.use(NAL.derive.TTL_COST_BRANCH);
    }
}
