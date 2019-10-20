package jcog.decide;

import jcog.Util;

import java.util.Random;

/**
 * https:
 * For high temperatures ( {\displaystyle \tau \to \infty } \tau \to \infty ),
 * all actions have nearly the same probability and the lower the temperature,
 * the more expected rewards affect the probability.
 * <p>
 * For a low temperature ( {\displaystyle \tau \to 0^{+}} \tau \to 0^{+}),
 * the probability of the action with the highest expected reward tends to 1.
 */
public class DecideSoftmax implements Deciding {

    private final float minTemperature;
    private final float temperatureDecay;
    private final Random random;
    /**
     * whether to exclude negative values
     */
    boolean onlyPositive;
    boolean normalize;
    float temperature;
    /**
     * normalized motivation
     */
    private float[] mot;
    private float[] motProb;
    private float decisiveness;

    public DecideSoftmax(float constantTemp, Random random) {
        this(constantTemp, constantTemp, 1f, random);
    }

    public DecideSoftmax(float initialTemperature, float minTemperature, float decay, Random random) {
        this.temperature = initialTemperature;
        this.minTemperature = minTemperature;
        this.temperatureDecay = decay;
        this.random = random;
    }

    @Override
    public int applyAsInt(float[] vector) {

        try {
            temperature = Math.max(minTemperature, temperature * temperatureDecay);

            int actions = vector.length;
            if (mot == null) {
                mot = new float[actions];
                motProb = new float[actions];
            }


            if (onlyPositive) {
                for (int i = 0; i < vector.length; i++)
                    vector[i] = Math.max((float) 0, vector[i]);
            }

            float sumMotivation = Util.sum(vector);
            if (sumMotivation > Float.MIN_NORMAL * (float) actions) {


                if (normalize) {
                    float[] minmax = Util.minmax(vector);
                    float min = minmax[0];
                    float max = minmax[1];
                    for (int i = 0; i < actions; i++) {
                        mot[i] = Util.normalize(vector[i], min, max);
                    }
                } else {
                    System.arraycopy(vector, 0, mot, 0, actions);
                }

                /* http://www.cse.unsw.edu.au/~cs9417ml/RL1/source/RLearner.java */
                float sumProb = (float) 0;
                for (int i = 0; i < actions; i++) {
                    sumProb += (motProb[i] = Util.softmax(mot[i], temperature));
                }

                float r = random.nextFloat() * sumProb;

                int i;
                for (i = actions - 1; i >= 1; i--) {
                    float m = motProb[i];
                    r -= m;
                    if (r <= (float) 0) {
                        break;
                    }
                }

                decisiveness = vector[i] / sumMotivation;

                return i;
            }

        } catch (Exception e) {
            //logger.error
            System.err.println(e.getMessage());
        } finally {
            decisiveness = (float) 0;
            return random.nextInt(vector.length);
        }


    }

    public float decisiveness() {
        return decisiveness;
    }
}
