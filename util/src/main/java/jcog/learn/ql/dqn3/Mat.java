package jcog.learn.ql.dqn3;

import java.util.Arrays;
import java.util.stream.IntStream;

class Mat {
    final double[] w;
    final double[] dw;
    final int d;
    final int n;

    Mat(final int n, final int d) {
        this.n = n;
        this.d = d;
        this.w = new double[n * d];
        this.dw = new double[n * d];
    }

    Mat(final int n, final int d, final double[] arr) {
        this.n = n;
        this.d = d;
        this.w = arr;
        assert(w.length == n*d);
        this.dw = new double[this.n * this.d];
    }

    void update(final double val) {
        IntStream.range(0, this.w.length).forEach(i -> this.w[i] -= val * this.dw[i]);
        Arrays.fill(this.dw, 0);
    }
}
