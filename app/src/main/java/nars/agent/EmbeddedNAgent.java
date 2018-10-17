package nars.agent;

import jcog.learn.Agent;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.concept.action.SwitchAction;
import nars.concept.sensor.Sensor;
import nars.term.Term;

import java.util.stream.IntStream;

import static java.lang.System.arraycopy;

/** wraps a complete NAR in an Agent interface for use with generic non-NAR 'Agent' MDP environments */
public class EmbeddedNAgent extends Agent {

    /** increase for more power */
    final static int DUR_CYCLES = 1;

    private final NAgent env;
    final float[] senseValue;
    private final Sensor[] sense;
    private final SwitchAction act;
    private int nextAction = -1;
    private float nextReward = Float.NaN;

    private static final NAR defaultNAR() {
        NAR n = NARS.tmp();
        n.termVolumeMax.set(10);
        n.freqResolution.set(0.1f);
        return n;
    }

    public EmbeddedNAgent(int inputs, int actions) {
        this(defaultNAR(), inputs, actions);
    }

    public EmbeddedNAgent(NAR n, int inputs, int actions) {
        super(inputs, actions);

        senseValue = new float[inputs];

        n.time.dur(DUR_CYCLES);

        this.env = new NAgent("agent", n);

        this.sense = IntStream.range(0, inputs).mapToObj(i->
            env.sense($.inh($.the(i), env.id), ()-> senseValue[i])
        ).toArray(Sensor[]::new);

        this.env.addSensor(act = new SwitchAction(n, (a) -> {
                nextAction = a;
                return true;
            }, IntStream.range(0, actions).mapToObj(a -> $.inh($.the(a), env.id)).toArray(Term[]::new))
        );

        this.env.reward(()->nextReward);

    }

    @Override
    public int act(float reward, float[] nextObservation) {

        this.nextReward = reward;

        arraycopy(nextObservation, 0, senseValue, 0, senseValue.length);

        env.nar().run(DUR_CYCLES);

        return nextAction;
    }

}
