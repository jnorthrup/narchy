package nars.control;

import nars.attention.AntistaticBag;
import nars.term.Term;

public class PartBag<X extends PriNARPart> extends AntistaticBag<X> {
	public PartBag(int capacity) {
		super(capacity);
	}

	@Override public Term key(X p) {
		return p.id;
	}
}
