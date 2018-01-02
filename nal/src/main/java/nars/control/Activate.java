package nars.control;

import jcog.bag.Bag;
import jcog.decide.Roulette;
import jcog.list.FasterList;
import jcog.pri.PLink;
import jcog.pri.Pri;
import jcog.pri.PriReference;
import jcog.pri.op.PriForget;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.link.TaskLinkCurveBag;
import nars.link.Tasklinks;
import nars.link.TermLinks;
import nars.term.Term;
import nars.term.Termed;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;


/**
 * concept firing, activation, etc
 */
public class Activate extends PLink<Concept> implements Termed {

    /**
     * controls the rate at which tasklinks 'spread' to interact with termlinks
     */
    static int termlinksPerTasklink = 3;

    public Activate(Concept c, float pri) {
        super(c, pri);
    }

    /**
     * hypothesize premises, up to a max specified #
     */
    /*@NotNull*/
    public void premises(NAR nar, BatchActivation ba, Predicate<Premise> each) {



        activateTemplates(nar, ba);

        final Bag<Term, PriReference<Term>> termlinks = id.termlinks();

        int ntermlinks = termlinks.size();
        if (ntermlinks == 0)
            return;
        float linkForgetting = nar.forgetRate.floatValue();
        termlinks.commit(termlinks.forget(linkForgetting));


        //(int) Math.ceil((float) Math.sqrt(premisesMax));

//        int tlSampled = Math.min(ntermlinks, TERMLINKS_SAMPLED);
//        FasterList<PriReference<Term>> terml = new FasterList(tlSampled);
//        termlinks.sample(tlSampled, ((Consumer<PriReference>) terml::add));
//        int termlSize = terml.size();
//        if (termlSize <= 0) return null;


        final Bag tasklinks = id.tasklinks();
        long now = nar.time();
        int dur = nar.dur();
        int ntasklinks = tasklinks.size();
        tasklinks.commit(PriForget.forget(tasklinks, linkForgetting, Pri.EPSILON, (r) ->
                new Tasklinks.TaskLinkForget(r, now, dur)));
        if (ntasklinks == 0)
            return;


        int termlinksPerTasklink = Activate.termlinksPerTasklink;
        int[] safetyLimit = { tasklinks.size() *  termlinksPerTasklink };

        tasklinks.sample((Predicate<PriReference<Task>>) tasklink -> {


//            int termlinksSampled = Math.min(Math.max(1,
//                    (int) Math.ceil(
//                            Util.normalize(tasklink.priElseZero(), tasklinks.priMin(), tasklinks.priMax())
//                                    * termlinksPerTasklink)),
//                    remaining[0]);

            Task task = tasklink.get();
            if (task != null) { //HACK



                termlinks.sample(termlinksPerTasklink, (termlink) -> {

                    Premise p = Premise.the(tasklink, termlink,
                            Param.taskTermLinksToPremise,
                            nar.amp(task.cause()));
                    if (p != null) {
                        if (!each.test(p)) {
                            safetyLimit[0] = 0;
                            return false;
                        }
                    }

                    return  (--safetyLimit[0] > 0);
                });
            } else {
                --safetyLimit[0]; //safety misfire decrement
            }

            return (safetyLimit[0] > 0);// ? Bag.BagSample.Next : Bag.BagSample.Stop;
        }/*, (tl) -> {
            Task x = tl.get();
            if (x == null)
                return 0; //deleted
            float p = tl.pri();
            if (p != p)
                return 0; //deleted
            else
                return p * nar.amp(x.cause());
        }*/);

    }


    public void activateTemplates(NAR nar, BatchActivation ba) {
        nar.emotion.conceptActivations.increment();

        float cost = TermLinks.linkTemplates(id, id.templates(), priElseZero(), nar.momentum.floatValue(), nar, ba);
        if (cost >= Pri.EPSILON)
            priSub(cost);
    }


    public static List<Concept> randomTemplateConcepts(List<Concept> tt, Random rng, int count) {

//            {
//                //this allows the tasklink, if activated to be inserted to termlinks of this concept
//                //this is messy, it propagates the tasklink further than if the 'callback' were to local templates
//                List<Concept> tlConcepts = terml.stream().map(t ->
//                        //TODO exclude self link to same concept, ie. task.concept().term
//                        nar.concept(t.get())
//                ).filter(Objects::nonNull).collect(toList());
//            }
        //Util.selectRoulette(templateConcepts.length, )


        int tts = tt.size();
        if (tts == 0) {
            return Collections.emptyList();
        } else if (tts < count) {
            return tt; //all of them
        } else {

            List<Concept> uu = $.newArrayList(count);
            Roulette.selectRouletteUnique(tts, (w) -> {
                return tt.get(w).volume(); //biased toward larger template components so the activation trickles down to atoms with less probabilty
                //return 1f; //flat
            }, (z) -> {
                uu.add(tt.get(z));
                return (uu.size() < count);
            }, rng);
            return uu;
        }
    }


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
    public boolean delete() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Term term() {
        return id.term();
    }

}
