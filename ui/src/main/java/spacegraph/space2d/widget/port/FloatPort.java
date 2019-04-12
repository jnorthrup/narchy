package spacegraph.space2d.widget.port;

import jcog.data.atomic.AtomicFloat;
import jcog.math.FloatSupplier;

/** buffers the last sent value and compares with the current; transmits if inequal
 * TODO also transmit if there are new downstream connections
 * TODO abstract equality test method (ex: threshold, resolution, epsilon)
 * */
public class FloatPort extends TypedPort<Float> implements FloatSupplier {

    private final AtomicFloat curValue = new AtomicFloat(Float.NaN);

    public FloatPort() {
        super(Float.class);
        on(curValue::set);
    }

    /** retransmit */
    public final void out() {
        super.out(asFloat());
    }

    @Override
    public float asFloat() {
        return curValue.get();
    }
}
