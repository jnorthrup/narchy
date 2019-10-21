package nars.perf;

import nars.NAR;
import nars.NARS;
import nars.test.impl.DeductiveMeshTest;
import org.junit.jupiter.api.Disabled;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;

import java.util.function.Consumer;

import static nars.perf.NARBenchmarks.perf;

@State(Scope.Thread)
@AuxCounters(AuxCounters.Type.EVENTS)
@Disabled
public class MultistepBenchmarks {


    @Param("4000")
    private
    String cycles;


    @Param({"12", "24"})
    private
    String termVolumeMax;

    private NAR n;

    public static void main(String[] args) throws RunnerException {
        perf(MultistepBenchmarks.class, new Consumer<ChainedOptionsBuilder>() {
            @Override
            public void accept(ChainedOptionsBuilder o) {
                o.warmupIterations(1);
                o.measurementIterations(1);

                o.forks(1);

            }
        });
    }

    @Setup
    public void start() {
//        Function<Term[], Subterms> h = null;


        n = NARS.tmp();
        n.termVolMax.set(Integer.parseInt(termVolumeMax));


    }

    @TearDown
    public void end() {
        long concepts = n.memory.size();
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public void deductiveMeshTest1() {
        new DeductiveMeshTest(n, 8, 8);
        n.run(Integer.parseInt(cycles));
    }

}
