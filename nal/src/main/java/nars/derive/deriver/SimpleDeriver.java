package nars.derive.deriver;

import jcog.bag.Bag;
import jcog.math.IntRange;
import jcog.pri.PriReference;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.control.Activate;
import nars.derive.Derivation;
import nars.derive.Deriver;
import nars.derive.Premise;
import nars.derive.premise.PremiseDeriverRuleSet;
import nars.link.TaskLink;
import nars.term.Term;

import java.util.Collection;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * samples freely from concept, termlink, and tasklink bags without any buffering of premises
 */
public class SimpleDeriver extends Deriver {

    /**
     * iterations -> premises multiplier
     */
    public final IntRange power = new IntRange(2, 1, 512);

    /**
     * controls concentration per concept
     */
    public final IntRange tasklinksPerConcept = new IntRange(2, 1, 32);
    public final IntRange termlinksPerConcept = new IntRange(2, 1, 32);

    public SimpleDeriver(PremiseDeriverRuleSet rules) {
        super(rules, rules.nar);
    }

    public SimpleDeriver(Consumer<Predicate<Activate>> source, Consumer<Collection<Task>> target, PremiseDeriverRuleSet rules) {
        super(source, target, rules);
    }

    @Override
    protected void derive(NAR n, int iterations, Derivation d) {


        final int[] ii = {iterations * power.intValue()};


        int deriveTTL = n.deriveTTL.intValue();
        int matchTTL = deriveTTL / 4;

        Random rng = d.random;

        source.accept(a -> {

            Concept c = a.get();

            float cPri = a.priElseZero();

            Bag<?, TaskLink> tasks = c.tasklinks();
            Bag<Term, PriReference<Term>> terms = c.termlinks();

            if (update(nar, tasks, terms)) {

                int tasklinks = /*Util.lerp(cPri, 1, */tasklinksPerConcept.intValue();
                int termlinks = /*Util.lerp(cPri, 1, */termlinksPerConcept.intValue();
                for (int i = 0; i < tasklinks; i++) {

                    TaskLink tasklink = tasks.sample(rng);
                    if (tasklink != null) {

                        activate(c, tasklink, nar);

                        Task task = tasklink.get(nar);
                        if (task != null) {

                            for (int z = 0; z < termlinks; z++) {

                                PriReference<Term> termlink = terms.sample(rng);
                                if (termlink != null) {

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
            }

            return ii[0]-- > 0; //miss
        });

    }
}
