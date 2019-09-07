package jcog.exe.realtime;

import jcog.TODO;
import jcog.Util;
import org.jctools.queues.atomic.MpscAtomicArrayQueue;

import java.util.Queue;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import static jcog.exe.realtime.TimedFuture.*;

public class QueueWheelModel extends HashedWheelTimer.WheelModel {

	/**
	 * the wheels (array of queues)
	 */
	final Queue<TimedFuture>[] q;

	/** thread-safe */
	public QueueWheelModel(int wheels, long resolution) {
		this(wheels, resolution, ()->new MpscAtomicArrayQueue<>(32));
	}

	public QueueWheelModel(int wheels, long resolution, Supplier<Queue<TimedFuture>> queueBuilder) {
		super(wheels, resolution);
		assert (wheels > 1);
		q = new Queue[wheels];
		for (int i = 0; i < wheels; i++)
			q[i] = queueBuilder.get();
	}

	@Override
	public int run(int c, HashedWheelTimer timer) {


		//TODO if n=2 and the previous or next queue is empty try moving one of the items there. this will distribute items across wheels so each has an ideal 0 or 1 size

		int n = 0, limit = Integer.MAX_VALUE;
		Queue<TimedFuture> q = this.q[c];

		TimedFuture r;
		while ((r = q.poll()) != null) {
			n++;
			switch (r.state()) {
				case CANCELLED:
					/* nop */
					break;
				case READY:
					r.execute(timer);
					break;
				case PENDING:
				    if (limit == Integer.MAX_VALUE)
                        limit = n + q.size(); //defer calculating queue size until the first reinsert otherwise it will keep polling what is offered in this loop
					//re-insert
					if (!q.offer(r)) {
						if (!this.q[(c+1)%wheels].offer(r)) { //try the next
							throw new TODO(); //TODO try all other queues in sequence
						}
					}
					break;
			}
			if (n >= limit)
			    break; //exit before polling PENDING tasks recycled to the back of the queue
		}

		return n;
	}

	@Override
	public boolean accept(TimedFuture<?> r, HashedWheelTimer t) {
		return t.reschedule(r); //immediately
	}

	@Override
	public boolean reschedule(int wheel, TimedFuture r) {

		int remain = q.length - 1;
		do {
			if (q[wheel].offer(r))
				return true;
			if (++wheel == q.length) wheel = 0;
		} while (--remain > 0);

		return false;
	}

	@Override
	public int size() {
		return Util.sum((ToIntFunction<Queue>) Queue::size, q);
	}

	@Override
	public boolean isEmpty() {
		for (Queue<TimedFuture> q : q)
			if (!q.isEmpty())
				return false;
		return true;
	}


}
