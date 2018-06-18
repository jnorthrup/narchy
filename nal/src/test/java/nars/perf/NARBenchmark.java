package nars.perf;

import nars.NAR;
import nars.NARS;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.test.impl.DeductiveMeshTest;
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
    private
    String cycles;


    @Param({"12", "24"})
    private
    String termVolumeMax;

    private long concepts;
    private NAR n;

    public static void main(String[] args) throws RunnerException {
        perf(NARBenchmark.class, (o) -> {
            o.warmupIterations(1);
            o.measurementIterations(2);

            o.forks(1);

        });
    }

    @Setup
    public void start() {
        Function<Term[], Subterms> h = null;


        n = NARS.tmp();
        n.termVolumeMax.set(Integer.parseInt(termVolumeMax));


    }

    @TearDown
    public void end() {
        concepts = n.concepts.size();
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public void deductiveMeshTest1() {
        new DeductiveMeshTest(n, 8, 8);
        n.run(Integer.parseInt(cycles));
    }

}
