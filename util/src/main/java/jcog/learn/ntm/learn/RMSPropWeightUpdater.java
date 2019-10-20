



package jcog.learn.ntm.learn;

import jcog.learn.ntm.control.UVector;
import jcog.learn.ntm.control.Unit;


public class RMSPropWeightUpdater implements WeightUpdaterBase {
    private final double __GradientMomentum;

    public double getGradientMomentum() {
        return __GradientMomentum;
    }

    private final double __DeltaMomentum;
    public double getDeltaMomentum() {
        return __DeltaMomentum;
    }

    private final double __ChangeMultiplier;
    public double getChangeMultiplier() {
        return __ChangeMultiplier;
    }

    private final double __ChangeAddConstant;
    public double getChangeAddConstant() {
        return __ChangeAddConstant;
    }

    private final double[] n;
    private final double[] g;
    private final double[] delta;

    /** time index */
    private int t;

    public RMSPropWeightUpdater(int weightsCount, double gradientMomentum, double deltaMomentum, double changeMultiplier, double changeAddConstant) {
        n = new double[weightsCount];
        g = new double[weightsCount];
        delta = new double[weightsCount];
        t = 0;

        __GradientMomentum = gradientMomentum;
        __DeltaMomentum = deltaMomentum;
        __ChangeMultiplier = changeMultiplier;
        __ChangeAddConstant = changeAddConstant;


    }

    @Override
    public void reset() {
        t = 0;
    }

    @Deprecated @Override
    public void updateWeight(Unit unit) {

        double gm = getGradientMomentum();
        double ugrad = unit.grad;
        double ugradGM = (1.0 - gm) * ugrad;

        double nt = n[t] = (gm * n[t]) + (ugradGM * ugrad);
        double gt = g[t] = (gm * g[t]) + ugradGM;

        
        unit.value +=
                
                ( delta[t] = (getDeltaMomentum() * delta[t]) - (getChangeMultiplier() * (ugrad / Math.sqrt(nt - (gt*gt) + getChangeAddConstant()))) );

        t++;
    }

    @Override
    public void updateWeight(UVector unit) {

        double[] ugrads = unit.grad;
        double[] uvalue = unit.value;

        double changeConst = getChangeAddConstant();
        double changeMult = getChangeMultiplier();
        double deltaMomentum = getDeltaMomentum();
        double gm = getGradientMomentum();

        for (int i = 0; i < uvalue.length; i++) {


            double ugrad = ugrads[i];
            double ugradGM = (1.0 - gm) * ugrad;

            double nt = n[t] = (gm * n[t]) + (ugradGM * ugrad);
            double gt = g[t] = (gm * g[t]) + ugradGM;

            
            uvalue[i] +=
                    
                    (delta[t] = (deltaMomentum * delta[t]) - (changeMult * (ugrad / Math.sqrt(nt - (gt * gt) + changeConst))));

            t++;
        }

    }
}


