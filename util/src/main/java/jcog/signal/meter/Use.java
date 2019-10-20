package jcog.signal.meter;

import jcog.Texts;
import jcog.exe.Exe;
import org.HdrHistogram.AtomicHistogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** resource stopwatch, useful for profiling */
public class Use {

	static final Logger logger = LoggerFactory.getLogger(Use.class);

	public final String id;

	static final long MAX_TIME_SEC = 10;
	static final long MAX_TIME_NS = MAX_TIME_SEC * 1_000_000_000;
	static final long MIN_TIME_NS = 10;

//	final AtomicLong totalNS = new AtomicLong();
	final AtomicHistogram timeNS = new AtomicHistogram(MIN_TIME_NS, MAX_TIME_NS, 0);

	public Use(String id) {
		this.id = id;
	}

	public void timeNS(long dur) {
//		totalNS.addAndGet(dur);
		if (dur > MIN_TIME_NS) {
			if (dur < MAX_TIME_NS)
				timeNS.recordValue(dur);
			else
				logger.error("{} excessive duration recorded: {}", id, dur);
		}
	}

	public void reset() {
//		totalNS.set(0);
		timeNS.reset();
	}

	@FunctionalInterface public interface SafeAutocloseable extends AutoCloseable {
		void close();
	}

	public final class time implements SafeAutocloseable {

		final long start;

		time() {
			this.start = System.nanoTime();
		}

		@Override
		public void close() {
			var end = System.nanoTime();
			timeNS(end-start);
		}
	}

	public SafeAutocloseable time() {
		return Exe.PROFILE ? new time() : NullAutocloseable;
	}

	static final SafeAutocloseable NullAutocloseable = () -> { };

	@Override
	public String toString() {
		var timeCopy = timeNS.copy(); //TODO dont need to copy to AtomicHistogram, non-atomic is ok
		timeNS.reset(); //HACK

		//https://www.mathsisfun.com/data/confidence-interval.html
		var mean = timeCopy.getMean();
		var Z = 0.9; //90%
		var N = timeCopy.getTotalCount();
		var confInterval = Z * timeCopy.getStdDeviation()/Math.sqrt(N) / mean;

		return
			Texts.timeStr(mean * N) + //total time, estimate
			" mean=" + Texts.timeStr(Math.round(mean)) +
			"±" + Texts.n2(confInterval*100) + "% x " + N;
			//+ Texts.histogramString(timeCopy, false);
	}
}
