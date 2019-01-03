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


    /**
     *
     * @param actionFeedback actions actually acted in previous cycle
     * @param reward reward associated with the previous cycle's actions
     * @param nextObservation next sensory observation
     * @return decision of which action to want for the next cycle
     */
    public abstract int act(float[] actionFeedback, float reward, float[] nextObservation);

//    /** for reporting action vectors, when implementation supports. otherwise it will be a zero vector except one chosen entry
//     * by the basic markov process act() method */
//    public void act(float reward, float[] input, float[] outputs /* filled by this method */) {
//        throw new TODO();
//    }







    @Override
    public String toString() {
        return summary();
    }

    public String summary() {
        return getClass().getSimpleName() + "<in=" + inputs + ", act=" + actions + ">";
    }
}
