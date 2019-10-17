/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jcog.signal.meter.event;

import jcog.signal.meter.FunctionMeter;

/**
 * Function meter with one specific ID
 */
public abstract class SourceFunctionMeter<T> extends FunctionMeter<T> {
    
    private final String name;

    public SourceFunctionMeter(String id) {
        super(id);
        name = id;
    }
    public SourceFunctionMeter(String id, String... components) {
        super(id, false, components);
        name = id;
    }

    public String id() { return name; }
    

    
}
