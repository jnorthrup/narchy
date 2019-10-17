package jcog.func;

@FunctionalInterface
public interface ObjectFloatToFloatFunction<X> {
    float value(X x, float v);
}
