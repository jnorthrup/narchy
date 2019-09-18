package nars.derive.op;

import nars.derive.PreDerivation;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;

public class TaskBeliefTermsEqual extends AbstractPred<PreDerivation> {

	public static final TaskBeliefTermsEqual the = new TaskBeliefTermsEqual();

	private TaskBeliefTermsEqual() {
		super(Atomic.atom(TaskBeliefTermsEqual.class.getSimpleName()));
	}

	@Override
	public boolean test(PreDerivation d) {
		return d.taskTerm.equals(d.beliefTerm);
	}

	@Override
	public float cost() {
		return 0.001f;
	}
}
