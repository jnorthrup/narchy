package nars.derive.op;

import jcog.WTF;
import nars.derive.model.Derivation;
import nars.derive.model.DerivationFailure;
import nars.term.Compound;
import nars.term.Term;
import nars.term.buffer.EvalTermBuffer;
import nars.term.util.transform.VariableTransform;
import nars.term.var.VarIndep;

import java.util.function.Predicate;

import static nars.Op.QUEST;
import static nars.Op.QUESTION;
import static nars.derive.model.DerivationFailure.Success;
import static nars.term.atom.Bool.Null;

public class UnifyMatchFork extends EvalTermBuffer implements Predicate<Derivation> {

    protected Taskify taskify;

    public UnifyMatchFork() {
    }

    public void reset(Taskify taskify) {
        this.taskify = taskify;
    }

    @Override
    public boolean test(Derivation d) {

        d.nar.emotion.deriveUnified.increment();

        Term x = taskify.termify.pattern(d);

        Term y = d.transformDerived.apply(x); //x.transform(d.transformDerived, this, workVolMax);

        Term z = postFilter(y, d);

        if (Success == DerivationFailure.failure(z, (byte) 0 /* dont consider punc consequences until after temporalization */, d)) {
            if (d.temporal)
                taskify.temporalTask(y, z, d);
            else
                taskify.eternalTask(z, d);
        }


        return true; //tried.size() < forkLimit;
    }

    private Term postFilter(Term y, Derivation d) {

        if (y instanceof Compound) {

            if (y.concept() == Null) //TEMPORARY
                throw new WTF();

            //if ((d.concPunc==QUESTION || d.concPunc==QUEST)  && !VarIndep.validIndep(y, true)) {
            if (!VarIndep.validIndep(y, true)) {
                //convert orphaned indep vars to query/dep variables
                Term z = y.transform(
                        (d.concPunc == QUESTION || d.concPunc == QUEST) ?
                                VariableTransform.indepToQueryVar
                                :
                                VariableTransform.indepToDepVar
                );
            }
        }
        return y;
    }

//    public static class DeferredUnifyMatchFork extends UnifyMatchFork {
//
//        @Override
//        public boolean test(Derivation d) {
//            d.post.put(new PostDerivation(d, DeferredUnifyMatchFork.super::test));
//            return true;
//        }
//    }


}
