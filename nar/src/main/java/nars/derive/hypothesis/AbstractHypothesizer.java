package nars.derive.hypothesis;

import jcog.math.IntRange;
import nars.NAR;
import nars.Task;
import nars.derive.Derivation;
import nars.derive.premise.Premise;
import nars.link.*;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.time.When;
import org.jetbrains.annotations.Nullable;

/**
 * unbuffered
 */
abstract public class AbstractHypothesizer implements Hypothesizer {

	public final IntRange premisesPerIteration = new IntRange(2, 1, 32);

	public final IntRange termLinksPerTaskLink = new IntRange(1, 1, 8);

	@Override
	public void premises(When<NAR> when, TaskLinks links, Derivation d) {
		int termLinksPerTaskLink = this.termLinksPerTaskLink.intValue();

		int nLinks = Math.max(1, (int)(premisesPerIteration.floatValue() / termLinksPerTaskLink));
		for (int i = 0; i < nLinks; i++) {
			TaskLink tasklink = links.sample(d.random);
			if (tasklink != null) {
				Task task = tasklink.get(when, d.tasklinkTaskFilter);
				if (task != null)
					fireTask(links, d, termLinksPerTaskLink, tasklink, task);
			}
		}
	}

	void fireTask(TaskLinks links, Derivation d, int termLinksPerTaskLink, TaskLink tasklink, Task task) {
		for (int i1 = 0; i1 < termLinksPerTaskLink; i1++) {
			process(links, d, tasklink, task).derive(d);
		}
	}

	@Deprecated protected Premise process(TaskLinks links, Derivation d, TaskLink link, Task task) {
		Term target = link.target(task, d);
		if (target == null)
			target = task.term();

		if (target.op().conceptualizable) {
			Term reverse = reverse(target, link, task, links, d);
			if (reverse != null)
				target = reverse;
		}

		if (link.isSelf()) {
			Term src =
				//task.term();
				target;
			Term forward = forward(src, link, task, d);
			if (forward != null) {
				if (!forward.op().eventable) { // && !src.containsRecursively(forward)) {
					//throw new WTF();
					target = forward;
				} else {
					links.grow(link, src, forward, task.punc());
				}
			}

		}
		//System.out.println(task + "\t" + target);
		return new Premise(task, target);
	}
//	@Deprecated protected Premise process(TaskLinks links, Derivation d, TaskLink tasklink, Task task) {
//		Term target = null;
//		if (d.random.nextFloat() < 0.5f) {
//			target = tasklink.target(task, d);
//		}
//		if (target == null) {
//			Term src = task.term();
//			Term forward = forward(src, tasklink, task, d);
//			if (forward != null) {
//				target = forward;
//			}
//		}
//		if (target == null) {
//			target = task.term();
//		}
//
//		if (target.op().conceptualizable) {
//			Term reverse = reverse(target, tasklink, task, links, d);
//			if (reverse != null)
//				target = reverse;
//		}
//
////		{
////			Term src = task.term();
////			Term forward = forward(src, tasklink, task, d);
////			if (forward != null) {
////				if (!forward.op().eventable && !src.containsRecursively(forward)) {
//////					//throw new WTF();
////				} else {
////					float freed = links.grow(tasklink, src, forward, task.punc()); //links.links.depressurize(freed);
////
////					//target = forward;
////				}
////			}
////		}
//		System.out.println(task + "\t" + target);
//		return new Premise(task, target);
//	}

	@Nullable
	protected TermLinker linker(Term t) {
		return t instanceof Atomic ? null : DynamicTermLinker.Weighted;
	}

	/**
	 * determines forward growth target. null to disable
	 * override to provide a custom termlink supplier
	 */
	@Nullable
	protected Term forward(Term target, TaskLink link, Task task, Derivation d) {
		if (!(link instanceof DynamicTaskLink) && target.op().conceptualizable) {
			TermLinker linker = linker(target); //TODO custom tasklink-provided termlink strategy
			if (linker != null) {
				Term forward = linker.sample(target, d.random);
				if (!forward.equals(target))
					return forward;
			}

		}
		return null;
	}

	/**
	 * @param target the final target of the tasklink (not necessarily link.to() in cases where it's dynamic)
	 *               resolves reverse termlink
	 *               return null to avoid reversal
	 * @param links
	 */
	@Nullable
	protected abstract Term reverse(Term target, TaskLink link, Task task, TaskLinks links, Derivation d);
}
