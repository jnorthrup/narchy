/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jurls.core.utils;

import jurls.core.approximation.ParameterizedFunction;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *
 * @author thorsten
 */
public class Utils {

    public static void join(double[] output, double[] state, int action) {
        System.arraycopy(state, 0, output, 0, state.length);
        output[output.length - 1] = action;
    }

    public static String makeIndent(int n) {
        var result = IntStream.range(0, n).mapToObj(i -> " ").collect(Collectors.joining());
        var sb = result;
        return sb;
    }

    public static double q(ParameterizedFunction f, double[] stateAction, double[] state, int action) {
        join(stateAction, state, action);
        return f.value(stateAction);
    }

    public static ActionValuePair v(ParameterizedFunction f, double[] stateAction, double[] state, int num) {
        var max = Double.NEGATIVE_INFINITY;
        var maxa = 0;

        for (var i = 0; i < num; ++i) {
            var _q = q(f, stateAction, state, i);
            if (_q > max) {
                max = _q;
                maxa = i;
            }
        }

        return new ActionValuePair(maxa, max);
    }

    public static double lengthSquare(double[] v) {
        var s = Arrays.stream(v).map(x -> x * x).sum();
        return s;
    }

    public static double length(double[] v) {
        return Math.sqrt(lengthSquare(v));
    }

    public static int checkAction(int numActions, double[] behaviourLearnerOutput) {
        assert behaviourLearnerOutput.length == 1;

        var action = (int) Math.round(behaviourLearnerOutput[0]);
        if (action >= numActions) {
            action = numActions - 1;
        }
        if (action < 0) {
            action = 0;
        }

        return action;
    }

    public static void multiplySelf(double[] dest, double source) {
        for (var i = 0; i < dest.length; ++i) {
            dest[i] *= source;
        }
    }
}
