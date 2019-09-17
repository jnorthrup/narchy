package nars.derive.hypothesis;

import jcog.math.IntRange;
import jcog.signal.meter.FastCounter;
import nars.Emotion;
import nars.NAR;
import nars.Task;
import nars.derive.Derivation;
import nars.derive.premise.Premise;
import nars.link.*;
import nars.term.Compound;
import nars.term.Term;
import nars.term.util.Image;
import nars.time.When;
import org.jetbrains.annotations.Nullable;

import static nars.Op.INH;
import static nars.Op.PROD;

/**
 * unbuffered
 */
abstract public class AbstractHypothesizer implements Hypothesizer {

	public final IntRange premisesPerIteration = new IntRange(2, 1, 32);

	public final IntRange termLinksPerTaskLink = new IntRange(1, 1, 8);

	@Override
	public void premises(When<NAR> when, TaskLinks links, Derivation d) {

		int termLinksPerTaskLink = this.termLinksPerTaskLink.intValue();

		int nLinks = Math.max(1, (int) (premisesPerIteration.floatValue() / termLinksPerTaskLink));

		for (int i = 0; i < nLinks; i++)
			premise(when, links, d, termLinksPerTaskLink);
	}

	public void premise(When<NAR> when, TaskLinks links, Derivation d, int termLinksPerTaskLink) {
		TaskLink tasklink = links.sample(d.random);
		if (tasklink != null) {
			Task task = tasklink.get(when, d.tasklinkTaskFilter);
			if (task != null)
				fireTask(links, d, termLinksPerTaskLink, tasklink, task);
		}
	}

	void fireTask(TaskLinks links, Derivation d, int termLinksPerTaskLink, TaskLink tasklink, Task task) {
		for (int i1 = 0; i1 < termLinksPerTaskLink; i1++)
			firePremise(links, d, tasklink, task);
	}

	private void firePremise(TaskLinks links, Derivation d, TaskLink tasklink, Task task) {

		NAR nar = d.nar;
		Emotion e = nar.emotion;

		int matchTTL = nar.premiseUnifyTTL.intValue();
		int deriveTTL = nar.deriveBranchTTL.intValue();

		//int ttlUsed;

		FastCounter result;

		Premise p;
		try (var __ = e.derive_A_PremiseNew.time()) {
			p = premise(links, d, tasklink, task);
		}
		try (var __ = e.derive_B_PremiseMatch.time()) {
			p = p.match(d, matchTTL);
		}

		if (p != null) {


			result = d.derive(p, deriveTTL);

			if (result == e.premiseUnderivable1) {
				//System.err.println("underivable1:\t" + p);
			} else {
//				System.err.println("  derivable:\t" + p);
			}

			//ttlUsed = Math.max(0, deriveTTL - d.ttl);

		} else {
			result = e.premiseUnbudgetableOrInvalid;
			//ttlUsed = 0;
		}

		//e.premiseTTL_used.recordValue(ttlUsed); //TODO handle negative amounts, if this occurrs.  limitation of HDR histogram
		result.increment();

	}

	@Deprecated
	protected Premise premise(TaskLinks links, Derivation d, TaskLink link, Task task) {
		Term target = link.target(task, d);

		if (target instanceof Compound && !(link instanceof DynamicTaskLink)) {
			//experiment: dynamic image transform
			if (target.opID() == INH.id && d.random.nextFloat() < 0.1f) { //task.term().isAny(INH.bit | SIM.bit)
				Term t0 = target.sub(0),  t1 = target.sub(1);
				boolean t0p = t0 instanceof Compound && t0.opID() == PROD.id, t1p = t1 instanceof Compound && t1.opID() == PROD.id;
				if ((t0p || t1p)) {

					Term forward = DynamicTermDecomposer.One.decompose((Compound)(t0p ? t0 : t1), d.random); //TODO if t0 && t1 choose randomly
					if (forward!=null) {
						Term it = t0p ? Image.imageExt(target, forward) : Image.imageInt(target, forward);
						if (it instanceof Compound && it.op().conceptualizable)
							return new Premise(task, it);
					}
				}
			}

			if (link.isSelf()) {

				if (d.random.nextFloat() < 1f / Math.pow(target.volume(), 1)) {
					//experiment: if self, this pass-thru = direct structural transform
				} else {
					//decompose
					Compound src = (Compound) target;    //link.from(); //task.term();
					Term forward = decompose(src, link, task, d);
					if (forward != null) {
						if (!forward.op().conceptualizable) { // && !src.containsRecursively(forward)) {
							target = forward;
						} else {



							links.grow(link, src, forward, task.punc());

							//if (d.random.nextFloat() > 1f / Math.sqrt(task.term().volume()))
							//if (d.random.nextBoolean())
							target = forward; //continue as self, or eager traverse the new link
						}
					}
				}
			}
		}

		if (target.op().conceptualizable) {
			Term reverse = reverse(target.root(), link, task, links, d);

			if (reverse != null) {
				assert (!reverse.equals(link.from()));
				assert (reverse.op().conceptualizable);
				target = reverse;

				//extra links: dont seem necessary
				//links.grow(link, link.from(), reverse, task.punc());
			}
		}

		//System.out.println(task + "\t" + target);
		return new Premise(task, target);
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
	protected Term decompose(Compound src, TaskLink link, Task task, Derivation d) {
		return decomposer(src).decompose(src, d.random);
	}

	/**
	 * @param target the final target of the tasklink (not necessarily link.to() in cases where it's dynamic).
	 *               <p>
	 *               this term is where the tasklink "reflects" or "bounces" to
	 *               find an otherwise unlinked tangent concept
	 *               <p>
	 *               resolves reverse termlink
	 *               return null to avoid reversal
	 * @param links
	 */
	@Nullable
	protected abstract Term reverse(Term target, TaskLink link, Task task, TaskLinks links, Derivation d);
}
