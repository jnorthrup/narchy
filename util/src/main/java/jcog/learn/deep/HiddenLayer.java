package jcog.learn.deep;

import java.util.Arrays;
import java.util.Random;
import java.util.function.DoubleFunction;
import java.util.stream.IntStream;

import static jcog.learn.deep.utils.binomial;
import static jcog.learn.deep.utils.uniform;

public class HiddenLayer {
    public int n_in;
    public int n_out;
    public double[][] W;
    public double[] b;
    public Random rng;
    public DoubleFunction<Double> activation;
    public DoubleFunction<Double> dactivation;

    public HiddenLayer(int n_in, int n_out, double[][] W, double[] b, Random rng, String activation) {
        this.n_in = n_in;
        this.n_out = n_out;

        if (rng == null) this.rng = new Random(1234);
        else this.rng = rng;

        if (W == null) {
            this.W = new double[n_out][n_in];
            var a = 1.0 / this.n_in;

            for(var i = 0; i<n_out; i++) {
                for(var j = 0; j<n_in; j++) {
                    this.W[i][j] = uniform(-a, a, rng);
                }
            }
        } else {
            this.W = W;
        }

        if (b == null) this.b = new double[n_out];
        else this.b = b;

        if (activation == null || "sigmoid".equals(activation)) {
            this.activation = utils::sigmoid;
            this.dactivation = utils::dsigmoid;
        } else if ("tanh".equals(activation)) {
            this.activation = utils::tanh;
            this.dactivation = utils::dtanh;
        } else if ("ReLU".equals(activation)) {
            this.activation = utils::ReLU;
            this.dactivation = utils::dReLU;
        } else {
            throw new IllegalArgumentException("activation function not supported");
        }

    }

    public double output(double[] input, double[] w, double b) {
        var bound = n_in;
        var linear_output = IntStream.range(0, bound).mapToDouble(j -> w[j] * input[j]).sum();
        linear_output += b;

        return activation.apply(linear_output);
    }


    public void forward(double[] input, double[] output) {
        for(var i = 0; i<n_out; i++) {
            output[i] = this.output(input, W[i], b[i]);
        }
    }

    public void backward(double[] input, double[] dy, double[] prev_layer_input, double[] prev_layer_dy, double[][] prev_layer_W, double lr) {
        if(dy == null) dy = new double[n_out];

        var prev_n_in = n_out;
        var prev_n_out = prev_layer_dy.length;

        for(var i = 0; i<prev_n_in; i++) {
            dy[i] = 0;
            for(var j = 0; j<prev_n_out; j++) {
                dy[i] += prev_layer_dy[j] * prev_layer_W[j][i];
            }

            dy[i] *= dactivation.apply(prev_layer_input[i]);
        }

        for(var i = 0; i<n_out; i++) {
            for(var j = 0; j<n_in; j++) {
                W[i][j] += lr * dy[i] * input[j];
            }
            b[i] += lr * dy[i];
        }
    }

    public static double[] dropout(int size, double p, Random rng) {
        var mask = new double[10];
        var count = 0;
        for (var i = 0; i < size; i++) {
            var binomial = binomial(1, p, rng);
            if (mask.length == count) mask = Arrays.copyOf(mask, count * 2);
            mask[count++] = binomial;
        }
        mask = Arrays.copyOfRange(mask, 0, count);

        return mask;
    }
}
