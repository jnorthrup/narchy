package nars.derive.impl;

import jcog.math.IntRange;
import jcog.pri.bag.Bag;
import nars.NAR;
import nars.Task;
import nars.derive.Derivation;
import nars.derive.Deriver;
import nars.derive.Premise;
import nars.derive.premise.PremiseDeriverRuleSet;
import nars.derive.premise.PremiseRuleProto;
import nars.index.concept.AbstractConceptIndex;
import nars.link.Activate;
import nars.link.TaskLink;
import nars.term.Term;

import java.util.Collection;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;


/** buffers premises in batches*/
public class BatchDeriver extends Deriver {

    public final IntRange tasklinksPerIteration = new IntRange(4, 1, 32);


    public BatchDeriver(PremiseDeriverRuleSet rules) {
        this(rules, rules.nar);
    }

    public BatchDeriver(Set<PremiseRuleProto> rules, NAR nar) {
        super(fire(nar), rules, nar);
    }


    public BatchDeriver(Consumer<Predicate<Activate>> source, PremiseDeriverRuleSet rules) {
        super(source, rules, rules.nar);
    }

    public BatchDeriver(Consumer<Predicate<Activate>> source, Set<PremiseRuleProto> rules, NAR nar) {
        super(source, rules, nar);
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

        Collection<Premise> premises = d.premiseBuffer;
        premises.clear();

        Bag<TaskLink, TaskLink> tasklinks = ((AbstractConceptIndex) nar.concepts).active;

//        tasklinks.print(); System.out.println();

        tasklinks.sample(d.random, tasklinksPerIteration.intValue(), tasklink->{
            Task task = tasklink.apply(nar);
            if (task != null) {
                Term term = tasklink.term(task, d);
                if (term != null) {
                    premises.add(new Premise(task, term));
                }
            }
        });

        return premises;
    }



}
