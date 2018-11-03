package nars.term.var;

import nars.Op;
import nars.term.Term;
import nars.term.Variable;

public final class CommonVariable extends UnnormalizedVariable {



    CommonVariable(/*@NotNull*/ Op type, Variable x, Variable y) {
        super(type, String.valueOf(type.ch) + x + y + type.ch /* include trailing so that if a common variable gets re-commonalized, it wont become confused with repeats in an adjacent variable */);
    }

    @Override
    public int opX() {
        return Term.opX(op(), 1 /* different from normalized variables with a subOp of 0 */);
    }

    public static Variable common(Variable A, Variable B) {
        Op op = A.op();
        //assert(B.op()==op);

        return new CommonVariable(op, A,B);
    }


}
