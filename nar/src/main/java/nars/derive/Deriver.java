package nars.derive;

import jcog.Util;
import jcog.data.set.ArrayHashSet;
import jcog.pri.HashedPLink;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import jcog.pri.bag.Bag;
import jcog.pri.bag.impl.hijack.PLinkHijackBag;
import jcog.pri.op.PriMerge;
import jcog.signal.meter.FastCounter;
import jcog.util.ArrayUtil;
import nars.Emotion;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.attention.TaskLinkWhat;
import nars.attention.What;
import nars.control.How;
import nars.control.Why;
import nars.derive.action.AdjacentLinks;
import nars.derive.action.CompoundDecompose;
import nars.derive.action.TaskResolve;
import nars.derive.adjacent.AdjacentIndexer;
import nars.derive.premise.AbstractPremise;
import nars.derive.premise.Premise;
import nars.derive.rule.DeriverProgram;
import nars.derive.rule.PremiseRuleSet;
import nars.derive.time.NonEternalTaskOccurenceOrPresentDeriverTiming;
import nars.derive.util.TimeFocus;
import nars.link.TaskLink;
import nars.link.TaskLinks;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * an individual deriver process: executes a particular Deriver model
 * specified by a set of premise rules.
 * <p>
 * runtime intensity is metered and throttled by causal feedback
 * <p>
 * this is essentially a Function<Premise, Stream<DerivedTask>> but
 * the current level of code complexity makes this non-obvious
 */
public class Deriver extends How {
    int innerLoops = 1;

	/**
	 * variable types unifiable in premise formation
	 */
	public static final int PremiseUnifyVars =
		//VAR_QUERY.bit
		Op.VAR_QUERY.bit | Op.VAR_DEP.bit
		//Op.Variable //all
		;


	final Supplier<DeriverExecutor> _exe =
		() ->//new BagDeriverExecutor();
			new QueueDeriverExecutor();

	final ThreadLocal<DeriverExecutor> exe = ThreadLocal.withInitial(_exe);

	public DeriverProgram rules;


	/**
	 * determines the temporal focus of (TODO tasklink and ) belief resolution to be matched during premise formation
	 * input: premise Task, premise belief target
	 * output: long[2] time interval
	 **/
	public TimeFocus timing;
	//shift heuristic condition probabalities TODO refine
	float PREMISE_SHIFT_EQUALS_ROOT = 0.02f;
	float PREMISE_SHIFT_CONTAINS_RECURSIVELY = 0.05f;
	float PREMISE_SHIFT_OTHER = 0.9f;
	float PREMISE_SHIFT_RANDOM = 0.75f;

	public Deriver(PremiseRuleSet rules) {
		this(rules, new NonEternalTaskOccurenceOrPresentDeriverTiming());
	}

	public Deriver(PremiseRuleSet rules, TimeFocus timing) {
		this(rules
				//HACK adds standard derivation behaviors
				.add(TaskResolve.the)
				.add(new CompoundDecompose(true))
				//.add(new CompoundDecompose(false))
				.add(new AdjacentLinks(new AdjacentIndexer()))
                //TODO functor evaluator
				.compile()
			,
			timing);
	}

	public Deriver(DeriverProgram rules, TimeFocus timing) {
		super();
		NAR nar = rules.nar;
		this.rules = rules;
		this.timing = timing;

		nar.start(this);
	}

	@Override
	public final void next(What w, final BooleanSupplier kontinue) {

		TaskLinks links = ((TaskLinkWhat) w).links;
		if (links.isEmpty())
			return;

		Derivation d = Derivation.derivation.get().next(this, w);


		do {

            for (int i = 0; i < innerLoops; i++)
                d.exe.next();

		} while (kontinue.getAsBoolean());

	}

	public final TaskLink hypothesize(Derivation d) {
		return ((TaskLinkWhat) d.what).links.sample(d.random);  //HACK
	}

	@Override
	public float value() {
		//TODO cache this between cycles
		double v = Util.sumDouble(Why::pri, rules.causes());
		//System.out.println(this + " " + v);
		return (float) v;
	}

	public abstract static class DeriverExecutor {

		protected Derivation d;

		public final void init(Derivation d) {
			this.d = d;
			start();
		}

		/**
		 * gets next premise
		 */
		protected final Premise premise() {
			TaskLink x = d.deriver.hypothesize(d);

			//Pre-resolve
			if (x!=null) {
				Task y = TaskResolve.the.get(x, d);
				if (y != null) // && !x.equals(y))
					return new AbstractPremise(y, x.to());
				else
					return x; //maybe the link can be resolved after further transformation
			}

			return null;
		}

		/**
		 * run a premise
		 */
		protected void run(Premise p, int ttl) {

			FastCounter result = d.derive(p, ttl);

			Emotion e = d.nar.emotion;
			if (result == e.premiseUnderivable1) {
				//System.err.println("underivable1:\t" + p);
			} else {
	//				System.err.println("  derivable:\t" + p);
			}

			//ttlUsed = Math.max(0, deriveTTL - d.ttl);

			//e.premiseTTL_used.recordValue(ttlUsed); //TODO handle negative amounts, if this occurrs.  limitation of HDR histogram
			result.increment();
		}

		public abstract void next();

		protected abstract void start();

		public abstract void add(Premise p);

        static protected float pri(Premise p) {
            Task t = p.task(), b = p.belief();
			float tPri = t.priElseZero();
            if (b != null)
                return
					//Util.or(
					//Util.min(
					Util.mean(
						tPri, b.priElseZero()
					);
            else
                return tPri;
        }

    }

	private static class QueueDeriverExecutor extends DeriverExecutor {

        int premisesPerIter = 4;
        int capacity = premisesPerIter;

		final ArrayHashSet<Premise> queue = new ArrayHashSet<>(capacity);
		//final MRUMap<Premise,Premise> novel = new MRUMap(premiseTTL/2);
        int ttl;

		@Override
		protected void start() {
			queue.clear();
		}

		@Override
		public void next() {

            //novel.clear();
			//queue.clear();

            Premise p = premise();
            if (p!=null)
            	queue.add(p);

            int s = queue.size();
            if (s == 0)
                return;

            if (s > 1) {
				//queue.list.sortThisByFloat(DeriverExecutor::pri); //ascending order because it poll's from the end
				Object[] qq = queue.list.array();
				ArrayUtil.sort(qq, 0, s, (FloatFunction) x -> DeriverExecutor.pri((Premise) x));
				//assert(DeriverExecutor.pri(queue.list.get(0)) <= DeriverExecutor.pri(queue.list.get(s-1)));
			}

			ttl = premisesPerIter;

			Premise r;
			int branchTTL = d.nar.deriveBranchTTL.intValue();
			while ((r = queue.poll()) != null) {
				//TODO scale ttl by the priority normalized relative to the other items in the queue
				run(r, branchTTL);
				if (--ttl <= 0)
					break;
			}

		}

		@Override
		public void add(Premise p) {
			if (/*novel.putIfAbsent(p,p)==null && */ queue.add(p)) {
                int qs = queue.size();
                if (qs >= capacity)
                    d.ttl = 0; //CUT the current premise by depleting its TTL, forcing it to return


//                else if (qs == 0)
//                    novel.clear();
            }

		}
	}

	private static class BagDeriverExecutor extends DeriverExecutor {
		/**
		 * queue of pending premises to fire
		 * TODO use a bag to deduplicate and rank
		 */
		final Bag<Premise, PLink<Premise>> bag =
			//new PLinkArrayBag<>(PriMerge.max, 0); //locking issues
			new PLinkHijackBag<>(PriMerge.max, 0, 3);
		int bufferCap = 4;
		int runBatch = bufferCap / 2;
		private Consumer<PLink<Premise>> each;


		@Override
		protected void start() {
			bag.clear();
			bag.capacity(bufferCap);

			each = (p) -> run(p.get(), d.nar.deriveBranchTTL.intValue());
		}

		@Override
		public void add(Premise p) {
			HashedPLink<Premise> x = new HashedPLink<>(p, pri(p));
			PriReference<Premise> y = bag.put(x);
			//return x == y && !x.isDeleted(); //non-duplicate and accepted
		}

		@Override
		public void next() {
			int cap = bag.capacity();

			int tries = 1 + (cap - bag.size()); //TODO penalize duplicates more
			do {
				Premise x = premise();
				if (x != null) {
					add(x);
					if (bag.size() >= cap)
						break;
				}
			} while (--tries > 0);

			//System.out.println();
			//p.print();
			//System.out.println();

			bag.commit(null);
			bag.pop(d.random, runBatch, each); //HijackBag
			//p.pop(null, runBatch, each); //ArrayBag
//                p.commit();
//                p.sample(rng, runBatch, each);


		}
	}
}

















































































































