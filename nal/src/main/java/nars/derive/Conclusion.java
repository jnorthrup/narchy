package nars.derive;

import nars.NAR;
import nars.Op;
import nars.control.Derivation;
import nars.derive.rule.PremiseRule;
import nars.derive.time.DeriveTime;
import nars.term.Term;
import nars.term.pred.AbstractPred;
import nars.term.transform.Retemporalize;

import static nars.Op.*;
import static nars.time.Tense.ETERNAL;

/**
 * Final conclusion step of the derivation process that produces a derived task
 * <p>
 * Each rule corresponds to a unique instance of this
 * <p>
 * runtime instance. each leaf of each NAR's derivation tree should have
 * a unique instance, assigned the appropriate cause id by the NAR
 * at initialization.
 */
public final class Conclusion extends AbstractPred<Derivation> {


    //private final static Logger logger = LoggerFactory.getLogger(Conclusion.class);
    public final Term pattern;


    //    public final Set<Variable> uniqueVars;
    public final PremiseRule rule;

    public Conclusion(Term id, Term pattern, PremiseRule rule) {
        super(id);
        this.rule = rule;
        this.pattern = pattern;
//        this.uniqueVars = pattern instanceof Compound ? ((PatternCompound)pattern).uniqueVars : Set.of();
    }


    @Override
    public final boolean test(Derivation d) {

        NAR nar = d.nar;

        nar.emotion.derivationEval.increment();

//        d.xyDyn.clear();
        Term c1 = pattern.eval(d);

        if (!valid(c1, d))
            return false;

        if (c1.op() == NEG) {
            c1 = c1.unneg();
            if (d.concTruth != null) //belief or goal
                d.concTruth = d.concTruth.neg();
        }

        d.concEviFactor = 1f;
        final long[] occ = d.concOcc;
        occ[0] = occ[1] = ETERNAL;

        Term c2;
        if (d.temporal) {

            boolean s = d.single;
            DeriveTime dt = s ? d.dtSingle : d.dtDouble;
            if (dt == null) {
                dt = new DeriveTime(d, s);
                if (s)
                    d.dtSingle = dt;
                else
                    d.dtDouble = dt;
            }

            DeriveTime dtt = dt.get();
            //dtt.print();
            c2 = dtt.solve(c1);


            //invalid or impossible temporalization; could not determine temporal attributes. seems this can happen normally
            if (c1!=c2 && !valid(c2, d))
                return false;


            if (d.concPunc == BELIEF || d.concPunc == GOAL) {
                //only should eliminate XTERNAL from beliefs and goals.  ok if it's in questions/quests since it's the only way to express indefinite temporal repetition
                //c2 = c2.temporalize(Retemporalize.retemporalizeXTERNALToDTERNAL);
                //if (c2 == null)
                //  return false;
                if (c2.hasXternal())
                    return false;
            }

            if (occ[0] > occ[1]) {
                //HACK swap the reversed occ
                long x = occ[0];
                occ[0] = occ[1];
                occ[1] = x;
            }

            if (d.concPunc == GOAL && d.taskPunc == GOAL && !d.single && Op.values()[d._beliefOp].temporal) {
                long derivedGoalStart = occ[0];

                if (derivedGoalStart != ETERNAL) {

                    long taskWants = d.task.myNearestTimeTo(d.time);

                    if (taskWants == ETERNAL) {
                        taskWants = d.time; //now
                    }

                    if (derivedGoalStart < taskWants) {
                        //derived goal occurrs before task goal, so shift to task start
                        long gdur = occ[1] - derivedGoalStart;
                        occ[0] = taskWants;
                        occ[1] = taskWants + gdur;
                    }
                }
            }

        } else {
            if (d.concPunc == BELIEF || d.concPunc == GOAL) {
                c2 = c1.temporalize(Retemporalize.retemporalizeXTERNALToDTERNAL);
                if (c1!=c2 && !valid(c2, d))
                    return false;
            } else {
                c2 = c1;
            }
        }


        Term c3 = c2.normalize();
        return (c2==c3 || valid(c3,d)) && d.derivedTerm.set(c3) != null;
    }

    protected boolean valid(Term x, Derivation d) {
        return (x != null) &&
                x.op().conceptualizable &&
                (x.volume() <= d.termVolMax) &&
                !x.hasAny(VAR_PATTERN) && x.hasAny(ConstantAtomics);
    }


}
