package jcog.learn.ql.dqn3;


import jcog.Util;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

class Graph {
    private final boolean needsBackprop;
    final Deque<Backprop> q;

    Graph(final boolean needsBackprop) {
        this.needsBackprop = needsBackprop;
        this.q = new ArrayDeque<>();
    }

    void backward() {
        Backprop next;
        while (((next = this.q.pollFirst()))!=null)
            next.run();
    }

    Mat tanh(final Mat mat) {
        final Mat out = new Mat(mat.n, mat.d);
        Arrays.setAll(out.w, i -> Math.tanh(mat.w[i]));
        if (this.needsBackprop)
            this.q.push(new Backprop(Backprop.BackpropMethod.TANH, mat, out));
        return out;
    }

    Mat mul(final Mat m1, final Mat m2) {
        final int m1d = m1.d;
        assert m1d == m2.n;
        final int m2d = m2.d;
        final int n = m1.n;

        assert(m1d == m2.n): "matmul dimensions misaligned: " + m1d + " != " + m2.n;

        final int d = m2d;
        final double[] m1w = m1.w;
        final double[] m2w = m2.w;
        var out = new Mat(n, d);
        final double[] outw = out.w;

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


//        for (int i = 0; i < n; i++) {
//            final int m1di = m1d * i;
//            final int m2di = m2d * i;
//            for (int idx = 0; idx < m2d; idx++) {
//                int j = idx;
//                double sum = 0.0;
//                for (int k = 0; k < m1d; k++) {
//                    sum += m1.w[m1di + k] * m2.w[m2d * k + j];
//                }
//                out.w[m2di + j] = sum;
//            }
//        }

        if (this.needsBackprop)
            this.q.push(new Backprop(Backprop.BackpropMethod.MUL, m1, m2, out));

        return out;
    }

    Mat add(final Mat mat1, final Mat mat2) {
        assert mat1.w.length == mat2.w.length;

        final Mat out = new Mat(mat1.n, mat1.d);
        int bound = mat1.w.length;
        for (int i = 0; i < bound; i++) {
            out.w[i] = mat1.w[i] + mat2.w[i];
        }
        if (this.needsBackprop)
            this.q.push(new Backprop(Backprop.BackpropMethod.ADD, mat1, mat2, out));

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

        private void mulBack(final Mat m1, final Mat m2, final Mat out) {


            final int n = m1.n;
            final int m2d = m2.d;
            final int d = m2d;
            final int m1d = m1.d;
            double[] m1w = m1.w;
            double[] m2w = m2.w;
            double[] m1dw = m1.dw;
            double[] m2dw = m2.dw;
            double[] outdw = out.dw;

            for (int i = 0; i < n; i++) {
                final int m2di = m2d * i;
                final int m1di = m1d * i;
                for (int j = 0; j < m2d; j++) {
                    final double b = outdw[m2d * i + j];
                    if (b!=0) {
                        for (int k = 0; k < m1d; k++) {
                            final int mm1 = m1d * i + k;
                            final int mm2 = m2d * k + j;
                            m1dw[mm1] += m2w[mm2] * b;
                            m2dw[mm2] += m1w[mm1] * b;
                        }
                    }
                }
            }
        }

        static private void addBack(final Mat mat1, final Mat mat2, final Mat out) {
            int bound = mat1.w.length;
            for (int i = 0; i < bound; i++) {
                double dwi = out.dw[i];
                if (dwi != 0) {
                    mat1.dw[i] += dwi;
                    mat2.dw[i] += dwi;
                }
            }
        }

        static private  void tanhBack(final Mat mat, final Mat out) {
            double[] matdw = mat.dw;
            int bound = mat.w.length;
            for (int i = 0; i < bound; i++) {
                matdw[i] += (1 - Util.sqr(out.w[i])) * out.dw[i];
            }
        }

        public enum BackpropMethod {
            ADD, MUL, TANH
        }
    }
}
