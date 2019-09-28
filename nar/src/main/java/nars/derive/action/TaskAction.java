package nars.derive.action;

import nars.Task;
import nars.derive.Derivation;

public abstract class TaskAction extends NativePremiseAction {
	@Deprecated public int volMax;

	protected TaskAction() {
	}

	@Override
	protected final void run(Derivation d) {
		this.volMax = d.nar.termVolMax.intValue();
		accept(d._task, d);
	}

	protected abstract void accept(Task y, Derivation d);
}
