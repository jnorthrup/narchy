package nars.derive.impl;

import jcog.data.list.FasterList;
import jcog.data.set.ArrayHashSet;
import jcog.math.IntRange;
import jcog.math.Range;
import jcog.pri.PriReference;
import jcog.pri.bag.Bag;
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
import nars.term.Term;

import java.util.Random;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * forms matrices of premises of M tasklinks and N termlinks which
 * are evaluated after buffering some limited amount of these in a set
 */
public class MatrixDeriver extends Deriver {

    public final IntRange conceptsPerIteration = new IntRange(4, 1, 32);

    /**
     * how many premises to keep per concept; should be <= Hypothetical count
     */
    @Range(min = 1, max = 8)
    private final int premisesPerConcept = 4;
    /**
     * controls the rate at which tasklinks 'spread' to interact with termlinks
     */
    @Range(min = 1, max = 8)
    private final int termLinksPerTaskLink = 2;


    public MatrixDeriver(PremiseDeriverRuleSet rules) {
        this(rules, rules.nar);
    }

    public MatrixDeriver(Set<PremiseRuleProto> rules, NAR nar) {
        super(nar.attn, rules, nar);
    }

    public MatrixDeriver(Consumer<Predicate<Activate>> source, PremiseDeriverRuleSet rules) {
        super(source, rules, rules.nar);
    }

    public MatrixDeriver(Consumer<Predicate<Activate>> source, Set<PremiseRuleProto> rules, NAR nar) {
        super(source, rules, nar);
    }

    @Override
    protected void derive(Derivation d, BooleanSupplier kontinue) {

        NAR n = d.nar;

        FasterList<Premise> premises = d.premiseBuffer;
        premises.clear();

//        final int[] tried = {0};


        do {

            if (premises.isEmpty()) {


                int premisesMax = conceptsPerIteration.intValue() * premisesPerConcept;

                hypothesize(premisesMax, (t, termlink) -> {

                    Premise premise = new Premise(t, termlink);
                    if (!premises.add(premise)) {
                        //n.emotion.premiseBurstDuplicate.increment();
                    }
                    return true;
                }, d);

                int s = premises.size();
                if (s == 0)
                    return;

                if (s > 2)
                    premises.sortThis((a, b) -> Long.compareUnsigned(a.hash, b.hash));
            }



            int matchTTL = matchTTL(), deriveTTL = n.deriveBranchTTL.intValue();

            for (Premise p : premises) {

                p.derive(d, matchTTL, deriveTTL);

            }
            premises.clear();

        } while (kontinue.getAsBoolean());



    }

    protected void premiseUnmatched(Premise p) {
        //p.termLink.priMult(0.9f); //HACK simple decay
        //TODO use the original concept and tasklink bag to determine appropriate decay
    }

    protected void premiseUnderivable(Premise p, Derivation d) {
//        System.out.println(nar.time() + " premise underiveable: " + p + " " + d._belief);
//        try {
//            Concept c = nar.concept(p.term());
//            if (c != null)
//                System.out.println("\t" + c.beliefs().streamTasks().collect(Collectors.toList()));
//        } catch (Throwable t) { }


        //p.termLink.priMult(0.9f); //HACK simple decay
        //TODO use the original concept and tasklink bag to determine appropriate decay
    }

    protected void premiseFired(Premise p, Derivation d) {
        //System.out.println(nar.time() + " premise fired: " + p + " " + d._belief);
    }


    /** forms premises */
    private void hypothesize(int premisesMax, BiPredicate<Task, PriReference<Term>> each, Derivation d) {

        int premisesRemain[] = new int[]{premisesMax};
        int premisePerConceptRemain[] = new int[1];

        int tasklinks = Math.max(1, Math.round(premisesMax / ((float) termLinksPerTaskLink)));


        @Deprecated BiPredicate<Task, PriReference<Term>> continueHypothesizing = (tasklink, termlink) ->
                (premisePerConceptRemain[0]-- > 0) && each.test(tasklink, termlink) && (--premisesRemain[0] > 0);

        int[] conceptsRemain = new int[]{2 * (int) Math.ceil(premisesMax / ((float) (termLinksPerTaskLink * termLinksPerTaskLink)))};

        source.accept(a -> {

            premisePerConceptRemain[0] = premisesPerConcept;

            premiseMatrix(a,
                    continueHypothesizing,
                    tasklinks, termLinksPerTaskLink, d);

            return premisesRemain[0] > 0 && conceptsRemain[0]-- > 0;
        });

    }



    /**
     * hypothesize a matrix of premises, M tasklinks x N termlinks
     */
    public void premiseMatrix(Activate a, BiPredicate<Task, PriReference<Term>> continueHypothesizing, int _tasklinks, int _termlinksPerTasklink, Derivation d) {

        nar.emotion.conceptFire.increment();

        Concept concept = a.id;

        Bag<?, TaskLink> tasklinks = concept.tasklinks();
        Bag<Term, PriReference<Term>> termlinks = concept.termlinks();

        final ArrayHashSet<TaskLink> tasklinksFired = d.firedTaskLinks;
        tasklinksFired.clear();

        Random rng = d.random;

        if (commit(nar, tasklinks, termlinks)) {


            int[] conceptTTL = {_tasklinks * (1 + _termlinksPerTasklink)};


            int nTermLinks = termlinks.size();
            int nTaskLinks = tasklinks.size();
//        final float[] taskPriSum = {0};

            int maxTasks = Math.min(_tasklinks, nTaskLinks);


            tasklinks.sample(rng, maxTasks, tasklink -> {

                Task task = tasklink.get(nar);
                if (task != null) {

//                taskPriSum[0] += task.priElseZero();

                    tasklinksFired.add(tasklink);

                    if (!termlinks.isEmpty()) {
                        termlinks.sample(rng, Math.min(nTermLinks, _termlinksPerTasklink), termlink -> {
                            if (!continueHypothesizing.test(task, termlink)) {
                                //conceptTTL[0] = 0;
                                return false;
                            } else {
                                return (--conceptTTL[0] > 0);
                            }
                        });
                    }
                } /*else {
                    tasklink.delete();
                }*/

                return (--conceptTTL[0] > 0);
            });
        }

        concept.linker().link(a, d);
    }


}
