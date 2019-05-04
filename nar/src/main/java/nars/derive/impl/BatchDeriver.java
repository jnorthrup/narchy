package nars.derive.impl;

import jcog.math.IntRange;
import nars.NAR;
import nars.Task;
import nars.attention.What;
import nars.derive.Derivation;
import nars.derive.Deriver;
import nars.derive.Premise;
import nars.derive.premise.PremiseDeriverRuleSet;
import nars.term.Term;
import nars.time.When;
import nars.time.event.WhenTimeIs;

import java.util.Collection;
import java.util.function.BooleanSupplier;


/** buffers premises in batches*/
public class BatchDeriver extends Deriver {

    public final IntRange tasklinksPerIteration = new IntRange(1, 1, 32);
    public final IntRange termlinksPerTaskLink = new IntRange(1, 1, 4);

    public BatchDeriver(PremiseDeriverRuleSet rules) {
        super(rules, rules.nar);
    }

    @Override
    protected final void derive(Derivation d, BooleanSupplier kontinue) {

        int matchTTL = matchTTL(), deriveTTL = d.nar().deriveBranchTTL.intValue();

        Collection<Premise> premises = d.premiseBuffer;


        do {

            When<NAR> when = WhenTimeIs.now(d);

            hypothesize(when, premises, d);

            if (!premises.isEmpty()) {
                try {
                    for (Premise p : premises) {
                        derive(p, d, matchTTL, deriveTTL);
                    }
                } finally {
                    premises.clear();
                }

            }

        } while (kontinue.getAsBoolean());

    }


    /**
     * forms premises
     */
    private void hypothesize(When when, Collection<Premise> target, Derivation d) {

        int tlAttempts = termlinksPerTaskLink.intValue(); assert(tlAttempts > 0);

        What what = d.what;
        what.sample(d.random, tasklinksPerIteration.intValue(), tasklink->{

            Task task = tasklink.get(when);
            if (task != null) {
                if (task.isDeleted())
                    return;

                for (int i = 0; i < tlAttempts; i++) {
                    Term term = ((What.TaskLinkWhat)what).links.term(tasklink, task, d);
                    if (term != null)
                        target.add(new Premise(task, term));
                }
            }
        });

    }



}