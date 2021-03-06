package nars.task;

import jcog.data.array.LongArrays;
import jcog.pri.Prioritized;
import jcog.pri.UnitPri;
import nars.NAL;
import nars.Op;
import nars.Task;
import nars.task.util.TaskException;
import nars.term.Compound;
import nars.term.Term;
import nars.term.util.TermedDelegate;
import nars.truth.Truth;
import nars.truth.Truthed;
import nars.util.Timed;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;

import static nars.$.*;
import static nars.Op.*;
import static nars.time.Tense.ETERNAL;

/**
 * Default Task implementation
 * TODO move all mutable methods to TaskBuilder and call this ImTaskBuilder
 * <p>
 * NOTE:
 * if evidence length == 1 (input) then do not include
 * truth or occurrence time as part of the hash, equality, and
 * comparison tests.
 * <p>
 * this allows an input task to modify itself in these two
 * fields without changing its hash and equality consistency.
 * <p>
 * once input, input tasks will have unique serial numbers anyway
 */
@Deprecated
public class TaskBuilder extends UnitPri implements TermedDelegate, Function<NAL, Task>, Truthed {


	private Term term;

	protected final byte punc;

	private @Nullable Truth truth;

	private @Nullable long[] evidence = LongArrays.EMPTY_ARRAY;

	private long creation = ETERNAL;
	private long start = ETERNAL;
    private long end = ETERNAL;


	public TaskBuilder(Term t, byte punct, float freq, NAL nar) throws TaskException {
		this(t, punct, INSTANCE.t(freq, nar.confDefault(punct)));
	}

	public TaskBuilder(Term term, byte punct, @Nullable Truth truth) throws TaskException {
		this(term, punct, truth,
			/* budget: */ (float) 0, Float.NaN);
	}

	public TaskBuilder(Term term, byte punctuation /* TODO byte */, @Nullable Truth truth, float p, float q) throws TaskException {
		super((float) 0);
		pri(p);

		this.punc = punctuation;


        Term tt = term.term();
		if (tt.op() == Op.NEG) {
            Term nt = tt.sub(0);
			if (nt instanceof Compound) {
				tt = nt;

				if ((int) punctuation == (int) Op.BELIEF || (int) punctuation == (int) Op.GOAL)
					truth = truth.neg();
			} else {
				throw new TaskException("Top-level negation", this);
			}
		}


		this.truth = truth;
		this.term = tt;
	}


	public boolean isInput() {
		return evidence().length <= 1;
	}

	@Override
	public Task apply(NAL n) throws TaskException {

		if (isDeleted())
			throw new TaskException("Deleted", this);

        Term t = term;

        byte punc = punc();
		if ((int) punc == 0)
			throw new TaskException("Unspecified punctuation", this);

        Term cntt = t.normalize();//.the();
		if (cntt == null)
			throw new TaskException("Failed normalization", t);

		if (!Task.validTaskTerm(cntt, punc, !isInput() && !NAL.DEBUG))
			throw new TaskException("Invalid content", cntt);

		if (cntt != t) {
			this.term = cntt;
		}


		switch (punc()) {
			case BELIEF:
			case GOAL:
				if (truth == null) {

					setTruth(INSTANCE.t(1.0F, n.confDefault(punc)));
				} else {

                    float confLimit = 1f - NAL.truth.TRUTH_EPSILON;
					if (!isInput() && conf() > confLimit) {

						setTruth(INSTANCE.t(freq(), confLimit));
					}
				}

				break;
			case QUEST:
			case QUESTION:
				if (truth != null)
					throw new RuntimeException("quests and questions must have null truth");
				break;
			case COMMAND:
				break;

			default:
				throw new UnsupportedOperationException("invalid punctuation: " + punc);

		}


		if (evidence.length == 0)
			setEvidence(n.time.nextStamp());


		if (creation() == ETERNAL) {
			this.creation = n.time();
		}


        float pp = priElseNeg1();
		if (pp < (float) 0) {
			pri(n.priDefault(punc));
		}


		Truth tFinal = truth != null ? Truth.theDithered(truth.freq(), truth.evi(), n) : null;

		Task i = NALTask.the(term, punc, tFinal, creation, start, end, evidence);
		i.pri(this);


		return i;
	}


	@Override
	public final Term term() {
		return term;
	}

	public float freq() { return truth.freq(); }

	@Override
	public double evi() {
		return truth.evi();
	}

	public boolean isBeliefOrGoal() {
		return (int) punc == (int) Op.BELIEF || (int) punc == (int) Op.GOAL;
	}

	public boolean isCommand() {
		return (int) punc == (int) Op.COMMAND;
	}

	public final @Nullable Truth truth() {
		return truth;
	}

	private void setTruth(@Nullable Truth t) {

		if (t == null && isBeliefOrGoal())
			throw new TaskException("null truth for belief or goal", this);

		if (!Objects.equals(truth, t)) {
			truth = t;
		}
	}


	/**
	 * the evidence should be sorted and de-duplicaed prior to calling this
	 */

	private TaskBuilder setEvidence(@Nullable long... evidentialSet) {
		this.evidence = evidentialSet;
		return this;
	}

	public final byte punc() {
		return punc;
	}


	public final long[] evidence() {
		return this.evidence;
	}

	public final long creation() {
		return creation;
	}

	public final long start() {
		return start;
	}


	private TaskBuilder setCreationTime(long creationTime) {


		this.creation = creationTime;


		return this;
	}

	/**
	 * TODO for external use in TaskBuilder instances only
	 */
	private void setStart(long o) {
		this.start = o;
	}

	/**
	 * TODO for external use in TaskBuilder instances only
	 */
	private void setEnd(long o) {
		if (o != end) {
			if (start == ETERNAL && o != ETERNAL)
				throw new RuntimeException("can not setAt end time for eternal task");
			if (o < start)
				throw new RuntimeException("end must be equal to or greater than start");

			this.end = o;
		}
	}


	@Override
	public final int hashCode() {
		throw new UnsupportedOperationException();


	}

	/**
	 * To check whether two sentences are equal
	 * Must be consistent with the values calculated in getHash()
	 *
	 * @param that The other sentence
	 * @return Whether the two sentences have the same content
	 */
	@Override
	public final boolean equals(@Nullable Object that) {
		throw new UnsupportedOperationException();
	}






    /*
    @Override
    public void delete() {
        super.delete();






    }*/


	/**
	 * end occurrence
	 */
	public final long end() {

		return end;


	}


	public final TaskBuilder present(Timed timed) {
		return time(timed.time());
	}


	public final TaskBuilder time(Timed timed, int dt) {
		return time(timed.time() + (long) dt);
	}


	public final TaskBuilder time(long when) {
		setStart(when);
		setEnd(when);
		return this;
	}

	public final TaskBuilder time(long start, long end) {
		setStart(start);
		setEnd(end);
		return this;
	}

	public TaskBuilder time(long creationTime, long start, long end) {
		setCreationTime(creationTime);
		setStart(start);
		setEnd(end);
		return this;
	}


	public final TaskBuilder occurr(long occurrenceTime) {
		setStart(occurrenceTime);
		setEnd(occurrenceTime);
		return this;
	}


	public TaskBuilder eternal() {
		setStart(ETERNAL);
		setEnd(ETERNAL);
		return this;
	}


	public final TaskBuilder evidence(long... evi) {
		setEvidence(evi);
		return this;
	}

	public final TaskBuilder evidence(Task evidenceToCopy) {
		return evidence(evidenceToCopy.stamp());
	}

	@Override
	public TaskBuilder withPri(float p) {
		pri(p);
		return this;
	}


	public final TaskBuilder pri(Prioritized bb) {
		super.pri(bb);
		return this;
	}


}
