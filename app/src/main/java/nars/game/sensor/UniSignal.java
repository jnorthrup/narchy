package nars.game.sensor;

import jcog.math.FloatRange;
import nars.NAR;
import nars.attention.AttnBranch;
import nars.attention.PriNode;
import nars.table.BeliefTable;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

/** an independent scalar (1-dimensional) signal */
public abstract class UniSignal extends Signal {
	private final FloatRange res;

	protected final short[] cause;
	public final PriNode pri;

	public UniSignal(Term term, @Nullable short[] cause, BeliefTable beliefTable, BeliefTable goalTable, NAR n) {
		this(term, cause, beliefTable, goalTable, null, n);
	}
	public UniSignal(Term term, @Nullable short[] cause, BeliefTable beliefTable, BeliefTable goalTable, PriNode pri, NAR n) {
		super(term, beliefTable, goalTable, n);

		this.cause = cause == null ? new short[]{n.newCause(term).id} : cause;

		this.res = FloatRange.unit(n.freqResolution);

		this.pri = pri!=null ? pri : new AttnBranch(term, components());
	}

	public float pri() {
		return pri.pri();
	}

	@Override
	public final FloatRange resolution() {
		return res;
	}

	public <S extends UniSignal> S resolution(float v) {
		res.set(v);
		return (S)this;
	}

	public short[] cause() {
		return cause;
	}
}
