package nars.term.util.transform;

import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Variable;
import nars.term.atom.Atomic;
import nars.term.var.SpecialOpVariable;
import nars.term.var.UnnormalizedVariable;

import static nars.Op.*;


public abstract class VariableTransform extends RecursiveTermTransform.NegObliviousTermTransform {

    @Override
    public Term applyPosCompound(Compound c) {
        return preFilter(c) ? super.applyPosCompound(c) : c;
    }

    @Override
    public Term applyAtomic(Atomic a) {
        return a instanceof Variable ? applyVariable((Variable)a) : a;
    }

    protected abstract Term applyVariable(Variable v);

    public boolean preFilter(Compound x) {
        return x.hasVars();
    }

    /**
     * change all query variables to dep vars by use of Op.imdex
     */
    public static final TermTransform queryToDepVar = variableTransformN(VAR_QUERY, VAR_DEP);
    public static final TermTransform indepToDepVar = variableTransformN(VAR_INDEP, VAR_DEP);
    public static final TermTransform indepToQueryVar = variableTransformN(VAR_INDEP, VAR_QUERY);



    private static TermTransform variableTransformN(Op from, Op to) {
        return new SimpleVariableTransform(from, to);
    }

    private static UnnormalizedVariable unnormalizedShadow(nars.term.Variable v, Op to) {
        //return new UnnormalizedVariable(to, atomic.toString());
        return new SpecialOpVariable(v, to);
    }


    /** transforms variables from one type to another */
    private static final class SimpleVariableTransform extends VariableTransform {


        public final Op to;
        final int fromBit;

        public SimpleVariableTransform(Op from, Op to) {
            this.fromBit = from.bit;
            this.to = to;
        }

        @Override
        protected Term applyVariable(nars.term.Variable v) {
            return v.opBit() == fromBit ? unnormalizedShadow(v, to) : v;
        }

        @Override
        public boolean preFilter(Compound x) {
            return x.hasAny(fromBit);
        }
    }

    //    private static TermTransform variableTransform1(Op from, Op to) {
//
//        return new OneTypeOfVariableTransform(from, to) {
//            @Override
//            public Term applyAtomic(Atomic atomic) {
//                if (!(atomic instanceof nars.term.Variable) || atomic.op() != from)
//                    return atomic;
//                else {
//                    if (atomic instanceof NormalizedVariable) {
//                        //just re-use the ID since the input term is expected to have none of the target type
//                        return NormalizedVariable.the(to, ((NormalizedVariable) atomic).id());
//                    } else {
//                        //unnormalized, just compute the complete unnormalized form
//                        return unnormalizedShadow((Variable)atomic, to);
//                    }
//
//                }
//            }
//        };
//    }

}
