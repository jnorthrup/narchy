package nars.derive.action;

import nars.derive.Derivation;
import nars.derive.rule.RuleWhy;

/** rankable branch in the derivation fork */
public abstract class PremiseAction {

	public final RuleWhy why;

	public PremiseAction(RuleWhy cause) {
		this.why = cause;
	}

	@Override
	public boolean equals(Object obj) {
		return this==obj;// || why.rule.equals(((DeriveAction)obj).why.rule);
	}

	@Override
	public int hashCode() {
		return why.hashCode();
	}

	public abstract float pri(Derivation d);

	public abstract void run(Derivation d);
}
