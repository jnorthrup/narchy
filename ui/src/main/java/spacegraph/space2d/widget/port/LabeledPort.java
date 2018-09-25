package spacegraph.space2d.widget.port;

import spacegraph.space2d.widget.text.VectorLabel;

import java.util.function.Function;

public class LabeledPort<X> extends Port {
    private final VectorLabel l = new VectorLabel("?");
    private final Function<X, String> toString;

    public static LabeledPort<?> generic() {
        return new LabeledPort(Object::toString);
    }

    private LabeledPort(Function<X, String> toString) {
        this.toString = toString;
        set(l);

        on((v)-> label((X) v));
    }

    @Override
    protected void out(Port sender, Object x) {
        super.out(sender, x);
        try {
            label((X)x);
        } catch (ClassCastException e) {
            throw new RuntimeException(e);
        }
    }

    protected VectorLabel label(X v) {
        return l.text(toString.apply(v));
    }

}
