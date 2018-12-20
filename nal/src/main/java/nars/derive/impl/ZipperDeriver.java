package nars.derive.impl;

import jcog.data.set.ArrayHashSet;
import jcog.math.IntRange;
import nars.Task;
import nars.concept.Concept;
import nars.derive.BeliefSource;
import nars.derive.Derivation;
import nars.derive.Deriver;
import nars.derive.Premise;
import nars.derive.premise.PremiseDeriverRuleSet;
import nars.link.Activate;
import nars.link.TaskLink;
import nars.term.Term;

import java.util.function.*;

/**
 * samples freely from concept, termlink, and tasklink bags without any batching/buffering of premises,
 * like a Zipper of 2 iterators
 */
public class ZipperDeriver extends Deriver {

    /**
     * controls concentration per concept
     */
    public final IntRange tasklinksPerConcept = new IntRange(2, 1, 32);
    public final IntRange termlinksPerConcept = new IntRange(2, 1, 32);

    /** TODO refactor */
    @Deprecated final BiFunction<Concept, Derivation, BeliefSource.LinkModel> hypothesizer;

    public ZipperDeriver(PremiseDeriverRuleSet rules) {
        this(fire(rules.nar), rules);
    }

    public ZipperDeriver(Consumer<Predicate<Activate>> source, PremiseDeriverRuleSet rules) {
        this(source, rules, BeliefSource.ConceptTermLinker);
    }

    public ZipperDeriver(Consumer<Predicate<Activate>> source, PremiseDeriverRuleSet rules,
                         BiFunction<Concept, Derivation, BeliefSource.LinkModel> hypothesizer) {
        super(source, rules);
        this.hypothesizer = hypothesizer;
    }


    @Override
    protected void derive(Derivation d, BooleanSupplier kontinue) {


        int deriveTTL = d.nar.deriveBranchTTL.intValue();
        int matchTTL = matchTTL();

        source.accept(a -> {
            assert (a != null);

            Concept concept = a.get();

            nar.attn.forgetting.update(concept, nar);

            BeliefSource.LinkModel model = hypothesizer.apply(concept, d);
            if (model!=null) {

                d.firedTasks.clear();
                d.firedTaskLinks.clear();

                ArrayHashSet<TaskLink> fired = model.tasklinks(tasklinksPerConcept.intValue(), d.firedTaskLinks);
                Supplier<Term> beliefTerms = model.beliefTerms();

                int termlinks = /*Util.lerp(cPri, 1, */termlinksPerConcept.intValue();
//            float taskPriSum = 0;


                fired.forEach(tasklink -> {


                    Task task = TaskLink.task(tasklink, nar);

                    if (task != null) {
                        int derived = 0;
                        for (int z = 0; z < termlinks; z++) {
                            Term b = beliefTerms.get();
                            if (b != null) {
                                if (derived == 0)
                                    d.firedTasks.add(task);
                                new Premise(task, b).derive(d, matchTTL, deriveTTL);
                                derived++;
                            }
                        }

                    }
                });

                //System.out.println((((DerivedTasks.DerivedTasksBag)d.deriver.derived).tasks.map.values()));

                concept.linker().link(a, d);
            }

            return kontinue.getAsBoolean();
        });

    }


}
