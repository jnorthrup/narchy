package nars.term.util.transform;

import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Variable;
import nars.term.atom.Atomic;
import nars.term.var.NormalizedVariable;
import nars.term.var.SpecialOpVariable;
import nars.term.var.UnnormalizedVariable;

import static nars.Op.*;


public abstract class VariableTransform extends RecursiveTermTransform.NegObliviousTermTransform {

    @Override
    public final Term apply(Term x) {
        return (x instanceof Compound && preFilter((Compound)x)) || (x instanceof Variable) ?
            super.apply(x) : x;
    }

    public boolean preFilter(Compound x) {
        return x.hasVars();
    }

    /**
     * change all query variables to dep vars by use of Op.imdex
     */
    public static final TermTransform queryToDepVar = variableTransformN(VAR_QUERY, VAR_DEP);
    public static final TermTransform indepToDepVar = variableTransformN(VAR_INDEP, VAR_DEP);
    public static final TermTransform indepToQueryVar = variableTransformN(VAR_INDEP, VAR_QUERY);


    private static TermTransform variableTransform1(Op from, Op to) {

        return new OneTypeOfVariableTransform(from, to) {
            @Override
            public Term applyAtomic(Atomic atomic) {
                if (!(atomic instanceof nars.term.Variable) || atomic.op() != from)
                    return atomic;
                else {
                    if (atomic instanceof NormalizedVariable) {
                        //just re-use the ID since the input term is expected to have none of the target type
                        return NormalizedVariable.the(to, ((NormalizedVariable) atomic).id());
                    } else {
                        //unnormalized, just compute the complete unnormalized form
                        return unnormalizedShadow((Variable)atomic, to);
                    }

                }
            }
        };
    }

    private static TermTransform variableTransformN(Op from, Op to) {
        return new SimpleVariableTransform(from, to);
    }

    private static UnnormalizedVariable unnormalizedShadow(nars.term.Variable v, Op to) {
        //return new UnnormalizedVariable(to, atomic.toString());
        return new SpecialOpVariable(v, to);
    }


    private static class OneTypeOfVariableTransform extends VariableTransform {

        final int fromBit;
        public final Op to;

        OneTypeOfVariableTransform(Op from, Op to) {
            this(from.bit, to);
        }

        OneTypeOfVariableTransform(int fromBit, Op to) {
            this.fromBit = fromBit;
            this.to = to;
        }

        @Override
        public boolean preFilter(Compound x) {
            return x.hasAny(fromBit);
        }

    }

    /** transforms variables from one type to another */
    private static final class SimpleVariableTransform extends OneTypeOfVariableTransform {


        public SimpleVariableTransform(Op from, Op to) {
            super(from.bit, to);
        }

        @Override
        public Term applyAtomic(Atomic x) {
            return x instanceof nars.term.Variable && x.opBit() == fromBit ?
                    unnormalizedShadow((Variable) x, to) :
                    x;
        }
    }
}
