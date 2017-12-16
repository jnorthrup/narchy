/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package nars.control;

import jcog.Util;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.table.BeliefTable;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.subst.UnifySubst;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.function.ToLongFunction;

import static nars.Op.BELIEF;
import static nars.link.Tasklinks.linkTask;
import static nars.time.Tense.ETERNAL;

/**
 * Defines the conditions used in an instance of a derivation
 * Contains the information necessary for generating derivation Tasks via reasoning rules.
 * <p>
 * It is meant to be disposable and should not be kept referenced longer than necessary
 * to avoid GC loops, so it may need to be weakly referenced.
 */
public class Premise {

    static final Logger logger = LoggerFactory.getLogger(Premise.class);

    public final PriReference<Task> taskLink;
    public final Term termLink;

    @Nullable
    public final Collection<Concept> links;

    public Premise(PriReference<Task> tasklink, Term termlink, Collection<Concept> links) {
        this.taskLink = tasklink;
        this.termLink = termlink;
        this.links = links;
    }

    /**
     * resolve the most relevant belief of a given term/concept
     * <p>
     * patham9 project-eternalize
     * patham9 depending on 4 cases
     * patham9 https://github.com/opennars/opennars2/blob/a143162a559e55c456381a95530d00fee57037c4/src/nal/deriver/projection_eternalization.clj
     * sseehh__ ok ill add that in a bit
     * patham9 you need  project-eternalize-to
     * sseehh__ btw i disabled immediate eternalization entirely
     * patham9 so https://github.com/opennars/opennars2/blob/a143162a559e55c456381a95530d00fee57037c4/src/nal/deriver/projection_eternalization.clj#L31
     * patham9 especially try to understand the "temporal temporal" case
     * patham9 its using the result of higher confidence
     * <p>
     * returns ttl used, -1 if failed before starting
     *
     * @param matchTime - temporal focus control: determines when a matching belief or answer should be projected to
     */
    @Nullable
    public Derivation match(Derivation d, ToLongFunction<Task> matchTime, int matchTTL) {

        NAR n = d.nar;
        n.emotion.conceptFirePremises.increment();

        //nar.emotion.count("Premise_run");

        final Task task = this.taskLink.get();
        if (task == null || task.isDeleted()) {
//            Task fwd = task.meta("@");
//            if (fwd!=null)
//                task = fwd; //TODO multihop dereference like what happens in tasklink bag
//            else {
//                delete();
//                return;
//            }
            return null;
        }


        Concept taskConcept = task.concept(n, true);
        if (taskConcept == null) {
            if (Param.DEBUG) {
                //HACK disable this error print-out if the problem is just excess term volume
                if (task.volume() < n.termVolumeMax.intValue() || Param.DEBUG_EXTRA)
                    logger.warn("{} unconceptualizable", task); //WHY was task even created
                //assert (false) : task + " could not be conceptualized"; //WHY was task even created
            }
            task.delete();
            return null;
        }


        Collection<Concept> l = links;
        if (l != null) {
            linkTask(task, l);
        }

        int dur = d.dur;
        long now = d.time;


        Term beliefTerm = termLink;


        Term taskTerm = task.term();

        boolean beliefConceptCanAnswerTaskConcept = false, unifiedBelief = false;

        Op to = taskTerm.op();
        Op bo = beliefTerm.op();
        if (to.var || bo.var || to == bo) {
            if (taskTerm.equalsRoot(beliefTerm)) {
                beliefConceptCanAnswerTaskConcept = true;
            } else {
                int var = Op.VAR_QUERY.bit | Op.VAR_DEP.bit | Op.VAR_INDEP.bit;
                if (taskTerm.hasAny(var) || beliefTerm.hasAny(var)) {

                    Term _beliefTerm = beliefTerm;
                    final Term[] unifiedBeliefTerm = new Term[]{null};
                    UnifySubst u = new UnifySubst(null, n, (y) -> {
                        if (y.op().conceptualizable
                                && !y.equals(_beliefTerm)
                                && !y.hasAny(Op.BOOL)
                                ) {
                            unifiedBeliefTerm[0] = y;
                            return false; //stop
                        }
                        return true; //keep going
                    }, matchTTL);
                    u.varSymmetric = false;
                    u.varCommonalize = false;
                    if (u.unify(taskTerm, beliefTerm, true)) {
                        beliefConceptCanAnswerTaskConcept = true;
                        if (unifiedBeliefTerm[0] != null) {
                            beliefTerm = unifiedBeliefTerm[0];
                            unifiedBelief = true;
                        }
                    }
                }
            }
        }
        beliefTerm = beliefTerm.unneg(); //HACK ?? assert(beliefTerm.op()!=NEG);

        //QUESTION ANSWERING and TERMLINK -> TEMPORALIZED BELIEF TERM projection
        Task belief = null;

        final Concept beliefConcept = n.conceptualize(beliefTerm);


        if (beliefConcept != null) {

            if (!beliefTerm.hasVarQuery()) { //doesnt make sense to look for a belief in a term with query var, it will have none

                if (task.isQuestOrQuestion()) {
                    if (beliefConceptCanAnswerTaskConcept) {
                        final BeliefTable answerTable =
                                (task.isGoal() || task.isQuest()) ?
                                        beliefConcept.goals() :
                                        beliefConcept.beliefs();

//                            //see if belief unifies with task (in reverse of previous unify)
//                            if (questionTerm.varQuery() == 0 || (unify((Compound)beliefConcept.term(), questionTerm, nar) == null)) {
//
//                            } else {
//
//                            }

                        Task match = answerTable.answer(task.start(), task.end(), dur, task, beliefTerm, n);
                        if (match != null) {
                            assert (task.isQuest() || match.punc() == BELIEF) : "quest answered with a belief but should be a goal";

                            @Nullable Task answered = task.onAnswered(match, n);
                            if (answered != null) {
                                n.emotion.onAnswer(this.taskLink, answered);
                            }

                            if (match.isBelief()) {
                                belief = match;
                            }

                        }
                    }
                }

                if (belief == null) {
                    long focus = matchTime.applyAsLong(task);
                    long focusStart, focusEnd;
                    if (focus == ETERNAL) {
                        focusStart = focusEnd = ETERNAL;
                    } else {
                        focusStart =
                                //focus - dur;
                                focus;
                        focusEnd =
                                //focus + dur;
                                focus;
                    }

                    belief = beliefConcept.beliefs().match(focusStart, focusEnd, beliefTerm, n);
                }
            }


            if (unifiedBelief) {
                Concept originalBeliefConcept = n.concept(this.termLink);
                if (originalBeliefConcept!=null)
                    linkVariable(taskConcept, originalBeliefConcept, beliefConcept);
            }

        }


        if (belief != null) {
            beliefTerm = belief.term().unneg(); //use the belief's actual possibly-temporalized term

            if (belief.equals(task)) { //do not repeat the same task for belief
                belief = null; //force structural transform; also prevents potential inductive feedback loop
            }
        }

        if (beliefTerm instanceof Bool) {
            //logger.warn("{} produced Bool beliefTerm", this);
            return null;
        }

        //assert (!(beliefTerm instanceof Bool)): "beliefTerm boolean; termLink=" + termLink + ", belief=" + belief;

        d.reset().set(task, belief, beliefTerm);
        return d;
    }

    /**
     * x has variables, y unifies with x and has less or no variables
     */
    private void linkVariable(Concept taskConcept, Concept lessConstant, Concept moreConstant) {


        /** creates a tasklink/termlink proportional to the tasklink's priority
         *  and inversely proportional to the increase in term complexity of the
         *  unified variable.  ie. $x -> (y)  would get a stronger link than  $x -> (y,z)
         */
        PriReference<Task> taskLink = this.taskLink;
        Term moreConstantTerm = moreConstant.term();
        Term lessConstantTerm = lessConstant.term();
        float pri = taskLink.priElseZero() * Util.unitize(lessConstantTerm.volume() / ((float) moreConstantTerm.volume()));

        moreConstant.termlinks().putAsync(new PLink<>(lessConstantTerm, pri/2));
        lessConstant.termlinks().putAsync(new PLink<>(moreConstantTerm, pri/2));
        //moreConstant.termlinks().putAsync(new PLink<>(taskConcept.term(), pri));
        //taskConcept.termlinks().putAsync(new PLink<>(moreConstantTerm, pri));


        //Tasklinks.linkTask(this.task.get(), pri, moreConstant);

    }





//    public void merge(Premise incoming) {
//        //WARNING this isnt thread safe but collisions should be rare
//
//        Collection<Concept> target = this.links;
//        Collection<Concept> add = incoming.links;
//
//        if (target == add || add == null)
//            return; //same or no change
//
//        if (target == null || target.isEmpty()) {
//            this.links = add;
//            return; //just replace it
//        }
//
//        if (!(target instanceof Set)) {
//            Set<Concept> merge =
//                    new HashSet(target.size() + add.size());
//                    //Collections.newSetFromMap(new ConcurrentHashMap<>(target.size() + add.size()));
//            merge.addAll(target);
//            merge.addAll(add);
//            this.links = merge;
//        } else {
//            target.addAll(add);
//        }
//    }

}
