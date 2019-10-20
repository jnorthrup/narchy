/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jurls.core.utils;

import jurls.core.approximation.ParameterizedFunction;

/**
 *
 * @author thorsten
 */
public class Utils {

    public static void join(double[] output, double[] state, int action) {
        System.arraycopy(state, 0, output, 0, state.length);
        output[output.length - 1] = (double) action;
    }

    public static String makeIndent(int n) {
        StringBuilder sb1 = new StringBuilder();
        for (int i = 0; i < n; i++) {
            String s = " ";
            sb1.append(s);
        }
        String result = sb1.toString();
        String sb = result;
        return sb;
    }

    public static double q(ParameterizedFunction f, double[] stateAction, double[] state, int action) {
        join(stateAction, state, action);
        return f.value(stateAction);
    }

    public static ActionValuePair v(ParameterizedFunction f, double[] stateAction, double[] state, int num) {
        double max = Double.NEGATIVE_INFINITY;
        int maxa = 0;

        for (int i = 0; i < num; ++i) {
            double _q = q(f, stateAction, state, i);
            if (_q > max) {
                max = _q;
                maxa = i;
            }
        }

        return new ActionValuePair(maxa, max);
    }

    public static double lengthSquare(double[] v) {
        double s = 0.0;
        for (double x : v) {
            double v1 = x * x;
            s += v1;
        }
        return s;
    }

    public static double length(double[] v) {
        return Math.sqrt(lengthSquare(v));
    }

    public static int checkAction(int numActions, double[] behaviourLearnerOutput) {
        assert behaviourLearnerOutput.length == 1;

        int action = (int) Math.round(behaviourLearnerOutput[0]);
        if (action >= numActions) {
            action = numActions - 1;
        }
        if (action < 0) {
            action = 0;
        }

        return action;
    }

    public static void multiplySelf(double[] dest, double source) {
        for (int i = 0; i < dest.length; ++i) {
            dest[i] *= source;
        }
    }
}
