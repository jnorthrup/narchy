package nars.derive.action;

import nars.Task;
import nars.attention.TaskLinkWhat;
import nars.derive.Derivation;
import nars.derive.rule.RuleCause;
import nars.link.AbstractTaskLink;
import nars.link.AtomicTaskLink;
import nars.link.DynamicTermDecomposer;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Term;
import nars.term.util.Image;

import static nars.Op.INH;
import static nars.Op.PROD;

/** dynamic image transform */
public class ImageUnfold extends NativeHow {

	{
		single();
		is(TheTask, INH);
		hasAny(TheTask, PROD);
		taskPunc(true,true,true,true);
	}

	/** TODO move more of these tests to trie predicates */
	@Override protected void run(RuleCause why, Derivation d) {
		//if (task.isCyclic()) return; //TODO can this be trie predicate

		Term t = d._taskTerm;

		Term t0 = t.sub(0), t1 = t.sub(1);
		boolean
			t0p = t0 instanceof Compound && t0.opID() == PROD.id,
			t1p = t1 instanceof Compound && t1.opID() == PROD.id;

		if (t0p)
			unfold(why, d, t, t0, t1, true);
		if (t1p)
			unfold(why, d, t, t0, t1, false);
	}

	static boolean unfold(RuleCause why, Derivation d, Term t, Term t0, Term t1, boolean subjOrPred) {
		Compound p = (Compound) (subjOrPred ? t0 : t1);
		Term forward = DynamicTermDecomposer.OnePolarized.decompose(p, d.random); //TODO if t0 && t1 choose randomly
		if (forward != null) {
			Term y = subjOrPred ? Image.imageExt(t, forward) : Image.imageInt(t, forward);
			if (y instanceof Compound) {
				if (y instanceof Neg) {
					y = y.unneg();
					if (!(y instanceof Compound))
						return false;
				}
				if (y.op().conceptualizable) {

					Task task = d._task;

					AbstractTaskLink l = AtomicTaskLink.link(y/*, d._beliefTerm*/).priSet(task.punc(),
						task.pri() * ((TaskLinkWhat) d.what).links.grow.floatValue());

					//TODO lazy calculate
					l.why = why.why(d);

					d.what.link(l);
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public float pri(Derivation d) {
		return 1f;
	}
}
