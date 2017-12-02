package nars.derive;

import nars.NAR;
import nars.Param;
import nars.control.Derivation;
import nars.derive.rule.PremiseRule;
import nars.derive.time.DeriveTime;
import nars.term.Term;
import nars.term.transform.Retemporalize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nars.Op.*;
import static nars.time.Tense.ETERNAL;
import static nars.truth.TruthFunctions.c2wSafe;
import static nars.truth.TruthFunctions.w2cSafe;

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


    private final static Logger logger = LoggerFactory.getLogger(Conclusion.class);
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
        
        Term c1 = pattern.eval(d);

        int volMax = d.termVolMax;
        if (c1 == null || !c1.op().conceptualizable || c1.volume() > volMax || c1.hasAny(BOOL,VAR_PATTERN) )
            return false;

//        c1 = c1.eval(p);
//        if (c1 == null || !c1.op().conceptualizable || c1.varPattern() > 0 || c1.volume() > volMax)
//            return false;


        d.concEviFactor = 1f;
        final long[] occ = d.concOcc;
        occ[0] = occ[1] = ETERNAL;


        Term c2;
        if (d.temporal) {

//            try {

//                TemporalizeDerived dt = d.temporalize;
//                if (dt == null) {
//                    d.temporalize = dt = new TemporalizeDerived(d); //cache in derivation
//                }
//


                boolean s = d.single;
                DeriveTime dt = s ? d.dtSingle : d.dtDouble;
                if (dt == null) {
                    dt = new DeriveTime(d, s);
                    if (s)
                        d.dtSingle = dt;
                    else
                        d.dtDouble = dt;
                }
                c2 = dt.solve(c1);

                {
                    //final sanity tests hack



                    if (occ[0] > occ[1]) {
                        //HACK swap the reversed occ
                        long x = occ[0];
                        occ[0] = occ[1];
                        occ[1] = x;
                    }
                }


//            } catch (InvalidTermException t) {
//                if (Param.DEBUG) {
//                    logger.error("temporalize error: {} {} {}", d, c1, t.getMessage());
//                }
//                return false;
//            }

            if ((d.concPunc==BELIEF || d.concPunc==GOAL) &&
                    w2cSafe(d.concEviFactor * d.concTruth.evi()) < d.confMin)
                return false;

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


            if (occ[1] == ETERNAL) occ[1] = occ[0]; //HACK probbly isnt needed

        } else {
            c2 = c1;
        }


        if (d.concPunc==BELIEF || d.concPunc==GOAL) {
            //only should eliminate XTERNAL from beliefs and goals.  ok if it's in questions/quests since it's the only way to express indefinite temporal repetition
            c2 = c2.temporalize(Retemporalize.retemporalizeXTERNALToDTERNAL);
            if (c2 == null)
                return false;
        }

        c2 = c2.normalize();
        if (c2 == null)
            return false;

        return d.derivedTerm.set(c2)!=null;
    }


}
