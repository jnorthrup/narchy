package jcog.util;

public interface FloatObjectToObjectFunction<X,Y> {
    Y value(float v, X x);
}
