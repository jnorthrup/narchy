/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jcog.meter.func;

import jcog.meter.FunctionMeter;
import jcog.meter.Metrics;
import jcog.meter.Signal;

import java.util.Iterator;
import java.util.List;

/**
 * @param Source Return type
 */
public abstract class DependsOnColumn<Source,Result> extends FunctionMeter<Result> {

    protected final int sourceColumn;
    protected final Metrics metrics;
    

    

    public DependsOnColumn(Metrics metrics, String source, int numResults) {
        super("", numResults);
        
        int i = 0;
        Signal m = metrics.getSignal(source);
        if (m == null)
            throw new RuntimeException("Missing signal: " + source);

        this.metrics = metrics;
        sourceColumn = metrics.indexOf(source);

        for (Signal s : getSignals()) {            
            
        }
        
    }


    /*public Iterator<Object> signalIterator() {
        return metrics.getSignalIterator(sourceColumn, true);        
    }*/
    
    public Source newestValue() { 
        Iterator<Object[]> r = metrics.reverseIterator();
        if (r.hasNext())
            return (Source) r.next()[sourceColumn];
        return null;
    }
    

    protected List<Object> newestValues(int column, int i) {
        return metrics.getNewSignalValues(column, i);
    }
    
    protected abstract String getColumnID(Signal dependent, int i);
    
}
