package nars.derive.util;

import nars.NAL;
import nars.Op;
import nars.term.Term;
import nars.unify.UnifySubst;
import org.jetbrains.annotations.Nullable;

/**
 * used to determine a premise's "belief task" for the provided "belief term",
 * and other premise functionality at the start of a derivation
 */
public class PremiseBeliefMatcher extends UnifySubst {

	/**
	 * variable types unifiable in premise formation
	 */
	public static final int PremiseUnifyVars =
		//Op.VAR_QUERY.bit
		Op.VAR_QUERY.bit | Op.VAR_DEP.bit
		//Op.Variable //all
		;

	private transient Term output;

	public PremiseBeliefMatcher() {
		super(PremiseUnifyVars, null);
		commonVariables = NAL.premise.PREMISE_UNIFY_COMMON_VARIABLES;
	}

	public @Nullable Term uniSubst(Term taskTerm, Term beliefTerm) {

		this.output = null;

		clear();

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
