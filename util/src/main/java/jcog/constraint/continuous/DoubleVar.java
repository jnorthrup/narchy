package jcog.constraint.continuous;

/**
 * Created by alex on 30/01/15.
 */
public class DoubleVar {

    public final String name;

    private double value;

    public DoubleVar(String name) {
        this.name = name;
    }

    public double value() {
        return value;
    }
    public float floatValue() {
        return (float) value;
    }

    public void value(double value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return name + '=' + value;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }
}
