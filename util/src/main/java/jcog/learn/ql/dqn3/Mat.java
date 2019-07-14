package jcog.learn.ql.dqn3;

import java.util.Arrays;
import java.util.stream.IntStream;

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
        IntStream.range(0, this.w.length).forEach(i -> this.w[i] -= val * this.dw[i]);
        Arrays.fill(this.dw, 0);
        //Util.mul(0.9f, dw);
    }
}
