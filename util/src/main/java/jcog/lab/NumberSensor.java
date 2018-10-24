package jcog.lab;

import jcog.io.Schema;
import org.eclipse.collections.api.block.function.primitive.BooleanFunction;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;

import java.util.function.*;

public abstract class NumberSensor<X> extends Sensor<X,Number> {

    public NumberSensor(String name) {
        super(name);
    }


    @Override
    public void addToSchema(Schema data) {
        data.defineNumeric(id);
    }

    public static <X> NumberSensor<X> ofNumber(String id, Function<X,Number> lambda) {
        return new NumberLambdaSensor<>(id, lambda);
    }

    public static <X> NumberSensor<X> of(String id, FloatFunction<X> f) {
        return ofNumber(id, f::floatValueOf);
    }
    public static <X> NumberSensor<X> of(String id, BooleanFunction<X> f) {
        return of(id, (Predicate<X>)(f::booleanValueOf));
    }
    public static <X> NumberSensor<X> of(String id, ToDoubleFunction<X> f) {
        return ofNumber(id, f::applyAsDouble);
    }
    public static <X> NumberSensor<X> of(String id, ToLongFunction<X> f) {
        return ofNumber(id, f::applyAsLong);
    }
    public static <X> NumberSensor<X> of(String id, ToIntFunction<X> f) {
        return ofNumber(id, f::applyAsInt);
    }
    public static <X> NumberSensor<X> of(String id, Predicate<X> f) {
        return ofNumber(id, ((X x) -> f.test(x) ? 1 : 0));
    }



    /** general-purpose numeric scalar value observation
     *  32-bit float.  may be NaN if unknown or N/A */
    private static class NumberLambdaSensor<X> extends NumberSensor<X> {

        private final Function<X,Number> func;

        public NumberLambdaSensor(String name, Function<X,Number> f) {
            super(name);
            this.func = f;
        }


        @Override
        public Number apply(X e) {
            return func.apply(e);
        }
    }
}
