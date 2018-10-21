package nars.concept.action.curiosity;

import jcog.Util;
import nars.$;
import nars.NAR;
import nars.concept.action.AbstractGoalActionConcept;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/** TODO pull out parameters to top-level
 * untested - suspect - may fall into a dead state
 * */
final class RandomPhasorCuriosity extends CuriosityMode {

    static class RandomPhasor {
        float fMin, fMax;
        float f, theta;
        long lastUpdate;
        float phaseShiftAmount = 1;
        float freqChangeAmount = 0.1f;

        public RandomPhasor(long start, Random random) {
            this.fMax = 1;
            this.lastUpdate = start;
            theta = (float) (random.nextFloat() * Math.PI*2); //TODO random
        }

        public float update(long t, int dur, Random rng) {
            fMin = 1f/(dur*dur);
            theta +=  (1 + (rng.nextFloat()-0.5f)*2f * phaseShiftAmount);
            f += Util.clamp( (rng.nextFloat()-0.5f)*2f * freqChangeAmount , fMin, fMax);
            return (float) ((1 + Math.sin( theta + (t*Math.PI*2) * f))/2.0);
        }
    }

    final Map<AbstractGoalActionConcept, RandomPhasor> phasor = new HashMap();

    @Override
    public @Nullable Truth get(AbstractGoalActionConcept action, Curiosity curiosity) {

        NAR n = curiosity.agent.nar();

        RandomPhasor pCuri = phasor.computeIfAbsent(action, (a) -> new RandomPhasor(n.time(), n.random()));

        return $.t(pCuri.update(n.time(), n.dur(), n.random()), curiosity.conf.floatValue());
    }
}
