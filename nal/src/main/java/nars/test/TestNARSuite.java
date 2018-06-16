package nars.test;

import jcog.list.FasterList;
import nars.NAR;

import java.lang.reflect.Method;
import java.util.function.Supplier;
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

    public void run() {
        testMethods.parallel().forEach(m -> {
            String testName = m.getDeclaringClass().getName() + " " + m.getName();
            MyTestNAR t = new MyTestNAR(narBuilder.get(), testName);
            add(t);
            NALTest.test(t, m);
        });
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
        forEach(x -> {
            System.out.println(x.name + " " + x.score);
        });
    }

    public double score(/* scoring mode */) {
        return stream().mapToDouble(x -> x.score).sum();
    }
}
