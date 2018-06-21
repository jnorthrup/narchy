package org.intelligentjava.machinelearning.decisiontree;

import java.util.function.Function;



class TestValue implements Function<Object,Object> {
    
    private final Object label;
    
    public TestValue(Object label) {
        super();
        this.label = label;
    }

    @Override
    public Object apply(Object what) {
        return label;
    }

}
