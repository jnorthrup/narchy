package nars.derive;

import jcog.Util;
import jcog.data.set.ArrayHashSet;
import jcog.pri.HashedPLink;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import jcog.pri.bag.Bag;
import jcog.pri.bag.impl.hijack.PLinkHijackBag;
import jcog.pri.op.PriMerge;
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
import nars.derive.adjacent.TangentIndexer;
import nars.derive.premise.Premise;
import nars.derive.rule.DeriverProgram;
import nars.derive.rule.PremiseRuleSet;
import nars.derive.time.NonEternalTaskOccurenceOrPresentDeriverTiming;
import nars.derive.util.TimeFocus;
import nars.link.TaskLink;
import nars.link.TaskLinks;

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
	final Supplier<DeriverExecutor> exe =
		() ->//new BagDeriverExecutor();
			new QueueDeriverExecutor();
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
				.add(new TaskResolve())
				.add(new CompoundDecompose(true))
				//.add(new CompoundDecompose(false))
				.add(new AdjacentLinks(new TangentIndexer()))
                //TODO functor evaluator
				.compile()
//           .print()
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

	public final Premise hypothesize(Derivation d) {
		return ((TaskLinkWhat) d.what).links.sample(d.random);  //HACK
	}

	@Override
	public float value() {
		//TODO cache this between cycles
		double v = Util.sumDouble(Why::pri, rules.causes());
		//System.out.println(this + " " + v);
		return (float) v;
	}

	public final short[] what(PreDerivation d) {
		return rules.pre.apply(d);
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
			return d.deriver.hypothesize(d);
		}

		/**
		 * run a premise
		 */
		@Deprecated
		protected void run(Premise p) {
			run(p, d.nar.deriveBranchTTL.intValue());
		}

		protected void run(Premise p, int ttl) {
			d.derive(p, ttl);
		}

		public abstract void next();

		protected abstract void start();

		public abstract void add(Premise p);

        static protected float pri(Premise p) {
            float TASKLINK_RATE = 1f; //1 / deriver.links ...
            //float TASK_STRUCTURE_RATE = 1;//0.5f;

            Task t = p.task();
            Task b = p.belief();
            if (t instanceof TaskLink)
                return t.priElseZero() * TASKLINK_RATE;
            else if (b != null)
                return Util.or(t.priElseZero(), p.belief().priElseZero());
            else
                return t.pri();// * (p.beliefTerm.equals(t.term()) ? TASK_STRUCTURE_RATE : 1);
        }

    }

	private static class QueueDeriverExecutor extends DeriverExecutor {

        int premiseTTL = 5;

		final ArrayHashSet<Premise> queue = new ArrayHashSet(premiseTTL/2);
		//final MRUMap<Premise,Premise> novel = new MRUMap(premiseTTL/2);
        int ttl;

		@Override
		protected void start() {
			queue.clear();
		}

		@Override
		public void next() {

            queue.clear();
            //novel.clear();
            ttl = premiseTTL - 1;

            Premise p = premise();
            if (p==null)
                return;

            run(p);

            int s = queue.size();
            if (s == 0)
                return;
            if (s > 1) {
                queue.list.sortThisByFloat(z -> DeriverExecutor.pri(z)); //ascending order because it poll's from the end
            }
			Premise derived;
			/* LIFO */
			while ((derived = queue.poll()) != null) {
				run(derived);
				if (--ttl <= 0)
					break;
			}

		}

		@Override
		public void add(Premise p) {
			if (/*novel.putIfAbsent(p,p)==null && */ queue.add(p)) {

//                int qs = queue.size();
//                if (qs >= ttl)
//                    d.ttl = 0; //CUT the current premise by depleting its TTL, forcing it to return

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

			each = (nextPremise) -> run(nextPremise.get());

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

















































































































