package nars.derive;

import com.google.common.util.concurrent.MoreExecutors;
import jcog.version.Versioned;
import nars.NAR;
import nars.NARS;
import nars.nal.nal5.NAL5Test;
import nars.nal.nal6.NAL6Test;
import nars.perf.JUnitPlanetX;
import nars.term.Term;
import nars.util.term.TermHashMap;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.junit.jupiter.api.Disabled;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.profile.GCProfiler;
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
public class BenchmarkUnify {

    @Param({"HashMap","UnifiedMap", "TermHashMap_HashMap", "TermHashMap_UnifiedMap"})
    String bagMapType;

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public void testY() {
        Supplier<NAR> s = ()->{
            NAR n = NARS.tmp();
            n.termVolumeMax.set(16);
            Supplier<Map<Term,Versioned<Term>>> mapBuilder;
            int initialCap = 8;
            float load = 0.99f;
            switch (bagMapType) {
                case "HashMap":
                    mapBuilder = () -> new HashMap(initialCap, load);
                    break;
                case "UnifiedMap":
                    mapBuilder = ()->new UnifiedMap(initialCap, load);
                    break;
                case "TermHashMap_HashMap":
                    mapBuilder = ()->new TermHashMap<>() {
                        @Override
                        protected int initialHashCapacity() {
                            return initialCap;
                        }
                        @Override
                        protected Map newOtherMap() {
                            return new HashMap(initialHashCapacity(), load);
                        }
                    };
                    break;
                case "TermHashMap_UnifiedMap":
                    mapBuilder = ()->new TermHashMap<>() {
                        @Override
                        protected int initialHashCapacity() {
                            return initialCap;
                        }

                        @Override
                        protected Map newOtherMap() {
                            return new UnifiedMap(initialHashCapacity(), load);
                        }
                    };
                    break;
                default:
                    throw new UnsupportedOperationException();
            }

            Derivation._termMapBuilder = mapBuilder;
            return n;
        };
        JUnitPlanetX.tests(MoreExecutors.directExecutor(), s,
            NAL5Test.class, NAL6Test.class
//                NAL1Test.class
//                , NAL2Test.class, NAL1MultistepTest.class
        );
    }

    public static void main(String[] args) throws RunnerException {

        Options opt = new OptionsBuilder()
                .include(BenchmarkUnify.class.getName())
//                .shouldDoGC(true)
                .warmupIterations(1)
                .measurementIterations(8)
                .timeout(TimeValue.seconds(30))
//                .addProfiler(StackProfiler2.class)
                .addProfiler(GCProfiler.class)
                //.addProfiler(LinuxPerfAsmProfiler.class)
                //.addProfiler(HotspotRuntimeProfiler.class)
//                .threads(4)
                .forks(1)
                .build();
        Collection<RunResult> result = new Runner(opt).run();
    }


}
