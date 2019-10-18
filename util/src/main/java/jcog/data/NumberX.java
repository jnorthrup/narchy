package jcog.data;

/** extends Number with mutable methods
 * TODO add more mutation methods
 * */
public abstract class NumberX extends Number {

    public abstract void add(float x);

    public abstract void set(float v);

    public float getAndSet(float r) {
        set(r);
        return floatValue();
    }

    public final void subtract(float x) {
        add(-x);
    }

    public final void subtract(Number operand) {
        add(-operand.floatValue());
    }
}
