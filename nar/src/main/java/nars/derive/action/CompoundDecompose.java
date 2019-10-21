package nars.derive.action;

import nars.Task;
import nars.attention.TaskLinkWhat;
import nars.derive.Derivation;
import nars.derive.premise.AbstractPremise;
import nars.derive.rule.RuleCause;
import nars.link.*;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Term;
import nars.unify.constraint.TermMatcher;
import org.jetbrains.annotations.Nullable;

public class CompoundDecompose extends NativeHow {

	private final boolean taskOrBelief;

	public CompoundDecompose(boolean taskOrBelief) {
		super();

		if (taskOrBelief)
			taskAndBeliefEqual();  //only structural
		else
			hasBelief(true);

		this.taskOrBelief = taskOrBelief;
		match(taskOrBelief ? TheTask : TheBelief, new TermMatcher.SubsMin((short)1));

	}

	@Override
	protected void run(RuleCause why, Derivation d) {
		Task srcTask = taskOrBelief ? d._task : d._belief;

		Compound src = (Compound) srcTask.term();


		Term tgt = decompose(src, d);
		if (tgt!=null) {
			assert(!(tgt instanceof Neg));
			if (tgt.op().conceptualizable) {
				assert(!tgt.equals(src));

				TaskLinks links = ((TaskLinkWhat) d.x).links;

				AbstractTaskLink l = AtomicTaskLink.link(src, tgt)
					.priSet(srcTask.punc(), srcTask.priElseZero() * links.grow.floatValue());

				//TODO lazy calculate
				l.why = why.why(srcTask /* d */);

				d.x.link(l);
			}


			d.add(new AbstractPremise(srcTask, tgt, why, d));
		}


////TODO

//				if (forward != null) {
//					if (!forward.op().conceptualizable) { // && !src.containsRecursively(forward)) {
//						target = forward;
//					} else {
//
//
//

//					}
//				}
		//}

	}

	/**
	 * selects the decomposition strategy for the given Compound
	 */
	protected static TermDecomposer decomposer(Compound t) {
		switch (t.op()) {
			case IMPL:
				return DynamicTermDecomposer.WeightedImpl;
			case CONJ:
				return DynamicTermDecomposer.WeightedConjEvent;
			default:
				return DynamicTermDecomposer.Weighted;
		}
	}

	/**
	 * determines forward growth target. null to disable
	 * override to provide a custom termlink supplier
	 */
    protected @Nullable
	static Term decompose(Compound src, Derivation d) {
		return decomposer(src).decompose(src, d.random);
	}

}
