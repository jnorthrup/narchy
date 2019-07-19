package nars.derive.premise;

import jcog.math.IntRange;
import nars.Task;
import nars.derive.model.Derivation;
import nars.link.DynamicTermLinker;
import nars.link.TaskLink;
import nars.link.TaskLinks;
import nars.link.TermLinker;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.time.When;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/** unbuffered */
abstract public class DefaultPremiseSource extends PremiseSource {

	public final IntRange premisesPerIteration = new IntRange(3, 1, 32);

	public final IntRange termLinksPerTaskLink = new IntRange(1, 1, 8);

	@Override public void premises(Predicate<Premise> p, When when, TaskLinks links, Derivation d) {
		int termLinksPerTaskLink = this.termLinksPerTaskLink.intValue();

		links.sample(d.random, (int) Math.max(1, premisesPerIteration.floatValue() / termLinksPerTaskLink), tasklink -> {
			Task task = tasklink.get(when);
			if (task != null && !task.isDeleted()) {
				for (int i = 0; i < termLinksPerTaskLink; i++) {

					Term target = tasklink.target(task, d);

					if (target.op().conceptualizable) {
						Term reverse = reverse(target, tasklink, task, links, d);
						if (reverse != null && reverse != target)
							target = reverse;
					}

					Term forward = forward(target, tasklink, task, d);
					if (forward != null)
						links.grow(tasklink, task, forward);


					if (!p.test(new Premise(task, target)))
						return false; //cut
				}
			}
			return true;
		});

	}

	@Nullable
	protected TermLinker linker(Term t) {
		return t instanceof Atomic ? null : DynamicTermLinker.Weighted;
	}

	/** determines forward growth target. null to disable
	 *  override to provide a custom termlink supplier */
	@Nullable protected Term forward(Term target, TaskLink link, Task task, Derivation d) {
		if (target.op().conceptualizable) {
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
	 * resolves reverse termlink
	 * return null to avoid reversal
	 * @param links
	 */
	@Nullable
	protected abstract Term reverse(Term target, TaskLink link, Task task, TaskLinks links, Derivation d);
}
