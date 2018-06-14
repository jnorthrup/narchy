package jcog.lab;

import jcog.io.Schema;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;

import java.util.function.Function;

/** data source.
 *  produces an instantaneous observation instance detected in the experiment
 */
abstract public class Sensor<E, S> implements Function<E,S> {
    /** sensor ID */
    final String id;

    protected Sensor(String id) {
        this.id = id;
    }

//        @Nullable
//        abstract S apply(E x);

    public void addToSchema(Schema data) {
        if (data.hasAttr(id))
            throw new RuntimeException(id + " already defined in " + data);

        data.defineNumeric(id);
    }

    public static <E> FloatSensor<? extends E> numeric(String name, FloatFunction<E> f) {
        return new FloatSensor<>(name, f);
    }

    /** general-purpose numeric scalar value observation
     *  32-bit float.  may be NaN if unknown or N/A */
    public static final class FloatSensor<E> extends Sensor<E,Float> {

        private final FloatFunction<E> func;

        public FloatSensor(String name, FloatFunction<E> f) {
            super(name);
            this.func = f;
        }

        @Override
        public Float apply(E e) {
            return func.floatValueOf(e);
        }
    }
    /** general-purpose boolean condition.  true, false, or null (unknown) */
    abstract static class BoolSensor<E> extends Sensor<E,Boolean> {


        public BoolSensor(String name) {
            super(name);
        }

    }
}
