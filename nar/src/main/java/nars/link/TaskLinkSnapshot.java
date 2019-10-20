package nars.link;

import jcog.data.list.FasterList;
import jcog.data.list.table.Table;

import java.util.Arrays;

/** takes snapshots of a tasklink bag */
public abstract class TaskLinkSnapshot implements Runnable {

	private final Table<?, TaskLink> active;
	protected final FasterList<TaskLink> snapshot;
	protected transient TaskLink[] items = TaskLink.EmptyTaskLinkArray;

	public TaskLinkSnapshot(Table<?, TaskLink> active) {
		this.active = active;
		this.snapshot = new FasterList<>(0, new TaskLink[active.capacity()]);
	}

	@Override
	public final void run() {
		synchronized (snapshot) {
			snapshot.clearFast();
			var c = active.capacity();
			//TODO resize bitmap
			snapshot.ensureCapacity(c);
			active.forEach(snapshot::addFast);
			items = snapshot.array();
			int s = snapshot.size();
			if (s < c)
				Arrays.fill(items, s, items.length,null); //clear remainder of array
			next();
		}
	}

	abstract protected void next();

}
