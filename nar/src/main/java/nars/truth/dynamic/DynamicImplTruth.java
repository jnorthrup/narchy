package nars.truth.dynamic;

import jcog.util.ObjectLongLongPredicate;
import nars.Task;
import nars.concept.TaskConcept;
import nars.task.util.Answer;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Term;
import nars.time.Tense;
import nars.truth.Truth;
import nars.truth.Truthed;
import nars.truth.func.NALTruth;

import java.util.function.Predicate;

import static nars.Op.IMPL;
import static nars.time.Tense.*;

class DynamicImplTruth extends AbstractDynamicTruth {

    private static final Predicate<Task> POSITIVE_FILTER =
            (Truthed::isPositive);
            //(t-> t.freq() >= 2/3f);
    private static final Predicate<Task> NEGATIVE_FILTER =
            (Truthed::isNegative);
            //(t-> t.freq() <= 1/3f);

    @Override
    public boolean temporal() {
        return true;
    }

    /**
     *     B, A, --is(A,"==>") |-          polarizeTask((polarizeBelief(A) ==> B)), (Belief:InductionDepolarized, Time:BeliefRelative)
     *     B, A, --is(B,"==>") |-          polarizeBelief((polarizeTask(B) ==> A)), (Belief:AbductionDepolarized, Time:TaskRelative)
     */
    @Override  public Truth truth(DynTaskify d) {
        Truth subjTruth = d.get(0).truth();
        if (!d.componentPolarity.get(0)) {
            subjTruth = subjTruth.neg();
        }
        assert(d.componentPolarity.get(1));

        Truth predTruth = d.get(1).truth();
        return NALTruth.Abduction.apply(subjTruth, predTruth, Float.MIN_NORMAL, null);
    }

    @Override
    public Task subTask(TaskConcept subConcept, Term subTerm, long subStart, long subEnd, Predicate<Task> filter, DynTaskify d) {
        int currentComponent = d.size();
        if (currentComponent == 0) {

            //use a wrapper filter to try to provide a task matching the target polarity first.
            // if that returns nothing then redo without extra filter

            //assert(NAL.DYN_TASK_MATCH_MODE==1): "match mode (not sampled) highly recommended";

            Predicate<Task> specificFilter = Answer.filter(
                !(subTerm instanceof Neg) ? POSITIVE_FILTER : NEGATIVE_FILTER, filter);
            Task specific = super.subTask(subConcept, subTerm, subStart, subEnd, specificFilter, d);
            if (specific!=null)
                return specific;
        }

        return super.subTask(subConcept, subTerm, subStart, subEnd, filter, d);
    }

    @Override
    public boolean evalComponents(Compound superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
        assert(superterm.op()==IMPL);
        Term subj = superterm.sub(0), pred = superterm.sub(1);

        ///TODO ensure non-collapsing dt if collapse imminent

        if (start == end && start!=ETERNAL) {
            int sdt = superterm.dt(); if (sdt == DTERNAL) sdt = 0;
            if (sdt != XTERNAL) {

                sdt += subj.eventRange();
                sdt += pred.eventRange();

                //stretch the match range
                if (sdt > 0)
                    end = start + sdt;
                else {
                    end = start;
                    start = start + sdt;
                }
            }
        }

        long as, ae, bs, be;
        if (start == ETERNAL) {
            as = ae = bs = be = ETERNAL;
        } else if (start == end) {
            as = bs = ae = be = start;
        } else {
//             switch (ThreadLocalRandom.current().nextInt(subj.unneg().equals(pred) ? 3 : 4)) {
//                 case 0: {
//                     as = ae = start;
//                     bs = be = end;
//                     break;
//                 }
//                 case 1: {
            long mid = (start + end) / 2;
            if (start <= end) {
                as = start;
                ae = mid;
                bs = mid;
                be = end;
            } else {
                as = end;
                ae = mid;
                bs = mid;
                be = start;
            }
//                     break;
//                 }
//                 case 2: {
//                     //reverse
//                     long mid = start == ETERNAL ? ETERNAL : (start + end) / 2;
//                     bs = start;
//                     be = mid;
//                     as = mid;
//                     ae = end;
//                     break;
//                 }
//                 default:
//                 case 3: {
//                     //shared
//                     as = bs = start; ae = be = end;
//                     break;
//                 }
//            }
        }


        return each.accept(subj, as, ae) && each.accept(pred, bs, be);
    }

    @Override
    public Term reconstruct(Compound superterm, long start, long end, DynTaskify d) {
        Task subj = d.get(0), pred = d.get(1);
        int dt = (subj.isEternal() || pred.isEternal()) ? 0 : Tense.occToDT(pred.start() - subj.start() - subj.term().eventRange());
        return IMPL.the(dt,
                subj.term().negIf(!d.componentPolarity.get(0)), pred.term());
    }

    @Override
    public int componentsEstimate() {
        return 2;
    }
}
