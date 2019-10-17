package jcog.learn.ntm.control;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Created by me on 7/18/15.
 */
public class UVector {
    public final double[] value;
    public final double[] grad;


    public UVector(int size) {
        value = new double[size];
        grad = new double[size];
    }

    public void clear() {
        Arrays.fill(value, 0);
        Arrays.fill(grad, 0);
    }

    public int size() {
        return value.length;
    }

    public void value(final int i, final double newValue) {
        this.value[i] = newValue;
    }
    public double value(final int i) {
        return this.value[i];
    }


    public void grad(final int i, final double newValue) {
        this.grad[i] = newValue;
    }
    public double grad(final int i) {
        return this.grad[i];
    }

    public double sumGradientValueProducts() {
        double s;
        final double[] value = this.value;
        s = IntStream.range(0, value.length).mapToDouble(i -> value[i] * grad[i]).sum();
        return s;
    }

    public void valueMultiplySelf(final double factor) {
        final double[] value = this.value;
        for (int i = 0; i < value.length; i++) {
            value[i] *= factor;
        }
    }

    public double gradAddSelf(int i, double dg) {
        return grad[i] += dg;
    }

    public void clearGrad() {
        Arrays.fill(grad, 0);
    }

    public void setDelta(final double[] target) {
        final double[] v = this.value;
        final double[] g = this.grad;

        for (int j = 0; j < v.length; j++) {
            g[j] = v[j] - target[j];
        }
    }

    public double sumDot(final double[] input) {
        double s;

        final double[] v = this.value;

        s = IntStream.range(0, size()).mapToDouble(j -> v[j] * input[j]).sum();
        return s;
    }
}
