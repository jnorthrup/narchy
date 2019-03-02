package nars.term.util.transform;

import jcog.Texts;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.var.NormalizedVariable;
import nars.term.var.UnnormalizedVariable;

import static nars.Op.*;


public abstract class VariableTransform extends AbstractTermTransform.FilteredTermTransform {

    @Override public boolean preFilter(Compound x) {
        return x.hasVars();
    }

    /**
     * change all query variables to dep vars by use of Op.imdex
     */
    public static final TermTransform queryToDepVar = variableTransformN(VAR_QUERY, VAR_DEP);
    private static final TermTransform indepToDepVar = variableTransformN(VAR_INDEP, VAR_DEP);
    private static final TermTransform indepToDepVarDirect = variableTransform1(VAR_INDEP, VAR_DEP);

    private static TermTransform variableTransform1(Op from, Op to) {

        return new OneTypeOfVariableTransform(from, to) {
            @Override
            public Term applyAtomic(Atomic atomic) {
                if (!(atomic instanceof nars.term.Variable) || atomic.op() != from)
                    return atomic;
                else {
                    if (atomic instanceof NormalizedVariable) {
                        //just re-use the ID since the input term is expected to have none of the target type
                        return NormalizedVariable.the(to, ((NormalizedVariable) atomic).id);
                    } else {
                        //unnormalized, just compute the complete unnormalized form.
                        return unnormalizedShadow(atomic, to);
                    }

                }
            }
        };
    }

    private static TermTransform variableTransformN(Op from, Op to) {
        return new OneTypeOfVariableTransform(from,to) {
            @Override
            public Term applyAtomic(Atomic atomic) {
                if (!(atomic instanceof nars.term.Variable) || atomic.op() != from)
                    return atomic;
                else
                    return unnormalizedShadow(atomic, to);
            }
        };
    }

    private static UnnormalizedVariable unnormalizedShadow(Atomic atomic, Op to) {
        return new UnnormalizedVariable(to, to.ch + Texts.quote(atomic.toString()));
    }

    public static Term indepToDepVar(Term x) {
        if (x.varDep()==0) {
            return indepToDepVarDirect.apply(x); //optimized case
        } else if (x.varIndep() >= 1) {
            return indepToDepVar.apply(x);
        } else {
            return x;
        }
    }

    private static class OneTypeOfVariableTransform extends VariableTransform {

        private final Op from;
        private final Op to;

        public OneTypeOfVariableTransform(Op from, Op to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public boolean preFilter(Compound x) {
            return x.hasAny(from.bit);
        }

    }
}
