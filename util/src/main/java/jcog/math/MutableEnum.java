package jcog.math;

public class MutableEnum<C extends Enum<C>> {

    public final Class<Enum<C>> klass;

    private volatile C value;

    public MutableEnum(Class klass) {
        this.klass = klass;
    }

    public MutableEnum(Class klass, C value) {
        this(klass);
        set(value);
    }

    public MutableEnum<C> set(C next) {
        this.value = next;
        return this;
    }

    public C get() {
        return value;
    }
}
