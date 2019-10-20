package nars.derive.action;

import nars.NAL;
import nars.Op;
import nars.Task;
import nars.control.Why;
import nars.derive.Derivation;
import nars.derive.premise.AbstractPremise;
import nars.derive.premise.Premise;
import nars.derive.rule.RuleCause;
import nars.derive.util.PremiseBeliefMatcher;
import nars.table.BeliefTable;
import nars.term.Compound;
import nars.term.Term;
import nars.term.util.TermException;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/** matches a belief task for a premise's beliefTerm, converting a single-premise to a double-premise */
public class MatchBelief extends NativeHow {

	public static final MatchBelief the = new MatchBelief();

	private MatchBelief() {
		hasBelief(false);
		neq(TheTask, TheBelief);
		isAny(TheBelief, Op.Conceptualizable); //Taskable, technically
	}

	@Override
	protected void run(RuleCause why, Derivation d) {
		try (var __ = d.nar.emotion.derive_B_PremiseMatch.time()) {
			var y = match(d.premise, PremiseBeliefMatcher.PremiseUnifyVars, why, d);
			if (y != null)
				d.add(y);
		}
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
    private static @Nullable Premise match(Premise p, int var, RuleCause why, Derivation d) {

		var beliefTerm = p.beliefTerm();
		if (!beliefTerm.op().taskable)
			return null; //HACK some non-taskable / non-conceptualizable beliefTerms op's are invisible to the predicate trie due to being masked in Anom's

		var task = p.task();

		var nextBeliefTerm = beliefTerm;

//		boolean beliefUnifiesTask = false;

		var taskTerm = task.term();
		if (taskTerm.equals(nextBeliefTerm)) {
//			beliefUnifiesTask = true;
		} else if (taskTerm.opID() == nextBeliefTerm.opID()) {

			if (taskTerm.equalsRoot(nextBeliefTerm)) {
				//difference involving XTERNAL etc
//				beliefUnifiesTask = true;
				if (nextBeliefTerm.hasXternal() && !taskTerm.hasXternal())
					nextBeliefTerm = taskTerm;

			} else if (nextBeliefTerm.hasAny(var) || taskTerm.hasAny(var)) {


				var stolen = d.unify.ttlGetAndSet(0); //HACK steal TTL temporarily
				d.beliefMatch.setTTL(stolen);

				var u = d.beliefMatch.uniSubst(taskTerm, nextBeliefTerm);

				d.unify.ttlGetAndSet(d.beliefMatch.ttl); //HACK restore remaining TTL

				if (u != null) {
					nextBeliefTerm = u;
//					beliefUnifiesTask = true;
				} else {
//					beliefUnifiesTask = false;
				}

			}

		}

		var belief = match(task, nextBeliefTerm, d);

		if (belief != null) {

			if (task != belief && task.stamp().length == 0) {
				//only allow unstamped tasks to apply with stamped beliefs.
				//otherwise stampless tasks could loop forever in single premise or in interaction with another stampless task
				if (belief.stamp().length == 0)
					return null;
			}

			nextBeliefTerm = belief.term();
		}

		if (NAL.test.DEBUG_EXTRA) {
			if (nextBeliefTerm.volume() > d.termVolMax)
				throw new TermException("excessive volume", nextBeliefTerm); //return null; //WTF
		}
		if (belief == null && nextBeliefTerm instanceof Compound && nextBeliefTerm.hasXternal() && taskTerm.volume() > nextBeliefTerm.volume() && !taskTerm.containsRecursively(nextBeliefTerm)) {
			//structurify: try to match the beliefTerm to a taskTerm component
			//  emulates (slowly) the termlink id codes from opennars
			var found = new Term[1];
			var bOp = nextBeliefTerm.opID();
			var bStruct = nextBeliefTerm.structure();
			var _nextBeliefTerm = nextBeliefTerm;
			//TODO shuffle recursion order
			taskTerm.recurseTerms(x -> x.hasAll(bStruct), s -> {
				var s1 = s;
                if (s1 instanceof Compound) {
					s1 = s1.unneg();
					if (s1.opID() == bOp) {
						if (_nextBeliefTerm.equalsRoot(s1)) {
							found[0] = s1;
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
			new AbstractPremise(task, belief, Why.whyLazy(why, p, belief)) :
			(!beliefTerm.equals(nextBeliefTerm) ? new AbstractPremise(task, nextBeliefTerm, why.why(p)) :
				null);

	}

	private static @Nullable Task match(Task task, Term beliefTerm, Derivation d) {

		var beliefTable = d.nar.tableDynamic(beliefTerm, true);

		return beliefTable != null && !beliefTable.isEmpty() ?
			match(task, beliefTerm, beliefTable, timeFocus(task, beliefTerm, d), d) : null;

	}



	public static @Nullable Task task(BeliefTable t, Term beliefTerm, long[] when, @Nullable Predicate<Task> beliefFilter, Derivation d) {
		return t
			.matching(when[0], when[1], beliefTerm, beliefFilter, d.dur, d.nar)
			.task(true, false, false);
	}


	private static @Nullable Task match(Task task, Term beliefTerm, BeliefTable bb, long[] when, Derivation d) {

		var tBelief = task.isBelief();

		var t = task(bb, beliefTerm, when, tBelief ? ((Predicate<Task>) task::equals).negate() : null, d);

		//HACK the filter helps but is not 100%
		return t != null && (tBelief && t.equals(task)) ? null : t;
	}

	private static long[] timeFocus(Task task, Term beliefTerm, Derivation d) {
		return d.deriver.timing.premise(d.x, task, beliefTerm);

//		if (NAL.premise.PREMISE_FOCUS_TIME_DITHER && l[0] != ETERNAL)
//			Tense.dither(l, d.ditherDT);
	}


}
