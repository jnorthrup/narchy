package jcog.signal.meter;

import jcog.Texts;
import org.HdrHistogram.AtomicHistogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** resource stopwatch, useful for profiling */
public class Use {

	static final Logger logger = LoggerFactory.getLogger(Use.class);

	public final String id;

	final static long MAX_TIME_SEC = 10;
	final static long MIN_TIME_uSEC = 1;
	final static long MAX_TIME_NS = MAX_TIME_SEC * 1_000_000_000, MIN_TIME_NS = MIN_TIME_uSEC * 1000;

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

	public static final class time implements AutoCloseable {

		final long start;
		final Use use;

		time(Use use) {
			this.use = use;
			this.start = System.nanoTime();
		}

		@Override
		public void close() {
			long end = System.nanoTime();
			use.timeNS(end-start);
		}
	}

	public time time() {
		return new time(this);
	}

	@Override
	public String toString() {
		AtomicHistogram timeCopy = timeNS.copy(); //TODO dont need to copy to AtomicHistogram, non-atomic is ok
		timeNS.reset(); //HACK

		//https://www.mathsisfun.com/data/confidence-interval.html
		double mean = timeCopy.getMean();
		double Z = 0.9; //90%
		double confInterval = Z * timeCopy.getStdDeviation()/Math.sqrt(timeCopy.getTotalCount()) / mean;

		return "mean=" + Texts.timeStr(Math.round(mean)) +
			"±" + Texts.n2(confInterval*100) + '%';
			//+ Texts.histogramString(timeCopy, false);
	}
}
