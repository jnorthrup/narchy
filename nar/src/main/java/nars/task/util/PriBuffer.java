package nars.task.util;

import jcog.event.Off;
import jcog.math.FloatRange;
import jcog.math.IntRange;
import jcog.pri.PriMap;
import jcog.pri.Prioritizable;
import jcog.pri.bag.Bag;
import jcog.pri.bag.impl.PriArrayBag;
import jcog.pri.bag.impl.SimpleBufferedBag;
import jcog.pri.op.PriMerge;
import jcog.pri.op.PriReturn;
import jcog.util.ConsumerX;
import nars.NAL;
import nars.NAR;
import nars.Task;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static jcog.pri.PriMap.newMap;

/**
 * regulates a flow of supplied tasks to a target consumer
 * TODO some of this can be moved to util/
 */
public abstract class PriBuffer<T extends Prioritizable> implements Consumer<T> {


	/**
	 * returns
	 * true if the implementation will manage async target suppliying,
	 * false if it needs periodic flushing
	 */
	public abstract void start(ConsumerX<T> target, NAR nar);

	public void stop() {
	}

	/**
	 * returns the input task, or the existing task if a pending duplicate was present
	 */
	public abstract void put(T x);


	public abstract void clear();

	/**
	 * known or estimated number of tasks present
	 */
	public abstract int size();

	public abstract int capacity();

	/**
	 * estimate current utilization: size/capacity (as a value between 0 and 100%)
	 */
	public final float load() {
		return ((float) size()) / capacity();
	}

	//TODO
	//final AtomicLong in = new AtomicLong(), out = new AtomicLong(), drop = new AtomicLong(), merge = new AtomicLong();

	@Override
	public final void accept(T task) {
		put(task);
	}


	/**
	 * pass-thru, no buffering
	 */
	public static class DirectTaskBuffer<T extends Prioritizable> extends PriBuffer<T> {

		private Consumer<T> each = null;

		public DirectTaskBuffer() {
		}


		@Override
		public final void put(T x) {
			each.accept(x);
		}

		@Override
		public void start(ConsumerX<T> target, NAR nar) {
			each = target;
		}

		@Override
		public void stop() {
			each = null;
		}

		@Override
		public void clear() {

		}

		@Override
		public int size() {
			return 0;
		}

		@Override
		public final int capacity() {
			return Integer.MAX_VALUE;
		}

	}

//    /**
//     * TODO
//     * when the concept index churn rate is low, use the Map to conceptualize quickly
//     * when the concept index churn rate is high, use the Bag with controlled throughput rate
//     * acting as the original NoveltyBag did in OpenNARS
//     * using this the system can shift energy towards exploration (more conceptualization) OR
//     * towards more refined truth (more selectivity in task generation with regard to relatively
//     * stable set of concepts they would add to)
//     */
//    public static abstract class AdaptiveTaskBuffer extends PriBuffer {
//
//    }

	/**
	 * buffers in a Map<> for de-duplication prior to a commit that flushes them as input to NAR
	 * does not obey adviced capacity
	 * TODO find old implementation and re-implement this
	 */
	public static class MapTaskBuffer<X extends Task> extends SyncPriBuffer<X> {

		final AtomicLong hit = new AtomicLong(0);
		final AtomicLong miss = new AtomicLong(0);

		private final Map<X, X> tasks;

		public MapTaskBuffer() {
			tasks = newMap(true);
		}

		@Override
		public int capacity() {
			return Integer.MAX_VALUE;
		}

		@Override
		public void clear() {
			tasks.clear();
		}

		@Override
		public int size() {
			return tasks.size();
		}

		@Override
		public final void put(X n) {
            X p = tasks.putIfAbsent(n, n);
			if (p != null) {
				Task.merge(p, n);
				hit.incrementAndGet();
			} else {
				miss.incrementAndGet();
			}
		}

		/**
		 * TODO time-sensitive
		 */
		@Override
		public void commit() {
            int num = tasks.size();
			if (num > 0) {
                Iterator<X> ii = tasks.values().iterator();
				while (ii.hasNext()) {
                    X r = ii.next();
					ii.remove();
					target.accept(r);
					if (--num <= 0)
						break; //enough
				}
			}
		}


	}

	public abstract static class SyncPriBuffer<X extends Prioritizable> extends PriBuffer<X> {
		protected ConsumerX<X> target;
		private Off onCycle;
		protected NAR nar;

		@Override
		public synchronized void start(ConsumerX<X> target, NAR nar) {
			this.nar = nar;
			this.target = target;
			this.onCycle = nar.onDur(this::commit);
		}

		@Override
		public final synchronized void stop() {
			this.onCycle.close();
			this.onCycle = null;
			this.target = null;
			this.nar = null;
		}

		protected abstract void commit();
	}

	/**
	 * buffers and deduplicates in a Bag<Task,Task> allowing higher priority inputs to evict
	 * lower priority inputs before they are flushed.  commits involve a multi-thread shareable drainer task
	 * allowing multiple inputting threads to fill the bag, potentially deduplicating each's results,
	 * while other thread(s) drain it in prioritized order as input to NAR.
	 */
	public static class BagTaskBuffer extends SyncPriBuffer<Task> {

		public final IntRange capacity = new IntRange(0, 0, 4 * 1024);

		/**
		 * perceptual valve
		 * dilation factor
		 * input rate
		 * tasks per cycle
		 */
		public final FloatRange valve = new FloatRange(0.5f, 0, 1);

		private transient long prev = Long.MIN_VALUE;
		/**
		 * temporary buffer before input so they can be merged in case of duplicates
		 */
		public final Bag<Task, Task> tasks;

		@Override
		public int capacity() {
			return capacity.intValue();
		}

		{
			final PriMerge merge = NAL.tasklinkMerge;
			tasks = new SimpleBufferedBag<>(new PriArrayBag<>(merge, 0) {
				@Override
				protected int histogramBins(int s) {
					return 0; //disabled
				}

				/**
				 * merge in the pre-buffer
				 */
				@Override
				protected float merge(Task existing, Task incoming, float incomingPri) {
					return Task.merge(existing, incoming, merge, PriReturn.Delta, true);
				}

			},
				new PriMap<>(merge) {
					/**
					 * merge in the post-buffer
					 */
					@Override
					public float merge(Prioritizable existing, Task incoming, float pri, PriMerge merge) {
						return Task.merge((Task) existing, incoming, merge, PriReturn.Delta, true);
					}
				}
			);
		}
//                new PriArrayBag<ITask>(PriMerge.max, new HashMap()
//                        //new UnifiedMap()
//                ) {
//                    @Override
//                    protected float merge(ITask existing, ITask incoming) {
//                        return TaskBuffer.merge(existing, incoming);
//                    }
//                };

		//new HijackBag...


		/**
		 * @capacity size of buffer for tasks that have been input (and are being de-duplicated) but not yet input.
		 * input may happen concurrently (draining the bag) while inputs are inserted from another thread.
		 */
		public BagTaskBuffer(int capacity, float rate) {
			this.capacity.set(capacity);
			this.valve.set(rate);
			this.tasks.setCapacity(capacity);
		}


		@Override
		public void clear() {
			tasks.clear();
		}

//        final AtomicBoolean busy = new AtomicBoolean(false);

		@Override
		public int size() {
			return tasks.size();
		}


		@Override
		public void put(Task x) {
			tasks.putAsync(x);
		}

		@Override
		public void start(ConsumerX<Task> target, NAR nar) {
			prev = nar.time();
			super.start(target, nar);
		}

		final AtomicInteger pending = new AtomicInteger();

		@Override
		public void commit() {

            long now = nar.time();

            long dt = now - prev;

			prev = now;

            Bag<Task, Task> b = this.tasks;

			b.setCapacity(capacity.intValue());
			b.commit(null);

            int s = b.size();
			if (s > 0) {
                int cc = target.concurrency();
                int toRun = cc - pending.getOpaque();
				if (toRun >= 0) {
                    int n = Math.min(s, batchSize((((float) toRun) / cc) * dt / nar.dur()));
					if (n > 0) {
						//TODO target.input(tasks, n, target.concurrency());


						if (toRun == 1 || n <= 1) {
							//one at a time
							b.pop(null, n, target);
						} else {
							//batch
                            int remain = n;
                            int nEach = (int) Math.ceil(((float) remain) / toRun);


							for (int i = 0; i < toRun && remain > 0; i++) {
                                int asked = Math.min(remain, nEach);
								remain -= asked;
								target.input(b, asked, nar.exe,
									asked > 2 && NAL.PRE_SORT_TASK_INPUT_BATCH ? Task.sloppySorter : null,
									pending
								);
							}
						}
					}

				}
			}


		}


		/**
		 * TODO abstract
		 */
		protected int batchSize(float durs) {
			return (int) Math.ceil(Math.min(durs, 1f) * capacity() * valve.floatValue());

			//rateControl.apply(tasks.size(), tasks.capacity());

//            float v = valve.floatValue();
//            if (v < ScalarValue.EPSILON)
//                return 0;
//            return Math.max(1, Math.round(
//                    //dt * v * tasks.capacity()
//                    v * tasks.capacity()
//            ));
		}


	}


//    public static class BagPuncTasksBuffer extends TaskBuffer {
//
//        public final TaskBuffer belief, goal, question, quest;
//        private final TaskBuffer[] ALL;
//
//        public BagPuncTasksBuffer(int capacity, float rate) {
//            belief = new BagTaskBuffer(capacity, rate);
//            goal = new BagTaskBuffer(capacity, rate);
//            question = new BagTaskBuffer(capacity, rate);
//            quest = new BagTaskBuffer(capacity, rate);
//
//            ALL = new TaskBuffer[]{belief, goal, question, quest};
//
//            this.capacity.set(capacity);
//        }
//
//        @Override
//        public boolean async(ConsumerX<Prioritizable> target) {
//            return false;
//        }
//
//        @Override
//        public float priMin() {
//            float q = question.priMin();
//            if (q < ScalarValue.EPSILON) return 0;
//            float qq = quest.priMin();
//            if (qq < ScalarValue.EPSILON) return 0;
//            float b = belief.priMin();
//            if (b < ScalarValue.EPSILON) return 0;
//            float g = goal.priMin();
//            if (g < ScalarValue.EPSILON) return 0;
//            return Math.min(Math.min(Math.min(b, g), q), qq);
//        }
//
//        private TaskBuffer buffer(byte punc) {
//            switch (punc) {
//                case BELIEF:
//                    return belief;
//                case GOAL:
//                    return goal;
//                case QUESTION:
//                    return question;
//                case QUEST:
//                    return quest;
//                default:
//                    return null;
//            }
//        }
//
//        @Override
//        public void clear() {
//            for (TaskBuffer x : ALL)
//                x.clear();
//        }
//
//        @Override
//        public <T extends ITask> T put(T x) {
//            return buffer(x.punc()).put(x);
//        }
//
//        @Override
//        public void commit(long now, ConsumerX<Prioritizable> target) {
//            //TODO parallelize option
//
//            int c = Math.max(1, capacity.intValue() / ALL.length);
//            for (TaskBuffer x : ALL) {
//                x.capacity.set(c);
//                x.commit(now, target);
//            }
//        }
//
//        @Override
//        public int size() {
//            int s = 0;
//            for (TaskBuffer x : ALL)
//                s += x.size();
//            return s;
//        }
//    }
}
