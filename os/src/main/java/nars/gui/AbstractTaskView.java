package nars.gui;

import jcog.event.Off;
import jcog.math.v2;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import jcog.pri.bag.Bag;
import jcog.pri.bag.impl.PLinkArrayBag;
import jcog.pri.bag.impl.SimpleBufferedBag;
import jcog.pri.op.PriMerge;
import nars.Task;
import nars.attention.What;
import spacegraph.space2d.container.ScrollXY;
import spacegraph.space2d.container.grid.ArrayBagGridModel;
import spacegraph.space2d.container.grid.GridRenderer;
import spacegraph.space2d.container.unit.MutableUnitContainer;

import java.util.function.Consumer;

public abstract class AbstractTaskView<X> extends MutableUnitContainer implements GridRenderer<PriReference<X>>, Consumer<Task> {
	final Bag<X, PriReference<X>> bag;
	private final What what;
	private final ScrollXY<ScrollXY.ScrolledXY> scroll;

	float rate;
	private Off onTask;

	public AbstractTaskView(What w, PriMerge merge, int capacity) {
		this.what = w;

		rate = 1f/capacity;

		PLinkArrayBag<X> _bag = new PLinkArrayBag<>(merge, capacity);
		bag = new SimpleBufferedBag<>(_bag);

		scroll = new ScrollXY<>(new ArrayBagGridModel<>(_bag), this);
		scroll.viewMinMax(new v2(1,1), new v2(1, capacity));
		scroll.view(1, Math.min(bag.capacity(), 32));

		set(DurSurface.get(scroll, w.nar, this::commit) );
	}

	private void commit() {
		bag.commit();
		if (visible()) {
			scroll.update();
		}
	}


	@Override
	protected void starting() {
		super.starting();
		onTask = what.onTask(this);
	}

	@Override
	public final void accept(Task x) {
		//TODO option for only if visible
		if (filter(x))
			bag.putAsync(new PLink<>(transform(x), x.priElseZero()*rate));
	}

	protected abstract X transform(Task x);

	protected static boolean filter(Task x) {
		return true;
	}

	@Override
	protected void stopping() {
		onTask.close();
		onTask = null;
		super.stopping();
	}

}
