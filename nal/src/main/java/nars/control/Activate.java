package nars.control;

import jcog.bag.Bag;
import jcog.decide.DecideRoulette;
import jcog.list.FasterList;
import jcog.pri.PLink;
import jcog.pri.Pri;
import jcog.pri.PriReference;
import jcog.pri.op.PriForget;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.concept.Tasklinks;
import nars.concept.TermLinks;
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

    /** controls the rate at which tasklinks 'spread' to interact with termlinks */
    static int termlinksPerTasklink = 3;

    public Activate(Concept c, float pri) {
        super(c, pri);
    }

    public Iterable<Premise> hypothesize(NAR nar, BatchActivation ba, int premisesMax) {

        assert (premisesMax > 0);

        nar.emotion.conceptFires.increment();

        List<Concept> conceptualizedTemplates = $.newArrayList();
        float cost = TermLinks.linkTemplates(id, id.templates(), conceptualizedTemplates, priElseZero(), nar.momentum.floatValue(), nar, ba);
        if (cost >= Pri.EPSILON)
            priSub(cost);

        final Bag<Term, PriReference<Term>> termlinks = id.termlinks();

        float linkForgetting = nar.forgetRate.floatValue();
        termlinks.commit(termlinks.forget(linkForgetting));
        int ntermlinks = termlinks.size();
        if (ntermlinks == 0)
            return null;

        //TODO add a termlink vs. tasklink balance parameter
        int TERMLINKS_SAMPLED = (int) Math.ceil((float) Math.sqrt(premisesMax));

//        int tlSampled = Math.min(ntermlinks, TERMLINKS_SAMPLED);
//        FasterList<PriReference<Term>> terml = new FasterList(tlSampled);
//        termlinks.sample(tlSampled, ((Consumer<PriReference>) terml::add));
//        int termlSize = terml.size();
//        if (termlSize <= 0) return null;


        final Bag<Task, PriReference<Task>> tasklinks = id.tasklinks();
        long now = nar.time();
        int dur = nar.dur();
        tasklinks.commit(PriForget.forget(tasklinks, linkForgetting, Pri.EPSILON, (r)-> new Tasklinks.TaskLinkForget(r, now, dur)));
        int ntasklinks = tasklinks.size();
        if (ntasklinks == 0) return null;

        Random rng = nar.random();


        //apply the nar valuation to further refine selection of the tasks collected in the oversample prestep
        List<Premise> next = new FasterList(premisesMax);
        final int[] remaining = {premisesMax};

        tasklinks.sample((Predicate<PriReference<Task>>) tasklink -> {

            final Task task = tasklink.get();
            if (task == null || task.isDeleted())
                return true;

            int termlinksSampled = Math.min(Math.max(1, (int) Math.ceil(task.priElseZero() * termlinksPerTasklink)), remaining[0]);
            termlinks.sample(termlinksSampled, (termlink)->{
                final Term term = termlink.get();
                if (term != null) {

                    Premise p = new Premise(task, term,

                            //targets:
                            randomTemplateConcepts(conceptualizedTemplates, rng, TERMLINKS_SAMPLED /* heuristic */, nar)

                    );

                    next.add(p);

                }

                --remaining[0];
            });

            return (remaining[0] > 0);
        });

        return next;
    }


    private static List<Concept> randomTemplateConcepts(List<Concept> tt, Random rng, int count, NAR nar) {

//            {
//                //this allows the tasklink, if activated to be inserted to termlinks of this concept
//                //this is messy, it propagates the tasklink further than if the 'callback' were to local templates
//                List<Concept> tlConcepts = terml.stream().map(t ->
//                        //TODO exclude self link to same concept, ie. task.concept().term
//                        nar.concept(t.get())
//                ).filter(Objects::nonNull).collect(toList());
//            }
        {
            //Util.selectRoulette(templateConcepts.length, )

        }


        int tts = tt.size();
        if (tts == 0) {
            return Collections.emptyList();
        } else if (tts < count) {
            return tt; //all of them
        } else {

            List<Concept> uu = $.newArrayList(count);
            DecideRoulette.selectRouletteUnique(rng, tts, (w) -> {
                //return tt.get(w).volume(); //biased toward larger template components so the activation trickles down to atoms with less probabilty
                return 1f; //flat
            }, (z) -> {
                uu.add(tt.get(z));
                return (uu.size() < count);
            });
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

    //    protected int premise(Derivation d, Premise p, Consumer<DerivedTask> x, int ttlPerPremise) {
//        int ttl = p.run(d, ttlPerPremise);
//        //TODO record ttl usage
//        return ttl;
//    }


    @Override
    public Term term() {
        return id.term();
    }

}
