package nars.truth.dynamic;

import jcog.util.ObjectLongLongPredicate;
import nars.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.time.Tense;
import nars.truth.Truth;
import nars.truth.func.NALTruth;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static nars.Op.IMPL;

class DynamicImplTruth extends AbstractDynamicTruth {

    /**
     *     B, A, --is(A,"==>") |-          polarizeTask((polarizeBelief(A) ==> B)), (Belief:InductionDepolarized, Time:BeliefRelative)
     *     B, A, --is(B,"==>") |-          polarizeBelief((polarizeTask(B) ==> A)), (Belief:AbductionDepolarized, Time:TaskRelative)
     */
    @Override  public Truth truth(DynTaskify d) {
        Truth subjTruth = d.get(0).truth();
        Truth predTruth = d.get(1).truth();
        if (!d.componentPolarity.get(0)) {
            subjTruth = subjTruth.neg();
        }
        assert(d.componentPolarity.get(1));

        //try both induction and abduction and choose the stronger
        Truth i = NALTruth.Induction.apply(subjTruth, predTruth, 0, null);
        Truth a = NALTruth.Abduction.apply(predTruth, subjTruth, 0, null);
        return Truth.stronger(i, a);
    }

    @Override @Nullable Predicate<Task> filter(Term subTerm, DynTaskify d) {
        return d.size() == 0 ? (!d.componentPolarity.get(0) ? Task::isNegative : Task::isPositive) : null;
    }

    @Override
    public boolean evalComponents(Compound superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
        assert(superterm.op()==IMPL);
        Term subj = superterm.sub(0);
        Term pred = superterm.sub(1);

        return each.accept(subj, start, end) && each.accept(pred, start, end);
    }

    @Override
    public Term reconstruct(Compound superterm, DynTaskify d, long start, long end) {
        Task subj = d.get(0);
        Task pred = d.get(1);
        int dt = (subj.isEternal() || pred.isEternal()) ? 0 : Tense.occToDT(pred.start() - subj.start() - subj.term().eventRange());
        return IMPL.the(dt,
                subj.term().negIf(!d.componentPolarity.get(0)), pred.term());
    }
}
