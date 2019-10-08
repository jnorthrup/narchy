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

	protected final Term why;
	public final PriNode pri;

	public UniSignal(Term term, @Nullable Term why, BeliefTable beliefTable, BeliefTable goalTable, NAR n) {
		this(term, why, beliefTable, goalTable, null, n);
	}
	public UniSignal(Term term, @Nullable Term why, BeliefTable beliefTable, BeliefTable goalTable, PriNode pri, NAR n) {
		super(term, beliefTable, goalTable, n);

		this.why = why == null ? n.newCause(term).why : why;

		this.res = FloatRange.unit(n.freqResolution);

		this.pri = pri!=null ? pri : new AttnBranch(term, components());
	}

	public final float pri() {
		return pri.pri();
	}

	public final FloatRange resolution() {
		return res;
	}

	public final <S extends UniSignal> S resolution(float v) {
		res.set(v);
		return (S)this;
	}

	public final Term why() {
		return why;
	}
}
