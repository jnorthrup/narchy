package nars.derive.rule;

import jcog.util.ArrayUtil;
import nars.control.Cause;
import nars.control.Caused;
import nars.control.Why;
import nars.term.Term;
import nars.term.Termed;

/**
 * just a cause, not an input channel.
 * derivation inputs are batched for input by another method
 * holds the deriver id also that it can be applied at the end of a derivation.
 */
public final class RuleCause extends Cause {

	public final Term rule;
	public String ruleString;

	public RuleCause(Term name, short id) {
		super(id, name);
		this.rule = name;
		//this.ruleString = rule.source;
	}

//	@Override
//	public String toString() {
//		return ruleString;
//		//return term().toString();
//	}

	public final Term why(Caused d) {
		return Why.why(why, d.why());
	}

	public final Termed why(Caused... c) {
		return Why.why(ArrayUtil.add(c, this));
	}
	public final Termed whyLazy(Caused... c) {
		return Why.whyLazy(ArrayUtil.add(c, this));
	}
}
