package nars.derive;

import nars.truth.MutableTruth;
import nars.truth.func.TruthFunction;

public class PostDerivable {

    public final PreDerivation d;
    public final MutableTruth truth = new MutableTruth();

    public float pri;
    private byte punc;

    private TruthFunction truthFunction;
    private boolean single;
    private DeriveAction action;

    PostDerivable(PreDerivation d) {
        this.d = d;
    }



    public float can(DeriveAction a, Derivation d) {
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

    @Deprecated public boolean apply(Derivation d) {
        d.truth.set(truth);
        d.punc = punc;
        d.single = single;
        d.truthFunction = truthFunction;
        return true;
    }


    public final boolean run() {
        return action.run(this);
    }
}
