package nars.derive.deriver;

import jcog.bag.Bag;
import jcog.data.ArrayHashSet;
import jcog.math.IntRange;
import jcog.math.Range;
import jcog.pri.PriReference;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.control.Activate;
import nars.derive.Derivation;
import nars.derive.Deriver;
import nars.derive.Premise;
import nars.derive.premise.PremiseDeriver;
import nars.derive.premise.PremiseDeriverProto;
import nars.derive.premise.PremiseDeriverRuleSet;
import nars.link.TaskLink;
import nars.term.Term;

import java.util.Collection;
import java.util.Collections;
import java.util.Random;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** forms matrices of premises of M tasklinks and N termlinks which
 *  are evaluated after buffering some limited amount of these in a set
 */
public class MatrixDeriver extends Deriver {

    public final IntRange conceptsPerIteration = new IntRange(3, 1, 512);

    /**
     * how many premises to keep per concept; should be <= Hypothetical count
     */
    @Range(min = 1, max = 8)
    public int premisesPerConcept = 2;
    /**
     * controls the rate at which tasklinks 'spread' to interact with termlinks
     */
    @Range(min = 1, max = 8)
    public int termLinksPerTaskLink = 2;

    /** max # premises per batch; dont make too large.  allow the reasoner to incrementally digest results */
    @Range(min = 1, max = 1024)
    public int burstMax = 32;

    public MatrixDeriver(PremiseDeriverRuleSet rules) {
        this(rules, rules.nar);
    }

    public MatrixDeriver(Set<PremiseDeriverProto> rules, NAR nar) {
        super(rules, nar);
    }

    public MatrixDeriver(Consumer<Predicate<Activate>> source, Consumer<Collection<Task>> target, PremiseDeriver rules, NAR nar) {
        super(source, target, rules, nar);
    }

    @Override protected void derive(NAR n, int iterations, Derivation d) {
        int matchTTL = Param.TTL_MIN * 2;
        int deriveTTL = n.deriveTTL.intValue();


        int iterMult = premisesPerConcept * conceptsPerIteration.intValue();
        int totalPremisesRemain = iterations * iterMult;


        /** temporary buffer for storing unique premises */
        ArrayHashSet<Premise> premiseBurst = d.premiseBuffer;

        while (totalPremisesRemain > 0) {

            int burstSize = Math.min(burstMax, totalPremisesRemain);
            totalPremisesRemain -= burstSize;

            premiseBurst.clear();

            //SELECT
            selectPremises(n, burstSize, (t, termlink) -> {

                Premise premise = new Premise(t, termlink);
                if (!premiseBurst.add(premise))
                    n.emotion.premiseBurstDuplicate.increment();

                return true;
            });


            int s = premiseBurst.size();
            if (s == 0)
                break;

            if (s > 2)
                Collections.sort(premiseBurst.list, Task.sortByTaskSloppy);

            //--- FIRE
            premiseBurst.forEach(premise -> {

                if (premise.match(d, matchTTL)) {

                    if (derivable(d)) {

                        d.derive(deriveTTL);

                        n.emotion.premiseFire.increment();

                    } else {
                        n.emotion.premiseUnderivable.increment();
                    }

                } else {
                    n.emotion.premiseFailMatch.increment();
                }

            });

        }

    }
    private void selectPremises(NAR nar, int premisesMax, BiPredicate<Task, PriReference<Term>> each) {

        int premisesRemain[] = new int[]{premisesMax};
        int perConceptRemain[] = new int[1];

        int tasklinks = (int) Math.ceil(premisesMax / ((float) termLinksPerTaskLink));

        //return false to stop the current concept but not the entire chain
        BiPredicate<Task, PriReference<Term>> continueHypothesizing = (tasklink, termlink) ->
                (perConceptRemain[0]-- > 0) && each.test(tasklink, termlink) && (--premisesRemain[0] > 0);

        //for safety in case nothing is generated, this will limit the max # of concepts tried
        int[] conceptsRemain = new int[]{2 * (int) Math.ceil(premisesMax / ((float) (termLinksPerTaskLink * termLinksPerTaskLink)))};

        this.source.accept(a -> {

            perConceptRemain[0] = premisesPerConcept;

            premiseMatrix(a,
                    nar, continueHypothesizing,
                    tasklinks, termLinksPerTaskLink);

            return premisesRemain[0] > 0 && conceptsRemain[0]-- > 0;
        });


    }

    /**
     * hypothesize a matrix of premises, M tasklinks x N termlinks
     */
    public void premiseMatrix(Activate conceptActivation, NAR nar, BiPredicate<Task, PriReference<Term>> continueHypothesizing, int _tasklinks, int _termlinksPerTasklink) {

        Concept concept = conceptActivation.id;

        nar.emotion.conceptFire.increment();

        Bag<?, TaskLink> tasklinks = concept.tasklinks();
        final Bag<Term, PriReference<Term>> termlinks = concept.termlinks();

        if (!update(nar, tasklinks, termlinks))
            return;

        int[] conceptTTL = { _tasklinks *  _termlinksPerTasklink };

        Random rng = nar.random();

        //((TaskLinkCurveBag)tasklinks).compress(nar);

        tasklinks.sample(rng, _tasklinks, tasklink -> {

            Task task = tasklink.get(nar);
            if (task != null) {

////                float taskLinkMomentum = nar.taskLinkMomentum.floatValue();
//                float tPri = tasklink.priElseZero();
////                float priTransferred = (1f - taskLinkMomentum) * tPri;
////                tasklink.priSub(priTransferred);
////                tasklinks.pressurize(-priTransferred); //HACK depressurize to compensate for the tasklink drain
//
//                float priTransferred = tPri;

                activate(concept, tasklink, nar);

                termlinks.sample(rng, _termlinksPerTasklink, termlink -> {
                    if (!continueHypothesizing.test(task, termlink)) {
                        conceptTTL[0] = 0;
                        return false;
                    } else {
                        return (--conceptTTL[0] > 0);
                    }
                });
            } else {
                tasklink.delete();
                --conceptTTL[0]; //safety misfire decrement
            }

            return (conceptTTL[0] > 0);// ? Bag.BagSample.Next : Bag.BagSample.Stop;
        });

    }



}
