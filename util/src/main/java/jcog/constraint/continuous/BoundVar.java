package jcog.constraint.continuous;

abstract public class BoundVar<X> extends DoubleVar {

    public BoundVar(String name) {
        super(name);

    }

    public void load() {
        super.value(get());
    }
    public void save() {
        set(floatValue());
    }

    abstract protected double get();
    abstract protected void set(double next);
}
