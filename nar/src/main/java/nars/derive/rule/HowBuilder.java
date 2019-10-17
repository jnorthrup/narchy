package nars.derive.rule;

import nars.derive.PreDerivation;
import nars.derive.action.How;
import nars.term.Term;
import nars.term.control.PREDICATE;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

public abstract class HowBuilder {

	protected final MutableSet<PREDICATE<PreDerivation>> pre = new UnifiedSet<>(8);
	public Term id;

	/** cause tag for grouping rules under the same cause. if null, receives a unique cause */
	@Nullable public String tag = null;

	/** optional for debugging and other purposes */
	public String source = super.toString();

	/** called once and applies to all rules.  perform any final commit procedure here */
	protected abstract PREDICATE<PreDerivation>[] conditions();

	/** called for each instance of a rule.  avoid recomputing invariants here that could otherwise be computed in conditions() */
	protected abstract How action(RuleCause cause);


	public final String tag() {
		return tag;
	}

	public final HowBuilder tag(@Nullable String tag) {
		this.tag = tag;
		return this;
	}

	@Override
	public String toString() {
		return id!=null ? id.toString() : source;
	}

	public final PremiseRule get() {

		final PREDICATE[] PRE = conditions();

		return new PremiseRule(this.id, this.tag, PRE, this::action);
	}


}
