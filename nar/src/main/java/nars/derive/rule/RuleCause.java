package nars.derive.rule;

import nars.NAL;
import nars.control.Cause;
import nars.control.Caused;
import nars.control.Why;
import nars.term.Term;

/**
 * just a cause, not an input channel.
 * derivation inputs are batched for input by another method
 * holds the deriver id also that it can be applied at the end of a derivation.
 */
public final class RuleCause extends Cause {

	public final HowBuilder rule;
	public final String ruleString;

	public RuleCause(HowBuilder rule, short id) {
		super(id, rule.id);
		this.rule = rule;
		this.ruleString = rule.source;
	}

	@Override
	public String toString() {
		return ruleString;
		//return term().toString();
	}


	public final Term why(Caused d) {
		return Why.why(why, d.why());
	}
	public final Term why(Caused... c) {
		return Why.why(why, Why.why(c, NAL.causeCapacity.intValue()-1), Integer.MAX_VALUE);
	}
}
