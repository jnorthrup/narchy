package nars.gui;

import jcog.event.Off;
import jcog.math.v2;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import jcog.pri.bag.impl.PLinkArrayBag;
import jcog.pri.op.PriMerge;
import nars.NAR;
import nars.Task;
import spacegraph.space2d.container.ScrollXY;
import spacegraph.space2d.container.grid.ArrayBagGridModel;
import spacegraph.space2d.container.grid.GridRenderer;
import spacegraph.space2d.container.unit.MutableUnitContainer;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.util.math.Color4f;

public class TaskListView extends MutableUnitContainer {
	final PLinkArrayBag<Task> bag;
	private final NAR nar;

	float rate = 0.01f;
	private Off onTask;

	public TaskListView(NAR nar, int capacity) {
		this.nar = nar;
		bag = new PLinkArrayBag<>(PriMerge.plus, capacity);

		GridRenderer<PriReference<Task>> taskRenderer = (x, y, t) -> {
			Task tt = t.get();
			VectorLabel l = new VectorLabel(tt.toStringWithoutBudget());
			Color4f color = l.fgColor;
			float linkPri = t.pri();
			color.x = linkPri;
			color.y = 1-linkPri;
			color.z = tt.priElseZero();
			return l;
		};

		ScrollXY<ScrollXY.ScrolledXY> scroll = new ScrollXY<>(new ArrayBagGridModel<>(bag), taskRenderer);
		scroll.viewMinMax(new v2(1,1), new v2(1, capacity));
		scroll.view(1, Math.min(bag.capacity(), 32));

		set(DurSurface.get(scroll, nar, ()->{
			bag.commit();
			if (visible()) {
				scroll.update();
			}
		}) );
	}

	@Override
	protected void starting() {
		super.starting();
		onTask = nar.onTask(x -> {
			//TODO only if visible
			bag.putAsync(new PLink<>(x, x.priElseZero()*rate));
		});
	}

	@Override
	protected void stopping() {
		onTask.close();
		onTask = null;
		super.stopping();
	}

}
