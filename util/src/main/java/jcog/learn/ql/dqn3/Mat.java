package jcog.learn.ql.dqn3;

import java.util.Arrays;

public class Mat {
    public final double[] w;
    public final double[] dw;
    public final int d;
    public final int n;

    Mat(final int n, final int d) {
        this.n = n;
        this.d = d;
        this.w = new double[n * d];
        this.dw = new double[n * d];
    }

    Mat(final int n, final int d, final double[] arr) {
        this.n = n;
        this.d = d;
        this.w = arr; assert(arr.length == n*d);
        this.dw = new double[n * d];
    }

    void update(final double val) {
        double[] w = this.w, dw = this.dw;
        for (int i = 0; i < w.length; i++)
            w[i] -= val * dw[i];
        Arrays.fill(dw, 0);
        //Util.mul(0.9f, dw);
    }
}
