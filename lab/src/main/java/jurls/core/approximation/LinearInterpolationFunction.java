/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jurls.core.approximation;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 *
 * @author me
 */
public class LinearInterpolationFunction implements ParameterizedFunction, UnaryOperator< Double> {
    
    protected int numPoints = 64;
    protected TreeMap<Double,Double> evidence = new TreeMap();

    public LinearInterpolationFunction(int numInputs, int numPoints) {
        super();
        this.numPoints = numPoints;
    }

    /** function approximation from collected evidence */
    @Override public Double apply(Double x) {
        
        return valueEvidence(x);
    }
    
    public double valueEvidence(double x) {

        var eLow = evidence.lowerEntry(x);
        var eHigh = evidence.higherEntry(x);
        
        if (eLow == null && eHigh == null)
            return 0;        
        else if ((eLow!=null) && (eHigh!=null)) {
            double lk = eLow.getKey();
            double hk = eHigh.getKey();
            double l = eLow.getValue();
            double h = eHigh.getValue();
            if (l == h) return l;
            if (x == lk) return l;
            if (x == hk) return h;

            var ld = Math.abs(lk - x);
            var lh = Math.abs(hk - x);
            var pl = ld / (ld + lh);
            
            return l * (1.0 - pl) + h * (pl);
        }
        else if (eLow==null) {
            return eHigh.getValue();
        }
        else if (eHigh==null) {
            return eLow.getValue();
        }
        return 0d;
        
    }

    @Override public double value(double[] xs) {
        return valueEvidence(xs[0]);
    }

    @Override
    public void parameterGradient(double[] output, double... xs) {

    }

    @Override
    public void addToParameters(double[] deltas) {

    }

    @Override
    public void learn(double[] X, double y) {
        if (X.length > 1) 
            throw new RuntimeException("Only one input variable supported currently");
        var x = X[0];
       
        while (evidence.size() > numPoints) {
            
            double low = evidence.firstKey();
            double high = evidence.lastKey();
            var r = Math.random() * ( high - low ) + low;
            var removed =  evidence.lowerKey(r);
            if (removed == null)
                removed = evidence.higherKey(r);
            if (removed!=null)
                evidence.remove( removed );
        }
            
        evidence.put(x, y);
        
        
            
    }
    
    @Override
    public int numberOfParameters() {
        
        return numPoints;
    }

    @Override
    public int numberOfInputs() {
        return 0;
    }

    @Override
    public double minOutputDebug() {
        return 0;
    }

    @Override
    public double maxOutputDebug() {
        return 0;
    }











}


