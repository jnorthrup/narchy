package nars.game.action;

import jcog.decide.DecideSoftmax;
import jcog.decide.Deciding;
import jcog.math.FloatSupplier;
import nars.NAR;
import nars.attention.What;
import nars.game.Game;
import nars.game.sensor.DigitizedScalar;
import nars.game.sensor.Signal;
import nars.term.Term;
import nars.time.When;
import nars.truth.Truth;

import java.util.function.IntPredicate;

public class SwitchAction extends DigitizedScalar implements FloatSupplier {

    final static float EXP_IF_UNKNOWN = 0;

    private final Deciding decider;
    final float[] exp;
    private final IntPredicate action;

    public SwitchAction(NAR nar, IntPredicate action, Term... states) {
        super(null, Needle, nar, states);
        this.input = value::floatValue;
        this.decider =
                //new DecideEpsilonGreedy(0.05f, nar.random());
                new DecideSoftmax(0.5f, nar.random());
        this.action = action;
        exp = new float[states.length];


    }

    protected int decide(long start, long end) {
        for (int i = 0, sensorsSize = sensors.size(); i < sensorsSize; i++) {
            Signal x = sensors.get(i);
            Truth g = x.goals().truth(start, end, nar);

            exp[i] = g != null ? q(g) : EXP_IF_UNKNOWN;
        }

        return decider.applyAsInt(exp);

    }

    /** truth -> decidability */
    public float q(Truth g) {
        //return g.expectation();
        return g.freq();
    }

    @Override
    public void accept(Game g) {

        When<What> w = g.nowPercept;

        int d = decide(w.start, w.end);

        if (d!=-1 && action.test(d))
            value.set((d +0.5f)/exp.length);
        else
            value.set(Float.NaN);

        super.accept(g);
    }
}
