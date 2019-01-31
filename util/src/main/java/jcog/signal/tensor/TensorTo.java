package jcog.signal.tensor;

public interface TensorTo {
    void setAt(float newValue, int linearCell);

    void set(float newValue, int[] cell);
}
