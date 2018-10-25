package nars.test;

import jcog.data.list.FasterList;
import nars.NAR;
import org.eclipse.collections.api.block.function.primitive.DoubleFunction;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class TestNARSuite extends FasterList<TestNARSuite.MyTestNAR> {

    private final Supplier<NAR> narBuilder;
    private final Stream<Method> testMethods;

    public TestNARSuite(Supplier<NAR> narBuilder, Class<? extends NALTest>... testClasses) {
        this(narBuilder, NALTest.tests(testClasses));
    }

    public TestNARSuite(Supplier<NAR> narBuilder, Stream<Method> testMethods) {
        this.narBuilder = narBuilder;
        this.testMethods = testMethods;
    }

    public void run(boolean parallel) {
        run(parallel, 1);
    }

    public void run(boolean parallel, int iterations) {



        List<Method> mm = testMethods.collect(Collectors.toList());

        for (int i = 0; i < iterations; i++) {


            (parallel ? mm.stream().parallel() : mm.stream()).forEach(m -> {
                String testName = m.getDeclaringClass().getName() + ' ' + m.getName();
                MyTestNAR t = new MyTestNAR(narBuilder.get(), testName);
                synchronized (TestNARSuite.this) {
                    add(t); //allow repeats
                }

                try {
                    NALTest.test(t, m);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            });
        }
    }


    public long sum(ToIntFunction<NAR> n) {
        return intStream(n).sum();
    }
    public double avg(ToIntFunction<NAR> n) {
        return intStream(n).average().getAsDouble();
    }
    public double sum(Function<NAR,Number> n) {
        return doubleStream(n).sum();
    }
    public double sum(DoubleFunction<NAR> n) {
        return doubleStream(n).sum();
    }
    public double avg(DoubleFunction<NAR> n) {
        return doubleStream(n).average().getAsDouble();
    }

    public DoubleStream doubleStream(DoubleFunction<NAR> n) {
        return narStream().mapToDouble(n);
    }

    public Stream<NAR> narStream() {
        return stream().map(x -> x.nar);
    }

    public DoubleStream doubleStream(Function<NAR,Number> n) {
        return narStream().map(n).mapToDouble(Number::doubleValue);
    }
    public IntStream intStream(ToIntFunction<NAR> n) {
        return narStream().mapToInt(n);
    }

    static class MyTestNAR extends TestNAR {
        public final String name;

        public MyTestNAR(NAR nar, String testName) {
            super(nar);
            this.name = testName;
        }
    }

    /** summary */
    public void print() {
        forEach(x -> System.out.println(x.name + ' ' + x.score));
    }

    public double score(/* scoring mode */) {
        return stream().mapToDouble(x -> x.score).sum();
    }
}
