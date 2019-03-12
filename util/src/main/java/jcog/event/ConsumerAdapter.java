package jcog.event;

import java.util.function.Consumer;

public final class ConsumerAdapter<X> implements Consumer<X> {

    public final Runnable r;

    public ConsumerAdapter(Runnable o) {
        this.r = o;
    }

    @Override
    public int hashCode() {
        return r.hashCode();
    }

    @Override
    public String toString() {
        return r.toString();
    }

    @Override
    public void accept(X x) {
        r.run();
    }
}
