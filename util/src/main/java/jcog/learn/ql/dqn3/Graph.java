package jcog.learn.ql.dqn3;


import jcog.Util;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.stream.IntStream;

class Graph {
    private boolean reverse;
    private final Deque<Backprop> q;

    Graph(final boolean reverse) {
        this.reverse = reverse;
        this.q = new ArrayDeque<>();
    }

    void backward() {
        Backprop next;
        while (((next = this.q.pollLast()))!=null)
            next.run();
    }

    Mat tanh(final Mat mat) {
        final Mat out = new Mat(mat.n, mat.d);
        Arrays.setAll(out.w, i -> Math.tanh(mat.w[i]));
        if (this.reverse)
            this.q.add(new Backprop(Backprop.BackpropMethod.TANH, mat, out));
        return out;
    }

    Mat mul(final Mat m1, final Mat m2) {
        final int m1d = m1.d;
        assert m1d == m2.n;
        final int m2d = m2.d;
        final int n = m1.n;


        //mul: function (m1, m2) {
            // multiply matrices m1 * m2


//            assert(m1d === m2.n, 'matmul dimensions misaligned: ' + m1d + ' != ' + m2.n);
            int d = m2d;
            double[] m1w = m1.w;
            double[] m2w = m2.w;
            Mat out = new Mat(n, d);
            double[] outw = out.w;
            for (int  i = 0; i < n; i++) { // loop over rows of m1
                int m1i = m1d * i;
                for (int j = 0; j < m2d; j++) { // loop over cols of m2
                    double dot = 0.0;
                    for (int  k = 0; k < m1d; k++) { // dot product loop
                        dot += m1w[m1i + k] * m2w[m2d * k + j];
                    }
                    outw[d * i + j] = dot;
                }
            }

//        IntStream.range(0, n).forEach(i -> {
//            final int m1i = m1d * i;
//            final int m2di = m2d * i;
//            IntStream.range(0, m2d)
//                    .forEach(j -> out.w[m2di + j] = IntStream.range(0, m1d)
//                            .mapToDouble(k -> m1.w[m1i + k] * m2.w[m2d * k + j])
//                            .sum());
//        });
        if (this.reverse) {
            this.q.add(new Backprop(Backprop.BackpropMethod.MUL, m1, m2, out));
        }
        return out;
    }

    Mat add(final Mat mat1, final Mat mat2) {
        assert mat1.w.length == mat2.w.length;

        final Mat out = new Mat(mat1.n, mat1.d);
        IntStream.range(0, mat1.w.length).forEach(i -> out.w[i] = mat1.w[i] + mat2.w[i]);
        if (this.reverse)
            this.q.add(new Backprop(Backprop.BackpropMethod.ADD, mat1, mat2, out));

        return out;
    }


    static private class Backprop {
        private final BackpropMethod backpropMethod;
        private final Mat[] args;

        private Backprop(final BackpropMethod backpropMethod, final Mat... args) {
            this.backpropMethod = backpropMethod;
            this.args = args;
        }


        void run() {
            switch (this.backpropMethod) {
                case ADD:
                    addBack(this.args[0], this.args[1], this.args[2]);
                    break;
                case MUL:
                    mulBack(this.args[0], this.args[1], this.args[2]);
                    break;
                case TANH:
                    tanhBack(this.args[0], this.args[1]);
                    break;
            }
        }

        private void mulBack(final Mat mat1, final Mat mat2, final Mat out) {
            final int n = mat1.n;
            final int m2d = mat2.d;
            final int m1d = mat1.d;
            for (int i = 0; i < n; i++) {
                final int m2di = m2d * i;
                final int m1di = m1d * i;
                for (int j = 0; j < m2d; j++) {
                    final double b = out.dw[m2di + j];
                    if (b!=0) {
                        for (int k = 0; k < m1d; k++) {
                            final int mm1 = m1di + k;
                            final int mm2 = m2d * k + j;
                            mat1.dw[mm1] += mat2.w[mm2] * b;
                            mat2.dw[mm2] += mat1.w[mm1] * b;
                        }
                    }
                }
            }
        }

        static private void addBack(final Mat mat1, final Mat mat2, final Mat out) {
            IntStream.range(0, mat1.w.length).forEach(i -> {
                double dwi = out.dw[i];
                if (dwi!=0) {
                    mat1.dw[i] += dwi;
                    mat2.dw[i] += dwi;
                }
            });
        }

        static private  void tanhBack(final Mat mat, final Mat out) {
            IntStream.range(0, mat.w.length).forEach(i ->
                    mat.dw[i] += (1 - Util.sqr(out.w[i])) * out.dw[i]
            );
        }

        public enum BackpropMethod {
            ADD, MUL, TANH
        }
    }
}
