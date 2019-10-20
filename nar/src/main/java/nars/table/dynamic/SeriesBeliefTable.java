package nars.table.dynamic;

import jcog.Util;
import nars.NAL;
import nars.Task;
import nars.attention.What;
import nars.table.BeliefTable;
import nars.table.TaskTable;
import nars.table.eternal.EternalTable;
import nars.table.temporal.TemporalBeliefTable;
import nars.task.TemporalTask;
import nars.task.UnevaluatedTask;
import nars.task.util.Answer;
import nars.task.util.series.AbstractTaskSeries;
import nars.term.Compound;
import nars.term.Term;
import nars.time.When;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static nars.Op.CONJ;
import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.TIMELESS;

/**
 * adds a TaskSeries additional Task buffer which can be evaluated from, or not depending
 * if a stored task is available or not.
 */
public class SeriesBeliefTable extends DynamicTaskTable {

	public final AbstractTaskSeries<SeriesTask> series;


	public SeriesBeliefTable(Term c, boolean beliefOrGoal, AbstractTaskSeries<SeriesTask> s) {
		super(c, beliefOrGoal);
		this.series = s;
	}

	/**
	 * adjust CONJ concepts for series task generation
	 */
	protected static Term taskTerm(Term x) {
		return x.opID() == (int) CONJ.id ? ((Compound) x).dt(0) : x;
	}

	@Override
	public int taskCount() {
		return series.size();
	}

	@Override
	public final void match(Answer a) {
//		long seriesStart = series.start();
//		if (seriesStart == TIMELESS)
//			return; //empty

		long s = a.start, e;

		if (s == ETERNAL || s == TIMELESS) {
			//choose now as the default focus time
            long now = a.time();
            float dur = a.dur;
			s = now - (long)Math.ceil((double) (dur / 2.0F));
			e = now + (long)Math.ceil((double) (dur / 2.0F));
		} else {
			e = a.end;
		}

//			//use at most a specific fraction of the TTL
//			int aTTL = a.ttl; //save
////			long range = Math.max(1, LongInterval.intersectLength(seriesStart, series.end(), s, e));
//			int seriesTTL = Math.min(aTTL, (int) (NAL.signal.SERIES_MATCH_MIN + Math.ceil(
//				NAL.signal.SERIES_MATCH_ADDITIONAL_RATE_PER_DUR / Math.max(1, dur) /* * range */)));
//			a.ttl = seriesTTL;
//
		series.whileEach(s, e, false, a);
//
//			int ttlUsed = seriesTTL - a.ttl; //assert(ttlUsed <= aTTL);
//			a.ttl = aTTL - ttlUsed; //restore
	}

	@Override
	public void clear() {
		series.clear();
	}

	@Override
	public Stream<? extends Task> taskStream() {
		return series.stream();
	}

	@Override
	public void forEachTask(long minT, long maxT, Consumer<? super Task> x) {
		series.forEach(minT, maxT, true, x);
	}

	@Override
	public void forEachTask(Consumer<? super Task> action) {
		series.forEach(action);
	}

	/**
	 * TODO only remove tasks which are weaker than the sensor
	 */
	public void clean(List<BeliefTable> tables, int marginCycles) {
		if (!NAL.signal.SIGNAL_TABLE_FILTER_NON_SIGNAL_TEMPORAL_TASKS)
			return;

		long sStart = series.start(), sEnd;
		if (sStart != TIMELESS && (sEnd = series.end()) != TIMELESS) {

			long finalEnd = sEnd - (long) marginCycles, finalStart = sStart + (long) marginCycles;
			if (finalStart < finalEnd) {

				Predicate<Task> cleaner = t -> absorbNonSignal(t, finalStart, finalEnd);

				for (int i = 0, tablesSize = tables.size(); i < tablesSize; i++) {
					TaskTable b = tables.get(i);
					if (b != this && !(b instanceof DynamicTaskTable) && !(b instanceof EternalTable)) {
						((TemporalBeliefTable) b).removeIf(cleaner, finalStart, finalEnd);
					}
				}
			}

		}
	}

	/**
	 * used for if you can cache seriesStart,seriesEnd for a batch of calls
	 * TODO only remove tasks which are weaker than the sensor
	 */
	boolean absorbNonSignal(Task t, long seriesStart, long seriesEnd) {

//        if (t.isGoal())
//            throw new WTF();
		//assert(!t.isGoal());


		if (t.isDeleted())
			return true;

        long tStart = t.start();
		if (tStart == ETERNAL)
			return false;

		if (seriesEnd < tStart)
			return false; //occurs after series ends

        long tEnd = t.end();
		if (seriesStart > tEnd)
			return false; //occurs before series starts

		//intersects with series range:

		if (tEnd > seriesEnd)
			return false; //predicts future of series


		//TODO actually absorb (transfer) the non-series task priority in proportion to the amount predicted, gradually until complete absorption
		//TODO store ranges tested for series rather than keep scanning for each one
		return !series.isEmpty(Math.max(seriesStart, tStart), Math.min(seriesEnd, tEnd));
	}

	public final void add(SeriesTask nextT) {
		series.compress();
		series.push(nextT);
	}

	public final long start() {
		return series.start();
	}

	public final long end() {
		return series.end();
	}

	/** @param dur can be either a perceptual duration which changes, or a 'physical duration' determined by
	 *             the interface itself (ex: clock rate) */
	public SeriesTask add(@Nullable Truth next, When<What> when, Term why) {

		long nextStart = when.start, nextEnd = when.end;

        AbstractTaskSeries<SeriesTask> series = this.series;

        SeriesTask last = series.last();
		if (last != null && lastContinues(next, when.dur, nextStart, nextEnd, series, last))
			return last;
		else {
			if (next == null)
				return null;
			else {
                SeriesTask s = newTask(this.term, this.punc(), nextStart, nextEnd, next, when.x.nar);
				s.why(why);
				this.add(s);
				return s;
			}
		}
	}

	private static boolean lastContinues(@Nullable Truth next, float dur, long nextStart, long nextEnd, AbstractTaskSeries<SeriesTask> series, SeriesTask last) {
        long lastEnd = last.end();

        long gapCycles = (nextStart - lastEnd);

		if ((float) gapCycles <= AbstractTaskSeries.latchDurs() * dur) {

			if (next!=null) {
                long lastStart = last.start();
                long stretchCycles = (nextStart - lastStart);
                boolean stretchable = (float) stretchCycles <= AbstractTaskSeries.stretchDurs() * dur;
				if (stretchable && last.truth().equals(next)) {
					//continue, if not excessively long
					stretch(last, nextEnd);
					return true;
				}
			}

			//form new task either because the value changed, or because the latch duration was exceeded
			if (lastEnd < nextStart- 1L) {
				//stretch the previous to the current starting point for the new task
				stretch(last, nextStart - 1L);
			}

		}
		return false;
	}

	private static SeriesTask newTask(Term term, byte punc, long s, long e, Truth truth, NAL nar) {
		return new SeriesTask(term, punc, truth, s, e, nar.evidence());
	}

	private static void stretch(SeriesTask t, long e) {
//        System.out.println("stretch " + t.end() + " .. " +  e + " (" + (e - t.end()) + " cycles)");
		t.setEnd(e);
	}

	/**
	 * has special equality and hashcode convention allowing the end to stretch;
	 * otherwise it would be seen as unique when tested after stretch
	 */
	public static final class SeriesTask extends TemporalTask implements UnevaluatedTask {

		/**
		 * current endpoint
		 */
		protected long e;

		public SeriesTask(Term term, byte punc, Truth value, long start, long end, long[] stamp) {
			super(SeriesBeliefTable.taskTerm(term), punc, value, start, start, end, stamp);
			if (stamp.length != 1)
				throw new UnsupportedOperationException("requires stamp of length 1 so it can be considered an Input Task and thus have consistent hashing even while its occurrrence time is stretched");
			this.e = end;
			//setCyclic(true); //prevent being immediately image transformed, etc
		}


		@Override
		protected int hashCalculate(long start, long end, long[] stamp) {
			//TODO also involve Term?
			return Util.hashCombine(term.hashCode(), Util.hashCombine(stamp[0], start));
		}


		/**
		 * series tasks can be assumed to be universally unique
		 */
		@Override
		public boolean equals(Object x) {
			return this == x;
//            if (x instanceof SeriesTask) {
//                //TODO also involve Term?
//                Task xx = (Task) x;
//                if (hashCode() != x.hashCode())
//                    return false;
//                return stamp()[0] == xx.stamp()[0] && start() == xx.start() && term().equals(xx.term());
//            }
		}

		public void setEnd(long e) {
			this.e = e;
		}

		@Override
		public long end() {
			return e;
		}

	}


}
