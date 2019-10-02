package nars.derive.action;

import nars.Task;
import nars.derive.Derivation;
import nars.derive.rule.RuleWhy;

public abstract class TaskAction extends NativePremiseAction {
	@Deprecated public int volMax;

	protected TaskAction() {
	}

	@Override
	protected final void run(RuleWhy why, Derivation d) {
		this.volMax = d.nar.termVolMax.intValue();
		accept(why, d._task, d);
	}

	protected abstract void accept(RuleWhy why, Task y, Derivation d);
}
