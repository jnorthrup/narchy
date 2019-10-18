package nars.task;

import jcog.pri.UnitPri;
import nars.NAL;
import nars.Task;
import nars.control.Why;
import nars.term.Term;

public abstract class AbstractTask extends UnitPri implements Task {

	public Term why;
	protected boolean cyclic;
	public long creation; //TODO protected or private


	/** merges with any existing why */
	public Task why(Term why) {
		this.why = Why.why(this.why, why, NAL.causeCapacity.intValue());
		return this;
	}

	@Override
	public Term why() {
		return why;
	}

	@Override
	public final void setCyclic(boolean c) {
		this.cyclic = c;
	}


	@Override
	public final boolean isCyclic() {
		return cyclic;
	}
	@Override
	public final void setCreation(long creation) {
		this.creation = creation;
	}

}
