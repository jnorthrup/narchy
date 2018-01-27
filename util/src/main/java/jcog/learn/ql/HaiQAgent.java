package jcog.learn.ql;

import jcog.learn.Autoencoder;
import jcog.math.FloatAveragedAsync;
import jcog.math.FloatSupplier;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiFunction;

/**
 * Created by me on 5/22/16.
 */
public class HaiQAgent extends HaiQ {

    public static final Logger logger = LoggerFactory.getLogger(HaiQAgent.class);


    public @NotNull Autoencoder ae;
    float perceptionAlpha;
    float perceptionNoise;
    float perceptionCorruption = 0.05f;
    float perceptionForget;
    public FloatSupplier perceptionError;
    public float lastPerceptionError;

    //float aeForget = 1f;


    public HaiQAgent(int inputs, int outputs) {
        this(inputs, (i,o)->(int) Math.ceil(Math.sqrt(1 + (i)*(o))), outputs);
    }

    public HaiQAgent(int inputs, BiFunction<Integer,Integer,Integer> states, int outputs) {
        this(inputs, states.apply(inputs, outputs), outputs);
    }

    public HaiQAgent(int inputs, int states, int outputs) {
        super(states, outputs);
        //logger.info("start {} -> {} -> {}", inputs, states, outputs);
        this.perceptionAlpha =
                //1f/(inputs);
                0.05f;
        this.perceptionError = FloatAveragedAsync.averaged(()->lastPerceptionError, inputs/2);
        this.ae = perception(inputs, states);
    }

    protected Autoencoder perception(int inputs, int states) {
        return new Autoencoder(inputs, states, rng);
    }


//    @Override
//    protected int nextAction(int state) {
//        //alpha is applied elsewhere, so here directly choose
//
//        return choose(state);
//    }

    @Override
    protected int perceive(float[] input) {
        lastPerceptionError = ae.put(input, perceptionAlpha, perceptionNoise, perceptionCorruption, false, true, false);
        int w = ae.decide(decideState);
        if (perceptionForget > 0)
            ae.forget(perceptionForget);
        return w;
    }
    @Override
    public int act(float reward, float[] input) {

        //learn more slowly while the perception is not settled
        float pErr = perceptionError.asFloat();
        return act(reward, input, pErr);
    }

    protected int act(float reward, float[] input, float pErr) {
        //float learningRate = 1f / (1f + pErr);
        float learningRate = 1f - (pErr); //pErr/states is more lenient
        if (learningRate > 0) {
            //System.out.println(learningRate + "  "+ pErr);
            int a = learn(perceive(input), reward, learningRate, true);
            return a;
        } else {
            perceive(input); //perceive only
            return rng.nextInt(actions);
        }


    }

//		@Override
//		protected int lastAction() {
//			//evaluate the actual desire values of the action concepts to get a sense of what the NAR actually wants/wanted to do
//			//and what it ideally would have done if it could execute them all in parallel in proportional amounts
//
//			//Termed best = null;
//			int best = -1;
//			float bestE = Float.NEGATIVE_INFINITY;
//
//			int s = actions;
//
//			final float epsi = Epsilon;
//
//
//			for (int j = 0; j < s; j++) {
//
//				//float e = output.get(j).ran();
//				float e = 0;
//
//				//add noise
//				if (epsi != 0) {
//					e += epsi * (rng.nextFloat() - 0.5f) * 2f;
//				}
//
//				//System.out.println(outs.get(j) + " " + e);
//
//				//System.out.println("last action: " + j + " "  + e);
//				if (e >= bestE) {
//					//best = c;
//					best = j;
//					bestE = e;
//				}
//			}
//
//			return best;
//		}

}
