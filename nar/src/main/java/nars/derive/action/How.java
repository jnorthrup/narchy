package nars.derive.action;

import jcog.Skill;
import jcog.Texts;
import nars.derive.Derivation;
import nars.derive.rule.RuleCause;
import nars.term.control.AbstractPred;


/**
 * rankable branch in the derivation fork
 *
 * instances of How represent a "mental strategy" of thought.
 * a mode of thinking/perceiving/acting,
 * which the system can learn to
 * deliberately apply.
 *
 * a How also implies the existence for a particular reason Why it Should.
 * so there is functional interaction between How's and Why's
 * and their combined role in thinking What-ever.
 *
 * see: https://cogsci.indiana.edu/pub/parallel-terraced-scan.pdf
 *
 * instruments the runtime resource consumption of its iteratable procedure.
 * this determines a dynamically adjusted strength parameter
 * that the implementation can use to modulate its resource needs.
 * these parameters are calculated in accordance with
 * other instances in an attempt to achieve a fair and
 * predictable (ex: linear) relationship between its scalar value estimate
 * and the relative system resources it consumes.
 * <p>
 * records runtime instrumentation, profiling, and other telemetry for a particular Causable
 * both per individual threads, and collectively
 */
@Skill({
	"Effects_unit",
	"Utility_maximization_problem",
	"Optimal_decision",
	"Action_axiom",
	"Norm_(artificial_intelligence)"
})
public abstract class How extends AbstractPred<Derivation>  {

	public final RuleCause why;

	public How(RuleCause cause) {
		super(cause.name);
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

//	public final float pri() {
//		return why.pri;
//	}

	public final float pri(Derivation d) {
		var v = why.pri; //priElseZero();
		return v >= Float.MIN_NORMAL ? v * priHeuristic(d) : 0;
	}

	/** can implement derivation-dependent gating functions in subclasses here. should return a value <= 1 */
	protected float priHeuristic(Derivation d) {
		return 1;
	}

	public abstract void run(Derivation d);

	@Override
	@Deprecated public final boolean test(Derivation d) {
		throw new UnsupportedOperationException();
//		run(d);
//		return d.use(NAL.derive.TTL_COST_BRANCH);
	}

	public void trace(Derivation d) {
		System.out.println("$" + Texts.n4(pri(d)) + " " + this + "\t" + d);
	}
}
