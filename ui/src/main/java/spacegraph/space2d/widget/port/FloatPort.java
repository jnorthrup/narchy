package spacegraph.space2d.widget.port;

import jcog.data.atomic.AtomicFloat;
import jcog.math.FloatSupplier;

/** buffers the last sent value and compares with the current; transmits if inequal
 * TODO also transmit if there are new downstream connections
 * TODO abstract equality test method (ex: threshold, resolution, epsilon)
 * */
public class FloatPort extends TypedPort<Float> implements FloatSupplier {

    private AtomicFloat curValue = new AtomicFloat(Float.NaN);

    public FloatPort() {
        super(Float.class);
    }

    @Override
    public boolean recv(Wire from, Float s) {
        if (super.recv(from, s)) {
            curValue.set(s);
            return true;
        }
        return false;
    }

    public boolean out(Float nextValue) {
        if (curValue.getAndSet(nextValue)!=nextValue) {
            return super.out(nextValue);
        }
        return false;
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
