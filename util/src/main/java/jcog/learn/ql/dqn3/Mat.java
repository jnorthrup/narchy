package jcog.learn.ql.dqn3;

import java.util.Arrays;

public class Mat {
    public final double[] w;
    public final double[] dw;
    public final int d;
    public final int n;

    Mat(int n, int d) {
        this.n = n;
        this.d = d;
        this.w = new double[n * d];
        this.dw = new double[n * d];
    }

    Mat(int n, int d, double[] arr) {
        this.n = n;
        this.d = d;
        this.w = arr; assert(arr.length == n*d);
        this.dw = new double[n * d];
    }

    void update(double alpha) {
        double[] w = this.w, dw = this.dw;
        for (int i = 0; i < w.length; i++)
            w[i] += -alpha * dw[i];
        Arrays.fill(dw, (double) 0);
        //Util.mul(0.9f, dw);
    }

}
