package nars.derive.op;

import nars.$;
import nars.NAR;
import nars.Param;
import nars.derive.Derivation;
import nars.term.Term;
import nars.term.control.AbstractPred;
import nars.term.util.transform.Retemporalize;
import org.eclipse.collections.api.tuple.Pair;

import java.util.Arrays;

import static nars.Op.*;
import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.TIMELESS;

/**
 * Derivation term construction step of the derivation process that produces a derived task
 * <p>
 * Each rule corresponds to a unique instance of this
 * <p>
 * runtime instance. each leaf of each NAR's derivation tree should have
 * a unique instance, assigned the appropriate cause id by the NAR
 * at initialization.
 */
public final class Termify extends AbstractPred<Derivation> {

    private final Term pattern;

    
    private final Occurrify.TaskTimeMerge time;

    public Termify(Term pattern, Truthify solve, Occurrify.TaskTimeMerge time) {
        super($.funcFast("derive", pattern, solve));
        this.pattern = pattern;

        this.time = time;


    }

    @Override
    public final boolean test(Derivation d) {


        d.concTerm = null;
        d.concOcc = null;
        d.retransform.clear();
//        d.refinements.clear();

        NAR nar = d.nar;

        nar.emotion.deriveTermify.increment();

        Term c1 = d.transform(pattern);




        if (!Taskify.valid(c1, (byte) 0 /* dont consider punc consequences until after temporalization */)) {
            //Term c1e = c1;
            d.nar.emotion.deriveFailEval.increment(/*() ->
                    rule + " |\n\t" + d.xy + "\n\t -> " + c1e
            */);
            return false;
        }

//        if (c1.op() == NEG) {
//            c1 = c1.unneg();
//            if (d.concTruth != null)
//                d.concTruth = d.concTruth.neg();
//        }

        if (c1.volume() - (c1.op()==NEG ? 1 : 0) > d.termVolMax) {
            d.nar.emotion.deriveFailVolLimit.increment();
            return false;
        }

//        if (!d.refinements.isEmpty()) {
//
//            for (Map.Entry<Term,Term> xy : d.refinements.entrySet()) {
//                Term x = xy.getKey(), y = xy.getValue();
//                Term cr = c1.replace(x, y);
//                if (cr.op() == c1.op()) //TODO other validity tests?
//                    c1 = cr;
//            }
//        }

        if (d.temporal) {

//            boolean unwrapNeg;
//            if (c1.op()==NEG) {
//                unwrapNeg = true;
//                c1 = c1.unneg();
//            } else {
//                unwrapNeg = false;
//            }



            Pair<Term, long[]> timing = time.occurrence(d, c1);
            if (timing == null) {
                d.nar.emotion.deriveFailTemporal.increment();
                ///*temporary:*/ time.solve(d, c1);
                return false;
            }


            Term c2 = timing.getOne();
            long[] occ = timing.getTwo();
            if (!((occ[0] != TIMELESS) && (occ[1] != TIMELESS) &&
                    (occ[0] == ETERNAL) == (occ[1] == ETERNAL) &&
                    (occ[1] >= occ[0])) || (occ[0] == ETERNAL && !d.occ.validEternal()))
                throw new RuntimeException("bad occurrence result: " + Arrays.toString(occ));

            if ((d.taskPunc==GOAL && d.concPunc == GOAL) && occ[0]!=ETERNAL && occ[0] < d.taskStart) {
                if (d.taskTerm.op()!=CONJ && d.beliefTerm.op()==IMPL) {
                    //immediate shift
                    long range = occ[1] - occ[0];
                    occ[0] = d.taskStart;
                    occ[1] = occ[0] + range;
                }
            }

//            if (d.concTruth!=null) {
//                long start = occ[0], end = occ[1];
//                if (start != ETERNAL) {
//                    if (d.taskStart!=ETERNAL && d.beliefStart!=ETERNAL) {
//
//                        long taskEvidenceRange = ((d.taskStart==ETERNAL || (d.taskPunc==QUESTION || d.taskPunc==QUEST)) ? 0 : d._task.range());
//                        long beliefEviRange = ((!d.concSingle && d._belief != null && d.beliefStart!=ETERNAL) ? d._belief.range() : 0);
//                        long commonRange = d._belief != null ? Longerval.intersectLength(d.taskStart, d.taskEnd, d.beliefStart, d.beliefEnd) : 0;
//
//                        long inputRange = taskEvidenceRange + beliefEviRange - (commonRange/2);
//                        //assert(inputRange > 0);
//
//                        long outputRange = (1 + (end - start));
//                        long expanded = outputRange - inputRange;
//                        if (expanded > nar.dtDither()) {
//                            //dilute the conclusion truth in proportion to the extra space
//                            double expansionFactor = ((double) expanded) / (expanded + inputRange);
//                            if (Double.isFinite(expansionFactor))
//                                d.concTruthEviMul((float) expansionFactor, false);
//                            ////assert (expansionFactor < 1);
//                        }
//                    }
//                }
//            }
            
            if (!Taskify.valid(c2, d.concPunc)) {
                boolean fail;

                if (Param.INVALID_DERIVATION_TRY_QUESTION && d.concPunc == BELIEF || d.concPunc == GOAL) {
                    //as a last resort, try forming a question from the remains
                    byte qPunc = d.concPunc == BELIEF ? QUESTION : QUEST;
                    if (!Taskify.valid(c2, qPunc)) {
                        fail = true;
                    } else {
                        d.concPunc = qPunc;
                        d.concTruth = null;
                        fail = false;
                    }
                } else {
                    fail = true;
                }

                if (fail) {
                    Term c1e = c1;
                    d.nar.emotion.deriveFailTemporal.increment(/*() ->
                        rule + "\n\t" + d + "\n\t -> " + c1e + "\t->\t" + c2
                */);
                    return false;
                }

            }


            d.concOcc = occ;
            d.concTerm = c2;
        } else {

            byte punc = d.concPunc;
            if ((punc == BELIEF || punc == GOAL) && c1.hasXternal()) { // && !d.taskTerm.hasXternal() && !d.beliefTerm.hasXternal()) {
                //HACK this is for deficiencies in the temporal solver that can be fixed
                c1 = Retemporalize.retemporalizeXTERNALToDTERNAL.transform(c1);
                if (!Taskify.valid(c1, d.concPunc)) {
                    d.nar.emotion.deriveFailTemporal.increment();
                    Taskify.spam(d, Param.TTL_DERIVE_TASK_FAIL);
                    return false;
                }
            }

            d.concTerm = c1;
            d.concOcc = null;
        }


        return true;
    }


}
