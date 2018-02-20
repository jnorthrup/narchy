package spacegraph.widget.windo;

import spacegraph.widget.text.Label;

import java.util.function.Function;

public class LabeledPort<X> extends Port {
    final Label l = new Label("?");

    public static LabeledPort<?> generic() {
        return new LabeledPort(Object::toString);
    }

    public LabeledPort(Function<X,String> toString) {
        content(l);
        on((v)->l.text(toString.apply((X) v)));
    }

}
