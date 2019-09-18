package nars.game.sensor;

import jcog.math.FloatRange;
import nars.NAR;
import nars.attention.AttnBranch;
import nars.table.BeliefTable;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** an independent scalar (1-dimensional) signal */
public abstract class UniSignal extends Signal {
	private final FloatRange res;

	protected final short[] cause;
	public final AttnBranch pri;

	public UniSignal(Term term, @Nullable short[] cause, BeliefTable beliefTable, BeliefTable goalTable, NAR n) {
		super(term, beliefTable, goalTable, n);

		this.cause = cause == null ? new short[]{n.newCause(term).id} : cause;

		this.res = FloatRange.unit(n.freqResolution);

		this.pri = newAttn(term);
	}

	protected AttnBranch newAttn(Term term) {
		return new AttnBranch(term, List.of(this));
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
