package nars.game.util;

import jcog.learn.Agent;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.game.Game;
import nars.game.action.SwitchAction;
import nars.game.sensor.GameLoop;
import nars.term.Term;

import java.util.stream.IntStream;

import static java.lang.System.arraycopy;

/** wraps a complete NAR in an Agent interface for use with generic non-NAR 'Agent' MDP environments */
public class EmbeddedNAgent extends Agent {

    /** increase for more power */
    final static int DUR_CYCLES = 1;

    private final Game env;
    final float[] senseValue;
    private final GameLoop[] sense;
    private final SwitchAction act;
    private int nextAction = -1;
    private float nextReward = Float.NaN;

    private static final NAR defaultNAR() {
        NAR n = NARS.tmp();
        n.termVolMax.set(10);
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

        this.env = new Game("agent", n);

        this.sense = IntStream.range(0, inputs).mapToObj(i -> env.sense($.inh($.the(i), env.id), ()-> senseValue[i])
        ).toArray(GameLoop[]::new);

        this.env.addSensor(act = new SwitchAction(n, (a) -> {
                nextAction = a;
                return true;
            }, IntStream.range(0, actions).mapToObj(a -> $.inh($.the(a), env.id)).toArray(Term[]::new))
        );

        this.env.reward(()->nextReward);

    }

    @Override
    public int decide(float[] actionFeedback, float reward, float[] input) {

        this.nextReward = reward;

        arraycopy(input, 0, senseValue, 0, senseValue.length);

        env.nar().run(DUR_CYCLES);

        return nextAction;
    }

}
