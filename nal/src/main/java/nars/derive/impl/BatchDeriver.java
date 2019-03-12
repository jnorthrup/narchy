package nars.derive.impl;

import jcog.math.IntRange;
import jcog.pri.bag.Bag;
import nars.Task;
import nars.derive.Derivation;
import nars.derive.Deriver;
import nars.derive.Premise;
import nars.derive.premise.PremiseDeriverRuleSet;
import nars.link.TaskLink;
import nars.task.util.TaskBuffer;
import nars.term.Term;
import nars.time.When;

import java.util.Collection;
import java.util.List;
import java.util.function.BooleanSupplier;


/** buffers premises in batches*/
public class BatchDeriver extends Deriver {

    public final IntRange tasklinksPerIteration = new IntRange(2, 1, 32);
    public final IntRange termlinksPerTaskLink = new IntRange(1, 1, 4);


    public BatchDeriver(PremiseDeriverRuleSet rules) {
        this(rules, new TaskBuffer.DirectTaskBuffer());
    }

    public BatchDeriver(PremiseDeriverRuleSet rules, TaskBuffer out) {
        super(rules, rules.nar);
        this.nar = rules.nar;
        output(rules.nar.exe, out, true);
    }

    @Override
    protected final void derive(Derivation d, BooleanSupplier kontinue) {

        int matchTTL = matchTTL(), deriveTTL = d.nar.deriveBranchTTL.intValue();

        do {

            Collection<Premise> pp = hypothesize(d);
            if (!pp.isEmpty()) {
                for (Premise p : pp)
                    p.derive(d, matchTTL, deriveTTL);
            }

        } while (kontinue.getAsBoolean());

    }


    /**
     * forms premises
     */
    private Collection<Premise> hypothesize(Derivation d) {


        int tlAttempts = termlinksPerTaskLink.intValue();
        if (tlAttempts == 0)
            return List.of();

        Collection<Premise> premises = d.premiseBuffer;
        premises.clear();

        Bag<TaskLink, TaskLink> tasklinks = nar.attn.active;

//        tasklinks.print(); System.out.println();

        long now = nar.time();
        int dur = nar.dur();
        long start = now - dur/2;
        long end = now + dur/2;

        When w = new When(start, end, dur, nar);

        tasklinks.sample(d.random, tasklinksPerIteration.intValue(), tasklink->{

            for (int i = 0; i < tlAttempts; i++) {
                Task task = tasklink.get(w);
                if (task != null) {
                    Term term = tasklink.term(task, d);
                    if (term != null)
                        premises.add(new Premise(task, term));
                }
            }
        });

        return premises;
    }



}
