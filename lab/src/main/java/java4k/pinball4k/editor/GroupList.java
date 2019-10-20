package java4k.pinball4k.editor;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;

public class GroupList implements ListModel {
	private final ArrayList<ArrayList<LevelObject>> groups = new ArrayList<>();
	
	private final ArrayList<ListDataListener> listeners = new ArrayList<>();

	public void add(ArrayList<LevelObject> obj) {
		groups.add(obj);
		
		for (var l : listeners) {
			l.intervalAdded(new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, 0, groups.size()));
		}
	}
	
	public void remove(LevelObject obj) {
		for (var group : groups) {
			group.remove(obj);
		}

		for (var l : listeners) {
			l.contentsChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, groups.size()));
		}
	}
	
	/**
	 * Removes the specified group.
	 * @param group the group to remove
	 */
	public void remove(ArrayList<LevelObject> group) {
		groups.remove(group);

		for (var l : listeners) {
			l.intervalRemoved(new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, 0, groups.size()));
		}
	}
	
	public void move(int idx, boolean up) {
		System.out.println(getClass() + " move(" + idx +", " + (up ? "up" : "down"));
		if (up) {
			if (idx < 1 || idx >= groups.size()) {
				return;
			}
		} else {
			if (idx < 0 || idx >= groups.size() - 1) {
				return;
			}
		}
		var obj = groups.remove(idx);
		groups.add((up ? idx - 1 : idx + 1), obj);
		
		for (var l : listeners) {
			l.contentsChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, groups.size()));
		}
	}
	
	/*
	 *  (non-Javadoc)
	 * @see javax.swing.ListModel#getSize()
	 */
	@Override
    public int getSize() {
		return groups.size();
	}

	/*
	 *  (non-Javadoc)
	 * @see javax.swing.ListModel#addListDataListener(javax.swing.event.ListDataListener)
	 */
	@Override
    public void addListDataListener(ListDataListener l) {
		listeners.add(l);
	}

	/*
	 *  (non-Javadoc)
	 * @see javax.swing.ListModel#getElementAt(int)
	 */
	@Override
    public Object getElementAt(int index) {
		return groups.get(index);
	}

	/*
	 *  (non-Javadoc)
	 * @see javax.swing.ListModel#removeListDataListener(javax.swing.event.ListDataListener)
	 */
	@Override
    public void removeListDataListener(ListDataListener l) {
		listeners.remove(l);
	}

}
