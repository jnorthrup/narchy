package nars.perf;

import nars.NAR;
import nars.NARS;
import nars.concept.util.DefaultConceptBuilder;
import nars.nal.nal1.NAL1MultistepTest;
import nars.nal.nal1.NAL1Test;
import nars.nal.nal2.NAL2Test;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.junit.jupiter.api.Disabled;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@State(Scope.Thread)
@Disabled
public class BenchmarkDefaultConceptBuilder {

    //@Param({"HashMap", "UnifiedMap" })
    @Param({"HashMap"})
    String bagMapType;

    //@Param({"0","2","8","16"})
    @Param({"2"})
    String initialBagSize;

    //@Param({"0.5","0.75","0.9","0.99"})
    @Param({"0.99","0.9","0.5","0.75"})
    String loadFactor;

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public void testY() {
        Supplier<NAR> s = ()->{
            NAR n = NARS.tmp();
            Supplier<Map> mapBuilder;
            int bagSize = Integer.parseInt(initialBagSize);
            float loadFactor = Float.parseFloat(this.loadFactor);
            switch (bagMapType) {
                case "HashMap":
                    mapBuilder = ()->new HashMap(bagSize, loadFactor);
                    break;
                case "UnifiedMap":
                    mapBuilder = ()->new UnifiedMap(bagSize, loadFactor);
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
            n.conceptBuilder(new DefaultConceptBuilder() {
                @Override
                protected Map newBagMap(int volume) {
                    return mapBuilder.get();
                }
            });
            return n;
        };
        JUnitNAR.tests(s.get(),
                NAL1Test.class, NAL2Test.class, NAL1MultistepTest.class
        );
    }

    public static void main(String[] args) throws RunnerException {

        Options opt = new OptionsBuilder()
                .include(nars.perf.BenchmarkDefaultConceptBuilder.class.getName())
                //.shouldDoGC(true)
                .warmupIterations(1)
                .measurementIterations(4)
                .timeout(TimeValue.seconds(10))
                .threads(1)
                .forks(0)
                .build();
        Collection<RunResult> result = new Runner(opt).run();
    }



}
