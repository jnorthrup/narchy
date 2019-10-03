package nars.derive.action;

import nars.$;
import nars.derive.Derivation;
import nars.derive.PreDerivation;
import nars.derive.rule.ConditionalPremiseRuleBuilder;
import nars.derive.rule.RuleCause;
import nars.term.control.PREDICATE;

/** stateless by default */
abstract public class NativePremiseAction extends ConditionalPremiseRuleBuilder  {

	protected abstract void run(RuleCause why, Derivation d);

	abstract public float pri(Derivation d);

	@Override
	protected PREDICATE<PreDerivation>[] conditions() {
		PREDICATE<PreDerivation>[] p = super.conditions();

		this.id = $.impl($.p(p), $.identity(this));
		this.source = id.toString();

		return p;
	}


	@Override
	protected final PremiseAction action(RuleCause cause) {
		return new MyDeriveAction(cause);
	}


	private final class MyDeriveAction extends PremiseAction {

		MyDeriveAction(RuleCause cause) {
			super(cause);
		}

		@Override
		public String toString() {
			return why.name.sub(1).toString();
		}

		@Override
		public final float priHeuristic(Derivation d) {
			return NativePremiseAction.this.pri(d);
		}

		@Override
		public final void run(Derivation d) {
			NativePremiseAction.this.run(why, d);
		}
	}
}
