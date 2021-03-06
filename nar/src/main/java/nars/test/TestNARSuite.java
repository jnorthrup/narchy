package nars.test;

import jcog.data.list.FasterList;
import nars.NAR;
import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

@Deprecated public class TestNARSuite extends FasterList<TestNARSuite.MyTestNAR> {

    private final Supplier<NAR> narBuilder;
    private final Stream<Method> testMethods;

    @SafeVarargs
    public TestNARSuite(Supplier<NAR> narBuilder, Class<? extends NALTest>... testClasses) {
        this(narBuilder, tests(testClasses));
    }

    public TestNARSuite(Supplier<NAR> narBuilder, Stream<Method> testMethods) {
        this.narBuilder = narBuilder;
        this.testMethods = testMethods;
    }

    @SafeVarargs
    public static Stream<Method> tests(Class<? extends NALTest>... c) {
        List<Method> list = new ArrayList<>();
        for (Class<? extends NALTest> cc : c) {
            for (Method x : cc.getDeclaredMethods()) {
                if (x.getAnnotation(Test.class) != null) {
                    x.trySetAccessible();
                    list.add(x);
                }
            }
        }
        return list.stream();
    }

    public void run(boolean parallel) {
        run(parallel, 1);
    }

    public TestNARSuite run(boolean parallel, int iterations) {


        List<Method> mm = testMethods.collect(Collectors.toList());

        for (int i = 0; i < iterations; i++) {


            (parallel ? mm.stream().parallel() : mm.stream()).forEach(new Consumer<Method>() {
                @Override
                public void accept(Method m) {
                    String testName = m.getDeclaringClass().getName() + ' ' + m.getName();
                    MyTestNAR t = new MyTestNAR(narBuilder.get(), testName);
                    synchronized (TestNARSuite.this) {
                        TestNARSuite.this.add(t); //allow repeats
                    }

                    try {
                        NALTest.test(t, m);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        return this;
    }


    public double sum(Function<NAR,Number> n) {
        return doubleStream(n).sum();
    }
    public double sum(DoubleFunction<NAR> n) {
        return doubleStream(n).sum();
    }

    public DoubleStream doubleStream(DoubleFunction<NAR> n) {
        return narStream().mapToDouble(n);
    }

    public Stream<NAR> narStream() {
        return stream().map(new Function<MyTestNAR, NAR>() {
            @Override
            public NAR apply(MyTestNAR x) {
                return x.nar;
            }
        });
    }

    public DoubleStream doubleStream(Function<NAR,Number> n) {
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
        forEach(new Procedure<MyTestNAR>() {
            @Override
            public void value(MyTestNAR x) {
                System.out.println(x.name + ' ' + x.score);
            }
        });
    }

    public double score(/* scoring mode */) {
        double sum = 0.0;
        for (MyTestNAR x : this) {
            double score = (double) x.score;
            sum += score;
        }
        return sum;
    }
}
