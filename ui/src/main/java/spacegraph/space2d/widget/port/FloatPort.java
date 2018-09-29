package spacegraph.space2d.widget.port;

import jcog.data.atomic.AtomicFloat;

public class FloatPort extends TypedPort<Float> {

    private AtomicFloat curValue = new AtomicFloat(Float.NaN);

    public FloatPort() {
        super(Float.class);
    }

    public boolean set(float nextValue) {
        if (curValue.getAndSet(nextValue)!=nextValue) {
            out(nextValue);
            return true;
        }
        return false;
    }

    public final void out() {
        out(curValue.get());
    }
}
