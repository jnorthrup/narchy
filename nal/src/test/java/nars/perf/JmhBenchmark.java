package nars.perf;

import org.junit.jupiter.api.Disabled;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.runner.options.VerboseMode;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * Created by me on 12/11/15.
 */
@Disabled
enum JmhBenchmark {
	;

	public static void perf(Class c, Consumer<ChainedOptionsBuilder> config) throws RunnerException {
		perf(c.getName(), config);
	}

	private static void perf(String include, Consumer<ChainedOptionsBuilder> config) throws RunnerException {
		ChainedOptionsBuilder opt = new OptionsBuilder()
				.include(include)
				.shouldDoGC(true)
				.warmupIterations(1)
				.threads(1)
				



				.resultFormat(ResultFormatType.TEXT)
				.verbosity(VerboseMode.EXTRA) 
				









				 

				
				
				

				
				

				




				


				.timeout(TimeValue.seconds(500))
		;

		config.accept(opt);



		Collection<RunResult> result = new Runner(opt.build()).run();















	}

}
