package jcog.learn;

import jcog.signal.Tensor;
import org.jetbrains.annotations.Nullable;

/**
 * lowest common denominator markov decision process / reinforcement learning agent interface
 * TODO support continuous output vectors
 */
public abstract class Agent {

    public final int inputs;
    public final int actions;
    private int lastDecision = -1;

    protected Agent(int inputs, int actions) {
        this.inputs = inputs;
        this.actions = actions;
    }


    public final int act(float reward, Tensor input) {
        return act(null, reward, input);
    }
    public final int act(@Nullable Tensor actionFeedback, float reward, Tensor input) {
        return act(actionFeedback!=null ? actionFeedback.floatArrayShared() : null, reward, input.floatArrayShared());
    }

    /** assumes the previous action decided had ideal compliance of the motor system and so no
     * transformation or reduction or noise etc was experienced.
     */
    public final int act(float reward, float[] nextInput) {
        return act(null, reward, nextInput);
    }


    /** TODO use tensor */
    public final int act(@Nullable float[] actionFeedback, float reward, @Deprecated float[] input) {
        if (actionFeedback == null) {
            actionFeedback = new float[actions];
            if (lastDecision >= 0)
                actionFeedback[lastDecision] = +1.0f;
        } else {
//            Util.normalize(actionFeedback);
        }
        var decided = decide(actionFeedback, reward, input);
        this.lastDecision = decided;
        return decided;
    }

    /**
     *
     * @param actionFeedback actions actually acted in previous cycle
     * @param reward reward associated with the previous cycle's actions
     * @param input next sensory observation
     * @return decision of which action to want for the next cycle
     */
    protected abstract int decide(float[] actionFeedback, float reward, float[] input);

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
        return getClass().getSimpleName() + "<in=" + inputs + ", act=" + actions + '>';
    }
}
