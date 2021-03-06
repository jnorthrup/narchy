package jcog.learn.ql;

import jcog.decide.DecideSoftmax;
import jcog.decide.Deciding;
import jcog.learn.Autoencoder;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiFunction;

/**
 * Created by me on 5/22/16.
 */
public class HaiQae extends HaiQ {

    public static final Logger logger = LoggerFactory.getLogger(HaiQae.class);


    public Autoencoder ae;
    float perceptionAlpha = 0.01f;
    float perceptionNoise = 0.0f;
    float perceptionCorruption = (float) 0; //0.01f;
//    float perceptionForget;
    public float perceptionError;

    

    /**
     * "horizontal" input state selection
     */
    protected Deciding decideState = //DecideEpsilonGreedy.ArgMax;
                                    new DecideSoftmax(0.5f, rng);

    public HaiQae(int inputs, int outputs) {
        this(inputs,
                new BiFunction<Integer, Integer, Integer>() {
                    @Override
                    public Integer apply(Integer i, Integer o) {
                        return (int) Math.ceil(/*Math.sqrt*/(double) (1 + i * 2));
                    }
                }, outputs);
    }

    public HaiQae(int inputs, BiFunction<Integer,Integer,Integer> states, int outputs) {
        this(inputs, states.apply(inputs, outputs), outputs);
    }

    public HaiQae(int inputs, int states, int outputs) {
        super(states, outputs);

        this.ae = new Autoencoder(inputs, states, rng);
    }

    @Override
    protected int perceive(float[] input) {

        perceptionError = ae.put(input, perceptionAlpha, perceptionNoise, perceptionCorruption,
                false, true, true)
            / (float) input.length;


        return decideState.applyAsInt(ae.y);
    }

    @Override
    public final int decide(@Nullable float[] actionFeedback, float reward, float[] input) {
        return act(actionFeedback, reward, input, perceptionError);
    }

    protected int act(float[] actionFeedback, float reward, float[] input, float pErr) {
        
        //float learningRate = 1f - (pErr);
        int p = perceive(input);
//        if (learningRate > 0) {

        return learn(actionFeedback, p, reward, true);
//        } else {
//            return rng.nextInt(actions);
//        }


    }



}
