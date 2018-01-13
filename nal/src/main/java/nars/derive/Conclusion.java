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

        int volMax = d.termVolMax;
        if (c1 == null || !c1.op().conceptualizable || c1.volume() > volMax || c1.hasAny(/*BOOL,*/VAR_PATTERN))
            return false;
        if (!c1.hasAny(Op.ConstantAtomics))
            return false; //entirely variablized

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
            if (c2 == null || c2.volume() > volMax || !c2.op().conceptualizable/*|| (Math.abs(occReturn[0]) > 2047483628)*/ /* long cast here due to integer wraparound */) {
//                            throw new InvalidTermException(c1.op(), c1.dt(), "temporalization failure"
//                                    //+ (Param.DEBUG ? rule : ""), c1.toArray()
//                            );

                //FOR DEBUGGING
//                if (t1==null)
//                    new Temporalize(d.random).solve(d, c1, new long[]{ETERNAL, ETERNAL});

                return false;
            }


            if (d.concPunc == BELIEF || d.concPunc == GOAL) {
                //only should eliminate XTERNAL from beliefs and goals.  ok if it's in questions/quests since it's the only way to express indefinite temporal repetition
                //c2 = c2.temporalize(Retemporalize.retemporalizeXTERNALToDTERNAL);
                //if (c2 == null)
                //  return false;
                if (c2.hasXternal()) {
                    return false;
                }
            }

            if (occ[0] > occ[1]) {
                //HACK swap the reversed occ
                long x = occ[0];
                occ[0] = occ[1];
                occ[1] = x;
            }

        } else {
            c2 = c1.temporalize(Retemporalize.retemporalizeXTERNALToDTERNAL);
        }


        c2 = c2.normalize();

        return (c2 != null) && (d.derivedTerm.set(c2) != null);

    }


}
