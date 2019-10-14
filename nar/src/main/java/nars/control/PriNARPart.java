package nars.control;

import jcog.pri.Prioritizable;
import nars.NAR;
import nars.attention.PriSource;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

public class PriNARPart extends NARPart implements Prioritizable {

	public final PriSource pri;

	public PriNARPart(@Nullable Term id) {
		super(id);
		this.pri = new PriSource(this.id, 0.5f);
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
		return pri.pri();
	}

	@Override
	public final PriNARPart pri(float p) {
		pri.pri(p);
		return this;
//		pri.amp(p);
//		return pri.amp.floatValue();
	}

}
