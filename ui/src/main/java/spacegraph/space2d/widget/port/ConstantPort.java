package spacegraph.space2d.widget.port;

public class ConstantPort<X> extends TypedPort<X> {

    private volatile X value = null;

    private final boolean outOnConnect = true;

    public ConstantPort(X value, Class<? super X> klass) {
        super(klass);
        set(value);
    }

    public ConstantPort(Class<? super X> klass) {
        super(klass);
        set((X)null);
    }

    public ConstantPort(X value) {
        this(value, (Class<? super X>) value.getClass());
    }

    public void set(X value) {
        out(this.value = value);
    }


    @Override public void connected(Port other) {
        if (outOnConnect) {
            other.in.accept(null, value);
        }
    }

    public final void out() {
        out(value);
    }
}
