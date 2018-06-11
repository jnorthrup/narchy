package nars.unify.op;

import nars.$;
import nars.derive.premise.PreDerivation;
import nars.term.control.AbstractPred;

/**
 * a condition on the op of a pattern term (task=0, or belief=1)
 */
public enum AbstractPatternOp  {
    ;






   public static final AbstractPred<PreDerivation> TaskBeliefOpEqual = new AbstractPred<PreDerivation>($.the("TaskBeliefOpEqual")) {

       @Override
       public boolean test(PreDerivation ff) {
           return ff._taskOp == ff._beliefOp;
       }

       @Override
       public float cost() {
           return 0.1f;
       }

   };








































}
