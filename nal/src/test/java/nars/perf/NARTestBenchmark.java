package nars.perf;

import jcog.test.JUnitPlanetX;
import nars.Op;
import nars.nal.nal1.NAL1Test;
import nars.nal.nal2.NAL2Test;
import nars.nal.nal3.NAL3Test;
import nars.util.term.builder.InterningTermBuilder;
import org.junit.jupiter.api.Disabled;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.RunnerException;

import static nars.perf.JmhBenchmark.perf;


@State(Scope.Thread)
@Disabled
public class NARTestBenchmark {

    public static void main(String[] args) throws RunnerException {

        perf(NARTestBenchmark.class, (x) -> {
            x.measurementIterations(4);
            x.warmupIterations(0);
            
            x.forks(1);
            x.threads(0);
        });
    }

    public static final Class[] tests = {
            NAL1Test.class,
            NAL2Test.class,
            NAL3Test.class,





    };


    void runTests() {
        new JUnitPlanetX().test(tests).run();
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Fork(1)
    public void testIterning() {
        Op.terms = new InterningTermBuilder();
        runTests();
//        System.err.println(
//                ((InterningTermBuilder)Op.terms).summary()
//        );
    }






















}











































