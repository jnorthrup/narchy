package nars.control;

import jcog.bag.Bag;
import jcog.pri.PLink;
import jcog.pri.Pri;
import jcog.pri.PriReference;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.link.Tasklinks;
import nars.link.TermLinks;
import nars.term.Term;
import nars.term.Termed;

import java.util.Random;
import java.util.function.BiPredicate;
import java.util.function.Predicate;


/**
 * concept firing, activation, etc
 */
public class Activate extends PLink<Concept> implements Termed {


    public Activate(Concept c, float pri) {
        super(c, pri);
    }

    /**
     * hypothesize premises, up to a max specified #
     */
    /*@NotNull*/
    public void premises(NAR nar, BiPredicate<PriReference<Task>, PriReference<Term>> each, int _tasklinks, int _termlinksPerTasklink) {


        if (!isDeleted())
            activateTemplates(nar);

        final Bag tasklinks = id.tasklinks();

        float linkForgetting = nar.forgetRate.floatValue();
        tasklinks.commit(tasklinks.forget(linkForgetting));
        int ntasklinks = tasklinks.size();
        if (ntasklinks == 0)
            return;

        final Bag<Term, PriReference<Term>> termlinks = id.termlinks();
        termlinks.commit(termlinks.forget(linkForgetting));
        int ntermlinks = termlinks.size();
        if (ntermlinks == 0)
            return; //TODO when can this happen




        int[] ttl = { _tasklinks *  _termlinksPerTasklink };

        Random rng = nar.random();

        tasklinks.sample(rng, _tasklinks, (Predicate<PriReference<Task>>) tasklink -> {


            Task task = tasklink.get();
            if (task != null) {

                //if (priApplied > Pri.EPSILON)
                Tasklinks.linkTaskTemplates(id, task, /*pri *  */ task.priElseZero(), nar);

                termlinks.sample(rng, _termlinksPerTasklink, (termlink) -> {
                    if (!each.test(tasklink, termlink)) {
                        ttl[0] = 0;
                        return false;
                    } else {
                        return (--ttl[0] > 0);
                    }
                });
            } else {
                --ttl[0]; //safety misfire decrement
            }

            return (ttl[0] > 0);// ? Bag.BagSample.Next : Bag.BagSample.Stop;
        });

    }


    void activateTemplates(NAR nar) {
        nar.emotion.conceptFire.increment();

        float cost = TermLinks.linkTemplates(id, priElseZero(), nar.momentum.floatValue(), nar);
        if (cost >= Pri.EPSILON)
            priSub(cost);
    }


//    public static List<Concept> randomTemplateConcepts(List<Concept> tt, Random rng, int count) {
//
////            {
////                //this allows the tasklink, if activated to be inserted to termlinks of this concept
////                //this is messy, it propagates the tasklink further than if the 'callback' were to local templates
////                List<Concept> tlConcepts = terml.stream().map(t ->
////                        //TODO exclude self link to same concept, ie. task.concept().term
////                        nar.concept(t.get())
////                ).filter(Objects::nonNull).collect(toList());
////            }
//        //Util.selectRoulette(templateConcepts.length, )
//
//
//        int tts = tt.size();
//        if (tts == 0) {
//            return Collections.emptyList();
//        } else if (tts < count) {
//            return tt; //all of them
//        } else {
//
//            List<Concept> uu = $.newArrayList(count);
//            Roulette.selectRouletteUnique(tts, (w) -> {
//                return tt.get(w).volume(); //biased toward larger template components so the activation trickles down to atoms with less probabilty
//                //return 1f; //flat
//            }, (z) -> {
//                uu.add(tt.get(z));
//                return (uu.size() < count);
//            }, rng);
//            return uu;
//        }
//    }


    //    public void activateTaskExperiment1(NAR nar, float pri, Term thisTerm, BaseConcept cc) {
//        Termed[] taskTemplates = templates(cc, nar);
//
//        //if (templateConceptsCount > 0) {
//
//        //float momentum = 0.5f;
//        float taskTemplateActivation = pri / taskTemplates.length;
//        for (Termed ct : taskTemplates) {
//
//            Concept c = nar.conceptualize(ct);
//            //this concept activates task templates and termlinks to them
//            if (c instanceof Concept) {
//                c.termlinks().putAsync(
//                        new PLink(thisTerm, taskTemplateActivation)
//                );
//                nar.input(new Activate(c, taskTemplateActivation));
//
////                        //reverse termlink from task template to this concept
////                        //maybe this should be allowed for non-concept subterms
////                        id.termlinks().putAsync(new PLink(c, taskTemplateActivation / 2)
////                                //(concept ? (1f - momentum) : 1))
////                        );
//
//            }
//
//
//        }
//    }


    @Override
    public Term term() {
        return id.term();
    }

}
