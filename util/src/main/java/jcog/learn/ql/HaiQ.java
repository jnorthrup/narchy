package jcog.learn.ql;

import jcog.decide.DecideEpsilonGreedy;
import jcog.decide.Deciding;
import jcog.learn.Agent;
import jcog.math.FloatRange;
import jcog.random.XoRoShiRo128PlusRandom;

import java.util.Random;

/**
 * q-learning + SOM agent, cognitive prosthetic. designed by patham9
 */
public class HaiQ extends Agent {

    protected final Random rng;

    public float[][] q;
    public float[][] et;


    int lastState, lastDecidedAction = -1;

    /*
     * http:
     * qlearning Alpha is the learning rate. If the reward or transition
     * function is stochastic (random), then alpha should change over time,
     * approaching zero at infinity. This has to do with approximating the
     * expected outcome of a inner product (T(transition)*R(reward)), when one
     * of the two, or both, have random behavior.
     *
     * Gamma is the value of future reward. It can affect learning quite a bit,
     * and can be a dynamic or static value. If it is equal to one, the agent
     * values future reward JUST AS MUCH as current reward. This means, in ten
     * actions, if an agent does something good this is JUST AS VALUABLE as
     * doing this action directly. So learning doesn't work at that well at high
     * gamma values. Conversely, a gamma of zero will cause the agent to only
     * value immediate rewards, which only works with very detailed reward
     * functions.
     *
     * http:
     * the rate of decay (in conjunction with gamma) of the eligibility trace.
     * This is the amount by which the eligibility of a state is reduced each
     * time step that it is not being visited. A low lambda causes a lower
     * reward to propagate back to states farther from the goal. While this can
     * prevent the reinforcement of a path which is not optimal, it causes a
     * state which is far from the goal to receive very little reward. This
     * slows down convergence, because the agent spends more time searching for
     * a path if it starts far from the goal. Conversely, a high lambda allows
     * more of the path to be updated with higher rewards. This suited our
     * implementation, because our high initial epsilon was able to correct any
     * state values which might have been incorrectly reinforced and create a
     * more defined path to the goal in fewer episodes.
     */
    public final FloatRange Gamma = new FloatRange(0, 0, 1f);

    public final FloatRange Lambda = new FloatRange(0, 0, 1f);

    public final FloatRange Alpha = new FloatRange(0, 0, 1f);

    /**
     * input selection; HaiQAgent will not use this in its override of perceive
     */
    public final Deciding decideInput;


    /**
     * "vertical" action selection
     */
    public final Deciding decideAction;

    public HaiQ(int inputs, int actions) {
        super(inputs, actions);

        q = new float[inputs][actions];
        et = new float[inputs][actions];

        setQ(0.02f, 0.5f, 0.75f);
        rng = new XoRoShiRo128PlusRandom(1);


        decideInput =
                DecideEpsilonGreedy.ArgMax;


        decideAction =
                new DecideEpsilonGreedy(0.03f, rng);
        //new DecideSoftmax(0.5f, rng);


    }

    int learn(int state, float reward) {
        return learn(state, reward, 1f, true);
    }

    int learn(int state, float reward, float learningFactor, boolean decide) {

        if (reward != reward)
            reward = 0;


        int action = decide ? nextAction(state) : -1;


        int lastAction = lastDecidedAction;
        if (lastAction != -1) {
            final int lastState = this.lastState;


            float lastQ = q[lastState][lastAction];
            float delta = reward + (Gamma.floatValue() * q[state][action]) - lastQ;
            q[state][action] = lastQ +
                    (learningFactor * Alpha.floatValue()) *
                            delta;
            et[lastState][lastAction] += learningFactor;
            update(delta, Alpha.floatValue() * learningFactor);


        }

        if (decide) {
            this.lastState = state;
            this.lastDecidedAction = action;
        }

        return action;
    }

    private void etScale(float s) {
        for (int i = 0; i < inputs; i++) {
            float[] eti = et[i];
            for (int k = 0; k < actions; k++) {
                eti[k] *= s;
            }
        }
    }

    protected int nextAction(int state) {
        return /*rng.nextFloat() < Epsilon ? randomAction() : */choose(state);
    }

    private int randomAction() {
        return rng.nextInt(actions);
    }

    private void update(float deltaQ, float alpha) {

        float[][] q = this.q;
        float[][] et = this.et;

        float alphaDelta = alpha * deltaQ;
        float gammaLambda = Gamma.floatValue() * Lambda.floatValue();


        for (int i = 0; i < inputs; i++) {
            float[] eti = et[i];
            float[] qi = q[i];

            for (int k = 0; k < actions; k++) {
                qi[k] += alphaDelta * eti[k];
                eti[k] *= gammaLambda;
            }
        }
    }

    protected int choose(int state) {
        return decideAction.applyAsInt(q[state]);
    }


    public void setQ(float alpha, float gamma, float lambda) {
        Alpha.set(alpha);
        Gamma.set(gamma);
        Lambda.set(lambda);
    }

    /**
     * main control function
     */
    @Override
    public int act(float reward, float[] input) {

        return learn(perceive(input), reward);
    }

    protected int perceive(float[] input) {
        return decideInput.applyAsInt(input);
    }


    /**
     * TODO make abstract
     */
    /*protected int perceive(float[] input) {
        som.learn(input);
		return som.winnerx + (som.winnery * som.SomSize);
	}*/


}
