package nars.truth.proj;

import jcog.Paper;
import jcog.Skill;
import jcog.Util;
import jcog.WTF;
import jcog.data.bit.MetalBitSet;
import jcog.data.list.FasterList;
import jcog.data.set.MetalLongSet;
import jcog.math.LongInterval;
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
import nars.term.atom.Bool;
import nars.term.util.Intermpolate;
import nars.term.util.TermTransformException;
import nars.time.Tense;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.util.Timed;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.PeekableIntIterator;
import org.roaringbitmap.RoaringBitmap;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.IntPredicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static java.lang.System.arraycopy;
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

	public TruthProjection init(Task[] t, int numTasks) {
		this.items = t;
		this.size = numTasks;
		return this;
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
	private MetalLongSet commitN(boolean shrink, int minComponents, boolean needStamp, NAL n) {
		MetalLongSet e = filter(minComponents, shrink, needStamp);
		if (needStamp && e == null)
			return null; //remain < minResults

		int activeBeforeIntermpolate = active();
		if (activeBeforeIntermpolate > 1) {

			if (!intermpolate(n, minComponents)) {
				clearFast(); return null;
			}

			int activePostCull = active();
			if (activePostCull < minComponents) {
				clearFast(); return null; //HACK this test shouldnt be necessary
			}

			if (activePostCull != activeBeforeIntermpolate) {

				refocus(shrink);

				if (needStamp)
					e = stampRemain(activePostCull);
			}
		}

		if (term == null)
			term = items[0].term();

		return needStamp ? e : null;
	}


	public int active() {
		int s = this.size;
		if (s == 0)
			return 0;
		double[] evi = this.evi;
//		if (evi == null)
//			throw new NullPointerException("evi cache is uncalculated");
		int y = 0;
		for (int i = 0; i < s; i++) {
			double aa = evi[i];
			if (valid(aa))
				y++;
		}
		return y;
	}

	public int update() {
		int s = size;
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
//			Arrays.fill(evi, 0);
			return 0;
		} else {
			if (count > 1)
				ArrayUtil.quickSort(0, s, this::eviComparator, this::swap); //descending
			return count;
		}
	}

	/** removes weakest tasks having overlapping evidence with stronger ones */
	@Nullable private MetalLongSet filter(int minComponents, boolean shrink, boolean needStamp) {


		int activeBefore = active();
		if (activeBefore < minComponents) {
			clearFast(); return null; //HACK
		}
		int remain = activeBefore;

		//int iterations = 0;
		//main: while ((remain = (iterations++ > 0 ? refocus(shrink) : active())) >= minComponents) {

			double[] evi = this.evi;
			Task[] items = this.items;

			int ss = size;
			MetalBitSet.IntBitSet conflict = new MetalBitSet.IntBitSet(); //max 32
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
					}
					//continue main;

				}
			}

		//post-filter cull weak tasks that excessively dilute the effective average evidence
		int activeAfter = active();
		if (shrink && activeAfter > minComponents && start!=ETERNAL) {
			if (activeAfter != activeBefore) {
				refocus(shrink);
			}
			activeBefore = activeAfter;

			for (int i = minComponents; i < activeBefore; i++) {
				Task ti = items[i];
				if (ti.isEternal()) continue;
				double thresh = 0.5 * eviSum() * 1.0/activeBefore;
				if (evi[i] < thresh) {
					LongInterval lti = (LongInterval) ti;
					if (!Util.or(k->k!=null && k.contains(lti), 0, i-1, items)) { //if no stronger task contains the interval, then it must be expanding the total focus
						remove(i);
						activeAfter--;
					}
				}
			}
		}

		if (activeAfter != activeBefore) {
			refocus(shrink);
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


	private double eviSum() {
		return eviSum(null);
	}

	private double eviSum(@Nullable IntPredicate each) {
		double e = 0;
		int n = size();
		for (int i = 0; i < n; i++) {
			if (each==null || each.test(i)) {
				double ce = evi[i];
				if (ce == ce && ce >= 0)
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

//	private TruthProjection add(Iterable<? extends Tasked> tasks) {
//		tasks.forEach(this::add);
//		return this;
//	}

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

	private int eviComparator(int a, int b) {
		return Double.compare(evi[b], evi[a]);
	}

	@Override public void swap(int a, int b) {
		if (a!=b) {
			ArrayUtil.swapObj(items, a, b);
			ArrayUtil.swapDouble(evi, a, b);
		}
	}


	private static final class IEntry {
		double eviSum = 0;
		final RoaringBitmap id = new RoaringBitmap();
		final Term root;

		IEntry(Term r) {
			this.root = r;
		}

		public void add(int index, double evi) {
			eviSum += evi;
			id.add(index);
		}
	}

	private boolean intermpolate(NAL nar, int minComponents) {
		int n = size(); //assumes nulls removed
		Map<Term,IEntry> roots = null;
		Task[] items = this.items;
		Term root0 = items[0].term().root();
		@Nullable double[] evi = this.evi;
		for (int i = 1; i < n; i++) {
			Term iRoot = items[i].term().root();
			if (roots==null) {
				if (!root0.equals(iRoot)) {
					roots = new UnifiedMap<>(n - i + 1);
					IEntry ie = new IEntry(root0);
					roots.put(root0, ie);
					for (int j = 0; j < i; j++)
						ie.add(j, evi[j]); //add existing evidence until the first that change (i)
				}
			}
			if (roots!=null) {
				roots.computeIfAbsent(iRoot, IEntry::new).add(i, evi[i]);
			}
		}
		if (roots == null)
			return true; //no intermpolation necessary

		if (minComponents > 1) {
			//eliminate roots for which less than minComponents are present
			roots.values().removeIf(ii -> {
				if (ii.id.getCardinality() < minComponents) {
					intermpolateRemove(ii);
					return true;
				}
				return false;
			});
		}

		int es = roots.size(); if (es < minComponents) { clearFast(); return false; }


		FasterList<Map.Entry<Term, IEntry>> e = new FasterList<>(roots.entrySet());
		e.sortThisByDouble(x -> -x.getValue().eviSum);
//		IEntry best = e.get(0).getValue();
		for (int i = 1; i < es; i++) {
			intermpolateRemove(e.get(i).getValue());
		}

		removeNulls();

		Compound a = (Compound) items[0].term();

		n = size();
		if (n > 1) {
			//Term bestRoot = best.root;
			double ea = evi[0];
			int remain = n;
			for (int B = 1; B < n; B++) {
				Compound b = (Compound) items[B].term();
				if (!a.equals(b)) {
					double eb = evi[B];
					double eab = ea + eb;
					Term ab = Intermpolate.intermpolate(a, b, (float) (ea / eab), nar);
					if (ab instanceof Bool) {
						remove(B); //unexpected error
						if (--remain < minComponents) {
							clearFast();
							return false;
						}
					} else {
						//TODO apply dtDiff error in proportion to the 2+n components
						double diffA = dtDiff(ab, a);
						double diffB = dtDiff(ab, b);
						if (diffA > 0) {
							double discA = 1/((1+diffA * (ea/eab)) * B); //estimate: shared between all
							for (int x = 0; x < B; x++)
								evi[x] *= discA;
						}
						if (diffB > 0) {
							double discB = 1/(1+diffB * (eb/eab));
							evi[B] *= discB;
						}

						a = (Compound) ab;
						ea += eb;
					}
				}
			}

			if (remain!=n) removeNulls();
		}
		this.term = a;

		return true;
	}

	private void intermpolateRemove(IEntry ii) {
		PeekableIntIterator ic = ii.id.getIntIterator();
		while (ic.hasNext())
			remove(ic.next());
	}

	/** if necessary, intermpolate a result term and reduce evidence by the dtDiff to each.
	 * if intermpolation is impossible for any result, nullify that result.
	 * return false if the projection becomes invalid
	 */
	private boolean intermpolate0(NAL nar, int minComponents) {

		int thisSize;
		main:
		while ((thisSize = size()) >= 1) {
			final int root = firstValidIndex();
			Task rootComponent = get(root);
			Term first = rootComponent.term();
			this.term = first;
			if (thisSize == 1 || !first.hasAny(Op.Temporal)) {
				//assumes that all the terms are from the same concept.  so if the first target has no temporal components the rest should not either.
				return true;
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
				return true;
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
								return true; //no discount for difference
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
					return true;
				}

//            //last option: remove all except the first
//            removeNulls();
//
//            this.term = first;
//            return 1;


			}
		}
		return true; //?
	}

	@Override
	public Task remove(int index) {
		items[index] = null;
		if (evi!=null)
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

		boolean changed = false;
		if (shrink || start == ETERNAL) {
			final long u0, u1;

            if (s > 1) {
				long[] union = Tense.union(IntStream.range(0, s)
                        .filter(evi!=null ? this::valid : this::nonNull)
                        .mapToObj(x -> (TaskRegion) items[x]).iterator());

				u0 = union[0];
				u1 = union[1];
			} else {
				Task only = evi!=null ? firstValid() : items[firstValidOrNonNullIndex(0)];
				u0 = only.start();
				u1 = only.end();
			}


			if (u0 != ETERNAL) {
				if (start == ETERNAL) {
					//override eternal range with the entire calculated union
					changed = time(u0, u1);
				} else {
					if (shrink) {
						long ss = (u0 > this.start && u0 <= this.end) ? u0 : start;
						long ee = (u1 < this.end && u1 >= ss) ? u1 : end;
						//long ss = u0, ee = u1;
						changed = time(ss, ee);
					}
				}
			} else {
				if (start != ETERNAL) {
					changed = time(ETERNAL, ETERNAL);
				}
			}

		}
		return changed || evi == null ? update() : s;
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
		@Nullable Truth tt = truth(eviMin, ditherTruth, !forceProject, n);
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


}
