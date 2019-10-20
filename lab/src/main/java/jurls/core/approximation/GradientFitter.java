/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jurls.core.approximation;

import jurls.core.utils.Utils;

/**
 *
 * @author thorsten
 */
public class GradientFitter implements ParameterizedFunction {

    private final ParameterizedFunction parameterizedFunction;
    private final double[] previousDeltas;
    private final ApproxParameters approxParameters;
    private final double[] gradient;

    public GradientFitter(ApproxParameters approxParameters,
            ParameterizedFunction parameterizedFunction) {
        this.approxParameters = approxParameters;
        this.parameterizedFunction = parameterizedFunction;
        previousDeltas = new double[parameterizedFunction.numberOfParameters()];
        gradient = new double[parameterizedFunction.numberOfParameters()];
    }

    @Override
    public void learn(double[] xs, double y) {
        var q = parameterizedFunction.value(xs);

        parameterGradient(gradient, xs);
        var e = y - q;
        Utils.multiplySelf(gradient,e);

        var l = Utils.length(gradient);
        if (l < 1) {
            l = 1;
        }

        var a = approxParameters.getAlpha();
        var m = approxParameters.getMomentum();
        for (var i = 0; i < gradient.length; ++i) {
            previousDeltas[i] = gradient[i] = a * gradient[i] / l + m * previousDeltas[i];
        }

        parameterizedFunction.addToParameters(gradient);
    }

    @Override
    public double value(double[] xs) {
        return parameterizedFunction.value(xs);
    }

    @Override
    public int numberOfParameters() {
        return parameterizedFunction.numberOfParameters();
    }

    @Override
    public int numberOfInputs() {
        return parameterizedFunction.numberOfInputs();
    }

    @Override
    public void parameterGradient(double[] output, double[] xs) {
        parameterizedFunction.parameterGradient(output, xs);
    }

    @Override
    public void addToParameters(double[] deltas) {
        parameterizedFunction.addToParameters(deltas);
    }

    @Override
    public double minOutputDebug() {
        return parameterizedFunction.minOutputDebug();
    }

    @Override
    public double maxOutputDebug() {
        return parameterizedFunction.maxOutputDebug();
    }

    public double getParameter(int i) {
        return parameterizedFunction.getParameter(i);
    }
}

