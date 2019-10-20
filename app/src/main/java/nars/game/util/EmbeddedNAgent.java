package nars.game.util;

import jcog.learn.Agent;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.game.Game;
import nars.game.action.SwitchAction;
import nars.game.sensor.GameLoop;
import nars.game.sensor.Signal;
import nars.term.Term;

import java.util.ArrayList;
import java.util.List;

import static java.lang.System.arraycopy;

/** wraps a complete NAR in an Agent interface for use with generic non-NAR 'Agent' MDP environments */
public class EmbeddedNAgent extends Agent {

    /** increase for more power */
    static final int DUR_CYCLES = 1;

    private final Game env;
    final float[] senseValue;
    private int nextAction = -1;
    private float nextReward = Float.NaN;

    private static final NAR defaultNAR() {
        final NAR n = NARS.tmp();
        n.termVolMax.set(10);
        n.freqResolution.set(0.1f);
        return n;
    }

    public EmbeddedNAgent(final int inputs, final int actions) {
        this(defaultNAR(), inputs, actions);
    }

    public EmbeddedNAgent(final NAR n, final int inputs, final int actions) {
        super(inputs, actions);

        senseValue = new float[inputs];

        n.time.dur((float) DUR_CYCLES);

        this.env = new Game("agent");

        final List<Signal> result = new ArrayList<>();
        for (int j = 0; j < inputs; j++) {
            final int i1 = j;
            final Signal signal = env.sense($.inh($.the(i1), env.id), () -> senseValue[i1]);
            result.add(signal);
        }
        final GameLoop[] sense = result.toArray(new GameLoop[0]);

        final List<Object> list = new ArrayList<>();
        for (int i = 0; i < actions; i++) {
            final Object inh = $.inh($.the(i), env.id);
            list.add(inh);
        }
        final SwitchAction act;
        this.env.addSensor(act = new SwitchAction(n, (a) -> {
                nextAction = a;
                return true;
            }, list.toArray(new Term[0]))
        );

        this.env.reward(()->nextReward);

    }

    @Override
    public int decide(final float[] actionFeedback, final float reward, final float[] input) {

        this.nextReward = reward;

        arraycopy(input, 0, senseValue, 0, senseValue.length);

        env.nar().run(DUR_CYCLES);

        return nextAction;
    }

}
