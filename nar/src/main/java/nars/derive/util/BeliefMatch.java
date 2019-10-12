package nars.derive.util;

import jcog.Util;
import nars.NAL;
import nars.NAR;
import nars.Task;
import nars.attention.What;
import nars.control.Why;
import nars.derive.Derivation;
import nars.derive.Deriver;
import nars.derive.premise.AbstractPremise;
import nars.derive.premise.Premise;
import nars.table.BeliefTable;
import nars.term.Compound;
import nars.term.Term;
import nars.term.util.TermException;
import nars.time.Tense;
import nars.unify.UnifySubst;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static nars.Op.COMMAND;
import static nars.time.Tense.ETERNAL;

/**
 * used to determine a premise's "belief task" for the provided "belief term",
 * and other premise functionality at the start of a derivation
 */
public class BeliefMatch extends UnifySubst {

	transient private Term output;

	public BeliefMatch() {
		super(Deriver.PremiseUnifyVars, null);
		commonVariables = NAL.premise.PREMISE_UNIFY_COMMON_VARIABLES;
	}

	@Nullable
	public Term uniSubst(Term taskTerm, Term beliefTerm) {

		this.output = null;

		return unify(beliefTerm, beliefTerm, taskTerm) ? output : null;
	}

	@Override
	protected boolean each(Term y) {
		y = y.unneg();
		if (y.op().conceptualizable) {
			if (!y.equals(input)) {
				output = y;
				return false;  //done
			}
		}
		return true; //continue
	}


	public Premise match(AbstractPremise x, Derivation d) {

		Task pt = x.task();
		if (pt.punc() != COMMAND) {
			Term pb = x.beliefTerm();
			if (!pt.term().equals(pb) && pb.op().taskable) {
				try (var __ = d.nar.emotion.derive_B_PremiseMatch.time()) {
					Premise y = match(x, Deriver.PremiseUnifyVars, d);
					if (y != null)
						return y;
				}
			}
		}

		return x;
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
	private Premise match(AbstractPremise p, int var, Derivation d) {

		Task task = (Task) p.task;

		Term nextBeliefTerm = (Term) p.belief;
		if (!nextBeliefTerm.op().taskable)
			return null; //structural

		boolean beliefUnifiesTask = false;

		Term taskTerm = task.term();
		if (taskTerm.equals(nextBeliefTerm)) {
			beliefUnifiesTask = true;
		} else if (taskTerm.opID() == nextBeliefTerm.opID()) {

			if (taskTerm.equalsRoot(nextBeliefTerm)) {
				//difference involving XTERNAL etc
				beliefUnifiesTask = true;
				if (nextBeliefTerm.hasXternal() && !taskTerm.hasXternal())
					nextBeliefTerm = taskTerm;

			} else if (nextBeliefTerm.hasAny(var) || taskTerm.hasAny(var)) {

				Term u = d.beliefMatch.uniSubst(taskTerm, nextBeliefTerm);
				if (u != null) {
					nextBeliefTerm = u;
					beliefUnifiesTask = true;
				} else {
					beliefUnifiesTask = false;
				}

			}

		}

		Task belief = match(task, nextBeliefTerm, beliefUnifiesTask, d);

		if (belief != null)
			nextBeliefTerm = belief.term();

		if (task != belief && task.stamp().length == 0) {
			//only allow unstamped tasks to apply with stamped beliefs.
			//otherwise stampless tasks could loop forever in single premise or in interaction with another stampless task
			if (belief == null || belief.stamp().length == 0)
				return null;
		}

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


		return belief != null ?
			new AbstractPremise(task, belief, Why.why(p.why(d), belief.why())) :
			(!p.belief.equals(nextBeliefTerm) ? new AbstractPremise(task, nextBeliefTerm, p.why(d)) :
				null);

	}

	private @Nullable Task match(Task task, Term beliefTerm, boolean beliefConceptUnifiesTaskConcept, Derivation d) {

		NAR n = d.nar;

		final BeliefTable beliefTable = n.tableDynamic(beliefTerm, true);

		boolean quest = task.isQuest();

		long[] when = timeFocus(task, beliefTerm, d);

		if (beliefConceptUnifiesTaskConcept && task.isQuestionOrQuest()) {

			BeliefTable answerTable = quest ? n.tableDynamic(beliefTerm, false) : beliefTable;
			if (answerTable != null && !answerTable.isEmpty()) {
				Task a = tryAnswer(task, beliefTerm, answerTable, when, d);
				if (!quest)
					return a; //premise belief
			}
		}

		return beliefTable != null && !beliefTable.isEmpty() ?
			tryMatch(task, beliefTerm, beliefTable, when, d) : null;

	}

	private Task task(BeliefTable bb, Term beliefTerm, long[] when, @Nullable Predicate<Task> beliefFilter, Derivation d) {
		float dur =
			d.dur;
		//0;

		return bb.matching(when[0], when[1], beliefTerm, beliefFilter, dur, d.nar)
			.task(true, false, false);
	}


	private Task tryMatch(Task task, Term beliefTerm, BeliefTable bb, long[] when, Derivation d) {

		Task t = task(bb, beliefTerm, when, task.isBelief() ? ((Predicate<Task>) task::equals).negate() : null, d);

		return t != null && t.equals(task) ? null : t; //HACK the filter helps but is not 100%
	}

	@Nullable
	private Task tryAnswer(Task question, Term beliefTerm, BeliefTable answerTable, long[] when, Derivation d) {


		Task answer = task(answerTable, beliefTerm, when, null, d);
		if (answer != null) {
			//assert (task.isQuest() || match.punc() == BELIEF) : "quest answered with a belief but should be a goal";
			answer = question.onAnswered(answer);
			if (answer != null)
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
		float aPri = a.priElseZero();
		float pri =
			//qPri * aPri;
			Util.or(qPri, aPri);

		What w = d.what;
		w.link(a, pri);
		w.emit(a);
	}

	private long[] timeFocus(Task task, Term beliefTerm, Derivation d) {

		long[] l = d.deriver.timing.premise(d.what, task, beliefTerm);

		if (NAL.premise.PREMISE_FOCUS_TIME_DITHER && l[0] != ETERNAL)
			Tense.dither(l, d.ditherDT);

		return l;
	}


}
