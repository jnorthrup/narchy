package nars.derive.step;

import nars.$;
import nars.NAR;
import nars.derive.Derivation;
import nars.derive.premise.PremiseDeriverProto;
import nars.term.Term;
import nars.term.control.AbstractPred;
import nars.util.term.transform.Retemporalize;
import nars.util.time.Tense;
import org.eclipse.collections.api.tuple.Pair;

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



    @Override
    public final boolean test(Derivation d) {


        NAR nar = d.nar;
        nar.emotion.deriveEval.increment();

        d.derivedTerm = null;
        d.untransform.clear();


        Term c1 = pattern.transform(d);
        if (c1 == null || !c1.op().conceptualizable)
            return false;
        c1 = c1.eval(d);


        if (c1.volume() > d.termVolMax) {
            d.nar.emotion.deriveFailVolLimit.increment();
            return false;
        }

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

        d.concOcc = Tense.ETERNAL_ETERNAL; assert(d.concOcc[0]==ETERNAL && d.concOcc[1]==ETERNAL);

        Term c2;
        if (d.temporal) {

            Pair<Term, long[]> timing = time.solve(d, c1);
            if (timing == null)
                return false;
            c2 = timing.getOne();
            long[] occ = timing.getTwo();
            assert (occ[1] >= occ[0]);

            //invalid or impossible temporalization; could not determine temporal attributes. seems this can happen normally
            //only should eliminate XTERNAL from beliefs and goals.  ok if it's in questions/quests since it's the only way to express indefinite temporal repetition
            if (!c1.equals(c2)) {

                if ((!Taskify.valid(c2)) || ((d.concPunc == BELIEF || d.concPunc == GOAL) && c2.hasXternal())) {
                    Term c1e = c1;
                    d.nar.emotion.deriveFailTemporal.increment(() ->
                            rule + "\n\t" + d + "\n\t -> " + c1e + "\t->\t" + c2
                    );
                    return false;
                }

            }

            if (d.concPunc == GOAL) {
                if (occ[0] == ETERNAL && d.task.isEternal() && (d.single || !d.belief.isEternal())) {
                    //desire in present moment
                    System.arraycopy(nar.timeFocus(), 0, occ, 0, 2);
                }
            }


            d.concOcc = occ;

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

        d.derivedTerm = c2;
        return true;
    }


}
