package nars.perf;

import nars.NARS;
import nars.Op;
import nars.nal.nal8.NAL8Test;
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
            "3",
            "4",
            "5",
            "6",
            "7",
            "8",
            "9",
            "12"
    })
    private String termBuilderInterningVolume;

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Fork(value = 1
            //, jvmArgsPrepend = "-Xint"

    )
    @Threads(1)
    @Warmup(iterations = 1)
    @Measurement(iterations = 5)
    public void testInterning() {

        runTests(true, () -> NARS.tmp(),
//                NAL1Test.class
//                NAL2Test.class,
//                NAL3Test.class
//                NAL6Test.class,
                NAL8Test.class
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











































