package nars.term;

import java.util.function.Supplier;

public class LazyTerm implements Termed {

	private Object x;

	/** subclasses using this constructor must implement build() */
	protected LazyTerm() {

	}

	public LazyTerm(Supplier<Term> s) {
		this.x = s;
	}

	@Override
	public final Term term() {
		var x = this.x;
		if (x instanceof Term)
			return ((Term)x);
		else {
			var y = build();
			this.x = y;
			return y;
		}
	}

	/** default impl; override to construct without creating separate Supplier */
	protected Term build() {
		return (((Supplier<Termed>)x).get()).term();
	}

}
