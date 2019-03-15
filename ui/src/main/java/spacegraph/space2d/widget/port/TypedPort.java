package spacegraph.space2d.widget.port;

import jcog.Util;
import jcog.math.v2;
import jcog.math.v3;
import jcog.reflect.ExtendedCastGraph;
import jcog.signal.Tensor;
import jcog.signal.tensor.ArrayTensor;

import java.util.function.Consumer;
import java.util.function.Function;

public class TypedPort<X> extends Port<X> {

    public final static ExtendedCastGraph CAST = new ExtendedCastGraph();

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
}
