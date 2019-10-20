package jcog.learn.deep;

import jcog.Util;

import java.util.Random;

public class utils {
    public static double uniform(double min, double max, Random rng) {
        return rng.nextDouble() * (max - min) + min;
    }

    public static double binomial(int n, double p, Random rng) {
        if(p < 0 || p > 1) return 0;

        var c = 0;

        for(var i = 0; i<n; i++) {
            var r = rng.nextDouble();
            if (r < p) c++;
        }

        return c;
    }

    public static double sigmoid(double x) {
        return Util.sigmoid(x);
        
    }

    public static double dsigmoid(double x) {
        return x * (1. - x);
    }

    public static double tanh(double x) {
        return Math.tanh(x);
    }

    public static double dtanh(double x) {
        return 1. - x * x;
    }

    public static double ReLU(double x) {
        if(x > 0) {
            return x;
        } else {
            return 0.;
        }
    }

    public static double dReLU(double x) {
        if(x > 0) {
            return 1.;
        } else {
            return 0.;
        }
    }
}
