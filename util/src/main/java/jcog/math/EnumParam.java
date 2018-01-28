package jcog.math;

public class EnumParam<C extends Enum<C>> {

    public final Class<C> klass;
    public Enum<C> value;

    public EnumParam(Enum<C> value) {
        this.klass = (Class<C>) value.getClass();
    }
    public void set(Enum<C> next) {
        this.value = next;
    }

    public Enum<C> get() {
        return value;
    }
}
