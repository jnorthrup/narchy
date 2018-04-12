/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package nars.derive;

import jcog.Util;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.concept.Concept;
import nars.table.BeliefTable;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.subst.UnifySubst;
import org.jetbrains.annotations.Nullable;

import static nars.Op.BELIEF;
import static nars.Op.VAR_QUERY;

/**
 * Defines the conditions used in an instance of a derivation
 * Contains the information necessary for generating derivation Tasks via reasoning rules.
 * <p>
 * It is meant to be disposable and should not be kept referenced longer than necessary
 * to avoid GC loops, so it may need to be weakly referenced.
 */
public class Premise {

    public final Task task;
    public final PriReference<Term> termLink;
    private final int hash;

    //TODO make global param



    Premise(Task task, PriReference<Term> termLink) {
        super();
        this.task = task;
        this.termLink = termLink;
        this.hash = Util.hashCombine(task, term());
    }

    public Term term() {
        return termLink.get();
    }

    final static int var = Op.VAR_QUERY.bit;// | Op.VAR_DEP.bit | Op.VAR_INDEP.bit;

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
    public Derivation match(Derivation d, long[] focus, int matchTTL) {


        Term taskTerm = task.term();
        Term taskConcept = task.term().concept();
        Op to = taskTerm.op();

        final boolean[] beliefConceptCanAnswerTaskConcept = {false};
        boolean unifiedBelief = false;

        Term beliefTerm = term();
        Op bo = beliefTerm.op();
        if (to == bo) {
            if (taskConcept.equals(beliefTerm.concept())) {
                beliefConceptCanAnswerTaskConcept[0] = true;
            } else {

                if ((bo.conceptualizable) && (beliefTerm.hasAny(var) || taskTerm.hasAny(var))) {

                    Term _beliefTerm = beliefTerm;
                    final Term[] unifiedBeliefTerm = new Term[]{null};
                    UnifySubst u = new UnifySubst(/*null*/VAR_QUERY, d.nar, (y) -> {
                        if (y.op().conceptualizable) {
                            y = y.normalize();

                            beliefConceptCanAnswerTaskConcept[0] = true;

                            if (!y.equals(_beliefTerm)) {
                                unifiedBeliefTerm[0] = y;
                                return false; //stop
                            }
                        }
                        return true; //keep going
                    }, matchTTL);
                    u.varSymmetric = true;
                    u.varCommonalize = true;
                    u.unify(taskTerm, beliefTerm, true);
                    if (unifiedBeliefTerm[0] != null) {
                        beliefTerm = unifiedBeliefTerm[0];
                        unifiedBelief = true;
                    }

                }
            }
        }
        beliefTerm = beliefTerm.unneg(); //HACK ?? assert(beliefTerm.op()!=NEG);

        //QUESTION ANSWERING and TERMLINK -> TEMPORALIZED BELIEF TERM projection
        Task belief = match(d, focus, beliefTerm, taskConcept, beliefConceptCanAnswerTaskConcept[0], unifiedBelief);


        if (belief != null) {
            beliefTerm = belief.term(); //use the belief's actual possibly-temporalized term
            if (belief.equals(task)) { //do not repeat the same task for belief
                belief = null; //force structural transform; also prevents potential inductive feedback loop
            }
        }
        assert (!(beliefTerm instanceof Bool)): "beliefTerm boolean; termLink=" + termLink + ", belief=" + belief;

        return !d.reset(task, belief, beliefTerm) ? null : d;

    }

    @Nullable Task match(Derivation d, long[] focus, Term beliefTerm, Term taskConcept, boolean beliefConceptCanAnswerTaskConcept, boolean unifiedBelief) {
        //        float timeFocus = n.timeFocus.floatValue();
//        int fRad = Math.round(Math.max(1,dur * timeFocus));

        NAR n = d.nar;
        int dur = d.dur;

        Task belief = null;

        final Concept beliefConcept = beliefTerm.op().conceptualizable ?
                n.conceptualize(beliefTerm) //conceptualize in case of dynamic concepts
                :
                null;
        if (beliefConcept != null) {

            if (!beliefTerm.hasVarQuery()) { //doesnt make sense to look for a belief in a term with query var, it will have none

                final BeliefTable bb = beliefConcept.beliefs();
                if (task.isQuestOrQuestion()) {
                    if (beliefConceptCanAnswerTaskConcept) {
                        final BeliefTable answerTable =
                                (task.isGoal() || task.isQuest()) ?
                                        beliefConcept.goals() :
                                        bb;

//                            //see if belief unifies with task (in reverse of previous unify)
//                            if (questionTerm.varQuery() == 0 || (unify((Compound)beliefConcept.term(), questionTerm, nar) == null)) {
//
//                            } else {
//
//                            }

                        Task match = answerTable.answer(task.start(), task.end(), dur, task, beliefTerm, n, d::add);
                        if (match != null) {
                            assert (task.isQuest() || match.punc() == BELIEF) : "quest answered with a belief but should be a goal";

                            @Nullable Task answered = task.onAnswered(match, n);
                            if (answered != null) {
                                n.emotion.onAnswer(task, answered);
                            }

                            if (match.isBelief()) {
                                belief = match;
                            }

                        }
                    }
                }

                if ((belief == null) && !bb.isEmpty()) {


                    belief = bb.match(
                            focus[0], focus[1],
                            beliefTerm,
                            n,
                            taskConcept.equals(beliefConcept.term()) ? x ->
                                !x.equals(task) : null);
                }
            }


            if (unifiedBelief) {
                Concept originalBeliefConcept = n.conceptualize(term());
                if (originalBeliefConcept != null)
                    linkVariable(originalBeliefConcept, beliefConcept);
            }

        }
        return belief;
    }


    @Override
    public boolean equals(Object obj) {
        return hash == obj.hashCode() && ((Premise)obj).task.equals(task) && ((Premise)obj).term().equals(term());
    }

    @Override
    public int hashCode() {
        return hash;
    }

    /**
     * x has variables, y unifies with x and has less or no variables
     */
    private void linkVariable(Concept lessConstant, Concept moreConstant) {

        //fraction of the source termlink's budget
        float pri = termLink.priElseZero() * 0.5f;
        //termLink.priMult(0.5f);

//        /** creates a tasklink/termlink proportional to the tasklink's priority
//         *  and inversely proportional to the increase in term complexity of the
//         *  unified variable.  ie. $x -> (y)  would get a stronger link than  $x -> (y,z)
//         */
//        PriReference taskLink = this;
        Term moreConstantTerm = moreConstant.term();
        Term lessConstantTerm = lessConstant.term();
//        float pri = taskLink.priElseZero()
//                * (1f/lessConstantTerm.volume());
//                //* Util.unitize(lessConstantTerm.complexity() / ((float) moreConstantTerm.complexity()));
//
        //share the budget in 2 opposite links: specific -> general & general -> specific
        moreConstant.termlinks().putAsync(new PLink<>(lessConstantTerm, pri/2f));
        lessConstant.termlinks().putAsync(new PLink<>(moreConstantTerm, pri/2f));
//        //moreConstant.termlinks().putAsync(new PLink<>(taskConcept.term(), pri));
//        //taskConcept.termlinks().putAsync(new PLink<>(moreConstantTerm, pri));
//
//
//        //Tasklinks.linkTask(this.task.get(), pri, moreConstant);

    }

    @Override
    public String toString() {
        return "Premise(" +
                task +
                " * " + term() +
                ')';
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
