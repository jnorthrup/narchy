package nars.derive.impl;

import jcog.data.list.FasterList;
import jcog.data.set.ArrayHashSet;
import jcog.math.IntRange;
import jcog.pri.bag.Bag;
import jcog.pri.bag.Sampler;
import jcog.sort.SortedList;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.derive.Derivation;
import nars.derive.Deriver;
import nars.derive.Premise;
import nars.derive.premise.PremiseDeriverRuleSet;
import nars.derive.premise.PremiseRuleProto;
import nars.link.Activate;
import nars.link.TaskLink;
import nars.task.Tasklike;
import nars.term.Term;

import java.util.Random;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;


/** buffers premises in batches*/
public class BatchDeriver extends Deriver {

    public final IntRange conceptsPerIteration = new IntRange(2, 1, 32);

    /**
     * how many premises to keep per concept; should be <= Hypothetical count
     */
    public final IntRange premisesPerConcept = new IntRange(2, 1, 8);

    /**
     * controls the rate at which tasklinks 'spread' to interact with termlinks
     */
    public final IntRange termLinksPerTaskLink = new IntRange(1, 1, 8);



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

//            if (!d.nar.exe.concurrent()) {
//                hypothesize(d).asParallel(ForkJoinPool.commonPool(), 2).forEach(p -> {
//                    p.derive(/* HACK */ Deriver.derivation.get().next(nar, this), matchTTL, deriveTTL);
//                });
//            } else {
                for (Premise p : hypothesize(d))
                    p.derive(d, matchTTL, deriveTTL);
//            }

        } while (kontinue.getAsBoolean());

    }


    /**
     * forms premises
     */
    private FasterList<Premise> hypothesize(Derivation d) {

        int premisesMax = conceptsPerIteration.intValue() * premisesPerConcept.intValue();

        int termLinksPerTaskLink = this.termLinksPerTaskLink.intValue();

        int tasklinks = Math.max(1, Math.round(premisesMax / ((float) termLinksPerTaskLink)));


        SortedList<Premise> premises = d.premiseBuffer;
        premises.clear();





        int[] conceptsRemain = new int[]{(int) Math.ceil(premisesMax / ((float) (termLinksPerTaskLink * termLinksPerTaskLink)))};

        source.accept(a -> {

            premiseMatrix(a, premisesMax, tasklinks, termLinksPerTaskLink, d);

            return (premises.size() < premisesMax) && conceptsRemain[0]-- > 0;
        });

//        int s = premises.size();
//        if (s > 2)
//            premises.sortThis((a, b) -> Long.compareUnsigned(a.hash, b.hash));

        return premises;
    }


    /**
     * hypothesize a matrix of premises, M tasklinks x N termlinks
     */
    private void premiseMatrix(Activate conceptActivation, int premisesMax, int _tasklinks, int _termlinksPerTasklink, Derivation d) {

        nar.emotion.conceptFire.increment();


        Concept concept = conceptActivation.get();

        Bag<?, TaskLink> tasklinks = concept.tasklinks();
        if (tasklinks.isEmpty())
            return;

        nar.attn.forgetting.update(concept, nar);

        Random rng = d.random;

        Supplier<Term> beliefSrc;
        if (concept.term().op().atomic) {
            Bag<Tasklike, TaskLink> src = concept.tasklinks();
            beliefSrc = ()->src.sample(rng).term();
        } else {
            Sampler<Term> src = concept.linker();
            beliefSrc = ()->src.sample(rng);
        }


        final ArrayHashSet<TaskLink> taskLinksFired = d.firedTaskLinks;
        final ArrayHashSet<Task> tasksFired = d.firedTasks;
        taskLinksFired.clear();
        tasksFired.clear();


        int nTaskLinks = tasklinks.size();

        FasterList<Premise> premises = d.premiseBuffer;

        tasklinks.sample(rng, Math.min(_tasklinks, nTaskLinks), tasklink -> {

            Task task = TaskLink.task(tasklink, nar);
            if (task != null) {

                int[] premisesPerTaskLink = { _termlinksPerTasklink };


                do {
                    Term b = beliefSrc.get();
                    if (b!=null) {
                        if (premises.add(new Premise(task, b))) {
                            if (premisesPerTaskLink[0]==_termlinksPerTasklink) //only add on the first
                                tasksFired.add(task);

                            if (premises.size() >= premisesMax)
                                return false;
                        }
                    }
                } while (premisesPerTaskLink[0]-- > 0);
            }

            return true;
        });

        concept.linker().link(conceptActivation, d);

    }


//    /**
//     * TODO forms matrices of premises of M tasklinks and N termlinks which
//     * are evaluated after buffering some limited amount of these in a set
//     */
//    abstract static class MatrixDeriver extends Deriver {
//        /* TODO */
//        protected MatrixDeriver(Consumer<Predicate<Activate>> source, Set<PremiseRuleProto> rules, NAR nar) {
//            super(source, rules, nar);
//        }
//    }

}
