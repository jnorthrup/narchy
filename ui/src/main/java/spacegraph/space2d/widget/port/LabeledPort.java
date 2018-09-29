package spacegraph.space2d.widget.port;

import spacegraph.space2d.widget.port.util.Wire;
import spacegraph.space2d.widget.text.VectorLabel;

import java.util.function.Function;

public class LabeledPort<X> extends Port<X> {
    private final VectorLabel l = new VectorLabel("?");
    private final Function<X, String> toString;

    public static LabeledPort<?> generic() {
        return new LabeledPort(Object::toString);
    }

    private LabeledPort(Function<X, String> toString) {
        this.toString = toString;
        set(l);

        on((w,x)->{
            update(w, x);
            //out((Port)w.other(LabeledPort.this), x); //rebroadcast
        });
    }

    protected VectorLabel update(Wire w, X x) {
        return l.text(toString.apply(x));
    }

}
