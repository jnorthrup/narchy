package nars.control;

import jcog.pri.Prioritizable;
import nars.NAR;
import nars.attention.PriNode;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

public class PriNARPart extends NARPart implements Prioritizable {

	public final PriNode pri;

	public PriNARPart(@Nullable Term id) {
		super(id);
		this.pri = new PriNode(this.id).output(PriNode.Branch.Equal);
		this.pri.pri(0.5f);
	}

	@Override
	protected void starting(NAR nar) {
		super.starting(nar);
		nar.control.add(pri);
	}

	@Override
	protected void stopping(NAR nar) {
		nar.control.remove(pri);
		super.stopping(nar);
	}

	@Override
	public final float pri() {
		return pri.asFloat();
	}

	@Override
	public final float pri(float p) {
		pri.amp(p);
		return pri.amp.floatValue();
	}

}
