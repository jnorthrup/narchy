package nars.derive.hypothesis;

import jcog.math.IntRange;
import jcog.signal.meter.FastCounter;
import nars.*;
import nars.attention.TaskLinkWhat;
import nars.attention.What;
import nars.derive.Derivation;
import nars.derive.action.NativePremiseAction;
import nars.derive.action.TaskAction;
import nars.derive.premise.Premise;
import nars.link.*;
import nars.table.TaskTable;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.util.Image;
import nars.time.When;
import nars.unify.constraint.TermMatcher;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static nars.Op.*;

/**
 * unbuffered
 */
abstract public class AbstractHypothesizer implements Hypothesizer {

	public final IntRange nLinks = new IntRange(2, 1, 32);

	public final IntRange iterPerTaskLink = new IntRange(1, 1, 8);

	@Override
	public void hypothesize(TaskLinks links, Derivation d) {

		int iterPerTaskLink = this.iterPerTaskLink.intValue();

		int nLinks = this.nLinks.intValue();

		for (int i = 0; i < nLinks; i++)
			fireTaskLink(links, d, iterPerTaskLink);
	}

	void fireTaskLink(TaskLinks links, Derivation d, int iterPerTaskLink) {
		TaskLink tasklink = links.sample(d.random);
		if (tasklink == null) return;


		Premise p = new Premise(tasklink);

		for (int n = 0; n < iterPerTaskLink; n++)
			firePremise(p, d);
	}


//	void fireTask(Task task, TaskLink tasklink, TaskLinks links, Derivation d) {



//		int matchTTL = nar.premiseUnifyTTL.intValue();
//		Premise p;
//		try (var __ = e.derive_A_PremiseNew.time()) {
//			p = premise(task, tasklink, links, d);
//		}
//		try (var __ = e.derive_B_PremiseMatch.time()) {
//			p = p.match(d, matchTTL);
//		}
//		if (p == null)
//			return;


//	}

	protected void firePremise(Premise _p, Derivation d) {

		Premise p = match(d, _p);

		NAR nar = d.nar;
		int deriveTTL = nar.deriveBranchTTL.intValue();

		FastCounter result = d.derive(p, deriveTTL);

		Emotion e = nar.emotion;
		if (result == e.premiseUnderivable1) {
			//System.err.println("underivable1:\t" + p);
		} else {
//				System.err.println("  derivable:\t" + p);
		}

		//ttlUsed = Math.max(0, deriveTTL - d.ttl);

		//e.premiseTTL_used.recordValue(ttlUsed); //TODO handle negative amounts, if this occurrs.  limitation of HDR histogram
		result.increment();
	}

	private Premise match(Derivation d, Premise p) {
		if (p.task.punc()!=COMMAND) //matchable?
			return p.match(d,d.nar.premiseUnifyTTL.intValue());
		else
			return p;
	}

	protected Premise premise(Task task, TaskLink link, TaskLinks links, Derivation d) {
		Term target = link.target(task, d);

		if (target instanceof Compound) {
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
	protected Term decompose(Compound src, Task task, Derivation d) {
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

	public static class TaskResolve extends NativePremiseAction {

		{
			taskPunc(false,false,false,false,true); //commands only
		}

		@Override
		protected void run(Derivation d) {
			Task t = d._task;

			Task task = get((TaskLink)t, d.when, d.tasklinkTaskFilter);
			if (task != null) {
				Premise p = new Premise(task);
				((AbstractHypothesizer)d.deriver.hypo).firePremise(p, d);
			}
		}

		@Nullable Task get(TaskLink t, When<What> when, @Nullable Predicate<Task> filter) {
			return get(t, t.punc(when.x.random()), when, filter);
		}


		@Nullable Task get(TaskLink t, byte punc, When<What> w, @Nullable Predicate<Task> filter) {

			Termed x = t.from();

			if (punc == 0)
				punc = TaskLink.randomPunc(x.term(), w.x.random()); //flat-lined tasklink

			TaskTable table =
				//n.concept(t);
				//n.conceptualizeDynamic(x);
				//beliefOrGoal ? n.conceptualizeDynamic(x) : n.beliefDynamic(x);
				w.x.nar.tableDynamic(x, punc);

			if (table == null || table.isEmpty())
				return null;



//            boolean beliefOrGoal = punc == BELIEF || punc == GOAL;

			//TODO abstract TaskLinkResolver strategy
			Task y;
			if ((punc==BELIEF && NAL.TASKLINK_ANSWER_BELIEF) || (punc==GOAL && NAL.TASKLINK_ANSWER_GOAL))
				y = table.match(w, null, filter, w.dur, false);
			else {
				y = table.sample(w, null, filter);
			}

//            if (y == null) {
//                if (!beliefOrGoal) {
//                    //form question?
//                    float qpri = NAL.TASKLINK_GENERATED_QUESTION_PRI_RATE;
//                    if (qpri > Float.MIN_NORMAL) {
//                        Task.validTaskTerm(x.term(), punc, true);
//                    }
//                }
//
////                if (y == null)
////                    delete(punc); //TODO try another punc?
//            }

			return y;

		}

		@Override
		protected float pri(Derivation d) {
			return 4;
		}
	}

	public static class ReverseLink extends TaskAction {

		public ReverseLink() {
			//allow anything
		}

		@Override
		protected void accept(Task y, Derivation d) {

			Term target = d._beliefTerm.root();

			@Deprecated AbstractHypothesizer h = (AbstractHypothesizer) d.deriver.hypo;

			Task task = d._task;

			TaskLink link = AtomicTaskLink.link(task.term(), target); //HACK

			Term reverse = h.reverse(target, link, task, ((TaskLinkWhat)d.what).links, d);

			if (reverse != null) {
				assert (!reverse.equals(link.from()));
				assert (reverse.op().conceptualizable);

				//extra links: dont seem necessary
				//links.grow(link, link.from(), reverse, task.punc());

				h.firePremise(new Premise(task, reverse), d);
			}

		}

		@Override
		protected float pri(Derivation d) {
			return 2;
		}
	}

	public static class CompoundDecompose extends TaskAction {

		public CompoundDecompose() {
			super();
			single(); //all but command
			match(TheTask, new TermMatcher.SubsMin((short)1));
		}

		@Override
		protected void accept(Task y, Derivation d) {
			Task srcTask = d._task;

			Compound src = (Compound) srcTask.term();

			@Deprecated AbstractHypothesizer h = (AbstractHypothesizer) d.deriver.hypo;
			Term tgt = h.decompose(src, srcTask, d);
			if (tgt!=null) {
				assert(!tgt.equals(src));
				if (tgt.op().conceptualizable) {
					TaskLinks links = ((TaskLinkWhat) d.what).links;

					TaskLink l = AtomicTaskLink.link(src, tgt);
					l.getAndSetPriPunc(srcTask.punc(), srcTask.priElseZero() * links.grow.floatValue());
					links.link(l);
				}

				Premise pp = new Premise(srcTask, tgt);

				h.firePremise(pp, d);
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

		@Override
		protected float pri(Derivation d) {
			return 4;
		}
	}
}
