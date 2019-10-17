package jcog.data;

/** extends Number with mutable methods
 * TODO add more mutation methods
 * */
public abstract class NumberX extends Number {

    public abstract void add(float x);

    public abstract void set(float v);

    public float getAndSet(float r) {
        float p = floatValue();
        set(r);
        return p;
    }

    public final void subtract(float x) {
        add(-x);
    }

    public final void subtract(final Number operand) {
        add(-operand.floatValue());
    }
}
