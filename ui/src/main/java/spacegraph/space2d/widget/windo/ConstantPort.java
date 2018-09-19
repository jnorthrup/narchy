package spacegraph.space2d.widget.windo;

public class ConstantPort<X> extends Port {

    volatile X value = null;

    boolean outOnConnect = false;

    public ConstantPort(X value) {
        set(value);
    }

    public void set(X value) {
        out(this.value = value);
    }

    @Override
    public boolean connected(Port other) {

        if (super.connected(other)) {
            if (outOnConnect) {
                other.in.accept(null, value);
            }
            return true;
        }

        return false;
    }

    public final void out() {
        out(value);
    }
}
