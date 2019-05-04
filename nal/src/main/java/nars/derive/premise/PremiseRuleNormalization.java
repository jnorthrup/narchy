package nars.derive.premise;

import nars.term.Compound;
import nars.term.Term;
import nars.term.Variable;
import nars.term.util.transform.VariableNormalization;
import nars.term.var.ellipsis.Ellipsis;

class PremiseRuleNormalization extends VariableNormalization {



    @Override
    protected Term applyFilteredPosCompound(Compound x) {
        /** process completely to resolve built-in functors,
         * to override VariableNormalization's override */
        return applyCompound(x, x.op(), x.dt());
    }

    /*@NotNull*/
    @Override
    protected Variable newVariable(/*@NotNull*/ Variable x) {
        if (x instanceof Ellipsis.EllipsisPrototype)
            return Ellipsis.EllipsisPrototype.make((byte) count,
                ((Ellipsis.EllipsisPrototype) x).minArity);
        else if (x instanceof Ellipsis)
            return x;
        else
            return super.newVariable(x);
    }


}
