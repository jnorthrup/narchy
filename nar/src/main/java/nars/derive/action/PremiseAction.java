package nars.derive.action;

import jcog.pri.Prioritized;
import nars.NAL;
import nars.attention.PriNode;
import nars.derive.Derivation;
import nars.derive.rule.RuleWhy;
import nars.term.control.AbstractPred;

/** rankable branch in the derivation fork */
public abstract class PremiseAction extends AbstractPred<Derivation> implements Prioritized {

	public final PriNode pri;

	public final RuleWhy why;

	public PremiseAction(RuleWhy cause) {
		super(cause.term);
		this.why = cause;
		this.pri = new PriNode.Source(this, 0.5f);
	}

	@Override
	public boolean equals(Object obj) {
		return this==obj;// || why.rule.equals(((DeriveAction)obj).why.rule);
	}

	@Override
	public int hashCode() {
		return why.hashCode();
	}

	public final float pri() {
		return pri.pri();
	}

	public final float pri(Derivation d) {
		float v = priElseZero();
		return v >= Float.MIN_NORMAL ? v * priHeuristic(d) : 0;
	}

	/** can implement derivation-dependent gating functions in subclasses here */
	protected float priHeuristic(Derivation d) {
		return 1;
	}

	public abstract void run(Derivation d);

	@Override
	public final boolean test(Derivation d) {
		run(d);
		return d.use(NAL.derive.TTL_COST_BRANCH);
	}
}
