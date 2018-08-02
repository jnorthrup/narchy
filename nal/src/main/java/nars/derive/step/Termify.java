package nars.derive.step;

import nars.$;
import nars.NAR;
import nars.derive.Derivation;
import nars.term.Term;
import nars.term.control.AbstractPred;
import org.eclipse.collections.api.tuple.Pair;

import static nars.Op.NEG;
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
        super($.funcFast("derive", pattern, solve, time.term()));
        this.pattern = pattern;

        this.time = time;


    }


    @Override
    public final boolean test(Derivation d) {

        NAR nar = d.nar;

        d.concTerm = null;
        d.concOcc = null;

        nar.emotion.deriveTermify.increment();

        d.untransform.clear();

        Term c1 = d.transform(pattern);
        if (c1 == null || !c1.op().conceptualizable)
            return false;

        nar.emotion.deriveEval.increment();
        //c1 = c1.eval(eval, d, d.random);

        if (!Taskify.valid(c1, (byte) 0 /* dont consider punc consequences until after temporalization */)) {
            Term c1e = c1;
            d.nar.emotion.deriveFailEval.increment(/*() ->
                    rule + " |\n\t" + d.xy + "\n\t -> " + c1e
            */);
            return false;
        }


        boolean negated;
        if (c1.op() == NEG) {
            c1 = c1.unneg();
            negated = true;
        } else
            negated = false;

        if (c1.volume() > d.termVolMax) {
            d.nar.emotion.deriveFailVolLimit.increment();
            return false;
        }

        if (negated) {
            if (d.concTruth != null) 
                d.concTruth = d.concTruth.neg();
        }



        if (d.temporal) {

            boolean unwrapNeg;
            if (c1.op()==NEG) {
                unwrapNeg = true;
                c1 = c1.unneg();
            } else {
                unwrapNeg = false;
            }
            Pair<Term, long[]> timing = time.solve(d, c1);
            if (timing == null) {
                d.nar.emotion.deriveFailTemporal.increment();
                //temporary: //time.solve(d, c1);
                return false;
            }

            Term c2 = timing.getOne();
            long[] occ = timing.getTwo();
            if (!((occ[0] != TIMELESS) && (occ[1] != TIMELESS) &&
                    (occ[0] == ETERNAL) == (occ[1] == ETERNAL) &&
                    (occ[1] >= occ[0])))
                throw new RuntimeException("bad occurrence result");

            
            
            if (!Taskify.valid(c2, d.concPunc)) {
                Term c1e = c1;
                d.nar.emotion.deriveFailTemporal.increment(/*() ->
                        rule + "\n\t" + d + "\n\t -> " + c1e + "\t->\t" + c2
                */);
                return false;
            }


            if (occ[0] == ETERNAL && !d.occ.validEternal()) {
                throw new RuntimeException("illegal eternal temporalization");
            }

            if (unwrapNeg)
                c2 = c2.neg();

            d.concOcc = occ;
            d.concTerm = c2;
        } else {
            d.concTerm = c1;
            d.concOcc = null;
        }


        return true;
    }


}
