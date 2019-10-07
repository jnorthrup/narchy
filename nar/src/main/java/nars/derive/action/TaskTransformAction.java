package nars.derive.action;

import nars.Task;
import nars.derive.Derivation;
import nars.derive.rule.RuleCause;
import nars.task.AbstractTask;
import org.jetbrains.annotations.Nullable;

import static nars.Op.*;

public abstract class TaskTransformAction extends TaskAction {


	public TaskTransformAction() {
		this(BELIEF, GOAL, QUESTION, QUEST);
	}

	public TaskTransformAction(byte... puncs) {
		single();
		taskPunc(puncs);
	}

	@Override public final float pri(Derivation d) {
		return 1;
	}

	@Override
	protected final void accept(RuleCause why, Task x, Derivation d) {
		Task y = transform(x, d);
		if (y != null)
			d.remember(((AbstractTask) y).why(why.why(d)));
	}

	@Nullable
	abstract protected Task transform(Task x, Derivation d);

}
