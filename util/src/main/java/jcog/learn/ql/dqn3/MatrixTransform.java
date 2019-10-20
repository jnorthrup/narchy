package jcog.learn.ql.dqn3;


import jcog.Util;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

class MatrixTransform {
    private final boolean needsBackprop;
    final Deque<Backprop> q;

    MatrixTransform(boolean needsBackprop) {
        this.needsBackprop = needsBackprop;
        this.q = new ArrayDeque<>();
    }

    void backward() {
        Backprop next;
        while (((next = this.q.pollLast()))!=null)
            next.run();
    }

    Mat tanh(Mat mat) {
        Mat out = new Mat(mat.n, mat.d);
        Arrays.setAll(out.w, i -> Math.tanh(mat.w[i]));
        if (this.needsBackprop)
            this.q.addLast(new Backprop(Backprop.BackpropMethod.TANH, mat, out));
        return out;
    }

    Mat mul(Mat m1, Mat m2) {
        int m1d = m1.d;
        assert m1d == m2.n;
        int m2d = m2.d;
        int n = m1.n;

        //assert(m1d == m2.n): "matmul dimensions misaligned: " + m1d + " != " + m2.n;

        int d = m2d;
        double[] m1w = m1.w;
        double[] m2w = m2.w;
        Mat out = new Mat(n, d);
        double[] outw = out.w;

            for (int i = 0; i < n; i++) { // loop over rows of m1
                int m1i = m1d * i;
                for (int j = 0; j < m2d; j++) { // loop over cols of m2
                    double dot = 0.0;
                    for (int k = 0; k < m1d; k++) // dot product loop
                        dot += m1w[m1i + k] * m2w[m2d * k + j];

                    outw[d * i + j] = dot;
                }
            }

        if (this.needsBackprop)
            this.q.addLast(new Backprop(Backprop.BackpropMethod.MUL, m1, m2, out));

        return out;
    }

    Mat add(Mat mat1, Mat mat2) {
        double[] m1W = mat1.w;
        double[] m2W = mat2.w;
        assert m1W.length == m2W.length;

        Mat out = new Mat(mat1.n, mat1.d);
        int bound = m1W.length;
        double[] outw = out.w;

        for (int i = 0; i < bound; i++)
            outw[i] = m1W[i] + m2W[i];

        if (this.needsBackprop)
            this.q.addLast(new Backprop(Backprop.BackpropMethod.ADD, mat1, mat2, out));

        return out;
    }


    private static class Backprop {
        private final BackpropMethod backpropMethod;
        private final Mat[] args;

        private Backprop(BackpropMethod backpropMethod, Mat... args) {
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

        private static void mulBack(Mat m1, Mat m2, Mat out) {

            int n = m1.n;
            int m1d = m1.d;
            int m2d = m2.d;
            double[] m1w = m1.w;
            double[] m2w = m2.w;
            double[] m1dw = m1.dw;
            double[] m2dw = m2.dw;
            double[] outdw = out.dw;

            for (int i = 0; i < n; i++) {
                for (int j = 0; j < m2d; j++) {
                    double b = outdw[m2d * i + j];
                    if (b!= (double) 0) {
                        for (int k = 0; k < m1d; k++) {
                            int mm1 = m1d * i + k;
                            int mm2 = m2d * k + j;
                            m1dw[mm1] += m2w[mm2] * b;
                            m2dw[mm2] += m1w[mm1] * b;
                        }
                    }
                }
            }
        }

        private static void addBack(Mat mat1, Mat mat2, Mat out) {
            int bound = mat1.w.length;
            double[] m1DW = mat1.dw,  m2DW = mat2.dw, outDW = out.dw;
            for (int i = 0; i < bound; i++) {
                double dwi = outDW[i];
                if (dwi != (double) 0) {
                    m1DW[i] += dwi;
                    m2DW[i] += dwi;
                }
            }
        }

        private static void tanhBack(Mat mat, Mat out) {
            int bound = mat.w.length;
            double[] matdw = mat.dw, outW = out.w, outDW = out.dw;
            for (int i = 0; i < bound; i++)
                matdw[i] += (1.0 - Util.sqr(outW[i])) * outDW[i];
        }

        public enum BackpropMethod {
            ADD, MUL, TANH
        }
    }
}
//    sigmoid: function (m) {
//            // sigmoid nonlinearity
//            var out = new Mat(m.n, m.d);
//            var n = m.w.length;
//            for (var i = 0; i < n; i++) {
//        out.w[i] = sig(m.w[i]);
//        }
//
//        if (this.needs_backprop) {
//
//        this.backprop.push(this.sigmoidBack, [m, out]);
//        }
//        return out;
//        },
//        sigmoidBack: function(m, out) {
//        var n = m.w.length;
//        for (var i = 0; i < n; i++) {
//        // grad for z = tanh(x) is (1 - z^2)
//        var mwi = out.w[i];
//        m.dw[i] += mwi * (1.0 - mwi) * out.dw[i];
//        }
//        },