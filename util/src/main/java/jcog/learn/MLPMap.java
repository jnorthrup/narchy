package jcog.learn;

import jcog.learn.ntm.control.IDifferentiableFunction;
import jcog.learn.ntm.control.SigmoidActivation;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
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

    public final MLPLayer[] layers;

    /**
     * gradient momentum.  The coefficient for how much of the previous delta is applied to each weight.
     * In theory, prevents local minima stall.
     */
    float momentum = 0.1f;

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

        public final float[] out;
        public final float[] in;
        final float[] delta;
        public final float[] W;
        final float[] dW;
        @Nullable
        final IDifferentiableFunction activation;
        private final boolean bias;


        public MLPLayer(int inputSize, int outputSize, @Nullable IDifferentiableFunction activation, boolean bias) {
            this.bias = bias;
            out = new float[outputSize];
            in = new float[inputSize + (bias ? 1 : 0)];
            delta = new float[in.length];
            W = new float[((bias ? 1 : 0) + inputSize) * outputSize];
            dW = new float[W.length];
            this.activation = activation;
        }



        public void randomizeWeights(Random r) {
            randomizeWeights(r, 1f);
        }

        public void randomizeWeights(Random r, float scale) {
            for (int i = 0; i < W.length; i++) {
                W[i] = (r.nextFloat() - 0.5f) * 2f * scale;
            }
        }

        public float[] run(float[] in) {
            System.arraycopy(in, 0, this.in, 0, in.length);
            if (bias)
                this.in[this.in.length - 1] = 1; //bias
            int offs = 0;
            int il = this.in.length;
            for (int i = 0; i < out.length; i++) {
                float o = 0;
                for (int j = 0; j < il; j++) {
                    o += W[offs + j] * this.in[j];
                }
                if (activation!=null)
                    o = (float) activation.value(o);
                out[i] = o;
                offs += il;
            }
            return out;
        }

        public float[] train(float[] incomingError, float learningRate, float momentum) {


            Arrays.fill(this.delta, 0);
            int inLength = in.length;

            int offs = 0;
            for (int o = 0; o < out.length; o++) {
                float gradient = incomingError[o];
                if (activation!=null)
                    gradient *= activation.derivative(out[o]);

                float outputDelta = gradient * learningRate;

                for (int i = 0; i < inLength; i++) {
                    int ij = offs++;

                    this.delta[i] += W[ij] * gradient;

                    float dw = in[i] * outputDelta;

                    //TODO check this

                    float delta = (dw) + (dW[ij] * momentum);
                    W[ij] += delta;
                    dW[ij] = delta;
//                    weights[idx] += dw * dweights[idx];
//                    dweights[idx] += dw;


                }

            }
            return this.delta;
        }
    }


    @Deprecated public MLPMap(int inputSize, int[] layersSize, Random r, boolean sigmoid) {
        layers = new MLPLayer[layersSize.length];
        for (int i = 0; i < layersSize.length; i++) {
            int inSize = i == 0 ? inputSize : layersSize[i - 1];
            layers[i] = new MLPLayer(inSize, layersSize[i], sigmoid ? SigmoidActivation.the : null, true);
        }
        randomize(r);
    }

    public MLPMap(int inputs, Layer... layer) {
        assert(layer.length > 0);
        layers = new MLPLayer[layer.length];
        for (int i = 0; i < layer.length; i++) {
            int inSize = i > 0 ? layer[i-1].size : inputs;
            int outSize = layer[i].size;
            layers[i] = new MLPLayer(inSize, outSize, layer[i].activation, true);
        }
    }
//    public MLPMap(Random r, int inputs, Layer... layer) {
//        this(inputs, layer);
//        randomize(r);
//    }

    public MLPMap randomize(Random r) {
        for (MLPLayer m : layers)
            m.randomizeWeights(r);
        return this;
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
        float[] errorOut = new float[calcOut.length];

        for (int i = 0; i < errorOut.length; i++)
            errorOut[i] = targetOutput[i] - calcOut[i];

        //backprop
        float[] error = errorOut;
        for (int i = layers.length - 1; i >= 0; i--)
            error = layers[i].train(error, learningRate, momentum);

        return errorOut;
    }

    /** gets the last computed out, from either a get() or put() call */
    public float[] out() {
        return layers[layers.length-1].out;
    }
}
