package nars.derive.action;

import nars.Task;
import nars.attention.TaskLinkWhat;
import nars.derive.Derivation;
import nars.derive.action.TaskAction;
import nars.derive.premise.AbstractPremise;
import nars.link.*;
import nars.term.Compound;
import nars.term.Term;
import nars.unify.constraint.TermMatcher;
import org.jetbrains.annotations.Nullable;

public class CompoundDecompose extends TaskAction {

	public CompoundDecompose() {
		super();
		match(TheTask, new TermMatcher.SubsMin((short)1));
	}

	@Override
	protected void accept(Task y, Derivation d) {
		Task srcTask = d._task;

		Compound src = (Compound) srcTask.term();


		Term tgt = decompose(src, d);
		if (tgt!=null) {
			assert(!tgt.equals(src));
			if (tgt.op().conceptualizable) {
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
//						//if (d.random.nextFloat() > 1f / Math.sqrt(task.term().volume()))
//						//if (d.random.nextBoolean())
//						target = forward; //continue as self, or eager traverse the new link
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
	protected float pri(Derivation d) {
		return 1;
	}
}
