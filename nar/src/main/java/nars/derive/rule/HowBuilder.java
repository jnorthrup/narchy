package nars.derive.rule;

import nars.derive.PreDerivation;
import nars.derive.action.How;
import nars.derive.util.DerivationFunctors;
import nars.derive.util.PremiseTermAccessor;
import nars.term.Term;
import nars.term.control.PREDICATE;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

public abstract class HowBuilder {

	public final MutableSet<PREDICATE<PreDerivation>> pre = new UnifiedSet<>(8);
	public Term id;

	/** optional for debugging and other purposes */
	public String source = super.toString();

	/** called once and applies to all rules.  perform any final commit procedure here */
	protected abstract PREDICATE<PreDerivation>[] conditions();

	/** called for each instance of a rule.  avoid recomputing invariants here that could otherwise be computed in conditions() */
	protected abstract How action(RuleCause cause);

	@Override
	public String toString() {
		return id!=null ? id.toString() : source;
	}

	public final PremiseRule get() {

		final PREDICATE[] PRE = conditions();

		return new PremiseRule(this.id, PRE, n ->
			action(n.newCause(s -> new RuleCause(this, s)))
		);
	}


	protected final static PremiseTermAccessor TaskTerm = new PremiseTermAccessor(0, DerivationFunctors.TaskTerm) {
		@Override
		public Term apply(PreDerivation d) {
			return d.taskTerm;
		}
	};
	protected final static PremiseTermAccessor BeliefTerm = new PremiseTermAccessor(1, DerivationFunctors.BeliefTerm) {

		@Override
		public Term apply(PreDerivation d) {
			return d.beliefTerm;
		}
	};

}
