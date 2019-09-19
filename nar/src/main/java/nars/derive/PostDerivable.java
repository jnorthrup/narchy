package nars.derive;

import jcog.decide.MutableRoulette;
import nars.derive.action.PremiseAction;
import nars.truth.MutableTruth;
import nars.truth.PreciseTruth;
import nars.truth.func.TruthFunction;

import java.util.function.Predicate;

public class PostDerivable {

    public final MutableTruth truth = new MutableTruth();

    public transient float pri;
    private transient byte punc;

    private transient TruthFunction truthFunction;
    private transient boolean single;
    private transient PremiseAction action;

    PostDerivable() {

    }

    public static void run(int valid, int lastValid, Derivation d) {
        PostDerivable[] post = d.post;
        if (valid == 1) {//optimized 1-option case
            //while (post[lastValid].run()) { }
            post[lastValid].run(d);
        } else {//

            //HACK copy post so that recursive calls wont affect it when it drops back to this one
            //temporary until a central BFS-like bag can dispatch streams of preferred actions
            //Predicate<Derivation>[] pp = new Predicate[valid];
            float[] pri = new float[valid];
            for (int i = 0; i < valid; i++) {
                PostDerivable pi = post[i];
                pri[i] = pi.pri;
                //pp[i] = pi.clone();
            }
            MutableRoulette.run(pri, d.random, wi -> 0, i -> post[i].run(d));

            //alternate roulette:
            //  int j; do { j = Roulette.selectRoulette(valid, i -> post[i].pri, d.random);   } while (post[j].run());
        }

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

    /** an immutable clone runnable only version */
    public Predicate<Derivation> clone() {
        PreciseTruth truth = this.truth.clone();
        PremiseAction action = this.action;
        TruthFunction truthFunction = this.truthFunction;
        return d -> d.run(truth, punc, single, truthFunction, action);
    }

    public final boolean run(Derivation d) {
        return d.run(truth, punc, single, truthFunction, action);
    }
}
