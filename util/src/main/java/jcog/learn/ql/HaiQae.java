package jcog.learn.ql;

import jcog.decide.DecideEpsilonGreedy;
import jcog.decide.Deciding;
import jcog.learn.Autoencoder;
import jcog.math.FloatSupplier;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiFunction;

/**
 * Created by me on 5/22/16.
 */
public class HaiQae extends HaiQ {

    public static final Logger logger = LoggerFactory.getLogger(HaiQae.class);


    public @NotNull Autoencoder ae;
    float perceptionAlpha;
    float perceptionNoise = 0.01f;
    float perceptionCorruption = 0.01f;
    float perceptionForget;
    public FloatSupplier perceptionError;
    public float lastPerceptionError;

    

    /**
     * "horizontal" state selection
     */
    protected final Deciding decideState;

    public HaiQae(int inputs, int outputs) {
        this(inputs,
                (i,o)->(int) Math.ceil(Math.sqrt(1 + (1+i)*(1+o))), outputs);
    }

    public HaiQae(int inputs, BiFunction<Integer,Integer,Integer> states, int outputs) {
        this(inputs, states.apply(inputs, outputs), outputs);
    }

    public HaiQae(int inputs, int states, int outputs) {
        super(states, outputs);
        
        this.perceptionAlpha =
                0.02f;
        this.perceptionError =
                ()->0.01f;
                

        this.decideState =
                DecideEpsilonGreedy.ArgMax;
                //new DecideSoftmax(0.1f, rng);
                

        this.ae = perception(inputs, states);
    }

    protected Autoencoder perception(int inputs, int states) {
        return new Autoencoder(inputs, states, rng);
    }









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

        
        float pErr = perceptionError.asFloat();
        return act(reward, input, pErr);
    }

    protected int act(float reward, float[] input, float pErr) {
        
        float learningRate = 1f - (pErr); 
        if (learningRate > 0) {
            
            int a = learn(perceive(input), reward, learningRate, true);
            return a;
        } else {
            perceive(input); 
            return rng.nextInt(actions);
        }


    }






































}
