package nars.derive.action;

import nars.NAR;
import nars.Task;
import nars.control.Why;
import nars.derive.Derivation;
import nars.task.NALTask;
import org.jetbrains.annotations.Nullable;

import static nars.Op.*;

public abstract class TaskTransformAction extends TaskAction {

	private final short[] cause;
	private final Why why;

	public TaskTransformAction(NAR n) {
		this(n, BELIEF, GOAL, QUESTION, QUEST);
	}

	public TaskTransformAction(NAR n, byte... puncs) {
		this.why = n.newCause(this);
		this.cause = new short[] { why.id };
		single();
		taskPunc(puncs);
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
