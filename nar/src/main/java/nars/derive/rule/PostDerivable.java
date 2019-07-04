package nars.derive.rule;

import nars.derive.model.Derivation;
import nars.derive.model.PreDerivation;
import nars.term.control.PREDICATE;
import nars.truth.Truth;
import nars.truth.func.TruthFunc;

/** TODO */
public class PostDerivable {

    public final PreDerivation d;

    public PREDICATE<Derivation> conclusion;

    public byte concPunc;
    public Truth concTruth;
    public TruthFunc truthFunction;
    public boolean concSingle;

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

    public boolean set(DeriveAction a, Derivation d) {
        if (a.truth.test(d)) {
            this.conclusion = a.conclusion;
            this.concTruth = d.concTruth;
            this.concPunc = d.concPunc;
            this.concSingle = d.concSingle;
            this.truthFunction = d.truthFunction;
            return true;
        } else {
            this.conclusion = null;
            return false;
        }
    }

    @Deprecated public boolean apply(Derivation dd) {
        dd.concTruth = concTruth;
        dd.concPunc = concPunc;
        dd.concSingle = concSingle;
        dd.truthFunction = truthFunction;
        return true;
    }

    public float value(float p) {
        float boost = concTruth!=null ? ((concSingle ? 1 : 2) * concTruth.conf()) : 0; /* biased against questions */
        return p + boost;
    }
}
