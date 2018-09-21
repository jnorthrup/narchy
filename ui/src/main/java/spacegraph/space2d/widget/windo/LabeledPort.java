package spacegraph.space2d.widget.windo;

import spacegraph.space2d.widget.text.VectorLabel;

import java.util.function.Function;

public class LabeledPort<X> extends Port {
    private final VectorLabel l = new VectorLabel("?");

    public static LabeledPort<?> generic() {
        return new LabeledPort(Object::toString);
    }

    private LabeledPort(Function<X, String> toString) {
        set(l);
        on((v)->l.text(toString.apply((X) v)));
    }

}
