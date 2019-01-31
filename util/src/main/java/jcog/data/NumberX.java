package jcog.data;

/** extends Number with mutable methods
 * TODO add more mutation methods
 * */
abstract public class NumberX extends Number {

    abstract public void add(float x);

    abstract public void set(float v);

    public float getAndSet(float r) {
        float p = floatValue();
        set(r);
        return p;
    }

    final public void subtract(float x) {
        add(-x);
    }

    public final void subtract(final Number operand) {
        add(-operand.floatValue());
    }
}
