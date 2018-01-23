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

        nar.emotion.deriveEval.increment();

//        d.xyDyn.clear();
        Term c1 = pattern.eval(d);

        if (!valid(c1)) {
            Term c1e = c1;
            d.nar.emotion.deriveFailEval.increment(()->{
                return rule + " |\n\t" + d.xy.toString() + "\n\t -> " + c1e;
            });
            return false;
        }
        if (c1.volume() > d.termVolMax) {
            d.nar.emotion.deriveFailVolLimit.increment();
            return false;
        }

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
            //only should eliminate XTERNAL from beliefs and goals.  ok if it's in questions/quests since it's the only way to express indefinite temporal repetition
            if ((c1!=c2 && !valid(c2)) || ((d.concPunc == BELIEF || d.concPunc == GOAL) && c2.hasXternal())) {
                Term c1e = c1;
                d.nar.emotion.deriveFailTemporal.increment(()->{
                    return rule + "\n\t" + d.toString() + "\n\t" + d.xy.toString() + "\n\t -> " + c1e + "\n\t" + dtt;
                });
                return false;
            }


            if (occ[0] > occ[1]) {
                //HACK swap the reversed occ
                long x = occ[0];
                occ[0] = occ[1];
                occ[1] = x;
            }

            if (d.concPunc == GOAL && d.taskPunc == GOAL && !d.single && Op.values()[d._beliefOp] == IMPL) {
                long derivedGoalStart = occ[0];

                if (derivedGoalStart != ETERNAL) {

                    long taskWants =
                            d.task.start();
                            //d.task.myNearestTimeTo(d.time);

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
                if (c1!=c2 && !valid(c2)) {
                    d.nar.emotion.deriveFailTemporal.increment();
                    return false;
                }
            } else {
                c2 = c1;
            }
        }

        return d.derivedTerm.set(c2) != null;
    }

    static boolean valid(Term x) {
        return (x != null) &&
                x.op().conceptualizable &&
                !x.hasAny(VAR_PATTERN) && x.hasAny(ConstantAtomics);
    }


}
