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
//        this.d = d;
//        this.c = c;
//        punc = d.concPunc;
//        t = d.concTruth;
//        tf = d.truthFunction;
//        single = d.concSingle;
//        this.conclusion = cc;
//        this.value = v + ( t != null ? t.conf() : 0 /* biased against questions */);
    }

    /** returns <= 0 for impossible */
    private float pri(DeriveAction a, Derivation d) {


        float p = a.pri(d);
        if (p <= 0)
            return 0;

        if (!a.truth.test(d))
            return 0;

        this.action = a;
        this.concTruth = d.concTruth;
        this.concPunc = d.concPunc;
        this.concSingle = d.concSingle;
        this.truthFunction = d.truthFunction;

        return d.what.derivePri.prePri(p, concTruth);
    }

    public float priSet(DeriveAction a, Derivation d) {
        float p = pri(a, d);
        this.pri = Math.max(0,p);
        return p;
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
