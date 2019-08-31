package nars.derive.premise;

import nars.term.Compound;
import nars.term.Term;
import nars.term.Variable;
import nars.term.util.transform.VariableNormalization;
import nars.term.var.ellipsis.Ellipsis;

public class PremiseRuleNormalization extends VariableNormalization {

    @Override
    protected Term applyPosCompound(Compound x) {
        /** HACK process completely to resolve built-in functors,
         * to override VariableNormalization's override */
        return x.transform(this, x.op(), x.dt());
    }

    @Override
    protected Variable newVariable(/*@NotNull*/ Variable x) {
        if (x instanceof Ellipsis.EllipsisPrototype)
            return Ellipsis.EllipsisPrototype.make((byte) count, ((Ellipsis.EllipsisPrototype) x).minArity);
        else if (x instanceof Ellipsis)
            return x;
        else
            return super.newVariable(x);
    }


}
