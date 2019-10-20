/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jcog.signal.meter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Convenience implementation for a 1-signal meter
 */
public abstract class FunctionMeter<M> implements Meters<M>, Serializable {

    private final List<ScalarColumn> signals;
    private M[] vector;

    public static String[] newDefaultSignalIDs(String prefix, int n) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String s1 = prefix + '_' + i;
            list.add(s1);
        }
        String[] s = list.toArray(new String[0]);
        return s;
    }
    public static String[] newDefaultSignalIDs(String prefix, String... prefixes) {
        List<String> list = new ArrayList<>();
        for (String item : prefixes) {
            String s1 = prefix + '_' + item;
            list.add(s1);
        }
        String[] s = list.toArray(new String[0]);
        return s;
    }
    
    public FunctionMeter(String prefix, int n) {
        this(newDefaultSignalIDs(prefix, n));
    }
    public FunctionMeter(String prefix, boolean noop, String... prefixes) {
        this(newDefaultSignalIDs(prefix, prefixes));
    }
    
    public FunctionMeter(String... ids) {

        List<ScalarColumn> list = new ArrayList<>();
        for (String n : ids) {
            ScalarColumn scalarColumn = new ScalarColumn(n, null);
            list.add(scalarColumn);
        }
        signals = Collections.unmodifiableList(list);
    }
    
    public void setUnits(String... units) {
        int i = 0;
        for (ScalarColumn s : signals)
            s.unit = units[i++];
    }

    @Override
    public List<ScalarColumn> getSignals() {
        return signals;
    }

    public abstract M getValue(Object key, int index);

    protected void fillVector(Object key, int fromIndex, int toIndex) {
        M[] v = this.vector;
        int len = v.length;

        for (int i = 0; i < len; i++) {
            v[i] = getValue(key, i);
        }

    }

    @Override
    public M[] sample(Object key) {
        M[] vector = this.vector;

        if (vector == null) {
            
            
            
            vector = this.vector = (M[]) new Object[signals.size()];

        }

        fillVector(key, 0, vector.length);
        

        return vector;
    }

}
