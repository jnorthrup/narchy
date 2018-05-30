package jcog.learn;

import java.util.Random;

/**
 * http:
 *
 * Notes ( http:
 * Surprisingly, the same robustness is observed for the choice of the neural
 network size and structure. In our experience, a multilayer perceptron with 2
 hidden layers and 20 neurons per layer works well over a wide range of applications.
 We use the tanh activation function for the hidden neurons and the
 standard sigmoid function at the output neuron. The latter restricts the output
 range of estimated path costs between 0 and 1 and the choice of the immediate
 costs and terminal costs have to be done accordingly. This means, in a typical
 setting, terminal goal costs are 0, terminal failure costs are 1 and immediate
 costs are usually set to a small value, e.g. c = 0.01. The latter is done with the
 consideration, that the expected maximum episode length times the transition
 costs should be well below 1 to distinguish successful trajectories from failures.
 As a general impression, the success of learning depends much more on the
 proper setting of other parameters of the learning framework. The neural network
 and its training procedure work very robustly over a wide range of choices.
 */
public class MLPMap {

    public static class MLPLayer {

        final float[] output;
        final float[] input;
        final float[] weights;
        final float[] dweights;
        boolean isSigmoid = true;

        public MLPLayer(int inputSize, int outputSize, Random r, boolean sigmoid) {
            output = new float[outputSize];
            input = new float[inputSize + 1];
            weights = new float[(1 + inputSize) * outputSize];
            dweights = new float[weights.length];
            this.isSigmoid = sigmoid;
            initWeights(r);
        }

        public void setIsSigmoid(boolean isSigmoid) {
            this.isSigmoid = isSigmoid;
        }

        public void initWeights(Random r) {
            for (int i = 0; i < weights.length; i++) {
                weights[i] = (r.nextFloat() - 0.5f) * 4f;
            }
        }

        public float[] run(float[] in) {
            System.arraycopy(in, 0, input, 0, in.length);
            input[input.length - 1] = 1;
            int offs = 0;
            int il = input.length;
            for (int i = 0; i < output.length; i++) {
                float o = 0;
                for (int j = 0; j < il; j++) {
                    o += weights[offs + j] * input[j];
                }
                if (isSigmoid) {
                    o = (float) (1 / (1 + Math.exp(-o)));
                }
                output[i] = o;
                offs += il;
            }
            return output;
        }

        public float[] train(float[] inError, float learningRate, float momentum) {

            float[] outError = new float[input.length];
            int inLength = input.length;

            int offs = 0;
            for (int i = 0; i < output.length; i++) {
                float d = inError[i];
                if (isSigmoid) {
                    float oi = output[i];
                    d *= oi * (1 - oi);
                }
                float dLR = d * learningRate;
                for (int j = 0; j < inLength; j++) {
                    int idx = offs + j;
                    outError[j] += weights[idx] * d;
                    float dw = input[j] * dLR;
                    weights[idx] += dweights[idx] * momentum + dw;
                    dweights[idx] = dw;
                }
                offs += inLength;
            }
            return outError;
        }
    }

    public final MLPLayer[] layers;

    public MLPMap(int inputSize, int[] layersSize, Random r, boolean sigmoid) {
        layers = new MLPLayer[layersSize.length];
        for (int i = 0; i < layersSize.length; i++) {
            int inSize = i == 0 ? inputSize : layersSize[i - 1];
            layers[i] = new MLPLayer(inSize, layersSize[i], r, sigmoid);
        }
    }

    public float[] get(float[] input) {
        float[] actIn = input;
        for (int i = 0; i < layers.length; i++) {
            actIn = layers[i].run(actIn);
        }
        return actIn;
    }

    /** returns an error vector */
    public float[] put(float[] input, float[] targetOutput, float learningRate, float momentum) {
        float[] calcOut = get(input);
        float[] error = new float[calcOut.length];
        for (int i = 0; i < error.length; i++) {
            error[i] = targetOutput[i] - calcOut[i]; 
        }
        for (int i = layers.length - 1; i >= 0; i--) {
            error = layers[i].train(error, learningRate, momentum);
        }
        return error;
    }

    public static void main(String[] args) {

        float[][] train = {new float[]{0, 0}, new float[]{0, 1}, new float[]{1, 0}, new float[]{1, 1}};

        float[][] res = {new float[]{0}, new float[]{1}, new float[]{1}, new float[]{0}};

        MLPMap mlp = new MLPMap(2, new int[]{2, 1}, new Random(), true);
        mlp.layers[1].setIsSigmoid(false);
        Random r = new Random();
        int en = 500;
        for (int e = 0; e < en; e++) {

            for (int i = 0; i < res.length; i++) {
                int idx = r.nextInt(res.length);
                mlp.put(train[idx], res[idx], 0.3f, 0.6f);
            }

            if ((e + 1) % 100 == 0) {
                System.out.println();
                for (int i = 0; i < res.length; i++) {
                    float[] t = train[i];
                    System.out.printf("%d epoch\n", e + 1);
                    System.out.printf("%.1f, %.1f --> %.5f\n", t[0], t[1], mlp.get(t)[0]);
                }
            }
        }
    }
}
