package nars.test.condition;

import nars.NAR;
import nars.Op;
import nars.Task;
import nars.term.Neg;
import nars.term.Term;
import nars.truth.Truthed;
import org.eclipse.collections.api.block.predicate.primitive.LongLongPredicate;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import static java.lang.Float.NaN;

/**
 * specific task matcher with known boundary parameters that can be used in approximate distance ranking calculations
 */
public class DefaultTaskCondition extends TaskCondition {

	protected final NAR nar;
	private final byte punc;
	private final Term term;
	private final LongLongPredicate time;
	private final float freqMin, freqMax;
	private final float confMin, confMax;
	private final long creationStart, creationEnd;


	public DefaultTaskCondition(NAR n, long creationStart, long creationEnd, Term term, byte punc, float freqMin, float freqMax, float confMin, float confMax, LongLongPredicate time) throws RuntimeException {


		if (freqMax < freqMin)
			throw new RuntimeException("freqMax < freqMin");
		if (confMax < confMin) throw new RuntimeException("confMax < confMin");

		if (creationEnd - creationStart < 1)
			throw new RuntimeException("cycleEnd must be after cycleStart by at least 1 cycle");

		this.nar = n;
		this.time = time;

		this.creationStart = creationStart;
		this.creationEnd = creationEnd;

		this.confMax = Math.min(1.0f, confMax);
		this.confMin = Math.max(0.0f, confMin);
		this.punc = punc;

		if (term instanceof Neg) {
			term = term.unneg();
			freqMax = 1f - freqMax;
			freqMin = 1f - freqMin;
			if (freqMin > freqMax) {
				float f = freqMin;
				freqMin = freqMax;
				freqMax = f;
			}
		}

		this.freqMax = Math.min(1.0f, freqMax);
		this.freqMin = Math.max(0.0f, freqMin);

		this.term = term;

	}

	public long getFinalCycle() {
		return creationEnd;
	}


	@Override
	public void log(String label, boolean successCondition, Logger logger) {

		super.log(label, successCondition, logger);

		if (logger.isInfoEnabled()) {
			StringBuilder sb = new StringBuilder((1 + (similar != null ? similar.size() : 0)) * 2048);
			if (firstMatch != null) {
				sb.append("Exact match:\n");
				log(firstMatch, sb);
			} else if (similar != null && !similar.isEmpty()) {
				sb.append("Similar matches:\n");
				for (Task s : similar)
					log(s, sb);
			}
			logger.info(sb.toString());
		}
	}

	public void log(Task t, StringBuilder sb) {
		nar.proof(t, sb);
	}

	@Override
	public String toString() {
		return term.toString() + ((char) punc) + " %" +
			rangeStringN2(freqMin, freqMax) + ';' + rangeStringN2(confMin, confMax) + '%' + ' ' +
			" creation: (" + creationStart + ',' + creationEnd + ')';
	}


	@Override
	public boolean matches(@Nullable Task t) {

		byte punc = this.punc;
		if (t.punc() == punc) {
			if (occurrenceTimeMatches(t) && creationTimeMatches()) {
				if (((punc != Op.BELIEF) && (punc != Op.GOAL)) || truthMatches(t)) {
					Term tt = t.term();
					if (tt.equals(this.term)) {
						firstMatch = t;
						return true;
					}/* else {
						if (NAL.DEBUG) {
							if (tt.opID() == term.opID() && tt.volume() == this.term.volume() && tt.structure() == this.term.structure() && this.term.toString().equals(tt.toString())) {
								throw new RuntimeException("target construction problem: " + this.term + " .toString() is equal to " + tt + " but inequal otherwise");
							}
						}
					}*/
				}
			}
		}

		if (similar != null)
			similar.accept(t);

		return false;
	}

	private boolean creationTimeMatches() {
		long now = nar.time();
		return now >= creationStart && now <= creationEnd;
	}

	private boolean occurrenceTimeMatches(Task t) {
		return time.accept(t.start(), t.end());
	}

	private boolean truthMatches(Truthed tt) {

		float co = tt.conf();
		if ((co > confMax) || (co < confMin))
			return false;

		float fr = tt.freq();
		return (fr <= freqMax && fr >= freqMin);

	}

	@Override
	protected float value(Task task, float worstDiffNeg) {

		float worstDiff = -worstDiffNeg;

		float difference = 0;
		if (task.punc() != punc)
			difference += 1000;
		if (difference >= worstDiff)
			return NaN;

		Term tterm = task.term();
		difference +=
			100 * termDistance(tterm, term, worstDiff);
		if (difference >= worstDiff)
			return NaN;

		if (task.isBeliefOrGoal()) {
			float f = task.freq();
			float freqDiff = Math.min(
				Math.abs(f - freqMin),
				Math.abs(f - freqMax));
			difference += 10 * freqDiff;
			if (difference >= worstDiff)
				return NaN;

			float c = task.conf();
			float confDiff = Math.min(
				Math.abs(c - confMin),
				Math.abs(c - confMax));
			difference += 1 * confDiff;
			if (difference >= worstDiff)
				return NaN;
		}

		difference += 0.5f * (Math.abs(task.hashCode()) / (Integer.MAX_VALUE * 2.0f)); //HACK differentiate by hashcode

		return -difference;

	}


}
