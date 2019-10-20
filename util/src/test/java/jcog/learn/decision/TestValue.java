package jcog.learn.decision;

import java.util.function.UnaryOperator;


class TestValue implements UnaryOperator<Object> {
    
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
