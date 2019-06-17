package nars.truth.dynamic;

import jcog.util.ObjectLongLongPredicate;
import nars.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.time.Tense;
import nars.truth.Truth;
import nars.truth.func.NALTruth;

import static nars.Op.IMPL;
import static nars.time.Tense.*;

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

        return NALTruth.Abduction.apply(subjTruth, predTruth, 0, null);
    }


    @Override
    public boolean evalComponents(Compound superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
        assert(superterm.op()==IMPL);
        Term subj = superterm.sub(0), pred = superterm.sub(1);

        ///TODO ensure non-collapsing dt if collapse imminent

        if (start == end && start!=ETERNAL) {
            int sdt = superterm.dt();
            if (sdt != XTERNAL && sdt != DTERNAL && sdt != 0) {

                sdt += subj.eventRange();

                //stretch the range to the template's dt
                if (sdt > 0)
                    end = start + sdt;
                else {
                    end = start;
                    start = start + sdt /* it's negative */;
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
    public Term reconstruct(Compound superterm, DynTaskify d, long start, long end) {
        Task subj = d.get(0), pred = d.get(1);
        int dt = (subj.isEternal() || pred.isEternal()) ? 0 : Tense.occToDT(pred.start() - subj.start() - subj.term().eventRange());
        return IMPL.the(dt,
                subj.term().negIf(!d.componentPolarity.get(0)), pred.term());
    }
}
