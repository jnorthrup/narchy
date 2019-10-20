package jcog.learn.ntm.control;

import java.util.Arrays;

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
        Arrays.fill(value, (double) 0);
        Arrays.fill(grad, (double) 0);
    }

    public int size() {
        return value.length;
    }

    public void value(int i, double newValue) {
        this.value[i] = newValue;
    }
    public double value(int i) {
        return this.value[i];
    }


    public void grad(int i, double newValue) {
        this.grad[i] = newValue;
    }
    public double grad(int i) {
        return this.grad[i];
    }

    public double sumGradientValueProducts() {
        double[] value = this.value;
        int bound = value.length;
        double s = 0.0;
        for (int i = 0; i < bound; i++) {
            double v = value[i] * grad[i];
            s += v;
        }
        return s;
    }

    public void valueMultiplySelf(double factor) {
        double[] value = this.value;
        for (int i = 0; i < value.length; i++) {
            value[i] *= factor;
        }
    }

    public double gradAddSelf(int i, double dg) {
        return grad[i] += dg;
    }

    public void clearGrad() {
        Arrays.fill(grad, (double) 0);
    }

    public void setDelta(double[] target) {
        double[] v = this.value;
        double[] g = this.grad;

        for (int j = 0; j < v.length; j++) {
            g[j] = v[j] - target[j];
        }
    }

    public double sumDot(double[] input) {

        double[] v = this.value;

        int bound = size();
        double s = 0.0;
        for (int j = 0; j < bound; j++) {
            double v1 = v[j] * input[j];
            s += v1;
        }
        return s;
    }
}
