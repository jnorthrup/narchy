package nars.derive.hypothesis;

import nars.NAL;
import nars.Op;
import nars.Task;
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
import nars.time.When;
import nars.unify.constraint.TermMatcher;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static jcog.util.ArrayUtil.EMPTY_BYTE_ARRAY;
import static nars.Op.BELIEF;
import static nars.Op.GOAL;

/**
 * unbuffered
 */
abstract public class AbstractHypothesizer implements Hypothesizer {



	@Override public Premise hypothesize(TaskLinks links, Derivation d) {
		TaskLink tasklink = links.sample(d.random);
		if (tasklink == null) return null;


		return new Premise(tasklink);
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
			Task x = d._task;

			Task y = get((TaskLink)x, d.when, d.tasklinkTaskFilter);
			if (y != null) // && !x.equals(y))
				d.add(new Premise(y));
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
			return 1;
		}
	}

	public static class ReverseLink extends TaskAction {

		public ReverseLink() {

			//belief term must be conceptualizable
			taskPunc(true,true,true,true);
			match(false, EMPTY_BYTE_ARRAY, new TermMatcher.Is(Op.Conceptualizable), true);
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

				d.add(new Premise(task, reverse));
			}

		}

		@Override
		protected float pri(Derivation d) {
			//return 2;
			return 0.5f/(1f+d.beliefTerm.volume());
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


			Term tgt = decompose(src, d);
			if (tgt!=null) {
				assert(!tgt.equals(src));
				if (tgt.op().conceptualizable) {
					TaskLinks links = ((TaskLinkWhat) d.what).links;

					TaskLink l = AtomicTaskLink.link(src, tgt);
					((AtomicTaskLink)l).priSet(srcTask.punc(), srcTask.priElseZero() * links.grow.floatValue());
					links.link(l);
				}
				
				d.add(new Premise(srcTask, tgt));
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

	public static class ImageUnfold extends TaskAction {

		{
			single();
		}

		@Override
		protected void accept(Task y, Derivation d) {
			//TODO
//			Term target = link.target(task, d);
//
//			if (target instanceof Compound) {
//				//experiment: dynamic image transform
//				if (target.opID() == INH.id && d.random.nextFloat() < 0.1f) { //task.term().isAny(INH.bit | SIM.bit)
//					Term t0 = target.sub(0),  t1 = target.sub(1);
//					boolean t0p = t0 instanceof Compound && t0.opID() == PROD.id, t1p = t1 instanceof Compound && t1.opID() == PROD.id;
//					if ((t0p || t1p)) {
//
//						Term forward = DynamicTermDecomposer.One.decompose((Compound)(t0p ? t0 : t1), d.random); //TODO if t0 && t1 choose randomly
//						if (forward!=null) {
//							Term it = t0p ? Image.imageExt(target, forward) : Image.imageInt(target, forward);
//							if (it instanceof Compound && it.op().conceptualizable)
//								return new Premise(task, it);
//						}
//					}
//				}
//
//			}

		}

		@Override
		protected float pri(Derivation d) {
			return 1;
		}
	}


}
