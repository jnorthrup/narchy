package jcog.lab;

import jcog.io.Schema;
import jcog.lab.util.ExperimentRun;
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

    /** absolute */
    public static final NumberSensor unixtime = new NumberSensor("time_ms") {
            @Override
            public Long apply(Object o) {
                return System.currentTimeMillis();
            }
        };

    /** relative to start */
    public static final NumberSensor nanotime() {
        return new NumberSensor("time_ns") {
            long start = System.nanoTime();

            @Override
            public Long apply(Object o) {
                return System.nanoTime() - start;
            }
        };
    }

    public static <X> LabelSensor<X> label(String id) {
        return new LabelSensor<>(id);
    }

//        @Nullable
//        abstract S apply(E x);

    abstract public void addToSchema(Schema data);


    public static <E> FloatLambdaSensor<E> floatLambda(String name, FloatFunction<E> f) {
        return new FloatLambdaSensor<>(name, f);
    }

    /** general-purpose numeric scalar value observation
     *  32-bit float.  may be NaN if unknown or N/A */
    public static class FloatLambdaSensor<E> extends Sensor<E,Float> {

        private final FloatFunction<E> func;

        public FloatLambdaSensor(String name, FloatFunction<E> f) {
            super(name);
            this.func = f;
        }

        @Override
        public void addToSchema(Schema data) {
            data.defineNumeric(id);
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

        @Override
        public void addToSchema(Schema data) {
            data.defineNumeric(id);
        }
    }

    public abstract static class NumberSensor<E> extends Sensor<E,Number> {

        public NumberSensor(String name) {
            super(name);
        }

        @Override
        public void addToSchema(Schema data) {
            data.defineNumeric(id);
        }
    }

    public static class LabelSensor<X> extends Sensor<X,String> {
        String cur = "";

        public LabelSensor(String id) {
            super(id);
        }

        public void set(String cur) {
            this.cur = cur;
        }

        public String get() {
            return cur;
        }

        @Override
        public String apply(Object o) {
            return get();
        }

        /** convenience method */
        public LabelSensor record(String value, ExperimentRun trial) {
            set(value);
            trial.record();
            return this;
        }

        @Override
        public void addToSchema(Schema data) {
            data.defineText(id);
        }
    }
}
