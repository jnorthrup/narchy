
package nars.perf;

import nars.NAR;
import nars.NARS;
import nars.The;
import nars.term.Term;
import nars.term.sub.Subterms;
import nars.test.DeductiveMeshTest;
import org.junit.jupiter.api.Disabled;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.RunnerException;

import java.util.function.Function;

import static nars.perf.JmhBenchmark.perf;

@State(Scope.Thread)
@AuxCounters(AuxCounters.Type.EVENTS)
@Disabled
public class NARBenchmark {


    @Param("8000")
    String cycles;

//    @Param({"heap", "hijack", "caffeine"})
//    String subtermBuilder;

    @Param({"12", "24" })
    String termVolumeMax;

    public long concepts;
    private NAR n;

    @Setup
    public void start() {
        Function<Term[], Subterms> h = null;
//        switch (subtermBuilder) {
//            case "heap": h = The.Subterms.RawSubtermBuilder; break;
//            case "hijack": h = The.Subterms.HijackSubtermBuilder.get(); break;
//            case "caffeine": h = The.Subterms.CaffeineSubtermBuilder.get(); break;
//        }
//        The.Subterms.the = h;

        n = NARS.tmp();
        n.termVolumeMax.set(Integer.parseInt(termVolumeMax));

        //n.inputActivation.setValue(0.5f);
        //n.derivedActivation.setValue(0.5f);
        //n.nal(4);
    }

    @TearDown
    public void end() {
        concepts = n.terms.size();
    }


//    @Benchmark
//    @BenchmarkMode({Mode.AverageTime})
//    public void deductiveChainTest1() {
//        new DeductiveChainTest(n, 8, 9999991, (x, y) -> $.p($.the(x), $.the(y)));
//        n.run(Integer.parseInt(cycles));
//    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public void deductiveMeshTest1() {
        new DeductiveMeshTest(n, 8, 8);
        n.run(Integer.parseInt(cycles));
    }



//    @Benchmark
//    @BenchmarkMode(value = Mode.AverageTime)
//    public void nal1Deduction() throws Narsese.NarseseException {
//        n.nal(1);
//        Compound a = $("<a-->b>");
//        Compound b = $("<b-->c>");
//
//        n.believe(a);
//        n.believe(b);
//        n.run(10000);
//    }
//
//    @Benchmark
//    @BenchmarkMode(value = Mode.AverageTime)
//    public void nal1DeductionInNAL8() throws Narsese.NarseseException {
//        n.nal(8);
//        Compound a = $("<a-->b>");
//        Compound b = $("<b-->c>");
//
//        n.believe(a);
//        n.believe(b);
//        n.run(10000);
//    }


    public static void main(String[] args) throws RunnerException {
        perf(NARBenchmark.class,(o)->{
            o.warmupIterations(1);
            o.measurementIterations(2);
            //o.threads(4);
            o.forks(1);

        });
    }

}
