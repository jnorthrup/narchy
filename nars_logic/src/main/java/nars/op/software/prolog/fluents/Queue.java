package nars.op.software.prolog.fluents;

import nars.op.software.prolog.io.IO;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Generic dynamic Queeue with (amortized) O(1) enq/deq (add and remove)
 * operations
 */
public class Queue implements Cloneable {
	final static int MIN_QUEUE = 4;

	final static int MAX_QUEUE = 1 << 24;

	private int head, tail;

	// private boolean busy;

	private Object queue[];

	public Queue(int size) {
		makeIt(size);
	}

	public Queue() {
		this(0);
	}

	public Queue(@NotNull ArrayList V) {
		this(V.size() + MIN_QUEUE);
		for (int i = 0; i < V.size(); i++) {
			enq(V.get(i));
		}
	}

	// synchronized
	private final void makeIt(int size) {
		size = (size < MIN_QUEUE) ? MIN_QUEUE : size;
		queue = new Object[size];
		head = tail = 0;
		// busy=false;
	}

	private final int count() {
		return (head <= tail) ? tail - head : queue.length - head + tail;
	}

	/**
	 * Dynamically resizes the queue
	 */
	private final boolean requeue(String Mes) {
		int newSize = 2 * count();
		if (newSize > MAX_QUEUE || newSize < MIN_QUEUE)
			return false;
		if (IO.trace())
			IO.trace(Mes + "!!! " + toString());
		Object[] nqueue = new Object[newSize];
		int j = 0;
		for (int i = head; i != tail; i = inc(i))
			nqueue[j++] = queue[i];
		queue = nqueue;
		head = 0;
		tail = j;
		return true;
	}

	/*
	 * private final void enterCritical() { while(busy) { try { wait(); }
	 * catch(InterruptedException e) { } } busy=true; }
	 * 
	 * private final void exitCritical() { busy=false; notifyAll(); }
	 */
	/**
	 * Adds an element to the end of the queue
	 */
	// synchronized
	public final boolean enq(Object V) {
		// enterCritical();
		if (inc(tail) == head) { // full !!!
			if (!requeue("expanding")) {
				IO.error("Warning: queue overflow at:" + V);
				return false;
			}
		}
		queue[tail] = V;
		tail = inc(tail);
		// exitCritical();
		return true;
	}

	/**
	 * Removes the first element of the queue
	 */
	// synchronized
	public final Object deq() {
		// enterCritical();
		if (tail == head) // empty !!!
			return null;
		if (4 * count() < queue.length)
			requeue("shrinking");
		Object V = queue[head];
		head = inc(head);
		// exitCritical();
		return V;
	}

	private final int inc(int val) {
		return (val + 1) % queue.length;
	}

	// synchronized
	public final boolean isEmpty() {
		boolean empty;
		// enterCritical();
		empty = tail == head;
		// exitCritical();
		return empty;
	}

	// synchronized
	@NotNull
	public ArrayList toVector() {
		// enterCritical(); DEADLOCKS!
		ArrayList v = new ArrayList();
		for (int i = head; i != tail; i = inc(i))
			v.add(queue[i]);
		// exitCritical();
		return v;
	}

	@NotNull
	public Iterator toEnumeration() {
		return toVector().iterator();
	}

	@NotNull
	public String toString() {
		return count() + "/" + queue.length + "=>" + toVector().toString();
	}

	/*
	 * //synchronized public Queue toClone() { Queue R=null; try {
	 * R=(Queue)clone(); } catch(CloneNotSupportedException e) {
	 * IO.errmes("Queue:toClone() "+e); } return R; }
	 */

}

/*
 * class QueueIterator { QueueIterator(Queue Q) { this.Q=Q.toClone(); }
 * 
 * private Queue Q;
 * 
 * Object getNext() { return Q.deq(); } }
 */