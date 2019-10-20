package nars.control.op;

import nars.Task;
import nars.attention.What;
import nars.concept.Concept;
import org.jetbrains.annotations.Nullable;

/** for repeatedly input / reinforced inputs */
public final class Rememorize extends Remember {

	@Nullable
	transient private Concept concept;

	public Rememorize(Task x, What w) {
		super(x, true, true, true, w);
	}

	public static @Nullable nars.control.op.Rememorize the(Task x, What w) {
		Remember.verify(x, w.nar);
		return new nars.control.op.Rememorize(x, w);
	}

	@Override
	protected Concept concept() {
		var c = this.concept;
		return ((c == null) || c.isDeleted()) ? ((this.concept = super.concept())) : c;
	}

	public void input(float pri) {
		input.pri(pri);
		store();
		link();
	}
}
