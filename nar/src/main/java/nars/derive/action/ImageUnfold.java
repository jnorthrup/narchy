package nars.derive.action;

import nars.NAL;
import nars.Task;
import nars.control.Why;
import nars.derive.Derivation;
import nars.derive.rule.RuleCause;
import nars.link.AbstractTaskLink;
import nars.link.AtomicTaskLink;
import nars.link.DynamicTermDecomposer;
import nars.term.Compound;
import nars.term.Term;
import nars.term.util.Image;

import static nars.Op.INH;
import static nars.Op.PROD;

/** dynamic image transform */
public class ImageUnfold extends NativePremiseAction {

	{
		single();
		is(TheTask, INH);
		hasAny(TheTask, PROD);
		taskPunc(true,true,true,true);
	}

	/** TODO move more of these tests to trie predicates */
	@Override protected void run(RuleCause why, Derivation d) {

		Term t = d._taskTerm;

		Term t0 = t.sub(0), t1 = t.sub(1);
		boolean t0p = t0 instanceof Compound && t0.opID() == PROD.id, t1p = t1 instanceof Compound && t1.opID() == PROD.id;
		if (t0p || t1p) {
			Term forward = DynamicTermDecomposer.One.decompose((Compound) (t0p ? t0 : t1), d.random); //TODO if t0 && t1 choose randomly
			if (forward != null) {
				Term y = t0p ? Image.imageExt(t, forward) : Image.imageInt(t, forward);
				if (y instanceof Compound && y.op().conceptualizable) {
					Task tt = d._task;

					AbstractTaskLink link = AtomicTaskLink.link(y/*, d._beliefTerm*/).priSet(tt.punc(), tt.pri());
					link.why = Why.why(d._premise.why(), why.why, NAL.causeCapacity.intValue());

					d.what.link(link);
				}
			}
		}
	}

	@Override
	public float pri(Derivation d) {
		return 1f;
	}
}
