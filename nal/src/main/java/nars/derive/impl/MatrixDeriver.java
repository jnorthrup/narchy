package nars.derive.impl;

import jcog.data.list.FasterList;
import jcog.data.set.ArrayHashSet;
import jcog.math.IntRange;
import jcog.math.Range;
import jcog.pri.PriReference;
import jcog.pri.bag.Bag;
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
import nars.term.Term;

import java.util.Random;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * forms matrices of premises of M tasklinks and N termlinks which
 * are evaluated after buffering some limited amount of these in a set
 */
public class MatrixDeriver extends Deriver {

    public final IntRange conceptsPerIteration = new IntRange(1, 1, 32);

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
    protected final void derive(Derivation d, BooleanSupplier kontinue) {

        int matchTTL = matchTTL(), deriveTTL = d.nar.deriveBranchTTL.intValue();

        do {

            for (Premise p : hypothesize(d))
                p.derive(d, matchTTL, deriveTTL);

        } while (kontinue.getAsBoolean());

    }


    /**
     * forms premises
     */
    private FasterList<Premise> hypothesize(Derivation d) {

        int premisesMax = conceptsPerIteration.intValue() * premisesPerConcept;

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
    private void premiseMatrix(Activate a, int premisesMax, int _tasklinks, int _termlinksPerTasklink, Derivation d) {

        nar.emotion.conceptFire.increment();


        Concept concept = a.id;

        Bag<?, TaskLink> tasklinks = concept.tasklinks();
        Bag<Term, PriReference<Term>> termlinks = concept.termlinks();

        commit(d, concept, tasklinks, termlinks);

        if (!tasklinks.isEmpty()) {

            final ArrayHashSet<TaskLink> tasklinksFired = d.firedTaskLinks;
            tasklinksFired.clear();

            int[] premisesPerConcept = { _tasklinks *  _termlinksPerTasklink };



            int nTermLinks = termlinks.size(), nTaskLinks = tasklinks.size();

            Random rng = d.random;
            FasterList<Premise> premises = d.premiseBuffer;

            tasklinks.sample(rng, Math.min(_tasklinks, nTaskLinks), tasklink -> {

                Task task = tasklink.get(nar);
                if (task != null) {

//                taskPriSum[0] += task.priElseZero();

                    tasklinksFired.add(tasklink);

                    if (nTermLinks > 0) {
                        termlinks.sample(rng, Math.min(nTermLinks, _termlinksPerTasklink), termlink -> {

                            Term beliefTerm = termlink.get();
                            if (premises.add( new Premise(task, beliefTerm ))) {
                                if ((--premisesPerConcept[0] <= 0) || (premises.size() >= premisesMax))
                                    return false;
                            }

                            return true;
                        });
                    }
                }

                return (--premisesPerConcept[0] > 0);
            });

            concept.linker().link(a, d);

        }

    }


}
