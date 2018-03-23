package jcog.learn;

import jcog.TODO;

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

    /** for reporting action vectors, when implementation supports. otherwise it will be a zero vector except one chosen entry
     * by the basic markov process act() method */
    public void act(float reward, float[] input, float[] outputs /* filled by this method */) {
        throw new TODO();
    }

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
