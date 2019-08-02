package nars.derive.rule;

import nars.derive.model.Derivation;
import nars.derive.model.PreDerivation;
import nars.truth.Truth;
import nars.truth.func.TruthFunc;

public class PostDerivable {

    public final PreDerivation d;

    public float pri;
    public byte concPunc;
    public Truth concTruth;
    public TruthFunc truthFunction;
    public boolean concSingle;
    private DeriveAction action;

    public PostDerivable(PreDerivation d) {
        this.d = d;
    }

    /** returns <= 0 for impossible */
    private float pri(DeriveAction a, Derivation d) {

        float p = a.pri(d);
        if (p <= Float.MIN_NORMAL || !a.truth.test(d))
            return 0;

        return d.what.derivePri.prePri(
            p,
            d.concTruth /* important: not this.concTruth which has not been set yet */
        );
    }

    public float priSet(DeriveAction a, Derivation d) {
        float p = pri(a, d);
        if (p > Float.MIN_NORMAL) {
            this.action = a;
            this.concTruth = d.concTruth;
            this.concPunc = d.concPunc;
            this.concSingle = d.concSingle;
            this.truthFunction = d.truthFunction;
            return this.pri = p;
        } else {
            this.pri = 0;
            this.action = null;
            return 0;
            //shouldnt be necessary to keep setting:
//            this.concTruth = null;
//            this.concPunc = 0;
//            this.concSingle = false;
//            this.truthFunction = null;
        }
    }

    @Deprecated public boolean apply(Derivation d) {
        d.concTruth = concTruth;
        d.concPunc = concPunc;
        d.concSingle = concSingle;
        d.truthFunction = truthFunction;
        return true;
    }


    public final boolean run() {
        return action.run(this);
    }
}
