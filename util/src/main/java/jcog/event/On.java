package jcog.event;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.function.Consumer;

/**
 * Represents the active state of a topic stream (Subscription)
 */
abstract public class On<V> implements Off {

    public final Consumer<Consumer<V>> disconnector;

    protected On() {
        this.disconnector = null;
    }

    protected On(Consumer<Consumer<V>> t) {
        this.disconnector = t;
    }

    protected On(Topic<V> t) {
        this(t::disable);
    }

    abstract public void off();

    public static class Strong<V> extends On<V> {

        public final Consumer<V> reaction;

        Strong(Topic<V> t, Consumer<V> o) {
            super(t);
            reaction = o;
            t.enable(o);
        }

        @Override
        public void off() {
            disconnector.accept(reaction);
        }

        @Override
        public String toString() {
            return "On:" + disconnector + "->" + reaction;
        }
    }








    public static class Weak<V> extends On<V> implements Consumer<V> {


        protected static final Logger logger = LoggerFactory.getLogger(Weak.class);

        public final WeakReference<Consumer<V>> reaction;

        Weak(Topic<V> t, Consumer<V> o) {
            super(t);
            reaction = new WeakReference<Consumer<V>>(o);
            t.enable(this);
        }

        @Override
        public void accept(V v) {
            Consumer<V> c = reaction.get();
            if (c != null) {
                try {
                    c.accept(v);
                } catch (Throwable any) {
                    logger.error(" {}", any);
                    off();
                }
            } else {
                
                off();
            }
        }

        @Override
        public void off() {
            disconnector.accept(this);
        }






    }

}
