package nars.truth.proj;

import jcog.Paper;
import jcog.Skill;
import jcog.TODO;
import jcog.Util;
import jcog.data.bit.MetalBitSet;
import jcog.math.LongInterval;
import jcog.sort.QuickSort;
import jcog.util.ArrayUtil;
import nars.NAL;
import nars.Op;
import nars.Task;
import nars.task.DynamicTruthTask;
import nars.task.ProxyTask;
import nars.task.Tasked;
import nars.task.proxy.SpecialTruthAndOccurrenceTask;
import nars.task.util.TaskList;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.util.Intermpolate;
import nars.time.Tense;
import nars.truth.MutableTruth;
import nars.truth.Stamp;
import nars.truth.Truth;
import org.eclipse.collections.api.block.procedure.primitive.IntIntProcedure;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.PeekableIntIterator;
import org.roaringbitmap.RoaringBitmap;

import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntPredicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static java.lang.System.arraycopy;
import static nars.NAL.STAMP_CAPACITY;
import static nars.Op.BELIEF;
import static nars.Op.GOAL;
import static nars.term.util.Intermpolate.dtDiff;

/**
 * Truth Interpolation and Extrapolation of Temporal Beliefs/Goals
 * see:
 * https:
 * https:
 */
@Skill({"Interpolation", "Extrapolation"})
@Paper
public abstract class TruthProjection extends TaskList {

	protected long start;
	protected long end;

	/**
	 * content target, either equal in all the tasks, or the result is
	 * intermpolated (and evidence reduction applied as necessary)
	 */
	public Term term = null;

	public boolean eternalizeComponents = false;

	/**
	 * used in final calculation of to start/end time intervals
	 */
	private int ditherDT = 1;

	/**
	 * active evidence cache
	 */
	@Nullable double[] evi = null;
	private int minComponents = 1;
	private float dur = 0;

	TruthProjection(long start, long end) {
		super(0);
		time(start, end);
	}

	public TruthProjection with(Task[] t, int numTasks) {
		this.items = t;
		this.size = numTasks;
		return this;
	}

	public static @Nullable Task merge(Supplier<Task[]> tasks, Term x, Truth t, Supplier<long[]> stamp, boolean beliefOrGoal, long start, long end, NAL n) {
		var y = Task.taskTerm(x, beliefOrGoal ? BELIEF : GOAL, !NAL.test.DEBUG_EXTRA);
		if (y == null)
			return null;

		Truth T;
		var neg = y instanceof Neg;
		if (neg) {
			y = y.unneg();
			T = t.neg();
		} else
			T = t;

		var tt = tasks.get();
		if (tt.length == 1) {
			var only = tt[0];

			//wrap the only task wtih Special proxy task
			if (only.start() == start && only.end() == end && only.truth().equals(T))
				return only;
			else {
				if (only instanceof SpecialTruthAndOccurrenceTask || !(only instanceof ProxyTask)) { //TODO other special proxy types
					return SpecialTruthAndOccurrenceTask.the(only, T, start, end);
				} //else: continue below
			}

		}

		var z = new DynamicTruthTask(
			y, beliefOrGoal,
			(T instanceof MutableTruth) ? ((MutableTruth)T).clone() : T,
			n, start, end,
			stamp.get());

		Task.fund(z, tt, true);

		return z;
	}

	private TruthProjection ditherDT(int ditherDT) {
		this.ditherDT = ditherDT;
		return this;
	}

	public TruthProjection ditherDT(NAL nal) {
		return ditherDT(nal.dtDither());
	}

	public final Truth truth(long s, long e, double eviMin, boolean dither, boolean tShrink, NAL n) {
			if (time(s, e)) {
				if (evi!=null)
					update();
				//else: done in commit()
			}

		return commit(tShrink, n) ? get(eviMin, dither, n) : null;
	}

	/**
	 * computes the final truth value
	 * (commit(..) is not invoked here; if commit is necessary, use truth(..) method */
	public abstract @Nullable Truth get(double eviMin, boolean dither, NAL nar);

	private boolean update(int i) {
		var t = items[i];
		return sane(evi[i] = evi(t));
	}

	private double evi(Task t) {
		return TruthIntegration.eviAbsolute(t, start, end, dur /* leak */, eternalizeComponents);
	}


	/**
	 * removes the weakest components sharing overlapping evidence with stronger ones.
	 * should be called after all entries are added
	 */
	public final boolean commit(boolean shrink, NAL n) {
		var s = size;
		if (s < minComponents) return false;

		//quick short-circuiting 2-ary test for overlap
		if (s == 2 && Stamp.overlap(items[0], items[1])) {
			if (minComponents > 1) {
				return false;
			} else {
				nullify(1);
				refocus(shrink);
				return true;
			}
		}

		var r = refocus(shrink);
		if (r < minComponents) return false;

		return r == 1 ? commit1() : commitN(shrink, r, n);
	}

	private boolean commitN(boolean shrink, int activeBefore, NAL n) {
		//note: this assumes the 2-ary test has already been applied

			if (!filter()) {
				//clearFast();
				return false;
			}

		var active = active();
			if (active!=activeBefore) {
				activeBefore = refocus(shrink);
				if (activeBefore==1)
					return true; //reduced to one
			}



		if (active > 1 && items[0].term() instanceof Compound && items[0].hasAny(Op.Temporal)) {

			if (!intermpolate(n))
				return false;


			active = active();
		}

		if (term == null)
			term = items[0].term();

		if (active != activeBefore)
			refocus(shrink);

		return true;
	}


	public int active() {
		var s = this.size;
		return s == 0 ? 0 : (int) IntStream.range(0, s).filter(i -> sane(this.evi[i])).count();
	}

	private int update() {
		var s = size;
		if (s <= 0)
			return 0;

		var evi = this.evi;
		if (evi == null || evi.length < s)
			this.evi = new double[s];

		var result = IntStream.range(0, s).filter(this::update).count();
		var count = (int) result;

        if (count > 0) {
			if (count!=s)
				removeNulls();

			if (count > 1)
				sortAndShuffle();
		}

		return count;
	}

	private void sortAndShuffle() {
		IntIntProcedure swapper = this::swap;
		var s = size;

		QuickSort.quickSort(0, s, this::eviComparator, swapper); //descending


		var evi = this.evi;

		//shuffle spans of equivalent items
		var last = evi[0];
		var contig = 0;
		for (var i = 1; i <= s; i++) {
			var ei = i < s ? evi[i] : Double.NaN;
			if (ei != last) {
				if (contig > 0) {
					if (i == s) i--;
					ArrayUtil.shuffle(i - contig, i, ThreadLocalRandom.current(), swapper);
					contig = 0;
				}
				last = ei;
			} else {
				contig++;
			}
		}
	}

	/**
	 * removes weakest tasks having overlapping evidence with stronger ones
	 */
	public boolean filter() {


		var activeBefore = active();
		if (activeBefore < minComponents)
			return false;

		var remain = activeBefore;

		//int iterations = 0;
		//main: while ((remain = (iterations++ > 0 ? refocus(shrink) : active())) >= minComponents) {

		var evi = this.evi;
		var items = this.items;

		var ss = size;
		var conflict = new MetalBitSet.IntBitSet(); //max 32
		for (var i = 0; i < ss - 1; i++) { //descending

			var ie = evi[i];
			if (!sane(ie))
				continue;
			var ii = items[i];

			conflict.x = 0;

			double eviConflict = 0;
			for (var j = ss - 1; j > i; j--) { //ascending, j will be weaker
				var je = evi[j];
				if (!sane(je))
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
					if (--remain < minComponents)
						return false;

					nullify(i);
				} else {
					//remove the conflicts
					remain -= conflict.cardinality();
					if (remain < minComponents) {
						return false; //fail
					}

					for (var k = ss - 1; k > i; k--) {
						if (conflict.getFast(k))
							nullify(k);
					}
				}
				//continue main;

			}
		}


		return remain > 0;
	}

	/**
	 * heuristic range contraction to avoid evidence dilution ie. preserve evidence density
	 *
	 * assumes that the current start,end interval is the temporal union of all tasks
	 * returns true if postfiltering removed something
	 * TODO refine
	 * */
	private void concentrate() {
		//assert(start!=ETERNAL && start!=TIMELESS);


		//TODO calculate exact threshold necessary to not dilute further
		var items = this.items;

//		long[] uu = unionInterval();
//		final long us = uu[0], ue = uu[1];
//		if (us == ue)
//			return; //all collapse at a point anyway

		long us = start, ue = end;
		if (us == TIMELESS)
			throw new TODO("compute union here");
		if (us == ue)
			return;


		//first non-eternal
		var rootIndex = 0;
		var size = this.size;
		while (items[rootIndex].isEternal()) {
			rootIndex++;
			if (rootIndex >= size)
				return; //all eternal
		}

		var root = items[rootIndex];
		var rs = root.start();
		var re = root.end();

		if (rs!=us || re!=ue) {
			var evi = this.evi;

			double eviSum = 0, eviRoot = 0;
			for (var i = rootIndex; i < size; i++) {
				var ti = items[i];
				var tis = ti.start();
				if (tis!=ETERNAL) {
					var ei = evi[i];
					eviSum += ei;
				 	if (i>rootIndex) {
						//eviOther += ei;
						var tie = ti.end();
						eviRoot += ei * ((1+LongInterval.intersectLength(tis, tie, rs,re)) / (tie-tis+1.0));
					} else
						eviRoot += ei; //root itself
				}
			}

			double ud = 1 + (ue - us);
			var densityUnion = eviSum / (1 + ud);
			var densityRoot = eviRoot / (1 + re - rs);
			//System.out.println(Texts.n4(densityRoot) +"/"+ Texts.n4(densityUnion) + " : " + this);

			if (densityUnion / densityRoot < NAL.truth.concentrate_density_threshold) {
				//collapse to root
				if (time(rs, re))
					update();
				else
					assert(false); //shouldnt
			} else {
				//keep union

			}
		}

//		double esum = evi[0];
//
//		boolean changed = time(us, ue);
//
//		double densityLossThreshold = 0.5/activeAfter;
//		//TODO utilize frequency deviation to increase acceptable loss
//		for (int i = 1; i < activeAfter; i++) {
//			Task ii = items[i];
//			long ts = ii.start();
//			if (ts == ETERNAL)
//				continue; //auto include eternal components
//
//			if (changed)
//				update(i);
//
//			double ei = evi[i];
//			long te = ii.end();
//			long ns = Math.min(us, ts), ne = Math.max(ue, te);
//			if (i > minComponents && (ns < us || ne > ue)) {
//				//if it has stretched the time range,
//				//determine if any additional evidence dilution is justifiable.
//				//if justifiable, allow stretch
//				double densityWithout = esum / (ue - us + 1);
//				double densityWith = (esum + ei) / (ne - ns + 1);
//				double densityDiff = (densityWithout - densityWith);
//				double densityLossFactor = (densityDiff)/(densityWithout);
//				if (densityLossFactor > densityLossThreshold) {
//					//dont stretch
//				} else {
//					if (time(ns, ne)) {
//						update(0, i+1);
//						changed = true;
//						//recalculate eSum
//						esum = 0;
//						for (int e = 0; e < i; e++)
//							esum += evi[e];
//						ei = evi[i];
//					}
//				}
//			}
//			esum += ei;
//		}
	}

	/**
	 * computes a stamp by sampling from components in proportion to their evidence contribution
	 */
	public long[] stampSample(int capacity, Random rng) {
		removeNulls(); //HACK
		var n = active();
		if (n == 0)
			throw new NullPointerException();

		@Nullable var s0 = stamp(0);
		if (n == 1) {
			assert (s0.length <= capacity);
			return s0;
		}

		if (n == 2) {
			return Stamp.zip(s0, stamp(1), (float) (evi[0] / (evi[0] + evi[1])), capacity);
		}

		var lenSum = IntStream.range(0, n).map(i1 -> stamp(i1).length).sum();

		if (lenSum <= capacity) {
			//return Stamp.toMutableSet(maxPossibleStampLen, this::stamp, n).toSortedArray();


			//TODO use insertion sort into array
			var l = new LongArrayList(lenSum);
			for (var i = 0; i < n; i++) {
				for (var s : stamp(i)) {
					if (!l.contains(s))
						l.add(s);
				}
			}
			return l.toSortedArray();

		} else {
			//sample n-ary
			// TODO weight contribution by evidence
			return Stamp.sample(capacity, Stamp.toMutableSet(lenSum, this::stamp, n), rng);
		}

		//		MetalLongSet e = new MetalLongSet(remain * STAMP_CAPACITY);
//		int ss = size;
//		for (int i = 0; i < ss; i++) {
//			if (valid(i))
//				e.addAll(stamp(i));
//		}
//		return e;
	}

	public void clear(long s, long e) {
		clear();
		start = s; end = e;
	}

	@Override
	public void clear() {
		super.clear();
		evi = null;
	}

	/**
	 * compacts both the items[] and evi[] cache, so that they remain in synch
	 */
	@Override
	public boolean removeNulls() {
		if (evi == null)
			return super.removeNulls(); //just modify the items[]

		var sizeBefore = size;
		var evi = this.evi;
		Object[] items = this.items;
		var sizeAfter = 0;
		//verify that all zero evidence slots also have null tasks, if not then nullify the corresponding task slot
		for (var i = 0; i < sizeBefore; i++) {
			if (sane(evi[i]) && items[i] != null) {
				sizeAfter++;
			} else {
				items[i] = null;
				evi[i] = 0;
			}
		}
		if (sizeBefore == sizeAfter)
			return false; //no change
		this.size = sizeAfter;

		var sizeCurrent = sizeBefore;
		for (var i = 0; i < sizeCurrent - 1; ) {
			if (evi[i] == 0) {
				var span = (--sizeCurrent) - i;
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


	private double eviSum(@Nullable IntPredicate each) {
		return IntStream.range(0, size).filter(i -> each == null || each.test(i)).mapToDouble(i -> evi[i]).filter(TruthProjection::sane).sum();
	}

	private boolean commit1() {
		var only = firstValid();
		term = only.term();
		return true;
	}

	private Task firstValid() {
		var i = firstValidIndex();
		return i != -1 ? get(i) : null;
	}

	private int firstValidIndex() {
		return firstValidIndex(0);
	}

	private int firstValidIndex(int after) {


		return indexOf(after, (IntPredicate) this::valid);
	}

	private int firstValidOrNonNullIndex(int after) {
		return indexOf(after,
			evi != null ?
				(IntPredicate) (this::valid) :
				(IntPredicate) (i -> items[i] != null) /* first non-null */);
	}


	public final boolean nonNull(int i) {
		return items[i] != null;
	}

	public final boolean valid(int i) {
		return nonNull(i) && sane(evi[i]);
	}

	/**
	 * test for whether an amount of evidence is valid
	 */
	public static boolean sane(double e) {
		return e > Double.MIN_NORMAL;
	}

	public final TruthProjection add(Task... tasks) {
		return add(tasks.length, tasks);
	}

	public final <T extends Tasked> TruthProjection add(int firstN, T[] tasks) {
		ensureCapacity(firstN);
		for (var i = 0; i < firstN; i++)
			addFast(tasks[i]);
		return this;
	}

//	private TruthProjection add(Iterable<? extends Tasked> tasks) {
//		tasks.forEach(this::add);
//		return this;
//	}

	private void addFast(Tasked t) {
		addFast(t.task());
	}

	public final TruthProjection add(Collection<? extends Tasked> tasks) {
		ensureCapacity(tasks.size());
		for (Tasked task : tasks) {
			addFast(task);
		}
		return this;
	}

	private TruthProjection add(Tasked tt) {
		add(tt.task());
		return this;
	}

	private int eviComparator(int a, int b) {
		return a==b ? 0 : Double.compare(evi[b], evi[a]);
	}

	@Override
	public void swap(int a, int b) {
		if (a != b) {
			ArrayUtil.swapObj(items, a, b);
			ArrayUtil.swapDouble(evi, a, b);
		}
	}

	public TruthProjection minComponents(int minComponents) {
		this.minComponents = minComponents;
		return this;
	}

	/** task evidence leak dur */
	public TruthProjection dur(float dur) {
		if (!Util.equals(this.dur, dur)) {
			this.dur = dur;
			update();
		}

		return this;
	}

	public TruthProjection eternalizeComponents(boolean b) {
		this.eternalizeComponents = b;
		return this;
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

	private boolean intermpolate(NAL nar) {
		var items = this.items;
		var term0 = items[0].term();


		var n = size; //assumes nulls removed

		//TODO special 2-ary case

//		Map<Term, IEntry> roots = null;
		var root0 = term0.root();
		@Nullable var evi = this.evi;
		var allEqual = true;
		for (var i = 1; i < n; i++) {
			var termI = items[i].term();
			if (!termI.equals(term0)) {
				allEqual = false;
				if (!termI.root().equals(root0))
					return false; //differing root
			}
//			else {
//				Term rootI = termI.root();
//				if (roots == null) {
//					if (!root0.equals(rootI)) {
//						roots = new UnifiedMap<>(n - i + 1);
//						IEntry ie = new IEntry(root0);
//						roots.put(root0, ie);
//						for (int j = 0; j < i; j++)
//							ie.add(j, evi[j]); //add existing evidence until the first that change (i)
//					}
//				}
//				if (roots != null) {
//					roots.computeIfAbsent(rootI, IEntry::new).add(i, evi[i]);
//				}
//			}

		}
		if (allEqual)
			return true; //no intermpolation necessary
//		if (roots != null) {
//			//different roots, so choose one and remove the others
//
//			//SHOULD NOT HAPPEN
//
//			if (minComponents > 1) {
//				//eliminate roots for which less than minComponents are present
//				roots.values().removeIf(ii -> {
//					if (ii.id.getCardinality() < minComponents) {
//						intermpolateRemove(ii);
//						return true;
//					}
//					return false;
//				});
//			}
//			int es = roots.size();
//			if (es < minComponents)
//				return false;
//
//			FasterList<Map.Entry<Term, IEntry>> e = new FasterList<>(roots.entrySet());
//			e.sortThisByDouble(x -> -x.getValue().eviSum);
//			//		IEntry best = e.get(0).getValue();
//			for (int i = 1; i < es; i++) {
//				intermpolateRemove(e.get(i).getValue());
//			}
//		}
//
//		removeNulls();

		var a = (Compound) items[0].term();

		n = size;
		if (n > 1) {
			//Term bestRoot = best.root;
			var ea = evi[0];
			var remain = n;
			for (var B = 1; B < n; B++) {
				var b = (Compound) items[B].term();
				if (!a.equals(b)) {
					var eb = evi[B];
					var eab = ea + eb;
					var ab = Intermpolate.intermpolate(a, b, (float) (ea / eab), nar);
					double diffA, diffB;
					if (ab instanceof Bool ||
						(diffA = dtDiff(ab, a)) >= 1 - Float.MIN_NORMAL ||
						(diffB = dtDiff(ab, b)) >= 1 - Float.MIN_NORMAL) {

						nullify(B); //unexpected error
						if (--remain < minComponents) {
							//clearFast();
							return false;
						}
					} else {

						if (diffB > 0) {
							var discB = 1-diffB; //1 / (1 + diffB * (eb / eab));
							evi[B] *= discB;
						}


						if (diffA > 0) {
							var discA = 1 - diffA; //1 / ((1 + diffA * (ea / eab)) * B); //estimate: shared between all

							if (remain-1 >= minComponents) {
								//determine whether to keep B if B can be removed
								var eviLoss = IntStream.range(0, B).mapToDouble(x -> evi[x] * (1 - discA)).sum();
								if (eviLoss > evi[B]) {
									nullify(B); //intermpolating with B is too costly
									remain--;
									assert(remain >= minComponents);
//									if (--remain < minComponents) {
//										//clearFast();
//										return false;
//									}
								}
							}

							ea = 0;
							var sum = IntStream.range(0, B).mapToDouble(x -> (evi[x] *= discA)).sum();
							ea += sum;
						}


						a = (Compound) ab;
						ea += evi[B];
					}
				}
			}

			if (remain != n) removeNulls();
		}
		this.term = a;

		return true;
	}

	private void intermpolateRemove(IEntry ii) {
		var ic = ii.id.getIntIterator();
		while (ic.hasNext())
			nullify(ic.next());
	}

//	/**
//	 * if necessary, intermpolate a result term and reduce evidence by the dtDiff to each.
//	 * if intermpolation is impossible for any result, nullify that result.
//	 * return false if the projection becomes invalid
//	 */
//	private boolean intermpolate0(NAL nar, int minComponents) {
//
//		int thisSize;
//		main:
//		while ((thisSize = size()) >= 1) {
//			final int root = firstValidIndex();
//			Task rootComponent = get(root);
//			Term first = rootComponent.term();
//			this.term = first;
//			if (thisSize == 1 || !first.hasAny(Op.Temporal)) {
//				//assumes that all the terms are from the same concept.  so if the first target has no temporal components the rest should not either.
//				return true;
//			}
//
//
//			MetalBitSet matchesFirst = MetalBitSet.bits(thisSize);
//			matchesFirst.set(root);
//			for (int i = firstValidIndex() + 1; i < thisSize; i++) {
//				Task t = this.get(i);
//				if (valid(i) && first.equals(t.term()))
//					matchesFirst.set(i);
//			}
//			int mc = matchesFirst.cardinality();
//			if (mc > 1) {
//				if (mc < thisSize) {
//					//HACK this is too greedy
//					//exact matches are present.  remove those which are not
//					for (int i = firstValidIndex() + 1; i < thisSize; i++)
//						if (!matchesFirst.get(i))
//							remove(i);
//				}
//				return true;
//			} else {
//
//
//				for (int next = root + 1; next < size; next++) {
//					int tryB = firstValidIndex(next);
//					if (tryB != -1) {
//						Task B = get(tryB);
//						Term a = first;
//						Term b = B.term();
//						final double e2Evi = evi[tryB];
//
//						float dtDiff;
//						//HACK this chooses the first available 2+-ary match, there may be better
//						if ((Float.isFinite(dtDiff = dtDiff(a, b)))) {
//
//							final double e1Evi = evi[root];
//
//							Term ab;
//							try {
//								//if there isnt more evidence for the primarily sought target, then just use those components
//								ab = Intermpolate.intermpolate((Compound) a, (Compound) b, (float) (e1Evi / (e1Evi + e2Evi)), nar);
//							} catch (TermTransformException e) {
//								//HACK TODO avoid needing to throw exception
//								if (NAL.DEBUG) {
//									throw new RuntimeException(e);
//								} else {
//									//ignore.  it may be a contradictory combination of events.
//									ab = Null;
//								}
//							}
//
//							if (Task.validTaskTerm(ab)) {
//
//								this.term = ab;
//								for (int i = 0; i < size; i++)
//									if (i != root && i != tryB) {
//										if (get(i) != null && !get(i).term().equals(ab))
//											remove(i);
//									}
//
//								//return 1 - dtDiff * 0.5f; //half discounted
//								//return 1 - dtDiff;
//								return true; //no discount for difference
//							}
//
//
//						}
//					}
//				}
//				//removeNulls(); //HACK
//				if (eviSum(i -> i != root) >= evi[root]) {
//					// if value of remainder > value(0):
//					remove(root);  //abdicate current root and continue remaining
//					continue main;
//				} else {
//					size = 1;
//					return true;
//				}
//
////            //last option: remove all except the first
////            removeNulls();
////
////            this.term = first;
////            return 1;
//
//
//			}
//		}
//		return true; //?
//	}

//	@Override
//	public Task remove(int index) {
//		throw new UnsupportedOperationException("use nullify");
//		//nullify(index);
//		//return null;
//	}

	private void nullify(int index) {
		items[index] = null;
		@Nullable var e = this.evi;
		if (e != null)
			e[index] = 0;
	}

	public final byte punc() {
		//if (isEmpty()) throw new RuntimeException();
		return get(0).punc();
	}

	public final @Nullable Truth truth() {
		return truth(start, end, NAL.truth.EVI_MIN, false, false, null);
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

		var s = size;
		if (s == 0)
			return 0;

//		if (start!=ETERNAL && start!=TIMELESS && shrink && anySatisfy(x->x.isEternal()))
//			shrink = false; //disable shrinking any further

		var changed = false;
		if (shrink || start == TIMELESS) {
			long u0, u1;

			if (s > 1) {
				//Tense.union equivalent
//				long[] union = Tense.union(IntStream.range(0, s)
//					.filter(evi != null ? this::valid : this::nonNull)
//					.mapToObj(x -> (TaskRegion) items[x]).iterator());
//				u0 = union[0];
//				u1 = union[1];

				var union = this.unionInterval();
				u0 = union[0]; u1 = union[1];
			} else {
				var only = evi != null ? firstValid() : items[firstValidOrNonNullIndex(0)];
				u0 = only.start();
				u1 = only.end();
			}


			if (u0 != ETERNAL) {
				//temporal result
				if (start == TIMELESS) {
					//override eternal range with the entire calculated union
					changed = time(u0, u1);
				} else {
					if (shrink) {
						var ss = (u0 > start && u0 <= end) ? u0 : start;
						var ee = (u1 < end && u1 >= ss) ? u1 : end;
						changed = time(ss, ee);
					}
				}
			} else {
				//eternal result
				changed = time(ETERNAL, ETERNAL);
			}

		}

		var active = changed || evi == null ? update() : s;

		if (shrink && active>1 && start!=ETERNAL &&  NAL.truth.concentrate)
			concentrate();

		return active;
	}

	protected long[] unionInterval() {
		long u0 = Long.MAX_VALUE, u1 = Long.MIN_VALUE;
		var items = this.items;
		var hasEvi = evi != null;
		var s = size();
		for (var i = 0; i < s; i++) {
			if (hasEvi ? valid(i) : nonNull(i)) {
				var t = items[i];
				var ts = t.start();
				if (ts != ETERNAL) {
					u0 = Math.min(u0, ts); u1 = Math.max(u1, t.end());
				}
			}
		}
		if (u0 == Long.MAX_VALUE) {
			u0 = u1 = ETERNAL; //all eternal
		}
		return new long[] { u0, u1 };
	}


	/**
	 * returns true if the start, end times have changed
	 */
	public boolean time(long s, long e) {
		if (s != TIMELESS && s!=ETERNAL) {
			var dith = this.ditherDT;
			if (dith > 1) {
				s = Tense.dither(s, dith, -1);
				e = Tense.dither(e, dith, +1);
			}
		}
		if (this.start != s || this.end != e) {
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
		var s = size;
		if (s == 0) return 0;
		if (s == 1) return 1;

		double avg = 0;
		for (int i = 0, thisSize = this.size; i < thisSize; i++) {
			if (valid(i))
				avg += this.items[i].freq();
		}
		avg /= s;

		var variance = 0.0;
		for (var i = 0; i < s; i++) {
			double p = items[i].freq();
			var d = p - avg;
			variance += d * d;
		}
		variance /= s;

		return 1 - variance;
	}

	@Deprecated
	public boolean valid() {
		return (active() == size);
	}

	public @Nullable Task task(double eviMin, boolean ditherTruth, boolean beliefOrGoal, boolean forceProject, NAL n) {
		@Nullable var tt = truth(start, end, eviMin, ditherTruth, !forceProject, n);
		if (tt == null)
			return null;

		if (active() == 1) {
			var only = firstValid();
			return !forceProject ?
				only :
				SpecialTruthAndOccurrenceTask.the(only, tt, start, end);
		} else {
			return merge(this::arrayCommit, term, tt, () -> stampSample(STAMP_CAPACITY, n.random()), beliefOrGoal, start, end, n);
		}
	}


}
