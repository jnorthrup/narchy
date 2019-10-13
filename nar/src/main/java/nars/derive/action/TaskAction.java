package nars.derive.action;

import nars.Task;
import nars.derive.Derivation;
import nars.derive.rule.RuleCause;

public abstract class TaskAction extends NativeHow {
	@Deprecated public int volMax;

	protected TaskAction() {
	}

	@Override
	protected final void run(RuleCause why, Derivation d) {
		this.volMax = d.termVolMax; //HACK
		accept(d._task, why, d);
	}

	protected abstract void accept(Task x, RuleCause why, Derivation d);
}
