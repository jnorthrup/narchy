package jcog.learn;

/**
 * lowest common denominator markov decision process / reinforcement learning agent interface
 */
public abstract class Agent {

    public final int inputs;
    public final int actions;

    protected Agent(int inputs, int actions) {
        this.inputs = inputs;
        this.actions = actions;
    }

    //default int act(float reward, TensorF input) {
        //TODO
    //}

    public abstract int act(float reward, float[] nextObservation);

//    default int act(double reward, double... nextObservation) {
//        float[] f = Util.toFloat(nextObservation);
//
//        return act((float)reward, f);
//    }

    @Override
    public String toString() {
        return summary();
    }

    public String summary() {
        return getClass() + "<ins=" + inputs + ", acts=" + actions + ">";
    }
}
