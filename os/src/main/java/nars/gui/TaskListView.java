package nars.gui;

import jcog.pri.PriReference;
import jcog.pri.op.PriMerge;
import nars.Task;
import nars.attention.What;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.util.math.Color4f;

public class TaskListView extends AbstractTaskView<Task> {

	public TaskListView(What w, int capacity) {
		super(w, PriMerge.plus, capacity);
	}

	@Override
	protected Task transform(Task x) {
		return x;
	}

	@Override
	public Surface apply(int x, int y, PriReference<Task> t) {
		var tt = t.get();
		var l = new VectorLabel(tt.toStringWithoutBudget());
		var color = l.fgColor;
		var linkPri = t.pri();
		color.x = linkPri;
		color.y = 1 - linkPri;
		color.z = tt.priElseZero();
		return l;
	}

}