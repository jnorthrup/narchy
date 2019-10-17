package nars.impiler;

import jcog.data.graph.Node;
import jcog.data.graph.path.FromTo;
import jcog.data.graph.search.Search;
import jcog.data.list.FasterList;
import nars.NAL;
import nars.NAR;
import nars.Task;
import nars.task.NALTask;
import nars.term.Neg;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Bool;
import nars.term.util.conj.ConjBuilder;
import nars.term.util.conj.ConjTree;
import nars.truth.MutableTruth;
import nars.truth.PreciseTruth;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.func.NALTruth;
import org.eclipse.collections.api.block.function.primitive.LongToObjectFunction;
import org.eclipse.collections.api.tuple.primitive.BooleanObjectPair;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import static nars.NAL.STAMP_CAPACITY;
import static nars.Op.BELIEF;
import static nars.Op.IMPL;
import static nars.term.atom.Bool.Null;
import static nars.time.Tense.*;

public class ImpilerDeduction extends Search<Term, Task> {

	static final int recursionMin = 2;
	static final int recursionMax = 3;
	static final int volPadding = 2;
	private static final int STAMP_LIMIT = Integer.MAX_VALUE;
	float confMin;

	private long start;
	private long now;
	private final NAR nar;

	boolean forward;

	/**
	 * collects results
	 */
	private @Nullable List<Task> in = null;

	private int volMax;

	final ConjBuilder cc = new ConjTree();

    public ImpilerDeduction(NAR nar) {
		this.nar = nar;
	}


	public @Nullable LongToObjectFunction<Truth> estimator(Termed target, boolean beliefOrGoal) {
		List<Task> t = get(target, nar.time(), false);
		if (t.isEmpty())
			return null;
		return ((when)->{

			double F = 0, E = 0;
			for (Task x : t) {
				Term subj = x.sub(0);
				long eStart = when - subj.eventRange();
				Truth sTruth = beliefOrGoal ? nar.beliefTruth(subj, eStart) : nar.goalTruth(subj, eStart);
				if (sTruth!=null) {
					Truth implTruth = x.truth();
					boolean neg = implTruth.isNegative();
					Truth c = NALTruth.Deduction.apply(sTruth, implTruth.negIf(neg), 0, nar); //TODO correct truth func for goal
					if (c != null) {
						double ce = c.evi();
						E += ce;
						float cf = c.freq();
						F += (neg ? 1 - cf : cf) * ce;
					}
				}
			}
			if (E > NAL.truth.EVI_MIN) {
				return PreciseTruth.byEvi((F/E), E);
			} else
				return null;
		});
	}

	/**
	 * get the results
	 */
	public /* synchronized */ List<Task> get(Termed _target, long when, boolean forward) {

		this.in = null; //reset for repeated invocation

		Term target = _target.term();
		if (target.op() == IMPL)
			target = target.sub(forward ? 0 /* subj */ : 1 /* pred */);

        Term target1 = target.unneg();
		this.forward = forward;
		this.start = when;

		Impiler.ImplNode rootNode = Impiler.node(target, true, nar);
		if (rootNode != null) {
			this.volMax = nar.termVolMax.intValue();
			this.confMin = nar.confMin.floatValue();
			this.now = nar.time();

			bfs(rootNode);
			clear(); //clear search log

			if (in != null)
				return in;
		}

		return Collections.EMPTY_LIST;
	}

	/**
	 * out only
	 */
	@Override
	protected Iterable<FromTo<Node<Term, Task>, Task>> find(Node<Term, Task> n, List<BooleanObjectPair<FromTo<Node<Term, Task>, Task>>> path) {

		if (path.size() >= recursionMin && !deduce(path))
			return Collections.EMPTY_LIST; //boundary

		//path may have grown?
		if (path.size() + 1 > recursionMax)
			return Collections.EMPTY_LIST;

		return n.edges(!forward, forward);
	}

	@Override
	protected boolean go(List<BooleanObjectPair<FromTo<Node<Term, Task>, Task>>> path, Node<Term, Task> next) {
		return true;
	}

	/**
	 * returns whether to continue further
	 */
	boolean deduce(List<BooleanObjectPair<FromTo<Node<Term, Task>, Task>>> path) {

		int n = path.size();

		Task[] pathTasks = new Task[n];

		{
			Term iPrev = null;
			int volEstimate = 1 + n / 2; //initial cost estimate
			for (int i = 0, pathSize = path.size(); i < pathSize; i++) {
				BooleanObjectPair<FromTo<Node<Term, Task>, Task>> ii = path.get(forward ? i : (n - 1 - i));
				Task e = ii.getTwo().id();
				Term ee = e.term();
				volEstimate += ee.volume();
				if (volEstimate > volMax - volPadding)
					return false; //estimated path volume exceeds limit


				if (i > 0 && !ee.sub(0).unneg().equals(iPrev.sub(1)))
					return false; //path is not continuous

				pathTasks[i] = e;


				for (int k = 0; k < i; k++)
					if (Stamp.overlap(e, pathTasks[k]))
						return false;

				iPrev = ee;
			}
		}


		MutableTruth tAccum = null;

		long offset = start;


		float dur =
			//0; //?
			nar.dur();

		for (int i = 0, pathTasksLength = pathTasks.length; i < pathTasksLength; i++) {
			Task e = pathTasks[i];
			Truth tCurr = e.truth(now, now, dur, true);
			if (tCurr == null)
				return false; //too weak

			Term ee = e.term();
			int edt = ee.dt();
			if (edt == DTERNAL) edt = 0;

			if (forward) {
				offset += ee.sub(0).eventRange() + edt;
			} else {
				offset += -ee.sub(1).eventRange() - edt;
			}

			if (tAccum == null) {
				tAccum = new MutableTruth(tCurr);
			} else {

				if (ee.sub(0) instanceof Neg) {
					tAccum.negateThis(); //negate incoming truth to match the negated precondition
				}

				if (i == n-1 && e.isNegative()) {
					//negate so that the deduction belief truth is positive because the final implication predicate will be inverted
					tCurr = tCurr.neg();
				}

				Truth tNext = NALTruth.Deduction.apply(tAccum, tCurr, confMin, null);
				if (tNext == null)
					return false;

				tAccum.set(tNext);
			}


		}

		cc.clear();

		Term before = Null, next = Null;
		int zDT = 0;
		long range = Long.MAX_VALUE;
		offset = 0;
		for (int i = 0, pathTasksLength = pathTasks.length; i < pathTasksLength; i++) {
			Task e = pathTasks[i];

			Term ee = e.term();

			long es = e.start();
			if (es != ETERNAL)
				range = Math.min(range, e.end() - es);

			int ees = forward ? 0 : 1;

			before = ee.sub(1 - ees);
			if (forward) before = before.negIf(e.isNegative());

			int dt = ee.dt();
			if (dt == DTERNAL) dt = 0; //HACK
			else if (dt == XTERNAL)
				throw new UnsupportedOperationException();

			next = ee.sub(ees);
			if (!forward) next = next.negIf(e.isNegative());

			zDT = dt;

			if (i == 0)
				cc.add(0, forward ? next : before);

			offset += (forward ? next : before).eventRange() + dt;

			if (i != n - 1) {
				if (!cc.add(offset, (forward ? before : next)))
					return false;
			}

		}

		Term ccc = cc.term();
		if (ccc == Null) return false;

		Term implication = forward ?
			IMPL.the(ccc, zDT, before)
			:
			IMPL.the(ccc, zDT, next);
		if (implication instanceof Bool || implication.volume() > volMax)
			return false;



		if (range == Long.MAX_VALUE)
			range = 0; //all eternal

		long finalStart = start, finalEnd = start + range;
		Task z = Task.tryTask(implication, BELIEF, tAccum, (ttt, tr) ->
			NALTask.the(ttt, BELIEF, tr.dither(nar), now, finalStart, finalEnd, Stamp.sample(STAMP_CAPACITY,
				Stamp.toMutableSet(Math.round(n / 2f * STAMP_CAPACITY), i -> pathTasks[i].stamp(), n),
				nar.random())));
		if (z != null) {

			Task.fund(z, pathTasks, true);

			if (in == null) in = new FasterList<>(1);
			in.add(z);
		}


		return true; //continue
	}


}
