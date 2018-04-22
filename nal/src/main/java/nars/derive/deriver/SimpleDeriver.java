package nars.derive.deriver;

import jcog.Util;
import jcog.bag.Bag;
import jcog.math.IntRange;
import jcog.pri.PriReference;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.derive.Derivation;
import nars.derive.Deriver;
import nars.derive.Premise;
import nars.derive.premise.PremiseDeriverRuleSet;
import nars.link.TaskLink;
import nars.term.Term;

import java.util.Random;

/**
 * samples freely from concept, termlink, and tasklink bags without any buffering of premises
 */
public class SimpleDeriver extends Deriver {

    /**
     * iterations -> premises multiplier
     */
    public final IntRange power = new IntRange(8, 1, 512);

    /** controls concentration per concept */
    public final IntRange premisesPerConcept = new IntRange(8, 1, 32);

    public SimpleDeriver(PremiseDeriverRuleSet rules) {
        super(rules, rules.nar);
    }

    @Override
    protected void derive(NAR n, int iterations, Derivation d) {


        final int[] ii = {iterations * power.intValue()};


        int deriveTTL = n.deriveTTL.intValue();
        int matchTTL = deriveTTL / 4;

        Random rng = n.random();

        source.accept(a -> {

            Concept c = a.get();

            float cPri = a.priElseZero();

            Bag<?, TaskLink> tasks = c.tasklinks();
            Bag<Term, PriReference<Term>> terms = c.termlinks();

            if (update(nar, tasks, terms)) {
                int premises = Util.lerp(cPri, 1, premisesPerConcept.intValue());
                for (int i = 0; i < premises; i++) {

                    TaskLink tasklink = tasks.sample(rng);
                    if (tasklink != null) {

                        activate(c, tasklink, nar);

                        PriReference<Term> termlink = terms.sample(rng);
                        if (termlink != null) {

                            Task task = tasklink.get(nar);
                            if (task!=null) {
                                Premise premise = new Premise(task, termlink);
                                if (premise.match(d, matchTTL))
                                    if (rules.derivable(d))
                                        d.derive(deriveTTL);

                                if (ii[0]-- <= 0)
                                    return false; //done
                            }
                        }

                    }
                }
            }

            return ii[0]-- > 0; //miss
        });

    }
}
