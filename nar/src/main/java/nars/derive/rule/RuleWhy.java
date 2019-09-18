package nars.derive.rule;

import nars.$;
import nars.control.Why;
import nars.term.Term;

/**
 * just a cause, not an input channel.
 * derivation inputs are batched for input by another method
 * holds the deriver id also that it can be applied at the end of a derivation.
 */
public final class RuleWhy extends Why {

	public final PremiseRuleBuilder rule;
	public final String ruleString;
	public final Term term;

	public RuleWhy(PremiseRuleBuilder rule, short id) {
		super(id, rule.id);
		this.rule = rule;
		this.ruleString = rule.source;
		this.term = $.p(rule.id, rule.id);
	}

	@Override
	public String toString() {
		return term().toString();
	}

	@Override public Term term() {
		return term;
	}

}
