package nars.derive.action;

import nars.Task;
import nars.derive.Derivation;

public class ImageUnfold extends TaskAction {

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
