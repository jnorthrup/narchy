/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jcog.signal.meter;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Convenience implementation for a 1-signal meter
 */
public abstract class FunctionMeter<M> implements Meters<M>, Serializable {

    private final List<ScalarColumn> signals;
    private M[] vector;

    public static String[] newDefaultSignalIDs(String prefix, int n) {
        var s = IntStream.range(0, n).mapToObj(i -> prefix + '_' + i).toArray(String[]::new);
        return s;
    }
    public static String[] newDefaultSignalIDs(String prefix, String... prefixes) {
        var s = Arrays.stream(prefixes).map(item -> prefix + '_' + item).toArray(String[]::new);
        return s;
    }
    
    public FunctionMeter(String prefix, int n) {
        this(newDefaultSignalIDs(prefix, n));
    }
    public FunctionMeter(String prefix, boolean noop, String... prefixes) {
        this(newDefaultSignalIDs(prefix, prefixes));
    }
    
    public FunctionMeter(String... ids) {

        signals = Arrays.stream(ids).map(n -> new ScalarColumn(n, null)).collect(Collectors.toUnmodifiableList());
    }
    
    public void setUnits(String... units) {
        var i = 0;
        for (var s : signals)
            s.unit = units[i++];
    }

    @Override
    public List<ScalarColumn> getSignals() {
        return signals;
    }

    public abstract M getValue(Object key, int index);

    protected void fillVector(Object key, int fromIndex, int toIndex) {
        var v = this.vector;
        var len = v.length;

        for (var i = 0; i < len; i++) {
            v[i] = getValue(key, i);
        }

    }

    @Override
    public M[] sample(Object key) {
        var vector = this.vector;

        if (vector == null) {
            
            
            
            vector = this.vector = (M[]) new Object[signals.size()];

        }

        fillVector(key, 0, vector.length);
        

        return vector;
    }

}
