package nars.perf;

import nars.NARS;
import nars.Op;
import nars.nal.nal1.NAL1Test;
import nars.nal.nal6.NAL6Test;
import nars.term.util.builder.HeapTermBuilder;
import nars.term.util.builder.InterningTermBuilder;
import org.openjdk.jmh.annotations.*;

import static nars.perf.NARBenchmarks.runTests;

@State(Scope.Benchmark)
public class InterningTermBuilderBenchmark {

    /**
     * 0 = heap TermBuilder
     */
    @Param({
        //"0"
        "32768"
    })
    private String termBuilderInterningSize;

    @Param({
            //"6"
            // "18"
            "11"
    })
    private String termBuilderInterningVolume;

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Fork(value = 1
            //, jvmArgsPrepend = "-Xint"
    )
    @Threads(1)
    @Warmup(iterations = 1)
    @Measurement(iterations = 2)
    public void testInterning() {

        runTests(true, () -> NARS.tmp(),
                NAL1Test.class,
//                NAL2Test.class,
//                NAL3Test.class
                NAL6Test.class
//                NAL8Test.class
        );
    }

    @Setup
    public void init() {

        switch (termBuilderInterningSize) {
            case "0":
                Op.terms = HeapTermBuilder.the;
                break;
            default:
                Op.terms = new InterningTermBuilder(Integer.parseInt(termBuilderInterningSize), Integer.parseInt(termBuilderInterningVolume));
                break;
        }
    }



}











































