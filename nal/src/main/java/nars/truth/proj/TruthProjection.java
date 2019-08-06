package nars.truth.proj;

import jcog.Paper;
import jcog.Skill;
import jcog.WTF;
import jcog.data.bit.MetalBitSet;
import jcog.data.set.MetalLongSet;
import jcog.pri.ScalarValue;
import jcog.util.ArrayUtil;
import nars.NAL;
import nars.Op;
import nars.Task;
import nars.task.DynamicTruthTask;
import nars.task.NALTask;
import nars.task.ProxyTask;
import nars.task.Tasked;
import nars.task.proxy.SpecialTruthAndOccurrenceTask;
import nars.task.util.TaskList;
import nars.task.util.TaskRegion;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Term;
import nars.term.util.Intermpolate;
import nars.term.util.TermTransformException;
import nars.time.Tense;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.util.Timed;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.IntPredicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static java.lang.System.arraycopy;
import static jcog.Util.assertFinite;
import static nars.NAL.STAMP_CAPACITY;
import static nars.Op.BELIEF;
import static nars.Op.GOAL;
import static nars.term.atom.Bool.Null;
import static nars.term.util.Intermpolate.dtDiff;

/**
 * Truth Interpolation and Extrapolation of Temporal Beliefs/Goals
 * see:
 * https:
 * https:
 */
@Paper
@Skill({"Interpolation", "Extrapolation"})
abstract public class TruthProjection extends TaskList {

	protected long start, end;

	/**
	 * content target, either equal in all the tasks, or the result is
	 * intermpolated (and evidence reduction applied as necessary)
	 */
	public Term term = null;
	float eviFactor = 1;

	/** used in final calculation of to start/end time intervals */
	int ditherDT = 1;
	float dur = 0;

	/**
	 * active evidence cache
	 */
	@Nullable double[] evi = null;

	TruthProjection(long start, long end, float dur) {
		super(0);
		time(start, end);
		dur(dur);
	}

	@Nullable
	public static Task merge(Supplier<Task[]> tasks, Term content, Truth t, Supplier<long[]> stamp, boolean beliefOrGoal, long start, long end, Timed w) {
		boolean neg = content instanceof Neg;
		if (neg)
			content = content.unneg();

		ObjectBooleanPair<Term> r = Task.tryTaskTerm(
				content,
				beliefOrGoal ? BELIEF : GOAL, !NAL.test.DEBUG_EXTRA);
		if (r==null)
			return null;

		Truth yt = t.negIf(neg != r.getTwo());

		Task[] tt = tasks.get();
		if (tt.length == 1) {
			Task only = tt[0];

			//wrap the only task wtih Special proxy task
			if (only.start() == start && only.end() == end && only.truth().equals(yt))
				return only;
			else {
				if (only instanceof SpecialTruthAndOccurrenceTask || !(only instanceof ProxyTask)) { //TODO other special proxy types
					return SpecialTruthAndOccurrenceTask.the(only, yt, start, end);
				} //else: continue below
			}

		}


		NALTask y = new DynamicTruthTask(
			r.getOne(), beliefOrGoal,
			yt,
				w, start, end,
				stamp.get());


//        y.pri(
//              tasks.reapply(TaskList::pri, NAL.DerivationPri)
//                        // * dyn.originality() //HACK
//        );
		Task.fund(y, tt, true);

		return y;
	}

	public TruthProjection dur(float dur) {
		assert(dur >= 0);
		this.dur = dur;
		return this;
	}

	public TruthProjection ditherDT(int ditherDT) {
		this.ditherDT = ditherDT;
		return this;
	}
	public TruthProjection ditherDT(NAL nal) {
		return ditherDT(nal.dtDither());
	}

	public final Truth truth(double eviMin, boolean dither, boolean tShrink, NAL nar) {
		return truth(eviMin, dither, tShrink, true, nar);
	}

	/**
	 * computes the final truth value
	 */
	@Nullable
	public abstract Truth truth(double eviMin, boolean dither, boolean tShrink, boolean commitFirst, NAL nar);

	private boolean update(int i) {
		Task t = items[i];
		return t != null && valid(evi[i] = evi(t));
	}

	private double evi(Task task) {
		double e;
		if (start == ETERNAL) {
			if (!task.isEternal())
				throw new WTF("eternal truthpolation requires eternal tasks");
			e = task.evi();
		} else {
			e = TruthIntegration.evi(task, start, end, dur);
		}
		return e < NAL.truth.EVI_MIN ? 0 : e;
	}


	/**
	 * removes the weakest components sharing overlapping evidence with stronger ones.
	 * should be called after all entries are added
	 *
	 * @param needStamp whether a stamp result should be returned, or if this can be elided if not necessary
	 */
	@Nullable
	public final MetalLongSet commit(boolean shrink, int minResults, boolean needStamp, NAL n) {
		int s = size();
		if (s < minResults) return null;

		int r = refocus(shrink);
		if (r < minResults) return null;
		if (r < s)
			removeNulls();

		return r == 1 ? commit1(needStamp) : commitN(shrink, minResults, needStamp, n);
	}

	@Nullable
	private MetalLongSet commitN(boolean shrink, int minResults, boolean needStamp, NAL n) {
		MetalLongSet e = filter(minResults, shrink, needStamp);
		if (needStamp && e == null)
			return null; //remain < minResults

		int activePreCull = active();

		float eviFactor = intermpolateAndCull(n); assertFinite(eviFactor);
		if (eviFactor < ScalarValue.EPSILONcoarse) {
			e = null;
		} else {
			this.eviFactor = eviFactor;
			int activePostCull = active();
			if (activePostCull < minResults) {
				e = null;
			} else if (shrink && activePostCull != activePreCull) {

				refocus(shrink);

				if (needStamp)
					e = stampRemain(activePostCull);
			}
		}

		return needStamp ? e : null;
	}


	public int active() {
		if (size == 0)
			return 0;
		double[] a = evi;
		if (a == null)
			throw new NullPointerException("evi cache is uncalculated");
		int y = 0;
		for (double aa : a) {
			if (valid(aa))
				y++;
		}
		return y;
	}

	public int update() {
		int s = size();
		if (s <= 0)
			return 0;

		int count = 0;
		if (evi == null || evi.length < s)
			evi = new double[s];
		for (int i = 0; i < s; i++) {
			if (update(i))
				count++;
		}
		if (count == 0) {
			Arrays.fill(evi, 0);
			return 0;
		}
		if (count > 1) {
			//descending
			ArrayUtil.quickSort(0, s, (a, b) ->
					Double.compare(evi[b], evi[a])
				, (a, b) -> {
					ArrayUtil.swapObj(items, a, b);
					ArrayUtil.swapDouble(evi, a, b);
				});
		}
		return count;
	}

	/** removes weakest tasks having overlapping evidence with stronger ones */
	@Nullable private MetalLongSet filter(int minComponents, boolean shrink, boolean needStamp) {

		MetalBitSet.IntBitSet conflict = new MetalBitSet.IntBitSet(); //max 32

		int remain;
		int iterations = 0;

		main: while ((remain = (iterations++ > 0 ? refocus(shrink) : active())) >= minComponents) {

			double[] evi = this.evi;
			Task[] items = this.items;

			int ss = size;
			for (int i = 0; i < ss-1; i++) { //descending

				double ie = evi[i];
				if (!valid(ie))
					continue;
				Task ii = items[i];

				conflict.x = 0;

				double eviConflict = 0;
				for (int j = ss - 1; j > i; j--) { //ascending, j will be weaker
					double je = evi[j];
					if (!valid(je))
						continue;
					if (Stamp.overlap(ii, items[j])) {
						conflict.setFast(j);
						eviConflict += je;
					}
				}

				if (conflict.x != 0) {

					//cost benefit analysis of keeping I or removing the J's that conflict
					if (eviConflict > ie) {

						//remove i
						if (--remain < minComponents) {
							clearFast(); return null; //fail
						}

						evi[i] = 0;
						continue main;
					} else {
						//remove the conflicts
						remain -= conflict.cardinality();
						if (remain < minComponents) {
							clearFast(); return null; //fail
						}

						for (int k = ss-1; k > i; k--) {
							if (conflict.getFast(k))
								evi[k] = 0;
						}
						continue main;
					}

				}
			}

            break; //done
		}

		return (!needStamp || remain <= 0) ? null : stampRemain(remain);
	}

	private MetalLongSet stampRemain(int remain) {
		MetalLongSet e = new MetalLongSet(remain * STAMP_CAPACITY);
		int ss = size;
		for (int i = 0; i < ss; i++) {
			if (valid(i))
				e.addAll(stamp(i));
		}
		return e;
	}

	@Override
	public void clear() {
		super.clear();
		evi = null;
	}

	/** compacts both the items[] and evi[] cache, so that they remain in synch */
	@Override public boolean removeNulls() {
		if (evi == null)
			return super.removeNulls(); //just modify the items[]

		int sizeBefore = size;
		double[] evi = this.evi;
		Object[] items = this.items;
		int sizeAfter = 0;
		//verify that all zero evidence slots also have null tasks, if not then nullify the corresponding task slot
		for (int i = 0; i < sizeBefore; i++) {
			if (valid(evi[i]) && items[i]!=null) {
				sizeAfter++;
			} else {
				items[i] = null;
				evi[i] = 0;
			}
		}
		if (sizeBefore == sizeAfter)
			return false; //no change
		this.size = sizeAfter;

		int sizeCurrent = sizeBefore;
		for (int i = 0; i < sizeCurrent-1; ) {
			if (evi[i] == 0) {
				int span = (--sizeCurrent) - i;
				arraycopy(evi, i + 1, evi, i, span);
				arraycopy(items, i + 1, items, i, span);
			} else
				i++;
		}
		Arrays.fill(evi, sizeAfter, sizeBefore, 0);
		Arrays.fill(items, sizeAfter, sizeBefore, null);
		return true;
	}


	//    /**
//     * a one-for-all and all-for-one decision
//     */
//    private void resolve(int conflict) {
//        if (size() == 2) {
//            removeFast(1); //the weaker will obviously be the 1th one regardless
//            return;
//        }
//        TaskComponent cc = get(conflict);
//        if (cc.evi < eviSumExcept(conflict)) {
//            removeFast(conflict);
//        } else {
//            //remove all except strongest conflict
//            clear();
//            add(cc);
//        }
//    }



	private double eviSum(IntPredicate each) {
		double e = 0;
		int n = size();
		for (int i = 0; i < n; i++) {
			if (each.test(i)) {
				double ce = evi[i];
				if (ce == ce)
					e += ce;
			}
		}
		return e;
	}

	private MetalLongSet commit1(boolean provideStamp) {
		Task only = firstValid();
		term = only.term();
		return provideStamp ? Stamp.toMutableSet(only) : null;
	}

	public Task firstValid() {
		int i = firstValidIndex();
		return i != -1 ? get(i) : null;
	}

    private int firstValidIndex() {
	    return firstValidIndex(0);
    }
    private int firstValidIndex(int after) {
        return indexOf(after, (IntPredicate)(this::valid));
    }

	private int firstValidOrNonNullIndex(int after) {
		return indexOf(after,
            evi!=null ?
				(IntPredicate)(i -> valid(i)) :
				(IntPredicate)(i -> items[i]!=null) /* first non-null */);
	}



    public final boolean nonNull(int i) {
	    return items[i] != null;
    }
	public final boolean valid(int i) {
		return nonNull(i) && valid(evi[i]);
	}

	/**
	 * test for whether an amount of evidence is valid
	 */
	public final boolean valid(double e) {
		return e > Double.MIN_NORMAL;
	}

	public final TruthProjection add(Task... tasks) {
		return add(tasks.length, tasks);
	}

	public final <T extends Tasked> TruthProjection add(int firstN, T... tasks) {
		ensureCapacity(firstN);
		for (int i = 0; i < firstN; i++)
			addFast(tasks[i]);
		return this;
	}

	private TruthProjection add(Iterable<? extends Tasked> tasks) {
		tasks.forEach(this::add);
		return this;
	}

	private void addFast(Tasked t) { addFast(t.task()); }

	public final TruthProjection add(Collection<? extends Tasked> tasks) {
		ensureCapacity(tasks.size());
		tasks.forEach(this::addFast);
		return this;
	}

	private TruthProjection add(Tasked tt) {
		add(tt.task());
		return this;
	}


	private float intermpolateAndCull(NAL nar) {

		int thisSize;
		main:
		while ((thisSize = size()) >= 1) {
			final int root = firstValidIndex();
			Task rootComponent = get(root);
			Term first = rootComponent.term();
			this.term = first;
			if (thisSize == 1 || !first.hasAny(Op.Temporal)) {
				//assumes that all the terms are from the same concept.  so if the first target has no temporal components the rest should not either.
				return 1;
			}


			MetalBitSet matchesFirst = MetalBitSet.bits(thisSize);
			matchesFirst.set(root);
			for (int i = firstValidIndex() + 1; i < thisSize; i++) {
				Task t = this.get(i);
				if (valid(i) && first.equals(t.term()))
					matchesFirst.set(i);
			}
			int mc = matchesFirst.cardinality();
			if (mc > 1) {
				if (mc < thisSize) {
					//HACK this is too greedy
					//exact matches are present.  remove those which are not
					for (int i = firstValidIndex() + 1; i < thisSize; i++)
						if (!matchesFirst.get(i))
							remove(i);
				}
				return 1f;
			} else {


				for (int next = root + 1; next < size; next++) {
					int tryB = firstValidIndex(next);
					if (tryB != -1) {
						Task B = get(tryB);
						Term a = first;
						Term b = B.term();
						final double e2Evi = evi[tryB];

						float dtDiff;
						//HACK this chooses the first available 2+-ary match, there may be better
						if ((Float.isFinite(dtDiff = dtDiff(a, b)))) {

							final double e1Evi = evi[root];

							Term ab;
							try {
								//if there isnt more evidence for the primarily sought target, then just use those components
								ab = Intermpolate.intermpolate((Compound) a, (Compound) b, (float) (e1Evi / (e1Evi + e2Evi)), nar);
							} catch (TermTransformException e) {
								//HACK TODO avoid needing to throw exception
								if (NAL.DEBUG) {
									throw new RuntimeException(e);
								} else {
									//ignore.  it may be a contradictory combination of events.
									ab = Null;
								}
							}

							if (Task.validTaskTerm(ab)) {

								this.term = ab;
								for (int i = 0; i < size; i++)
									if (i != root && i != tryB) {
										if (get(i) != null && !get(i).term().equals(ab))
											remove(i);
									}

								//return 1 - dtDiff * 0.5f; //half discounted
								//return 1 - dtDiff;
								return 1; //no discount for difference
							}


						}
					}
				}
				//removeNulls(); //HACK
				if (eviSum(i -> i != root) >= evi[root]) {
					// if value of remainder > value(0):
					remove(root);  //abdicate current root and continue remaining
					continue main;
				} else {
					size = 1;
					return 1;
				}

//            //last option: remove all except the first
//            removeNulls();
//
//            this.term = first;
//            return 1;


			}
		}
		return 1; //?
	}

	@Override
	public Task remove(int index) {
		items[index] = null;
		evi[index] = 0;
		return null; //HACK
	}

	public byte punc() {
		if (isEmpty()) throw new RuntimeException();
		return get(0).punc();
	}

	@Nullable
	public final Truth truth() {
		return truth(NAL.truth.EVI_MIN, false, false, null);
	}


	public void print() {
		forEach(t -> System.out.println(t.proof()));
	}

	public final long start() {
		return start;
	}

	public final long end() {
		return end;
	}


	/**
	 * aka "shrinkwrap", or "trim". use after filtering cyclic.
	 * adjust start/end to better fit the (remaining) task components and minimize temporalizing truth dilution.
	 * if the start/end has changed, then evidence for each will need recalculated
	 * returns the number of active tasks
	 *
	 * @param all - true if applying to the entire set of tasks; false if applying only to those remaining active
	 */
	private int refocus(boolean shrink) {
		removeNulls();

		int s = size;
		if (s == 0)
			return 0;
		if (shrink || start == ETERNAL) {
			final long u0, u1;

            if (s > 1) {
				long[] union = Tense.union(IntStream.range(0, s)
                        .filter(evi!=null ? this::valid : this::nonNull)
                        .mapToObj(x -> (TaskRegion) get(x)).iterator());

				u0 = union[0];
				u1 = union[1];
			} else {
				Task only = evi!=null ? firstValid() : items[firstValidOrNonNullIndex(0)];
				u0 = only.start();
				u1 = only.end();
			}

            boolean changed = false;
			if (u0 != ETERNAL) {
				if (start == ETERNAL) {
					//override eternal range with the entire calculated union
					changed = time(u0, u1);
				} else {
					if (shrink) {
						if (start < u0 && u0 < this.end) {
							changed = time(u0, end);
						}
						if (this.end > u1 && u1 > start) {
							changed = time(start, u1);
						}
					}
				}
			} else {
				if (start != ETERNAL) {
					changed = time(ETERNAL, ETERNAL);
				}
			}

			return changed || evi == null ? update() : s;
		} else {
			return evi == null ? update() : s;
		}
	}

	/** returns true if the start, end times have changed */
	public boolean time(long s, long e) {
		if (s!=ETERNAL) {
			int dith = this.ditherDT;
			if (dith > 1) {
				s = Tense.dither(s, dith, -1);
				e = Tense.dither(e, dith, +1);
			}
		}
		if (this.start!=s || this.end!=e) {
			this.start = s;
			this.end = e;
			return true;
		} else
			return false;
	}

	/**
	 * Truth Coherency Metric
	 * inversely proportional to the statistical variance of the contained truth's frequency components
	 * <p>
	 * TODO refine, maybe weight by evi
	 */
	@Paper
	public double coherency() {
		int s = size();
		if (s == 0) return 0;
		if (s == 1) return 1;

		double avg = 0;
		for (int i = 0, thisSize = this.size(); i < thisSize; i++) {
		    if (valid(i))
			    avg += this.items[i].freq();
		}
		avg /= s;

		double variance = 0.0;
		for (int i = 0; i < s; i++) {
			double p = items[i].freq();
			double d = p - avg;
			variance += d * d;
		}
		variance /= s;

		return 1 - variance;
	}

	@Deprecated
	public boolean valid() {
		return (active() == size());
	}

	@Nullable
	public Task newTask(double eviMin, boolean ditherTruth, boolean beliefOrGoal, boolean forceProject, NAL n) {
		@Nullable Truth tt = truth(eviMin, ditherTruth, true, n);
		if (tt == null)
			return null;

		if (active()==1) {
			Task only = firstValid();
			return !forceProject ?
				only :
				SpecialTruthAndOccurrenceTask.the(only, tt, start, end);
		} else {
			return merge(this::arrayCommit, term, tt, stamp(n.random()), beliefOrGoal, start, end, n);
		}
	}


//    /**
//     * TODO extend TaskList as TruthTaskList storing evi,freq pairs of floats in a compact float[]
//     */
//    @Deprecated
//    public static class TaskComponent implements Tasked {
//        public final Task task;
//
//        /**
//         * NaN if not yet computed
//         */
//        double evi = Double.NaN;
//
//        TaskComponent(Task task) {
//            this.task = task;
//        }
//
//        @Override
//        public String toString() {
//            return task + "=" + evi;
//        }
//
//        public boolean valid() {
//            return evi == evi;
//        }
//
//        @Override
//        public @Nullable Task task() {
//            return task;
//        }
//
////        void invalidate() {
////            evi = Double.NaN;
////        }
//
//
//        public final long[] stamp() {
//            assert(valid());
////            if (!valid())
////                throw new WTF();
//            return task.stamp();
//        }
//    }

}
