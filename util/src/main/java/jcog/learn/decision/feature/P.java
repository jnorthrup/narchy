package jcog.learn.decision.feature;

import java.util.function.Predicate;

/**
 * Convenience class for various predicates.
 *
 * @author Ignas
 */
public class P {

    public static <T> Predicate<T> isEqual(T value) {
        return new Predicate<T>() {
            @Override
            public boolean test(T p) {
                return p.equals(value);
            }
        };
    }

    public static Predicate<Double> moreThanD(double value) {
        return new Predicate<Double>() {
            @Override
            public boolean test(Double p) {
                return p > value;
            }
        };
    }

    public static Predicate<Double> lessThanD(double value) {
        return new Predicate<Double>() {
            @Override
            public boolean test(Double p) {
                return p < value;
            }
        };
    }

    public static Predicate<Number> moreThan(Number value) {
        return new Predicate<Number>() {
            @Override
            public boolean test(Number p) {
                return p.floatValue() > value.floatValue();
            }
        };
    }


    public static Predicate<Integer> moreThan(int value) {
        return new Predicate<Integer>() {
            @Override
            public boolean test(Integer p) {
                return p > value;
            }
        };
    }

    public static Predicate<Integer> lessThan(int value) {
        return new Predicate<Integer>() {
            @Override
            public boolean test(Integer p) {
                return p < value;
            }
        };
    }

    public static Predicate<Integer> between(int from, int to) {
        return moreThan(from).and(lessThan(to));
    }

    public static Predicate<Double> betweenD(double from, double to) {
        return moreThanD(from).and(lessThanD(to));
    }

    public static Predicate<String> startsWith(String prefix) {
        return new Predicate<String>() {
            @Override
            public boolean test(String p) {
                return p != null && p.startsWith(prefix);
            }
        };
    }

}
