package nars.derive.action;

import nars.Task;
import nars.attention.TaskLinkWhat;
import nars.derive.Derivation;
import nars.derive.premise.AbstractPremise;
import nars.derive.rule.RuleWhy;
import nars.link.*;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Term;
import nars.unify.constraint.TermMatcher;
import org.jetbrains.annotations.Nullable;

public class CompoundDecompose extends NativePremiseAction {

	private final boolean taskOrBelief;

	public CompoundDecompose(boolean taskOrBelief) {
		super();

		if (taskOrBelief)
			single();  //only structural
		else
			hasBelief(true);

		this.taskOrBelief = taskOrBelief;
		match(taskOrBelief ? TheTask : TheBelief, new TermMatcher.SubsMin((short)1));

	}

	@Override
	protected void run(RuleWhy why, Derivation d) {
		Task srcTask = taskOrBelief ? d._task : d._belief;

		Compound src = (Compound) srcTask.term();


		Term tgt = decompose(src, d);
		if (tgt!=null) {
			assert(!(tgt instanceof Neg));
			if (tgt.op().conceptualizable) {
				assert(!tgt.equals(src));

				TaskLinks links = ((TaskLinkWhat) d.what).links;

				TaskLink l = AtomicTaskLink.link(src, tgt);
				((AtomicTaskLink)l).priSet(srcTask.punc(), srcTask.priElseZero() * links.grow.floatValue());
				links.link(l);
			}


			d.add(new AbstractPremise(srcTask, tgt));
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
	protected TermDecomposer decomposer(Compound t) {
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
	@Nullable
	protected Term decompose(Compound src, Derivation d) {
		return decomposer(src).decompose(src, d.random);
	}

	@Override
	public float pri(Derivation d) {
		return 1;
		//return (float) Math.min(1, (0.1f * Math.sqrt(((float)((taskOrBelief ? d._task : d._belief).volume())))/2f));
		//return 1 - 1/(float) Math.sqrt(((float)d._task.volume()) / (d.hasBeliefTruth() ? d.beliefTerm.volume() : 1)) ;
	}
}
