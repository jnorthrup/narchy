package nars.derive.action;

import nars.$;
import nars.Task;
import nars.derive.Derivation;
import nars.term.var.VarPattern;

public abstract class NativeTaskFireAction extends NativePremiseAction {
	protected static final VarPattern TheTask = $.varPattern(1);
	@Deprecated
	public int volMax;

	@Override
	protected final void run(Derivation d) {
		this.volMax = d.nar.termVolMax.intValue();
		accept(d._task, d);
	}

	protected abstract void accept(Task y, Derivation d);
}
