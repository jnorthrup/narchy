/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package nars.derive.premise;

import jcog.Util;
import nars.NAL;
import nars.NAR;
import nars.Task;
import nars.derive.Derivation;
import nars.table.BeliefTable;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.util.TermException;
import nars.time.Tense;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static nars.Op.COMMAND;
import static nars.term.atom.Bool.Null;
import static nars.time.Tense.ETERNAL;

/** immutable premise */
public class AbstractPremise implements Premise {

	public final Termed task, belief;


	/** structural */
	public AbstractPremise(Task t) {
		this(t, t.term());
	}

	public AbstractPremise(Termed task, Termed belief) {
		this.task = task;
		this.belief = belief;
	}



	@Override
	public final Term taskTerm() {
		return task instanceof Term ? (Term)task : task.term();
	}

	@Override
	public final Term beliefTerm() {
		return belief instanceof Term ? (Term)belief : belief.term();
	}

	@Override
	@Nullable public final Task task() {
		return task instanceof Task ? (Task)task : null;
	}
	@Override
	@Nullable public final Task belief() {
		return belief instanceof Task ? (Task)belief : null;
	}





	/**
	 * resolve the most relevant belief of a given target/concept
	 * <p>
	 * patham9 project-eternalize
	 * patham9 depending on 4 cases
	 * patham9 https:
	 * sseehh__ ok ill add that in a bit
	 * patham9 you need  project-eternalize-to
	 * sseehh__ btw i disabled immediate eternalization entirely
	 * patham9 so https:
	 * patham9 especially try to understand the "temporal temporal" case
	 * patham9 its using the result of higher confidence
	 * <p>
	 * returns ttl used, -1 if failed before starting
	 *
	 * @param matchTime - temporal focus control: determines when a matching belief or answer should be projected to
	 */
	@Nullable
	public AbstractPremise match(int var, Derivation d, int matchTTL) {

		Task task = (Task)this.task;
		Term nextBeliefTerm = (Term) this.belief;

		if (nextBeliefTerm == Null || !nextBeliefTerm.op().taskable || task.punc()==COMMAND)// || /*beliefTerm.isNormalized() && */nextBeliefTerm.hasAny(VAR_QUERY))
			return this; //structural

		boolean beliefConceptUnifiesTaskConcept = false;

		Term taskTerm = task.term();
		if (taskTerm.equals(nextBeliefTerm)) {
			beliefConceptUnifiesTaskConcept = true;
		} else if (taskTerm.opID() == nextBeliefTerm.opID()) {

			if (taskTerm.equalsRoot(nextBeliefTerm)) {
				//difference involving XTERNAL etc
				beliefConceptUnifiesTaskConcept = true;
				if (nextBeliefTerm.hasXternal() && !taskTerm.hasXternal())
					nextBeliefTerm = taskTerm;

			} else if (nextBeliefTerm.hasAny(var) || taskTerm.hasAny(var)) {

				Term unifiedBeliefTerm = d.premisePreUnify.unified(taskTerm, nextBeliefTerm, matchTTL);

				if (unifiedBeliefTerm != null) {

//                    unifiedBeliefTerm = //d.random.nextBoolean() ?
////                            unifiedBeliefTerm
////                            :
//                            unifiedBeliefTerm.normalize();

//                    beliefTerm = unifiedBeliefTerm.normalize();
					nextBeliefTerm = unifiedBeliefTerm;

					beliefConceptUnifiesTaskConcept = true;
				} else {
					beliefConceptUnifiesTaskConcept = false;
				}

			}

		}


		Task belief = match(d, nextBeliefTerm, beliefConceptUnifiesTaskConcept);

		if (task != belief && task.stamp().length == 0) {
			//only allow unstamped tasks to apply with stamped beliefs.
			//otherwise stampless tasks could loop forever in single premise or in interaction with another stampless task
			if (belief == null || belief.stamp().length == 0)
				return null;
		}

		if (belief!=null)
			nextBeliefTerm = belief.term();

		if (NAL.test.DEBUG_EXTRA) {
			if (nextBeliefTerm.volume() > d.termVolMax)
				throw new TermException("excessive volume", nextBeliefTerm); //return null; //WTF
		}
		if (belief == null && nextBeliefTerm instanceof Compound && nextBeliefTerm.hasXternal() && taskTerm.volume() > nextBeliefTerm.volume() && !taskTerm.containsRecursively(nextBeliefTerm)) {
			//structurify: try to match the beliefTerm to a taskTerm component
			//  emulates (slowly) the termlink id codes from opennars
			Term[] found = new Term[1];
			int bOp = nextBeliefTerm.opID();
			int bStruct = nextBeliefTerm.structure();
			Term _nextBeliefTerm = nextBeliefTerm;
			//TODO shuffle recursion order
			taskTerm.recurseTerms(x -> x.hasAll(bStruct), s -> {
				if (s instanceof Compound) {
					s = s.unneg();
					if (s.opID() == bOp) {
						if (_nextBeliefTerm.equalsRoot(s)) {
							found[0] = s;
							return false;
						}
					}
				}
				return true;
			}, null);
			if (found[0] != null)
				nextBeliefTerm = found[0];
		}

		return belief != null ? new AbstractPremise(task, belief) :
			!this.belief.equals(nextBeliefTerm) ? new AbstractPremise(task, nextBeliefTerm) :
				this;

	}


	private @Nullable Task match(Derivation d, Term beliefTerm, boolean beliefConceptUnifiesTaskConcept) {

		NAR n = d.nar();

		final BeliefTable beliefTable = n.tableDynamic(beliefTerm, true);

		Task task = (Task)this.task;

		boolean quest = task.isQuest();

		if (beliefConceptUnifiesTaskConcept && task.isQuestionOrQuest()) {

			BeliefTable answerTable = quest ? n.tableDynamic(beliefTerm, false) : beliefTable;
			if (answerTable != null && !answerTable.isEmpty()) {
				Task a = tryAnswer(task, beliefTerm, answerTable, d);
				if (!quest)
					return a; //premise belief
			}
		}

		return beliefTable != null && !beliefTable.isEmpty() ?
			tryMatch(beliefTerm, beliefTable, d) : null;

	}

//        if (unifiedBelief && belief != null && Param.LINK_VARIABLE_UNIFIED_PREMISE) {
//            linkVariable(unifiedBelief, d.nar, beliefConcept);
//        }


	private Task task(BeliefTable bb, Term beliefTerm, long[] when, @Nullable Predicate<Task> beliefFilter, Derivation d) {
		float dur =
			d.dur();
			//0;

		return bb.matching(when[0], when[1], beliefTerm, beliefFilter, dur, d.nar)
			.task(true, false, false);
	}


	private Task tryMatch(Term beliefTerm, BeliefTable bb, Derivation d) {

        Task t = task(bb, beliefTerm, timeFocus(beliefTerm, d), this::taskNotEquals, d);

		return t!=null && t.equals(task) ? null : t; //HACK the filter helps but is not 100%
	}

	private boolean taskNotEquals(Task x) {
		return !x.equals(task);
	}

	@Nullable
	private Task tryAnswer(Task question, Term beliefTerm, BeliefTable answerTable, Derivation d) {

        Task answer = task(answerTable, beliefTerm, timeFocus(beliefTerm, d), null, d);
		if (answer != null) {
			//assert (task.isQuest() || match.punc() == BELIEF) : "quest answered with a belief but should be a goal";
			answer = question.onAnswered(answer);
			if (answer!=null)
				answer(question, answer, d);
		}

		return answer;
	}

    private void answer(Task q, Task a, Derivation d) {
//        if (x.conf() > d.confMin) {
//            if (x.isGoal())
//                d.what.accept(x);
//            else
		float qPri = q.priElseZero();
		float aPri = a.pri();
		float pri =
			//qPri * aPri;
			Util.or(qPri, aPri);
		d.what.link(a, pri);
		d.what.emit(a);
//        }
    }

	private long[] timeFocus(Term beliefTerm, Derivation d) {

		long[] l = d.deriver.timing.premise(d.what, task(), beliefTerm);

		if (NAL.premise.PREMISE_FOCUS_TIME_DITHER && l[0] != ETERNAL)
			Tense.dither(l, d.ditherDT);

		return l;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		AbstractPremise p = (AbstractPremise) obj;
		return p.task.equals(task) && p.belief.equals(belief);
	}

	@Override
	public final int hashCode() {
		return Util.hashCombine(task.hashCode(), belief.hashCode());
//		Task b = belief();
//		return Util.hashCombine(task.hashCode(), b ==null ? belief.hashCode() : b.hashCode());
		//return (int) (hash >> 10) /* shift down about 10 bits to capture all 3 elements in the hash otherwise the task hash is mostly excluded */;
		//throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return "(" + task + " >> " + belief + ')';
	}


//    @Override
//    public int compareTo(Premise premise) {
//        if (this == premise)
//            return 0;
//
////        int h = Long.compare(hash, premise.hash);
////        if (h != 0)
////            return h;
//
//        if (task.equals(premise.task) && beliefTerm.equals(premise.beliefTerm))
//            return 0;
//
//        //TODO since Task doesnt implement Comparable, they could be compared by their byte[] serialization
////        int t = Integer.compare(System.identityHashCode(task), System.identityHashCode(premise.task));
////        if (t!=0)
////            return t;
////
////        int b = Integer.compare(System.identityHashCode(beliefTerm.hashCode()), System.identityHashCode(premise.beliefTerm.hashCode()));
////        if (b!=0)
////            return b;
//
////        return Integer.compare(System.identityHashCode(this), System.identityHashCode(premise));
//    }

}
