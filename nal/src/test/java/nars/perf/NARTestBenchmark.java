package nars.perf;

import jcog.test.JUnitPlanetX;
import nars.Op;
import nars.nal.nal1.NAL1Test;
import nars.nal.nal2.NAL2Test;
import nars.nal.nal3.NAL3Test;
import nars.nal.nal4.NAL4Test;
import nars.nal.nal5.NAL5Test;
import nars.nal.nal6.NAL6Test;
import nars.nal.nal7.NAL7Test;
import nars.nal.nal8.NAL8Test;
import nars.util.term.builder.HeapTermBuilder;
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
            //x.jvmArgs("-Xint");
            x.forks(1);
            x.threads(1);
        });
    }

    public static final Class[] tests = {
            NAL1Test.class,
            NAL2Test.class,
            NAL3Test.class,
            NAL4Test.class,
            NAL5Test.class,
            NAL6Test.class,
            NAL7Test.class,
            NAL8Test.class
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
        System.err.println(
                ((InterningTermBuilder)Op.terms).summary()
        );
    }


    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Fork(1)
    public void testHeap() {
        Op.terms = new HeapTermBuilder();
        runTests();
    }



//    @Benchmark
//    @BenchmarkMode(Mode.AverageTime)
//    @Fork(1)
//    public void testY() {
//        The.Compound.the = FastCompound.FAST_COMPOUND_BUILDER;
////        Param.SynchronousExecution_Max_CycleTime = 0.0001f;
//
//        junit(testclass);
//    }

}

//public class TestBenchmark1 {
//
////    static String eval(String script) {
////        // We don't actually need the context object here, but we need it to have
////        // been initialized since the
////        // constructor for Ctx sets static state in the Clojure runtime.
////
////        Object result = Compiler.eval(RT.readString(script));
////
////        return RT.printString(result) + " (" +result.getClass() + ")";
////    }
////    @Benchmark
////    @BenchmarkMode(value = Mode.SingleShotTime)
////    public void eval1() {
////
////        new Dynajure().eval("(+ 1 1)");
////    }
////
////    @Benchmark
////    @BenchmarkMode(value = Mode.SingleShotTime)
////    public void eval2() {
////        new Dynajure().eval("(* (+ 1 1) 8)");
////        //out.println(eval("'(inh a b)") );
////        //out.println(eval("'[inh a b]") );
////    }
//
//    @Benchmark
//    @BenchmarkMode(Mode.SingleShotTime)
//    public void testExecution() throws Narsese.NarseseException {
//        NAR n = new NARS().get();
//        //n.log();
//        n.input("a:b!");
//        n.input("<(rand 5)==>a:b>.");
//
//        n.run(6);
//    }
//
////    public static void main(String[] args) throws RunnerException {
////        perf(TestBenchmark1.class, 6, 10);
////
////    }
//}
