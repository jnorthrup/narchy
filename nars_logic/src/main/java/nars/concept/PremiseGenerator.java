package nars.concept;

import nars.Global;
import nars.NAR;
import nars.Op;
import nars.bag.BLink;
import nars.nal.Tense;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.Terms;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/** not thread safe; call from only one thread at a time */
abstract public class PremiseGenerator extends UnifySubst implements Function<Term, Task> {

    public final NAR nar;
    private BLink<? extends Termed> termLink;
    private BLink<? extends Concept> concept;
    private BLink<? extends Task> taskLink;
    private final Map<Term, Task> beliefCache = Global.newHashMap();
    long lastMatch = Tense.TIMELESS;

    public PremiseGenerator(NAR nar) {
        super(Op.VAR_QUERY, nar.memory, true);
        this.nar = nar;
        this.lastMatch = nar.time();
    }

    /**
     * temporary re-usable array for batch firing
     */
    private final Collection<BLink<Termed>> terms =
            Global.newArrayList();
            //Global.newHashSet(1);

    /**
     * temporary re-usable array for batch firing
     */
    private final Collection<BLink<Task>> tasks =
            Global.newArrayList();
            //Global.newHashSet(1);

    @NotNull
    private BLink[] termsArray = new BLink[0];
    @NotNull
    private BLink[] tasksArray = new BLink[0];


    /**
     * main entry point:
     * iteratively supplies a matrix of premises from the next N tasklinks and M termlinks
     * (recycles buffers, non-thread safe, one thread use this at a time)
     */
    public void firePremiseSquared(
            @NotNull BLink<? extends Concept> conceptLink,
            int tasklinks, int termlinks,
            @NotNull Consumer<BLink<? extends Termed>> eachTermLink /* forget/update func */,
            @NotNull Predicate<BLink<? extends Task>> eachTaskLink /* forget/update func */
    ) {

        long now = nar.time();
        if (lastMatch!= now) {
            lastMatch = now;
            beliefCache.clear();
        }


        Concept concept = conceptLink.get();

        Collection<BLink<Task>> tasksBuffer = this.tasks;
        //concept.getTaskLinks().sample(tasklinks, eachTaskLink, tasksBuffer).commit();
        concept.tasklinks().filter(eachTaskLink).sample(tasklinks, tasksBuffer);
        if (tasksBuffer.isEmpty()) return;

        Collection<BLink<Termed>> termsBuffer = this.terms;
        //concept.getTermLinks().sample(termlinks, eachTermLink, termsBuffer).commit();
        concept.termlinks().forEachThen(eachTermLink).sample(termlinks, termsBuffer);
        if (termsBuffer.isEmpty()) return;

        //convert to array for fast for-within-for iterations
        BLink[] tasksArray = this.tasksArray = tasksBuffer.toArray(this.tasksArray);
        tasksBuffer.clear();

        BLink[] termsArray = this.termsArray = termsBuffer.toArray(this.termsArray);
        termsBuffer.clear();

        for (BLink<? extends Termed> termLink : (BLink<? extends Termed>[]) termsArray) {

            if (termLink == null) break;

            match(conceptLink, tasksArray, termLink);

        }

    }



    void match(BLink<? extends Concept> concept, BLink<? extends Task>[] taskLinks, @NotNull BLink<? extends Termed> termLink) {


        this.concept = concept;
        this.termLink = termLink;

        for (BLink<? extends Task> taskLink : taskLinks) {

            if (taskLink == null) break; //null-terminated array, ends

            Compound taskLinkTerm = taskLink.get().term();
            Term termLinkTerm  = termLink.get().term();

            if (Terms.equalSubTermsInRespectToImageAndProduct(taskLinkTerm, termLinkTerm )) {
                continue;
            }

            this.taskLink = taskLink;

            matchAll(taskLinkTerm, termLinkTerm );
        }
    }


    /** uses the query-unified term to complete a premise */
    @Override public final boolean accept(Term beliefTerm, Term unifiedBeliefTerm) {

        Task belief = beliefTerm.isCompound() ?
                beliefCache.computeIfAbsent(beliefTerm, this) :
                null; //atomic terms will have no beliefs anyway

        //if the unified belief term is different, then clone the known belief with it as the new belief
        if (belief!=null && (unifiedBeliefTerm instanceof Compound) && !beliefTerm.equals(unifiedBeliefTerm)) {
            Task unifiedBelief = new MutableTask(belief, (Compound)unifiedBeliefTerm).normalize(memory);
            if (unifiedBelief!=null) {
                belief = unifiedBelief;
            }

        }

        //TODO also modify the termlink / "belief term" in the premise? or leave as variable

        premise(concept, taskLink, termLink, belief);

        Task task = taskLink.get();
        //Report questions/solution pairs involving query variable that have been matched
        if (belief != null && task.isQuestOrQuestion() && !belief.isQuestOrQuestion())
            memory.onSolve(task, belief);

        return true;
    }

    abstract protected void premise(BLink<? extends Concept> concept, BLink<? extends Task> taskLink, BLink<? extends Termed> termLink, Task belief);

    /** resolves the most relevant task for a given term/concept */
    @Override public final Task apply(Term beliefTerm) {

        Concept beliefConcept = nar.concept(beliefTerm);

        Task belief = null;
        if ((beliefConcept != null) && (beliefConcept.hasBeliefs())) {

            //long taskTime = taskLink.get().occurrence();
            long now = nar.time();

            belief = beliefConcept.beliefs().top(
                    now, //taskTime,
                    now);

            if (belief == null || belief.isDeleted()) {
                throw new RuntimeException("deleted belief: " + belief + " " + beliefConcept.hasBeliefs());
            }

        }

        return belief;
    }

}
