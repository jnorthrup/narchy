package nars.concept.action;

import jcog.decide.DecideEpsilonGreedy;
import jcog.decide.Deciding;
import jcog.math.FloatSupplier;
import nars.NAR;
import nars.concept.sensor.DigitizedScalar;
import nars.concept.sensor.Signal;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;

import java.util.function.IntPredicate;

public class SwitchAction extends DigitizedScalar implements FloatSupplier {

    final static float EXP_IF_UNKNOWN =
            
            0;

    private final Deciding decider;
    final float[] exp;
    private final IntPredicate action;

    public SwitchAction(@NotNull NAR nar, IntPredicate action, @NotNull Term... states) {
        super(null, Needle, nar, states);
        this.decider =
                
                new DecideEpsilonGreedy(0.1f, nar.random());
        this.action = action;
        exp = new float[states.length];
        
    }

    protected int decide(long start, long end) {
        for (int i = 0, sensorsSize = sensors.size(); i < sensorsSize; i++) {
            Signal x = sensors.get(i);
            Truth g = x.goals().truth(start, end, nar);

            exp[i] = g != null ?
                    
                    2*(g.freq()-0.5f) * g.evi()
                    :
                    EXP_IF_UNKNOWN;
        }
        return decider.decide(exp, -1);
    }

    @Override
    public void update(long start, long end, int dur, NAR n) {
        int d = decide(start, end);

        if (action.test(d))
            value.set(((float)d)/exp.length);
        else
            value.set(Float.NaN);

        super.update(start, end, dur, n);
    }
}
