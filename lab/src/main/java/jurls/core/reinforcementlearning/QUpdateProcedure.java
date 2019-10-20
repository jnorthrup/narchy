/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jurls.core.reinforcementlearning;

import jurls.core.approximation.ApproxParameters;
import jurls.core.approximation.ParameterizedFunction;
import jurls.core.utils.Utils;

/**
 *
 * @author thorsten
 */
public class QUpdateProcedure implements UpdateProcedure {

    private double[] deltas = null;
    private double[] gradient = null;
    private double[] stateAction = null;

    @Override
    public void update(
            ApproxParameters approxParameters,
            RLParameters rLParameters,
            Context context,
            double reward,
            double[][] s,
            int[] a,
            ParameterizedFunction f,
            int num
    ) {
        if ((deltas == null) || (deltas.length != context.e.length)) {
            deltas = new double[context.e.length];
        }
        if ((gradient == null) || (gradient.length != context.e.length)) {
            gradient = new double[context.e.length];
        }
        if (stateAction == null || (stateAction.length != s[0].length + 1)) {
            stateAction = new double[s[0].length + 1];
        }

        double qtm1 = Utils.q(f, stateAction, s[0], a[0]);
        double vtm1 = Utils.v(f, stateAction, s[0], num).getV();
        double vt = Utils.v(f, stateAction, s[1], num).getV();
        Utils.join(stateAction, s[0], a[0]);
        f.parameterGradient(gradient, stateAction);

        double gamma = rLParameters.getGamma();

        for (int i = 0; i < deltas.length; ++i) {
            deltas[i] = gradient[i] * (reward + gamma * vt - qtm1)
                    + context.e[i] * (reward + gamma * vt - vtm1);
        }

        double l = Utils.length(deltas);
        if (l < 1.0) {
            l = 1.0;
        }

        double alpha = approxParameters.getAlpha();
        double momentum = approxParameters.getMomentum();
        for (int i = 0; i < deltas.length; ++i) {
            deltas[i] = alpha * deltas[i] / l
                    + momentum * context.previousDeltas[i];
        }

        f.addToParameters(deltas);

        double lambda = rLParameters.getLambda();
        for (int i = 0; i < context.e.length; ++i) {
            context.e[i] = gradient[i] - context.e[i] * gamma * lambda;
        }

        System.arraycopy(deltas, 0, context.previousDeltas, 0, deltas.length);
    }
}
