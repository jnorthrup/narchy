package nars.derive.action;

import nars.$;
import nars.derive.Derivation;
import nars.derive.PreDerivation;
import nars.derive.rule.CondHow;
import nars.derive.rule.RuleCause;
import nars.term.control.PREDICATE;

/** stateless by default */
public abstract class NativeHow/*Builder*/ extends CondHow {

	protected abstract void run(RuleCause why, Derivation d);

	public float pri(Derivation d) {
		return 1;
	}

	@Override
	protected PREDICATE<PreDerivation>[] conditions() {
		PREDICATE<PreDerivation>[] c = super.conditions();

		this.id = $.impl($.p(c), $.identity(this));
		this.source = id.toString();

		return c;
	}


	@Override
	protected final How action(RuleCause cause) {
		return new MyDeriveAction(cause);
	}


	private final class MyDeriveAction extends How {

		MyDeriveAction(RuleCause cause) {
			super(cause);
		}

		@Override
		public String toString() {
			return why.name.sub(1).toString();
		}

		@Override
		public final float priHeuristic(Derivation d) {
			return NativeHow.this.pri(d);
		}

		@Override
		public final void run(Derivation d) {
			NativeHow.this.run(why, d);
		}
	}
}
