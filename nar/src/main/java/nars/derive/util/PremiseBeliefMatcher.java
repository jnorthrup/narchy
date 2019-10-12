package nars.derive.util;

import nars.NAL;
import nars.derive.Deriver;
import nars.term.Term;
import nars.unify.UnifySubst;
import org.jetbrains.annotations.Nullable;

/**
 * used to determine a premise's "belief task" for the provided "belief term",
 * and other premise functionality at the start of a derivation
 */
public class PremiseBeliefMatcher extends UnifySubst {

	transient private Term output;

	public PremiseBeliefMatcher() {
		super(Deriver.PremiseUnifyVars, null);
		commonVariables = NAL.premise.PREMISE_UNIFY_COMMON_VARIABLES;
	}

	@Nullable
	public Term uniSubst(Term taskTerm, Term beliefTerm) {

		this.output = null;

		return unify(beliefTerm, beliefTerm, taskTerm) ? output : null;
	}

	@Override
	protected boolean each(Term y) {
		y = y.unneg();
		if (y.op().conceptualizable) {
			if (!y.equals(input)) {
				output = y;
				return false;  //done
			}
		}
		return true; //continue
	}


}
