package jcog.event;


import jcog.TODO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.function.Consumer;

/**
 * Represents the active state of a topic stream (Subscription)
 */
abstract public class AbstractOff<V> implements Off {

    protected final Consumer<Consumer<V>> disconnector;

    protected AbstractOff() {
        this.disconnector = null;
    }

    protected AbstractOff(Consumer<Consumer<V>> t) {
        this.disconnector = t;
    }

    protected AbstractOff(Topic<V> t) {
        this(t::stop);
    }

    public static <X> Off weak(Topic<X> x, Consumer<X> y) {
        if (y.getClass().isSynthetic())
            throw new TODO("this may be a lambda or some abstract class that gets a reference to it defeating the purpose of this weak registrant");

        return new AbstractOff.Weak<>(x, y);
    }

    abstract public void off();

    public static class Strong<V> extends AbstractOff<V> {

        public final Consumer<V> reaction;

        Strong(Topic<V> t, Consumer<V> o) {
            super(t);
            reaction = o;
            t.start(o);
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


    public static class Weak<V> extends AbstractOff<V> implements Consumer<V> {


        protected static final Logger logger = LoggerFactory.getLogger(Weak.class);

        public final WeakReference<Consumer<V>> reaction;

        Weak(Topic<V> t, Consumer<V> o) {
            super(t);
            reaction = new WeakReference<>(o);
            t.start(this);
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
