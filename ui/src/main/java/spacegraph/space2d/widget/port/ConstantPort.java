package spacegraph.space2d.widget.port;

public class ConstantPort<X> extends Port {

    volatile X value = null;

    boolean outOnConnect = false;

    public ConstantPort(X value) {
        set(value);
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
