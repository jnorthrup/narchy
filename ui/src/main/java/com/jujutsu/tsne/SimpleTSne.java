package com.jujutsu.tsne;

import com.jujutsu.tsne.barneshut.TSneConfiguration;
import jcog.math.FloatRange;
import jcog.pri.ScalarValue;

import static com.jujutsu.tsne.matrix.MatrixOps.*;

/**
 * Author: Leif Jonsson (leif.jonsson@gmail.com)
 * <p>
 * This is a Java implementation of van der Maaten and Hintons t-sne
 * dimensionality reduction technique that is particularly well suited
 * for the visualization of high-dimensional datasets
 * <p>
 * http:
 * <p>
 * cost function parameters: perplexity Perp,
 * optimization parameters: number of iterations T, learning rate η, momentum α(
 */
public class SimpleTSne implements TSne {

    /**
     * The perplexity can be interpreted as a smooth measure of the effective number of neighbors. The
     * performance of SNE is fairly robust to changes in the perplexity, and typical values are between 5
     * and 50.
     */
    public final FloatRange perplexity = new FloatRange(10, 0.5f, 50f);

    public final FloatRange momentum = new FloatRange(0.5f, 0f, 1f);

    private double[][] P;
    protected double[][] X;
    public double[][] Y;
    private double[][] dY;
    private double[][] iY;
    protected double[][] gains;

    static final int TRIES = 50; //??


    private final double eta =
            //0.1f;
            0.5;
            //1f;

    private final double min_gain =
            //Double.MIN_NORMAL;
            ScalarValue.EPSILON;
            //ScalarValue.EPSILONsqrt;
            //0.01f;

    private double[][] numMatrix;

    private final boolean pca = false;

    @Override
    public double[][] reset(double[][] X, TSneConfiguration config) {
        ///*if(/*use_pca && *//*X[0].length > initial_dims && initial_dims > 0*/) {
        if (pca) {
            PrincipalComponentAnalysis pca = new PrincipalComponentAnalysis();
            X = pca.pca(X, X[0].length);
            System.out.println("X:Shape after PCA is = " + X.length + " x " + X[0].length);
        }

        this.X = X;
        int no_dims = config.getOutputDims();
        double perplexity = this.perplexity.getAsDouble();

//        String IMPLEMENTATION_NAME = this.getClass().getSimpleName();
//        System.out.println("X:Shape is = " + X.length + " x " + X[0].length);
//        System.out.println("Running " + IMPLEMENTATION_NAME + '.');


        int n = X.length;


        Y = rnorm(n, no_dims);
        dY = fillMatrix(n, no_dims, 0.0);
        iY = fillMatrix(n, no_dims, 0.0);
        gains = fillMatrix(n, no_dims, 1.0);


        P = x2p(X, 1e-5, perplexity).P;
        P = plus(P, transpose(P));
        P = scalarDivide(P, sum(P));
        P = scalarMult(P, 4);
        P = maximum(P, Double.MIN_NORMAL);

//        System.out.println("Y:Shape is = " + Y.length + " x " + Y[0].length);


        return Y;
    }


    public final double[][] next(int iter) {
        for (int i = 0; i < iter; i++)
            next();
        return Y;
    }

    @Override public double[][] next() {
        int n = X.length;

        double[][] sum_Y = transpose(sum(square(Y), 1));

        numMatrix = scalarInverse(scalarPlus(addRowVector(transpose(addRowVector(scalarMult(
                times(Y, transpose(Y)),
                -2),
                sum_Y)),
                sum_Y),
                1), numMatrix);

        int[] rn = range(n);
        assignAtIndex(numMatrix, rn, rn, 0);
        double[][] Q = scalarDivide(numMatrix, sum(numMatrix));

        Q = maximum(Q, Float.MIN_NORMAL /*1e-12*/);


        double[][] L = scalarMultiply(minus(P, Q), numMatrix);
        dY = scalarMult(times(minus(diag(sum(L, 1)), L), Y), 4);

        gains = plus(scalarMultiply(scalarPlus(gains, .2), abs(negate(equal(biggerThan(dY, 0.0), biggerThan(iY, 0.0))))),
                scalarMultiply(scalarMult(gains, .8), abs(equal(biggerThan(dY, 0.0), biggerThan(iY, 0.0)))));

        assignAllLessThan(gains, min_gain, min_gain);
        iY = minus(iY, scalarMult(scalarMultiply(gains, dY), eta*n));
        iY = scalarMult(iY,  (1-momentum.getAsDouble()));
        Y = plus(Y, iY);

        //Y = minus(Y, tile(mean(Y, 0), n, 1));


        if (logger.isDebugEnabled()) {
            double error = sum(scalarMultiply(P, replaceNaN(log(scalarDivide(P, Q)),
                    0
            )));
            logger.debug("error={}", error);
        }

        return Y;

    }

    private static R Hbeta(double[][] D, double beta) {
        double[][] P = exp(scalarMult(scalarMult(D, beta), -1));
        double sumP = sum(P);
        double H = Math.log(sumP) + beta * sum(scalarMultiply(D, P)) / sumP;
        P = scalarDivide(P, sumP);
        R r = new R();
        r.H = H;
        r.P = P;
        return r;
    }

    private static R x2p(double[][] X, double tol, double perplexity) {
        int n = X.length;
        double[][] sum_X = sum(square(X), 1);
        double[][] times = scalarMult(times(X, transpose(X)), -2);
        double[][] prodSum = addColumnVector(transpose(times), sum_X);
        double[][] D = addRowVector(prodSum, transpose(sum_X));

        double[][] P = fillMatrix(n, n, 0.0);
        double[] beta = fillMatrix(n, n, 1.0)[0];
        double logU = Math.log(perplexity);
//        System.out.println("Starting x2p...");
        for (int i = 0; i < n; i++) {
            if (i % 500 == 0)
                System.out.println("Computing P-values for point " + i + " of " + n + "...");
            double betamin = Double.NEGATIVE_INFINITY;
            double betamax = Double.POSITIVE_INFINITY;
            double[][] Di = getValuesFromRow(D, i, concatenate(range(0, i), range(i + 1, n)));

            R hbeta = Hbeta(Di, beta[i]);
            double H = hbeta.H;
            double[][] thisP = hbeta.P;


            double Hdiff = H - logU;
            int tries = 0;
            while (Math.abs(Hdiff) > tol && tries < TRIES) {
                if (Hdiff > 0) {
                    betamin = beta[i];
                    if (Double.isInfinite(betamax))
                        beta[i] = beta[i] * 2;
                    else
                        beta[i] = (beta[i] + betamax) / 2;
                } else {
                    betamax = beta[i];
                    if (Double.isInfinite(betamin))
                        beta[i] = beta[i] / 2;
                    else
                        beta[i] = (beta[i] + betamin) / 2;
                }

                hbeta = Hbeta(Di, beta[i]);
                H = hbeta.H;
                thisP = hbeta.P;
                Hdiff = H - logU;
                tries = tries + 1;
            }
            assignValuesToRow(P, i, concatenate(range(0, i), range(i + 1, n)), thisP[0]);
        }

        R r = new R();
        r.P = P;
        r.beta = beta;
        double sigma = mean(sqrt(scalarInverse(beta)));

        //System.out.println("Mean value of sigma: " + sigma);

        return r;
    }

}
