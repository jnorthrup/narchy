package nars.derive.action;

import nars.derive.Derivation;
import nars.link.AtomicTaskLink;
import nars.link.DynamicTermDecomposer;
import nars.term.Compound;
import nars.term.Term;
import nars.term.util.Image;

import static nars.Op.INH;
import static nars.Op.PROD;

public class ImageUnfold extends NativePremiseAction {

	{
		single();
		is(TheTask, INH);
		hasAny(TheTask, PROD);
		taskPunc(true,true,true,true);
	}

	@Override
	protected void run(Derivation d) {
		//TODO
//			Term target = link.target(task, d);
//
		Term target = d._taskTerm;
		//if (target instanceof Compound) {
		{
			//experiment: dynamic image transform
			if (/*target.opID() == INH.id && */ d.random.nextFloat() < 0.1f) { //task.term().isAny(INH.bit | SIM.bit)
				Term t0 = target.sub(0), t1 = target.sub(1);
				boolean t0p = t0 instanceof Compound && t0.opID() == PROD.id, t1p = t1 instanceof Compound && t1.opID() == PROD.id;
				if ((t0p || t1p)) {

					Term forward = DynamicTermDecomposer.One.decompose((Compound) (t0p ? t0 : t1), d.random); //TODO if t0 && t1 choose randomly
					if (forward != null) {
						Term y = t0p ? Image.imageExt(target, forward) : Image.imageInt(target, forward);
						if (y instanceof Compound && y.op().conceptualizable)
							d.what.link(AtomicTaskLink.link(y/*, d._beliefTerm*/).priSet(d._task.punc(), d._task.pri()));
					}
				}
			}

		}

	}

	@Override
	public float pri(Derivation d) {
		return 1f;
	}
}
