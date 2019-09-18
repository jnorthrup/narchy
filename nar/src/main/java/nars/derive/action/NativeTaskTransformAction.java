package nars.derive.action;

import nars.NAR;
import nars.Task;
import nars.control.Why;
import nars.derive.Derivation;
import nars.task.NALTask;
import org.jetbrains.annotations.Nullable;

public abstract class NativeTaskTransformAction extends NativeTaskFireAction {

	private final short[] cause;
	private final Why why;

	public NativeTaskTransformAction(NAR n) {
		this.why = n.newCause(this);
		this.cause = new short[] { why.id };
		taskPattern(TheTask);
		beliefPattern(TheTask);
		taskPunc(true, true, true, true);
	}

	@Override public final float pri(Derivation d) {
		return 0.5f + why.value(); //TODO
	}

	@Override
	protected final void accept(Task x, Derivation d) {
		Task y = transform(x, d);
		if (y == null)
			return;

		((NALTask)y).cause(cause);
		d.remember(y);
	}

	@Nullable
	abstract protected Task transform(Task x, Derivation d);

}
