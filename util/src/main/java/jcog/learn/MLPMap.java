package jcog.learn;

import jcog.learn.ntm.control.IDifferentiableFunction;
import jcog.learn.ntm.control.SigmoidActivation;
import jcog.random.XoRoShiRo128PlusRandom;
import org.jetbrains.annotations.Nullable;

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

    /** layer def */
    public static class Layer {
        final int size;
        final IDifferentiableFunction activation;

        public Layer(int size, IDifferentiableFunction activation) {
            this.size = size;
            this.activation = activation;
        }
    }
    public static class MLPLayer {

        public final float[] output;
        public final float[] input;
        public final float[] weights;
        final float[] dweights;
        @Nullable
        final IDifferentiableFunction activation;

        float momentum = 0.5f;

        public MLPLayer(int inputSize, int outputSize, @Nullable IDifferentiableFunction activation) {
            output = new float[outputSize];
            input = new float[inputSize + 1];
            weights = new float[(1 + inputSize) * outputSize];
            dweights = new float[weights.length];
            this.activation = activation;
        }



        public void randomizeWeights(Random r) {
            randomizeWeights(r, 1f);
        }

        public void randomizeWeights(Random r, float scale) {
            for (int i = 0; i < weights.length; i++) {
                weights[i] = (r.nextFloat() - 0.5f) * 2f * scale;
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
                if (activation!=null)
                    o = (float) activation.value(o);
                output[i] = o;
                offs += il;
            }
            return output;
        }

        public float[] train(float[] inError, float learningRate) {

            float[] outError = new float[input.length];
            int inLength = input.length;

            int offs = 0;
            for (int i = 0; i < output.length; i++) {
                float d = inError[i];
                if (activation!=null)
                    d *= activation.derivative(output[i]);
                float dLR = d * learningRate;
                for (int j = 0; j < inLength; j++) {
                    int idx = offs + j;
                    outError[j] += weights[idx] * d;
                    float dw = input[j] * dLR;

                    //TODO check this

                    weights[idx] += dweights[idx] * (1-momentum) + dw;
                    dweights[idx] = dw;
//                    weights[idx] += dw * dweights[idx];
//                    dweights[idx] += dw;


                }
                offs += inLength;
            }
            return outError;
        }
    }

    public final MLPLayer[] layers;

    @Deprecated public MLPMap(int inputSize, int[] layersSize, Random r, boolean sigmoid) {
        layers = new MLPLayer[layersSize.length];
        for (int i = 0; i < layersSize.length; i++) {
            int inSize = i == 0 ? inputSize : layersSize[i - 1];
            layers[i] = new MLPLayer(inSize, layersSize[i], sigmoid ? SigmoidActivation.the : null);
        }
        randomize(r);
    }

    public MLPMap(Random r, int inputs, Layer... layer) {
        assert(layer.length > 0);
        layers = new MLPLayer[layer.length];
        for (int i = 0; i < layer.length; i++) {
            int inSize = i > 0 ? layer[i-1].size : inputs;
            int outSize = layer[i].size;
            layers[i] = new MLPLayer(inSize, outSize, layer[i].activation);
        }
        randomize(r);
    }

    public void randomize(Random r) {
        for (MLPLayer m : layers) {
            m.randomizeWeights(r);
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
    public float[] put(float[] input, float[] targetOutput, float learningRate) {
        float[] calcOut = get(input);
        float[] error = new float[calcOut.length];
        for (int i = 0; i < error.length; i++) {
            error[i] = targetOutput[i] - calcOut[i]; 
        }
        for (int i = layers.length - 1; i >= 0; i--) {
            error = layers[i].train(error, learningRate);
        }
        return error;
    }

    public static void main(String[] args) {

        float[][] train = {new float[]{0, 0}, new float[]{0, 1}, new float[]{1, 0}, new float[]{1, 1}};

        float[][] res = {new float[]{0}, new float[]{1}, new float[]{1}, new float[]{0}};

        MLPMap mlp = new MLPMap(new XoRoShiRo128PlusRandom(1),2,
                new Layer(2,SigmoidActivation.the),
                new Layer(1,null)
        );
                //, new int[]{2, 1}, new Random(), true);

        Random r = new Random();
        int en = 500;
        for (int e = 0; e < en; e++) {

            for (int i = 0; i < res.length; i++) {
                int idx = r.nextInt(res.length);
                mlp.put(train[idx], res[idx], 0.5f);
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
