package nars.op.stm;

import jcog.Util;
import jcog.pri.ScalarValue;
import nars.$;
import nars.NAL;
import nars.Task;
import nars.derive.Derivation;
import nars.derive.action.NativePremiseAction;
import nars.link.AtomicTaskLink;
import nars.term.Term;

import static nars.time.Tense.ETERNAL;

public class STMLinkage2 extends NativePremiseAction {

	volatile Task prev = null;

	{
		taskPattern($.varPattern(1));
		beliefPattern($.varPattern(2));
		taskPunc(true, true, false, false);
	}

	@Override
	protected void run(Derivation d) {

		//TODO opcode
		if (!d.temporal)
			return;

		Task n;
		if (d.taskStart!=ETERNAL && d._task.isInput())
			n = d._task;
		else if (d._belief!=null && d.beliefStart!=ETERNAL && d._belief.isInput())
			n = d._belief;
		else
			return;

		Task p = this.prev;
		if (p == null) {
			this.prev = n;
		} else {
			//if (prev.start() < n.start()) {
				Term pt = p.term().concept();
				Term nt = n.term().concept();
				if (!pt.equals(nt)) {

					float pri =
						(float) NAL.evi(
							Util.mean(n.priElseZero(), p.priElseZero()),
							prev.minTimeTo(n.start(), n.end()),
							d.dur);

					if (pri > ScalarValue.EPSILON) {
						d.what.link(AtomicTaskLink.link(pt, nt).priSet(p.punc(), pri));
						d.what.link(AtomicTaskLink.link(nt, pt).priSet(n.punc(), pri));
					}
					this.prev = n;
				}
			//}
		}
	}

	@Override
	protected float pri(Derivation d) {
		return 1.0f;
	}
}
