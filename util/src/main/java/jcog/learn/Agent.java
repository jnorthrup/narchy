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

    
        
    

    public abstract int act(float reward, float[] nextObservation);

    /** for reporting action vectors, when implementation supports. otherwise it will be a zero vector except one chosen entry
     * by the basic markov process act() method */
    public void act(float reward, float[] input, float[] outputs /* filled by this method */) {
        throw new TODO();
    }







    @Override
    public String toString() {
        return summary();
    }

    public String summary() {
        return getClass().getSimpleName() + "<in=" + inputs + ", act=" + actions + ">";
    }
}
