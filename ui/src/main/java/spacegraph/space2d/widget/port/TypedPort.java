package spacegraph.space2d.widget.port;

import spacegraph.space2d.widget.port.util.AdaptingWire;
import spacegraph.space2d.widget.windo.GraphEdit;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static spacegraph.space2d.widget.port.util.Wiring.CAST;

public class TypedPort<X> extends Port<X> {

    public final Class<? super X> type;

    public TypedPort(Class<? super X> type) {
        super();
        this.type = type;
    }

    public TypedPort(Class<? super X> type, In<? super X> o) {
        super(o);
        this.type = type;
    }

    public TypedPort(Class<? super X> type, Consumer<? super X> o) {
        super(o);
        this.type = type;
    }


    public static Wire adapt(Wire w, GraphEdit g) {
        if (w.a instanceof TypedPort && w.b instanceof TypedPort) {

            //TODO lazy construct and/or cache these

            //apply type checking and auto-conversion if necessary
            Class aa = ((TypedPort) w.a).type, bb = ((TypedPort) w.b).type;
            if (aa.equals(bb)) {
                //ok
            } else {

                List<Function> ab = CAST.convertors(aa, bb), ba = CAST.convertors(bb, aa);

                if (!ab.isEmpty() || !ba.isEmpty())
                    w = new AdaptingWire(w, ab, ba);

            }

        }
        return w;
    }
}
