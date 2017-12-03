/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package nars.control;

import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.table.BeliefTable;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.subst.Unify;
import nars.term.subst.UnifySubst;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import static nars.Op.BELIEF;
import static nars.concept.Tasklinks.linkTask;
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

    public final Task task;
    public final Term termLink;

    @Nullable
    public final Collection<Concept> links;

    public Premise(Task tasklink, Term termlink, Collection<Concept> links) {
        //assert(!(termlink instanceof Bool));
        this.task = tasklink;
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
     */
    @Nullable
    public Derivation match(Derivation d, int matchTTL) {

        NAR n = d.nar;
        n.emotion.conceptFirePremises.increment();

        //nar.emotion.count("Premise_run");

        final Task task = this.task;
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

        boolean beliefConceptCanAnswerTaskConcept = false;

        if (!taskTerm.equals(beliefTerm)) {
            int var = Op.VAR_QUERY.bit | Op.VAR_DEP.bit | Op.VAR_INDEP.bit;
            boolean beliefHasVars = beliefTerm.hasAny(var);
            if (taskTerm.hasAny(var) || beliefHasVars) {
                Unify u = unify(taskTerm, beliefTerm, n, matchTTL);
                if (u != null) {
                    if (beliefHasVars) {
                        beliefTerm = beliefTerm.transform(u);
                        if (beliefTerm == null || beliefTerm instanceof Bool)
                            return null;
                    }
                    beliefConceptCanAnswerTaskConcept = true;
                }
            }
        }

        beliefTerm = beliefTerm.unneg(); //HACK ?? assert(beliefTerm.op()!=NEG);

        //QUESTION ANSWERING and TERMLINK -> TEMPORALIZED BELIEF TERM projection
        Task belief = null;
        Concept beliefConcept = n.conceptualize(beliefTerm);


        if (beliefConcept != null && !beliefTerm.hasVarQuery()) { //doesnt make sense to look for a belief in a term with query var, it will have none

            Task match;

            if (task.isQuestOrQuestion()) {
                if ((beliefConceptCanAnswerTaskConcept || beliefConcept.equals(taskConcept))) {
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

                    match = answerTable.answer(task.start(), task.end(), dur, task, beliefTerm, n);
                    if (match != null) {
                        assert (task.isQuest() || match.punc() == BELIEF) : "quest answered with a belief but should be a goal";

                        @Nullable Task answered = task.onAnswered(match, n);
                        if (answered != null) {

                            n.emotion.onAnswer(task, answered);

                        }
                    }
                } else {
                    long focus = matchTime(task, now, dur, n);
                    long focusStart, focusEnd;
                    if (focus == ETERNAL) {
                        focusStart = focusEnd = ETERNAL;
                    } else {
                        focusStart = focus - dur;
                        focusEnd = focus + dur;
                    }

//                boolean tryMatch = true;
////                if (beliefIsTask && task.punc() == BELIEF && task.during(when)) {
////                    if (Math.abs(when - now) > 0 /*= dur*/) {
////                        //try projecting to now (maybe also a future time) because it will be a different time
////                        when = now;
////                    } else {
////                        //leave belief blank. it already matches itself
////                        tryMatch = false;
////                    }
////                }
//                if (tryMatch) {
                    match = beliefConcept.beliefs().match(focusStart, focusEnd, beliefTerm, n);
//                } else {
//                    match = null;
//                }
                }
            } else {
                long focus = matchTime(task, now, dur, n);
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

//                boolean tryMatch = true;
////                if (beliefIsTask && task.punc() == BELIEF && task.during(when)) {
////                    if (Math.abs(when - now) > 0 /*= dur*/) {
////                        //try projecting to now (maybe also a future time) because it will be a different time
////                        when = now;
////                    } else {
////                        //leave belief blank. it already matches itself
////                        tryMatch = false;
////                    }
////                }
//                if (tryMatch) {
                match = beliefConcept.beliefs().match(focusStart, focusEnd, beliefTerm, n);
//                } else {
//                    match = null;
//                }
            }

            if (match != null && match.isBelief()) {
                belief = match;
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

        d.set(this, belief, beliefTerm);
        return d;
    }


    /**
     * temporal focus control: determines when a matching belief or answer should be projected to
     */
    static long matchTime(Task task, long now, int dur, NAR nar) {
        assert (now != ETERNAL);

        if (task.isEternal()) {
            return ETERNAL;
        } else {

            //return now;

            //return task.nearestTimeTo(now);

            return nar.random().nextBoolean() ?
                    now : task.nearestTimeTo(now);

            //        return nar.random().nextBoolean() ?
            //                task.nearestTimeTo(now) :
            //                now + Math.round((-0.5f + nar.random().nextFloat()) * 2f * (Math.abs(now - task.mid())));
        }

        //return now + dur;

//        if (task.isEternal()) {
//            return ETERNAL;
//        } else //if (task.isInput()) {
//            return task.nearestTimeTo(now);

//        } else {
//            if (task.isBelief()) {
//                return now +
//                        nar.dur() *
//                            nar.random().nextInt(2*Param.PREDICTION_HORIZON)-Param.PREDICTION_HORIZON; //predictive belief
//            } else {
//                return Math.max(now, task.start()); //the corresponding belief for a goal or question task
//            }
//        }

        //now;
        //now + dur;
    }


    /**
     * unify any (and only) query variables which may be present in
     * the 'a' term with any non-query terms in the 'q' term
     * returns non-null if unification succeeded and resulted in a transformed 'a' term
     * sets a negative number in the ttl array, which is to be added to the callee's
     * ttl.  if zero, then no TTL was consumed
     */
    private static UnifySubst unify(Term q, Term a, NAR nar, int ttl) {


        if (q.op() != a.op() /*|| q.size() != a.size()*/)
            return null; //fast-fail: no chance

        final boolean[] result = {false};
        UnifySubst u = new UnifySubst(null, nar, (aa) -> {

            result[0] = true;
            return false;

//            if (!aa.equals(a)) {
//
//                aa = aa.eval(nar.terms);
//                if (aa!=null) {
//                    result[0] = ((Compound) aa);
//                    return false; //only this match
//                }
//            }
//
//
//            return true; //keep trying

        }, ttl);
        u.unify(q, a, true);

        //ttl[0] = -(startTTL - u.ttl); //how much consumed

        return result[0] ? u : null;

//        if (Terms.equal(q, a, false, true /* no need to unneg, task content is already non-negated */))
//            return q;
//        else
//            return null;
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
