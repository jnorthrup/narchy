/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jurls.core.reinforcementlearning;

import jcog.math.MutableDouble;

/**
 *
 * @author thorsten
 */
public class RLParameters {

    /** learning rate */
    public final MutableDouble alpha;

    /** farsight */
    public final MutableDouble gamma;

    /** lambda
     * "A value of λ=1.0 effectively makes algorithm run an online Monte Carlo in which the effects of all future interactions are fully considered in updating each Q-value of an episode."
     */
    public final MutableDouble lambda;

    /** randomness */
    public final MutableDouble epsilon;

    public RLParameters(double alpha, double gamma, double lambda, double epsilon) {
        this.alpha = new MutableDouble(alpha);
        this.gamma = new MutableDouble(gamma);
        this.lambda = new MutableDouble(lambda);
        this.epsilon = new MutableDouble(epsilon);
    }

    public double getAlpha() {
        return alpha.doubleValue();
    }

    public void setAlpha(double alpha) {
        this.alpha.set(alpha);
    }

    public double getGamma() {
        return gamma.doubleValue();
    }

    public void setGamma(double gamma) {
        this.gamma.set(gamma);
    }

    public double getLambda() {
        return lambda.doubleValue();
    }

    public void setLambda(double lambda) {
        this.lambda.set(lambda);
    }

    public double getEpsilon() {
        return epsilon.doubleValue();
    }

    public void setEpsilon(double epsilon) {
        this.epsilon.set(epsilon);
    }
}
