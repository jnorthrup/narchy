package nars.term.util.conj;

import nars.term.Term;
import nars.unify.UnifyTransform;

import java.util.function.BiPredicate;

public class EventUnifier implements BiPredicate<Term, Term> {
	public final UnifyTransform u;

	public EventUnifier(UnifyTransform s) {
		this.u = s;
	}

	@Override
	public boolean test(Term x, Term y) {
		return x.unify(y, u);
	}

	public void clear() {
		u.clear();
	}

	public Term apply(Term x) {
		return u.apply(x);
	}

}
