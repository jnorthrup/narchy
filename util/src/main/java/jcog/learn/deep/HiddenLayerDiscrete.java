package jcog.learn.deep;

import java.util.Random;
import java.util.stream.IntStream;

import static jcog.learn.deep.utils.*;

public class HiddenLayerDiscrete extends HiddenLayer {
    public int N;
    public int n_in;
    public int n_out;
    public double[][] W;
    public double[] b;
    public Random rng;


    public HiddenLayerDiscrete(int n_in, int n_out, double[][] W, double[] b, Random rng) {
        super(n_in, n_out, W, b, rng, null);

        this.n_in = n_in;
        this.n_out = n_out;

        if(rng == null)	this.rng = new Random(1234);
        else this.rng = rng;

        if(W == null) {
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

        if(b == null) this.b = new double[n_out];
        else this.b = b;
    }

    public double output(int[] input, double[] w, double b) {
        var bound = n_in;
        var linear_output = IntStream.range(0, bound).mapToDouble(j -> w[j] * input[j]).sum();
        linear_output += b;
        return sigmoid(linear_output);
    }

    public void sample_h_given_v(double[] input, double[] sample) {
        for(var i = 0; i<n_out; i++) {
            sample[i] = binomial(1, output(input, W[i], b[i]), rng);
        }
    }
}
