package nars.test;

import jcog.data.list.FasterList;
import nars.NAL;
import nars.NAR;
import org.eclipse.collections.api.block.function.primitive.DoubleFunction;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

public class TestNARSuite extends FasterList<TestNARSuite.MyTestNAR> {

    private final Supplier<NAL<NAL<NAR>>> narBuilder;
    private final Stream<Method> testMethods;

    public TestNARSuite(Supplier<NAL<NAL<NAR>>> narBuilder, Class<? extends NALTest>... testClasses) {
        this(narBuilder, NALTest.tests(testClasses));
    }

    public TestNARSuite(Supplier<NAL<NAL<NAR>>> narBuilder, Stream<Method> testMethods) {
        this.narBuilder = narBuilder;
        this.testMethods = testMethods;
    }

    public void run(boolean parallel) {
        run(parallel, 1);
    }

    public TestNARSuite run(boolean parallel, int iterations) {



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
        return this;
    }


    public double sum(Function<NAL<NAL<NAR>>,Number> n) {
        return doubleStream(n).sum();
    }
    public double sum(DoubleFunction<NAL<NAL<NAR>>> n) {
        return doubleStream(n).sum();
    }

    public DoubleStream doubleStream(DoubleFunction<NAL<NAL<NAR>>> n) {
        return narStream().mapToDouble(n);
    }

    public Stream<NAL<NAL<NAR>>> narStream() {
        return stream().map(x -> x.nar);
    }

    public DoubleStream doubleStream(Function<NAL<NAL<NAR>>,Number> n) {
        return narStream().map(n).mapToDouble(Number::doubleValue);
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
