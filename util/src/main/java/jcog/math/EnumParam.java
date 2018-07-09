package jcog.math;

public class EnumParam<C extends Enum<C>> {

    public final Class<Enum<C>> klass;
    public C value;

    public EnumParam(C value, Class klass) {
        this.value = value;
        this.klass = klass;
    }
    public void set(C next) {
        this.value = next;
    }

    public C get() {
        return value;
    }
}
