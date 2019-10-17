package nars.task;

import jcog.pri.UnitPri;
import nars.NAL;
import nars.Task;
import nars.control.Why;
import nars.term.Term;

public abstract class AbstractTask extends UnitPri implements Task {

	public Term why;


	/** merges with any existing why */
	public AbstractTask why(Term why) {
		this.why = Why.why(this.why, why, NAL.causeCapacity.intValue());
		return this;
	}

	@Override
	public Term why() {
		return why;
	}

}
