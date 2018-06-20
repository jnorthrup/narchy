package nars.perf;

import nars.NAR;
import nars.NARS;
import nars.Op;
import nars.nal.nal1.NAL1Test;
import nars.nal.nal2.NAL2Test;
import nars.nal.nal3.NAL3Test;
import nars.nal.nal6.NAL6Test;
import nars.nal.nal8.NAL8Test;
import nars.test.TestNARSuite;
import nars.util.term.builder.HeapTermBuilder;
import nars.util.term.builder.InterningTermBuilder;
import org.junit.jupiter.api.Disabled;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.RunnerException;

import java.util.function.Supplier;

import static nars.perf.JmhBenchmark.perf;


@State(Scope.Thread)
@Disabled
public class NARTestBenchmark {

    @Param({ "interning", "heap"/*, "interningDeep"*/ })
    private String termBuilder;
    @Param({ "true"/*, "false"*/})
    private String parallel;

    private static final Class[] tests = {
            NAL1Test.class,
            NAL2Test.class,
            NAL3Test.class,
            NAL6Test.class,
            NAL8Test.class,
    };

    public static void main(String[] args) throws RunnerException {

        perf(NARTestBenchmark.class, (x) -> {
            x.measurementIterations(2);
            x.warmupIterations(1);

            //x.addProfiler(GCProfiler.class);

            x.forks(1);
//            x.threads(1);
        });
    }

    private void runTests(Supplier<NAR> n) {
        //new JUnitPlanetX().test(tests).run();
        new TestNARSuite(n, tests).run(parallel.equals("true"));
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Fork(1)
    public void testInterning() {
        int size = 32 * 1024;

        switch (termBuilder) {
            case "heap":
                Op.terms = HeapTermBuilder.the;
                break;
            case "interning":
                Op.terms = new InterningTermBuilder(size, false);
                break;
            case "interningDeep":
                Op.terms = new InterningTermBuilder(size, true);
                break;
        }

        runTests(() -> NARS.tmp());
    }


}











































