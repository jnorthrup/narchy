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

    /** returns <= 0 for impossible */
    private float pri(DeriveAction a, Derivation d) {

        byte punc = a.truth.preFilter(d);
        if (punc == 0)
            return 0f; //disabled or not applicable to the premise

        float p = a.pri(d);
        if (p <= Float.MIN_NORMAL)
            return 0;

        if (!a.truth.test(d))
            return 0;

        return p * d.what.derivePri.prePri(d);
    }

    public float priSet(DeriveAction a, Derivation d) {
        float p = pri(a, d);
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
            //shouldnt be necessary to keep setting:
//            this.concTruth = null;
//            this.concPunc = 0;
//            this.concSingle = false;
//            this.truthFunction = null;
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
