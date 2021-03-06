package nars.table.temporal;

import jcog.Util;
import jcog.WTF;
import jcog.data.list.FasterList;
import jcog.math.LongInterval;
import jcog.math.Longerval;
import jcog.sort.FloatRank;
import jcog.sort.Top;
import jcog.tree.rtree.*;
import jcog.tree.rtree.split.AxialSplit;
import nars.NAL;
import nars.Task;
import nars.control.op.Remember;
import nars.task.ProxyTask;
import nars.task.util.Answer;
import nars.task.util.Revision;
import nars.task.util.TaskRegion;
import nars.task.util.TimeRange;
import nars.term.Term;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.proj.TruthIntegration;
import nars.truth.proj.TruthProjection;
import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static nars.truth.proj.TruthIntegration.eviFast;

public class RTreeBeliefTable extends ConcurrentRTree<TaskRegion> implements TemporalBeliefTable {

	private static final int MAX_TASKS_PER_LEAF = 3;


	private int capacity;

	public RTreeBeliefTable() {
		super(new RTree<>(RTreeBeliefModel.the));
	}


	/**
	 * immediately returns false if space removed at least one as a result of the scan, ie. by removing
	 * an encountered deleted task.
	 */
	private static boolean findEvictable(Space<TaskRegion> tree, RNode<TaskRegion> next, Top<Task> weakest, Top<RLeaf<TaskRegion>> mergeableLeaf) {
		if (next instanceof RLeaf) {

            RLeaf l = (RLeaf) next;
            Object[] data = l.data;
            short s = l.size;
			for (int i = 0; i < (int) s; i++) {
                Task x = (Task) data[i];
				if (x.isDeleted()) {
                    boolean removed = tree.remove(x);
					assert (removed);
					return false;
				}

				weakest.accept(x);
			}

			if ((int) s >= 2)
				mergeableLeaf.accept(l);

		} else {
            //null-terminated
			for (RNode<TaskRegion> bb : ((RBranch<TaskRegion>) next).data) {
				if (bb == null) {
					break;
				}
				if (!findEvictable(tree, bb, /*closest, */weakest, mergeableLeaf)) {
					return false;
				}
			}
			return true;
		}

		return true;
	}


	/**
	 * returns true if at least one net task has been removed from the table.
	 */

	private static void compress(Space<TaskRegion> tree, Remember r) {

        long now = r.time();
		//long tableDur = tableDur(now);

        Top<Task> weakest = new Top<Task>(new FurthestWeakest(now, 1.0));

        Top<RLeaf<TaskRegion>> mergeableLeaf = new Top<RLeaf<TaskRegion>>(
			//WeakestTemporallyDense(now)
			MergeableLeaf
		);

		if (findEvictable(tree, tree.root(), weakest, mergeableLeaf))
			compress(tree, weakest, mergeableLeaf, r);

	}

	private static void compress(Space<TaskRegion> treeRW,
								 Top<Task> theWeakest,
								 Top<RLeaf<TaskRegion>> mergeableLeaf,
								 Remember r) {

        Task weakest = theWeakest.get();
		TruthProjection merging = null;
		Task merged = null;

		if (!mergeableLeaf.isEmpty()) {
            Pair<Task, TruthProjection> AB = mergeLeaf(mergeableLeaf, r);
			if (AB != null) {
				if (!mergeOrEvict(weakest, merged = Revision.afterMerge(AB), merging = AB.getTwo(), r))
					merged = null;
			}
		}

		if (merged != null)
			merge(merged, merging, r, treeRW);
		else
			evict(weakest, r, treeRW);
	}

	private static @Nullable Pair<Task, TruthProjection> mergeLeaf(Top<RLeaf<TaskRegion>> mergeableLeaf, Remember r) {
        RLeaf<TaskRegion> leaf = mergeableLeaf.get();
		return Revision.merge(r.what.nar, false, 2, (int) leaf.size, leaf.data);
	}

	private static boolean mergeOrEvict(Task weakest, Task merged, TruthProjection merging, Remember r) {
        long now = r.time();
        double weakEvictionValue = -eviFast(weakest, now);
        double mergeValue = eviFast(merged, now) - merging.sumOfDouble(new DoubleFunction<Task>() {
            @Override
            public double doubleValueOf(Task t) {
                return eviFast(t, now);
            }
        });

		return mergeValue >= weakEvictionValue;
	}

	private static void merge(Task merged, TruthProjection merging, Remember r, Space<TaskRegion> treeRW) {
        int ababSize = merging.size();
		if (ababSize < 2)
			throw new WTF();
		for (int i = 0; i < ababSize; i++) {
			if (merging.valid(i)) {
                Task rr = merging.get(i);
				if (!treeRW.remove(rr)) {
					if (NAL.DEBUG)
						throw new WTF(); //not sure what this means
				}
				r.forget(rr);
			}
		}
		if (treeRW.add(merged)) {
			r.remember(merged);
		}//else: possibly already contained the merger?
	}

	private static void evict(Task weakest, Remember r, Space<TaskRegion> treeRW) {
		if (treeRW.remove(weakest)) {
			r.forget(weakest);
		} else {
			//tree may have been cleared/deleted while compressing.  if this isnt the case then someting unexpected happened
			if (!treeRW.isEmpty()) throw new WTF();
		}
	}

	@Override
	public final boolean isEmpty() {
		return super.isEmpty();
	}

	@Override
	public final int taskCount() {
		return size();
	}

	@Override
	public final void match(Answer a) {


		//tree.readOptimistic(
		readIfNonEmpty(new Consumer<RTree<TaskRegion>>() {
            @Override
            public void accept(RTree<TaskRegion> t) {
                int n = t.size();
                if (n == 0)
                    return;

                int ac = a.ttl;
                if (n <= ac) {
                    t.forEach(((Predicate) a)::test);
                } else {

                    long s = a.start, e = a.end;
                    RNode<TaskRegion> tRoot = t.root();
//				float confMax = Math.max(NAL.truth.TRUTH_EPSILON, ((TaskRegion) tRoot.bounds()).confMax());
//				float confPerTime =
//					//(float) ((1 + ((e - s) / 2.0 + a.dur)) / confMax);
//					//(a.dur / confMax);
//					(Math.max(1, a.dur) / confMax);
                    //float dur = (float) (tRoot.bounds().range(0) / (1+n));

                    HyperIterator.HyperIteratorRanker<TaskRegion, TaskRegion> timeRank =
                            new HyperIterator.HyperIteratorRanker<TaskRegion, TaskRegion>(new Function<TaskRegion, TaskRegion>() {
                                @Override
                                public TaskRegion apply(TaskRegion z) {
                                    return z;
                                }
                            }, Answer.regionNearness(s, e));

                    int cursorCapacity = Math.min(n,
                            //ac
                            Math.max(1, (int) Math.ceil((double) (ac / Math.max(1.0F, ((float) RTreeBeliefTable.MAX_TASKS_PER_LEAF / 2f)))))
                    );

                    HyperIterator<TaskRegion> h = new HyperIterator<TaskRegion>(new Object[cursorCapacity], timeRank);
//				{
//					@Override
//					protected void scan() {
//						super.scan();
//						//second stage filter
//						if (plan.size() > cursorCapacity) {
//							plan.rank((x, min)-> ((TaskRegion)((RNode<TaskRegion>)x).bounds()).confMin());
//							plan.setCapacity(cursorCapacity);
//						}
//					}
//				};
                    //h.dfs(t.root(), whle);
                    h.bfs(tRoot, a, a.random());
                }
            }
        });
	}

	@Override
	public void setTaskCapacity(int capacity) {
		this.capacity = capacity;
	}

	@Override
	public void remember(Remember r) {

		if (capacity == 0 || r.input.isEternal())
			return;

		/** buffer removal handling until outside of the locked section */


        Task input;
        if (r.input instanceof ProxyTask) {
            //dont store TaskProxy's
            ProxyTask p = (ProxyTask) r.input;
			r.input = input = p.the();
            if (input == null)
                throw new WTF();
        } else {
            input = r.input;
        }

//		Task input;
//        if (r.input instanceof SpecialTruthAndOccurrenceTask) {
//            //dont do this for SpecialTermTask coming from Image belief table
//            input = ((TaskProxy) r.input).the();
//        } else if (r.input instanceof SpecialTermTask) {
//            //ex: Image belief table proxy
//            input = ((TaskProxy) r.input).the();
//        } else {Sub
//            input = r.input;
//        }

//		Task input = r.input;

		/** TODO only enter write lock after deciding insertion is necessary (not merged with existing)
		 *    subclass RInsertion to RConcurrentInsertion, storing Stamped Lock lock value along with it */
        TaskInsertion insertion = writeWith(r, this::insert);

        Task mergeReplaced = (Task) insertion.mergeReplaced;
		if (mergeReplaced != null) {
			if (mergeReplaced != input) {
				onReject(input);
				r.forget(input);
			}
			r.merge(mergeReplaced);
		} else if (!input.isDeleted()) {
			onRemember(input);
			r.remember(input);
		} else {
			onReject(input);
			r.forget(input);
		}

//        //TEMPORARY for debug
//        ListMultimap<String, Task> xx = MultimapBuilder.hashKeys().arrayListValues().build();
//        taskStream().forEach(x -> xx.put(x.toString(), x));
//        for (String s : xx.keys()) {
//            List<Task> ll = xx.get(s);
//            if (ll.size() > 1) {
//                Set<LongArraySet> stamps = new HashSet();
//                for (Task lll : ll)
//                    stamps.add(new LongArraySet(lll.stamp()));
//                if (stamps.size() < ll.size())
//                    Util.nop();
//            }
//        }


	}

	protected void onReject(Task input) {
		/* optional: implement in subclasses */
	}

	protected void onRemember(Task input) {
		/* optional: implement in subclasses */

	}

	private void ensureCapacity(Space<TaskRegion> treeRW, Remember r) {
		int e = 0, cap;
		while (treeRW.size() > (cap = capacity())) {


			if (cap == 0) {
				//became deleted
				treeRW.clear();
				return;
			}

			if (e > 0) {
				if (e > 1)
					throw new WTF();
			}
			compress(treeRW, r);

			e++;
			assert (e < cap) : this + " compressed " + e + " times (cap=" + cap + ")";
		}

	}

//    /**
//     * decides the value of keeping a task, used in compression decision
//     */
//    protected FloatRank<Task> taskStrength(boolean beliefOrGoal, long now, float narDur, int tableDur) {
//        //return FloatRank.the(t->t.evi(now, dur));
//        return Answer.temporalTaskStrength(Math.round(now - narDur / 2), Math.round(now + narDur / 2), tableDur);
//
////        return Answer.taskStrengthWithFutureBoost(now, now - dur,
////                beliefOrGoal ? PRESENT_AND_FUTURE_BOOST_BELIEF : PRESENT_AND_FUTURE_BOOST_GOAL,
////                dur
////        );
//    }

	/**
	 * this is the range as a radius surrounding present moment and optionally further subdivided
	 * to represent half inside the super-duration, half outside the super-duration
	 */
	@Override
	public long tableDur(long now) {
        TaskRegion root = bounds();
		return (root == null) ? 1L :
				1L + Math.round((double) Math.max(Math.abs(now - root.start()), Math.abs(now - root.end())) * NAL.TEMPORAL_BELIEF_TABLE_DUR_SCALE);
	}

	@Override
	public Stream<? extends Task> taskStream() {
		return stream().map(TaskRegion::_task);
	}

	@Override
	public Task[] taskArray() {
        int s = size();
		if (s == 0) {
			return Task.EmptyArray;
		} else {
            FasterList<Task> l = new FasterList<Task>(s + 1);
			forEachTask(l::add);
			return l.toArrayRecycled(Task[]::new);
		}
	}


	@Override
	public void whileEach(Predicate<? super Task> each) {
		intersectsWhile(root().bounds(), TaskRegion.asTask(each));
	}

	@Override
	public void whileEach(long s, long e, Predicate<? super Task> each) {
		intersectsWhile(new TimeRange(s, e), TaskRegion.asTask(each));
	}

	@Override
	public void forEachTask(long minT, long maxT, Consumer<? super Task> x) {
		if (minT == Long.MIN_VALUE && maxT == Long.MAX_VALUE) {
			forEach(TaskRegion.asTask(x));
		} else {
			whileEach(minT, maxT, new Predicate<Task>() {
                @Override
                public boolean test(Task t) {
                    x.accept(t);
                    return true;
                }
            });
		}
	}

	@Override
	public void forEachTask(Consumer<? super Task> each) {
		forEach(new Consumer<TaskRegion>() {
            @Override
            public void accept(TaskRegion t) {
                each.accept((Task) t);
            }
        });
	}

	@Override
	public void removeIf(Predicate<Task> remove, long s, long e) {

        long l = readLock();
		try {

            FasterList<Task> deleteAfter = new FasterList<Task>();
			tree.intersectsWhile(new TimeRange(s, e), new Predicate<TaskRegion>() {
                @Override
                public boolean test(TaskRegion _t) {
                    Task t = (Task) _t;
                    if (remove.test(t)) {
                        deleteAfter.add(t); //buffer the deletions because it will interfere with the iteration
                    }
                    return true;
                }
            });
			if (!deleteAfter.isEmpty()) {
				l = Util.readToWrite(l, this);
				deleteAfter.forEach(new Procedure<Task>() {
                    @Override
                    public void value(Task t) {
                        tree.remove(t);
                        t.delete();
                    }
                });
			}
		} finally {
			unlock(l);
		}
	}

	@Override
	public boolean removeTask(Task x, boolean delete) {
		assert (!x.isEternal());

		if (remove(x)) {
			if (delete)
				x.delete();
			return true;
		}
		return false;
	}

	public void print(PrintStream out) {
		forEachTask(new Consumer<Task>() {
            @Override
            public void accept(Task t) {
                out.println(t.toString(true));
            }
        });
		stats().print(out);
	}

	public int capacity() {
		return capacity;
	}

	/**
	 * bounds of the entire table
	 */
	public @Nullable TaskRegion bounds() {
		return (TaskRegion) root().bounds();
	}

	private TaskInsertion insert(Space<TaskRegion> treeRW, Remember R) {

        TaskInsertion ii = (TaskInsertion) treeRW.insert(R.input);
		if (ii.added())
			ensureCapacity(treeRW, R);

		return ii;
	}


	static final class FurthestWeakest implements FloatRank<Task> {

		final long now;
		final double dur;

		FurthestWeakest(long now, double dur) {
			this.now = now;
			this.dur = dur;
		}

		@Override
		public float rank(Task t, float min) {
			//return -Answer.beliefStrength(t, now, dur);
			return -(float) TruthIntegration.eviFast(t, now);
		}
	}


	private static final Split SPLIT =
		//new QuadraticSplit();
		new AxialSplit();

	private static final class RTreeBeliefModel extends Spatialization<TaskRegion> {

		static final Spatialization<TaskRegion> the = new RTreeBeliefModel();


		private RTreeBeliefModel() {
			super((new Function<TaskRegion, HyperRegion>() {
                        @Override
                        public HyperRegion apply(TaskRegion t) {
                            return t;
                        }
                    }), SPLIT,
				RTreeBeliefTable.MAX_TASKS_PER_LEAF);
		}

//		@Override
//		public void commit(RLeaf<TaskRegion> l) {
//			//sort tasks by descending confidence
//			short s = l.size;
//			if (s > 1 && ((TaskRegion)l.bounds).confMinI() < ((TaskRegion)l.bounds).confMaxI()) {
//				ArrayUtil.quickSort(0, s,
//					(a,b)-> Float.compare(l.data[b].confMean(), l.data[a].confMean()),
//					(a,b)-> ArrayUtil.swap(l.data, a, b, 1));
//			}
//		}

		@Override
		public TaskInsertion insertion(TaskRegion t, boolean addOrMerge) {
			return new TaskInsertion(t, addOrMerge);
		}


		@Override
		public TaskRegion bounds(TaskRegion taskRegion) {
			return taskRegion;
		}

		@Override
		public RLeaf<TaskRegion> newLeaf(int capacity) {
			return new RLeaf<>(new TaskRegion[capacity]);
		}

		@Override
		public @Nullable TaskRegion merge(TaskRegion existing, TaskRegion incoming, RInsertion<TaskRegion> i) {

			Task ex = (Task) existing, in = (Task) incoming;
            Truth t = ex.truth();
			if (t.equals(in.truth())) {
                Term exT = ex.term();
				if (exT.equals(in.term())) {
                    int xys = Stamp.equalsOrContains(ex.stamp(), in.stamp());
					if (xys != Integer.MIN_VALUE) { //Arrays.equals(ex.stamp(), in.stamp())
						long is = in.start(), ie = in.end();
						long es = ex.start(), ee = ex.end();
						if ((xys == 0 || xys == -1) && Longerval.containsRaw(is, ie, es, ee)) //incoming contains existing
							return merge(in, i);
						else if ((xys == 0 || xys == +1) && Longerval.containsRaw(es, ee, is, ie)) //existing contais incoming
							return merge(ex, i);
						else if (xys == 0 && LongInterval.intersectsRaw(is, ie, es, ee)) //temporal union
							return merge(Task.clone(ex, exT, t, ex.punc(), Math.min(is, es), Math.max(ie, ee)), i);
					}
				}
			}
			return null;
		}

		private static TaskRegion merge(Task m, RInsertion<TaskRegion> i) {
			((TaskInsertion) i).mergeReplaced = m;
			return m;
		}

		@Override
		public boolean canMerge() {
			return true;
		}

		public boolean canMergeStretch() {
			return true;
		}


	}

	private static final class TaskInsertion extends RInsertion<TaskRegion> {

		@Nullable TaskRegion mergeReplaced = null;

		TaskInsertion(TaskRegion t, boolean addOrMerge) {
			super(t, addOrMerge, RTreeBeliefModel.the);
		}


		@Override
		public @Nullable TaskRegion merge(TaskRegion existing) {
            TaskRegion y = super.merge(existing);
			if (y != null)
				mergeReplaced = y;
			return y;
		}

		@Override
		public void mergeEqual(TaskRegion existing) {
			super.mergeEqual(existing);
			mergeReplaced = existing;
		}


	}

	private static final FloatRank<RLeaf<TaskRegion>> MergeableLeaf = new FloatRank<RLeaf<TaskRegion>>() {
        @Override
        public float rank(RLeaf<TaskRegion> l, float min) {
            //TODO use min parameter to early exit
            HyperRegion bounds = l.bounds;
            double conf = bounds.coord(2, true);
            return -(float) (conf * bounds.range(0));
        }
    };


//    /** TODO */
//    public static class EternalizingRTreeBeliefTable extends RTreeBeliefTable {
//
//        final TruthAccumulator ete = new TruthAccumulator();
//        private final boolean beliefOrGoal;
//
//        public EternalizingRTreeBeliefTable(boolean beliefOrGoal) {
//            this.beliefOrGoal = beliefOrGoal;
//
//        }
//
//        @Override
//        public void match(Answer m) {
//            super.match(m);
//            if (m.template!=null) {
//                Truth t = ete.peekAverage();
//                if (t != null) {
//
//
//                    Task tt = new NALTask(m.template, beliefOrGoal ? BELIEF : GOAL, t,
//                            m.nar.time(), m.time.start, m.time.end,
//                            m.nar.evidence() //TODO hold rolling evidence buffer
//                    ).pri(m.nar);
//
////                System.out.println(tt);
//                    m.tryAccept(tt);
//                }
//            } else {
//                if (Param.DEBUG)
//                    throw new WTF("null template"); //HACK
//            }
//        }
//
//        @Override
//        protected void remember(Remember r, Task input) {
//            super.remember(r, input);
//
//            {
//
//                @Nullable TaskRegion bounds = bounds();
//                if (bounds==null)
//                    return;
//
//                int cap = capacity();
//                float e = TruthIntegration.evi(input) * (((float) input.range()) / bounds.range() );
//                float ee = TruthFunctions.eternalize(e);
//                float ce = w2cSafe(ee);
//                if (ce > Param.TRUTH_EPSILON) {
//                    ete.addAt(input.freq(), ce);
//                }
//            }
//        }
//    }


//    private final static class ExpandingScan extends CachedTopN<TaskRegion> implements Predicate<TaskRegion> {
//
//        private final Predicate<Task> filter;
//        private final int minResults, attempts;
//        int attemptsRemain;
//
//
//        ExpandingScan(int minResults, int maxResults, FloatFunction<TaskRegion> strongestTask, int maxTries) {
//            this(minResults, maxResults, strongestTask, maxTries, null);
//        }
//
//
//        ExpandingScan(int minResults, int maxResults, FloatFunction<TaskRegion> strongestTask, int maxTries, Predicate<Task> filter) {
//            super(maxResults, strongestTask);
//            this.minResults = minResults;
//            this.attempts = maxTries;
//            this.filter = filter;
//        }
//
//        @Override
//        public boolean accept(TaskRegion taskRegion) {
//
//            return (!(taskRegion instanceof Task)) || super.accept(taskRegion);
//        }
//
//        @Override
//        public boolean test(TaskRegion x) {
//            accept(x);
//            return --attemptsRemain > 0;
//        }
//
//        @Override
//        public boolean valid(TaskRegion x) {
//            return ((!(x instanceof Task)) || (filter == null || filter.test((Task) x)));
//        }
//
//        boolean continueScan(TimeRange t) {
//            return size() < minResults && attemptsRemain > 0;
//        }
//
//        /**
//         * TODO add a Random argument so it can decide randomly whether to scan the left or right zone first.
//         * order matters because the quality limit may terminate it.
//         * however maybe the quality can be specified in terms that are compared
//         * only after the pair has been scanned making the order irrelevant.
//         */
//        ExpandingScan scan(RTreeBeliefTable table, long _start, long _end) {
//
//            /* whether eternal is the time bounds */
//            boolean eternal = _start == ETERNAL;
//
//
//            this.attemptsRemain = attempts;
//
//            int s = table.size();
//            if (s == 0)
//                return this;
//
//            /* if eternal is being calculated, include up to the maximum number of truthpolated terms.
//                otherwise limit by the Leaf capacity */
//            if ((!eternal && s <= COMPLETE_SCAN_SIZE_THRESHOLD) || (eternal && s <= Answer.TASK_LIMIT)) {
//                table.forEach /*forEachOptimistic*/(this::accept);
//                //TODO this might be faster to add directly then sort the results after
//                //eliminating need for the Cache map
//                return this;
//            }
//
//            TaskRegion bounds = (TaskRegion) (table.root().bounds());
//
//            long boundsStart = bounds.start();
//            long boundsEnd = bounds.end();
//            if (boundsEnd == XTERNAL || boundsEnd < boundsStart) {
//                throw WTF();
//            }
//
//            int ss = s / COMPLETE_SCAN_SIZE_THRESHOLD;
//
//            long scanStart, scanEnd;
//            int confDivisions, timeDivisions;
//            if (!eternal) {
//
//                scanStart = Math.min(boundsEnd, Math.max(boundsStart, _start));
//                scanEnd = Math.max(boundsStart, Math.min(boundsEnd, _end));
//
//
//                timeDivisions = Math.max(1, Math.min(SCAN_TIME_OCTAVES_MAX, ss));
//                confDivisions = Math.max(1, Math.min(SCAN_CONF_OCTAVES_MAX,
//                        ss / Util.sqr(1 + timeDivisions)));
//            } else {
//                scanStart = boundsStart;
//                scanEnd = boundsEnd;
//
//                confDivisions = Math.max(1, Math.min(SCAN_TIME_OCTAVES_MAX /* yes TIME here, ie. the axes are switched */,
//                        Math.max(1, ss - minResults)));
//                timeDivisions = 1;
//            }
//
//            long expand = Math.max(1, (
//                    Math.round(((double) (boundsEnd - boundsStart)) / (1 << (timeDivisions))))
//            );
//
//
//            long mid = (scanStart + scanEnd) / 2;
//            long leftStart = scanStart, leftMid = mid, rightMid = mid, rightEnd = scanEnd;
//            boolean leftComplete = false, rightComplete = false;
//
//
//            TimeRange ll = confDivisions > 1 ? new TimeConfRange() : new TimeRange();
//            TimeRange rr = confDivisions > 1 ? new TimeConfRange() : new TimeRange();
//
//            float maxConf = bounds.confMax();
//            float minConf = bounds.confMin();
//
//            int FATAL_LIMIT = s * 2;
//            int count = 0;
//            boolean done = false;
//            do {
//
//                float cMax, cDelta, cMin;
//                if (confDivisions == 1) {
//                    cMax = 1;
//                    cMin = 0;
//                    cDelta = 0;
//                } else {
//                    cMax = maxConf;
//                    cDelta =
//                            Math.max((maxConf - minConf) / Math.min(s, confDivisions), Param.TRUTH_EPSILON);
//                    cMin = maxConf - cDelta;
//                }
//
//                for (int cLayer = 0;
//                     cLayer < confDivisions && !(done = !continueScan(ll.setAt(leftStart, rightEnd)));
//                     cLayer++, cMax -= cDelta, cMin -= cDelta) {
//
//
//                    TimeRange lll;
//                    if (!leftComplete) {
//                        if (confDivisions > 1)
//                            ((TimeConfRange) ll).setAt(leftStart, leftMid, cMin, cMax);
//                        else
//                            ll.setAt(leftStart, leftMid);
//
//                        lll = ll;
//                    } else {
//                        lll = null;
//                    }
//
//                    TimeRange rrr;
//                    if (!rightComplete && !(leftStart == rightMid && leftMid == rightEnd)) {
//                        if (confDivisions > 1)
//                            ((TimeConfRange) rr).setAt(rightMid, rightEnd, cMin, cMax);
//                        else
//                            rr.setAt(rightMid, rightEnd);
//                        rrr = rr;
//                    } else {
//                        rrr = null;
//                    }
//
//                    if (lll != null || rrr != null) {
//                        table.read /*readOptimistic*/((Space<TaskRegion> tree) -> {
//                            if (lll != null)
//                                tree.whileEachIntersecting(lll, this);
//                            if (rrr != null)
//                                tree.whileEachIntersecting(rrr, this);
//                        });
//                    }
//
//                    if (count++ == FATAL_LIMIT) {
//                        throw new RuntimeException("livelock in rtree scan");
//                    }
//                }
//
//                if (done)
//                    break;
//
//
//                long ls0 = leftStart;
//                leftComplete |= (ls0 == (leftStart = Math.max(boundsStart, leftStart - expand - 1)));
//
//                if (leftComplete && rightComplete) break;
//
//                long rs0 = rightEnd;
//                rightComplete |= (rs0 == (rightEnd = Math.min(boundsEnd, rightEnd + expand + 1)));
//
//                if (leftComplete && rightComplete) break;
//
//                leftMid = ls0 - 1;
//                rightMid = rs0 + 1;
//                expand *= 2;
//            } while (true);
//
//            return this;
//        }
//    }


}


//    static final ToDoubleFunction<RLeaf<TaskRegion>> MostComponents = (n) -> {
//        return n.size;
//    };
//    static final ToDoubleFunction<RLeaf<TaskRegion>> LeastOriginality = (n) -> {
//        double s = originalitySum(n);
//        //return 1 / (1 + s);
//        return -s;
//    };
//    //2f;
//    static final ToDoubleFunction<RLeaf<TaskRegion>> MostOriginality = (n) -> {
//        return originalitySum(n);
//    };
//    //new AxialSplitLeaf();
//    //new LinearSplitLeaf();
//    //    static final ToDoubleFunction<TaskRegion> LeastOriginal = (t) -> {
////        return 1.0 / (((double)t.range()) * t.confMean());
////    };
//    static final ToDoubleFunction<Task> LeastOriginal = (t) -> {
//        return 1.0 / (1 + t.originality());
//    };
//    static final ToDoubleFunction<Task> MostComplex = (t) -> {
//        return t.volume();
//    };
//    static final ToDoubleFunction<Task> LeastPriority = (t) -> {
//        return 1.0 / (1 + t.priElseZero());
//    };
//    /**
//     * TODO use the same heuristics as task strength
//     */
//    private static FloatRank<TaskRegion> regionWeakness(long now, float futureFactor, float dur) {
//
//
//        float pastDiscount = 1.0f - (futureFactor - 1.0f);
//
//        return (TaskRegion r, float min) -> {
//
//            float y =
//                    //(float)Math.log(1+r.meanTimeTo(now));
//                    (1 + r.maxTimeTo(now)) / dur;
//
//            if (y < min)
//                return Float.NaN;
//
//            float conf =
//                    r.confMin();
//
//            y = (1+y) * (1+(1 - conf));
//            if (y < min)
//                return Float.NaN;
//
//            if (pastDiscount != 1 && r.end() < now)
//                y *= pastDiscount;
//
//            return (float) y;
//
////            long regionTimeDist = r.midTimeTo(when);
////
////            float timeDist = (regionTimeDist) / ((float) perceptDur);
////
////
////            float evi =
////                    c2wSafe((float) r.coord(true, 2));
////
////
////            float antivalue = 1f / (1f + evi);
////
////            if (PRESENT_AND_FUTURE_BOOST != 1 && r.end() >= when - perceptDur)
////                antivalue /= PRESENT_AND_FUTURE_BOOST;
////
////
////            return (float) ((antivalue) * (1 + timeDist));
//        };
//    }
//    private static final float PRESENT_AND_FUTURE_BOOST_BELIEF =
//        1.0f;
//    //1.5f;
//    private static final float PRESENT_AND_FUTURE_BOOST_GOAL =
//        1.0f;
//    static final ToDoubleFunction<? super TaskRegion> LeastConf = (t) -> {
//        return 1.0 / (1 + t.confMean());
//    };

//    static final ToDoubleFunction<? super Task> WeakestEviInteg = (t) -> {
//        double eviRange = t.evi() * t.range();
//        //return 1.0 / (1+eviRange);
//        return -eviRange;
//    };
//    static final ToDoubleFunction LeastTimeRange = (t) -> {
//        if (t instanceof RLeaf) t = ((RLeaf)t).bounds;
//        return 1.0 / ((TaskRegion)t).range();
//    };
//    static final ToDoubleFunction LeastFreqRange = (t) -> {
//        if (t instanceof RLeaf) t = ((RLeaf)t).bounds;
//        return 1.0 / (1+((TaskRegion)t).range(1));
//    };
//    static final class Furthest implements ToDoubleFunction {
//
//        public final long now;
//
//        Furthest(long now) {
//            this.now = now;
//        }
//
//        @Override
//        public double applyAsDouble(Object t) {
//            TaskRegion tt = (t instanceof RLeaf) ? (TaskRegion) ((RLeaf) t).bounds : (TaskRegion) t;
//            //return Math.abs( tt.mid() - now );
//            return tt.maxTimeTo(now);
//        }
//    }

//    static FloatFunction<RLeaf<TaskRegion>> WeakestTemporallyDense(long now) { return (n) -> {
//        long s = Long.MAX_VALUE, e = Long.MIN_VALUE;
//        long u = 0;
////        long minRange = Long.MAX_VALUE, maxRange = Long.MIN_VALUE;
////        int stampMin = Integer.MAX_VALUE, stampMax = Integer.MIN_VALUE;
//        for (TaskRegion t : n.data) {
//            if (t == null) break;
//            long ts = t.start();
//            long te = t.end();
//            s = Math.min(s, ts);
//            e = Math.max(e, te);
//            long r = 1 + (te - ts);
//            u += r;
////            int stampLen = ((Task)t).stamp().length;
////            stampMin = Math.min(stampLen, stampMin);
////            stampMax = Math.max(stampLen, stampMax);
//        }
////        int stampDelta = stampMax - stampMin;
//        //double rangeDeltaMax = ...
//        //double df = (n.bounds.range(1)) /* freq */;
//        double confFactor = (1+n.bounds.center(2));
//        long range = (1+(e-s));
////        long timeDist = ((TaskRegion)n.bounds).maxTimeTo(now);
//        return (float)( Math.sqrt(n.size) * ( u  /* * (1 + ((double)timeDist)) */ ) / ( confFactor * range  )); //* (timeToNow/range)
//    };
//    }


























