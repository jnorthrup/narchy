package nars.derive.rule;

import nars.control.Cause;

/**
 * just a cause, not an input channel.
 * derivation inputs are batched for input by another method
 * holds the deriver id also that it can be applied at the end of a derivation.
 */
public final class RuleCause extends Cause {

	public final PremiseRuleBuilder rule;
	public final String ruleString;

	public RuleCause(PremiseRuleBuilder rule, short id) {
		super(id, rule.id);
		this.rule = rule;
		this.ruleString = rule.source;
	}

	@Override
	public String toString() {
		return ruleString;
		//return term().toString();
	}


}
