package nars.derive.step;

import nars.$;
import nars.NAR;
import nars.Param;
import nars.derive.Derivation;
import nars.derive.premise.PremiseDeriverProto;
import nars.term.Term;
import nars.term.control.AbstractPred;
import nars.util.term.transform.Retemporalize;

import static nars.Op.*;
import static nars.util.time.Tense.ETERNAL;

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

    public final Term pattern;

    //    public final Set<Variable> uniqueVars;
    public final PremiseDeriverProto rule;
    private final Occurrify.TaskTimeMerge time;

    public Termify(Term pattern, PremiseDeriverProto rule, Truthify solve, Occurrify.TaskTimeMerge time) {
        super($.func("derive", pattern, solve, time.term()));
        this.rule = rule;
        this.pattern = pattern;

        this.time = time;

//        this.uniqueVars = pattern instanceof Compound ? ((PatternCompound)pattern).uniqueVars : Set.of();
    }

    /**
     * fast test; more exhaustive test is performed in Taskify but it may not be necessary if this is applied
     */
    public static boolean isParentQueDifferent(Derivation d, Term c1) {

        //potentially duplicates parent as a result of unaffected functor work
        byte punc = d.concPunc;
        if ((punc ==QUESTION || punc == QUEST) && (punc == d.taskPunc)) {
            if (c1.equals(d.taskTerm)) {
                //same punctuation as parent task.
                // the result will be of lower confidence that we can calculate by maximum bound
                // in terms of evidence contribution from task and optionally belief that might boost
                // higher than the input task.
                if (d.belief == null) {
                    d.nar.emotion.deriveFailParentDuplicate.increment();
                    Taskify.spam(d, Param.TTL_DERIVE_TASK_SAME);
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public final boolean test(Derivation d) {

        NAR nar = d.nar;

        nar.emotion.deriveEval.increment();

        d.untransform.clear();

        Term c1 = pattern.transform(d);
        if (c1 == null || !c1.op().conceptualizable)
            return false;
        c1 = c1.eval(d);


        if (c1.volume() > d.termVolMax) {
            d.nar.emotion.deriveFailVolLimit.increment();
            return false;
        }

        if (!isParentQueDifferent(d, c1))
            return false; //belief doesnt contribute anything

        if (!Taskify.valid(c1)) {
            Term c1e = c1;
            d.nar.emotion.deriveFailEval.increment(() ->
                    rule + " |\n\t" + d.xy + "\n\t -> " + c1e
            );
            return false;
        }

        if (c1.op() == NEG) {
            c1 = c1.unneg();
            if (d.concTruth != null) //belief or goal
                d.concTruth = d.concTruth.neg();
        }

        final long[] occ = d.concOcc;
        occ[0] = occ[1] = ETERNAL;

        Term c2;
        if (d.temporal) {

            c2 = d.occ.solve(time, c1);

            //invalid or impossible temporalization; could not determine temporal attributes. seems this can happen normally
            //only should eliminate XTERNAL from beliefs and goals.  ok if it's in questions/quests since it's the only way to express indefinite temporal repetition
            if ((c1 != c2 && !Taskify.valid(c2)) || ((d.concPunc == BELIEF || d.concPunc == GOAL) && c2.hasXternal())) {
                Term c1e = c1;
                d.nar.emotion.deriveFailTemporal.increment(() ->
                        rule + "\n\t" + d + "\n\t -> " + c1e + "\t->\t" + c2
                );
                return false;
            }


            assert (occ[1] >= occ[0]);
//            if (occ[0] > occ[1]) {
//                //HACK swap the reversed occ
//                long x = occ[0];
//                occ[0] = occ[1];
//                occ[1] = x;
//            }

//            if (d.concPunc == GOAL && d.taskPunc == GOAL && !d.single &&
//                    Op.values()[d._beliefOp].temporal
//                //d._beliefOp == IMPL.ordinal() //impl only
//                    ) {
//                long derivedGoalStart = occ[0];
//
//                if (derivedGoalStart != ETERNAL) {
//
//                    long taskWants =
//                            d.task.start();
//                    //d.task.myNearestTimeTo(d.time);
//
//                    if (taskWants == ETERNAL) {
//                        taskWants = d.time; //now
//                    }
//
//                    if (derivedGoalStart < taskWants) {
//                        //derived goal occurrs before task goal, so shift to task start
//                        long gdur = occ[1] - derivedGoalStart;
//                        occ[0] = taskWants;
//                        occ[1] = taskWants + gdur;
//                    }
//                }
//            }

        } else {
            if ((d.concPunc == BELIEF || d.concPunc == GOAL) && c1.hasXternal()) {
                c2 =
                        c1.temporalize(Retemporalize.retemporalizeXTERNALToDTERNAL);
                if (c1 != c2 && !Taskify.valid(c2)) {
                    d.nar.emotion.deriveFailTemporal.increment();
                    return false;
                }
            } else {
                c2 = c1;
            }
        }

        return d.derivedTerm.set(c2) != null;
    }


}
