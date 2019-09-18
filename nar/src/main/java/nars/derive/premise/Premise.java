/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package nars.derive.premise;

import jcog.Util;
import nars.NAL;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.derive.Derivation;
import nars.link.TaskLink;
import nars.table.BeliefTable;
import nars.task.AbstractCommandTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.util.TermException;
import nars.time.Tense;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static nars.Op.VAR_QUERY;
import static nars.time.Tense.ETERNAL;

/**
 * Defines the conditions used in an instance of a derivation
 * Contains the information necessary for generating derivation Tasks via reasoning rules.
 * <p>
 * It is meant to be disposable and should not be kept referenced longer than necessary
 * to avoid GC loops, so it may need to be weakly referenced.
 * <p>
 * note: Comparable as implemented here is not 100% consistent with Task.equals and Term.equals.  it is
 * sloppily consistent for its purpose in collating Premises in optimal sorts during hypothesizing
 */
public class Premise  {
	/**
	 * variable types unifiable in premise formation
	 */
	public final static int var =
		//VAR_QUERY.bit
		Op.VAR_QUERY.bit | Op.VAR_DEP.bit
		//Op.Variable //all
		;

	public final Task task;
	public final Term beliefTerm;
//    public final long hash;

	/** pre */
	public Premise(TaskLink t) {
		this(t, t.term() /* HACK */ );
	}

	/** structural */
	public Premise(Task t) {
		this(t, t.term());
	}

	public Premise(Term taskTerm, Term beliefTerm) {
		this.task = new AbstractCommandTask(taskTerm);
		this.beliefTerm = beliefTerm;
	}

	public Premise(Task task, Term beliefTerm) {
		this.task = task;
		this.beliefTerm = beliefTerm;
//        this.hash = premiseHash(task, beliefTerm);
	}

	/** @return array of CAN-execute pathways */
	public short[] apply(Derivation d) {
		d.reset(this.task, belief(), beliefTerm);
		return d.deriver.what(d);
	}

	public Task belief() {
		return null;
	}

//    /**
//     * specially constructed hash that is useful for sorting premises by:
//     * a) task equivalency (hash)
//     * a) task target equivalency (hash)
//     * b) belief target equivalency (hash)
//     * <p>
//     * designed to maximize sequential repeat of derived task target
//     */
//    private static long premiseHash(Task task, Term beliefTerm) {
//        //task's lower 23 bits in bits 40..64
//        return (((long) task.hashCode()) << (64 - 24))
//                | //task target's lower 20 bits in bits 20..40
//                (((long) (task.term().hashCode() & 0b00000000000011111111111111111111)) << 20)
//                | //termlink's lower 20 bits in bits 0..20
//                ((beliefTerm.hashCode() & 0b00000000000011111111111111111111));
//    }

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
	public Premise match(Derivation d, int matchTTL) {
		Term nextBeliefTerm = this.beliefTerm;

		if (!nextBeliefTerm.op().taskable || /*beliefTerm.isNormalized() && */nextBeliefTerm.hasAny(VAR_QUERY))
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

		if (!d.budget(task, belief))
			return null;

//        System.out.println(task + "\t" + belief + "\t" + nextBeliefTerm);
		return belief != null ? new MatchedPremise(task, belief, nextBeliefTerm) :
			(this.beliefTerm.equals(nextBeliefTerm) ? this : new Premise(task, nextBeliefTerm));

	}


	private @Nullable Task match(Derivation d, Term beliefTerm, boolean beliefConceptUnifiesTaskConcept) {

		NAR n = d.nar();

		final BeliefTable beliefTable = n.tableDynamic(beliefTerm, true);

		boolean quest = task.isQuest();

		if (beliefConceptUnifiesTaskConcept && task.isQuestionOrQuest()) {

			BeliefTable answerTable = quest ? n.tableDynamic(beliefTerm, false) : beliefTable;
			if (answerTable != null && !answerTable.isEmpty()) {
				Task a = tryAnswer(beliefTerm, answerTable, d);
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
		float dur = d.dur();

		return bb.matching(when[0], when[1], beliefTerm, beliefFilter, dur, d.nar())
			.task(true, false, false);
	}


	private Task tryMatch(Term beliefTerm, BeliefTable bb, Derivation d) {

        Task t = task(bb, beliefTerm, timeFocus(beliefTerm, d), this::taskNotEquals, d);

        return t;
	}

	private boolean taskNotEquals(Task x) {
		return !x.equals(task);
	}

	@Nullable
	private Task tryAnswer(Term beliefTerm, BeliefTable answerTable, Derivation d) {

        Task a = task(answerTable, beliefTerm, timeFocus(beliefTerm, d), null, d);
		if (a != null) {
			//assert (task.isQuest() || match.punc() == BELIEF) : "quest answered with a belief but should be a goal";
			a = task.onAnswered(a);
			if (a!=null)
				answer(a, d);
		}

		return a;
	}

    private void answer(Task a, Derivation d) {
//        if (x.conf() > d.confMin) {
//            if (x.isGoal())
//                d.what.accept(x);
//            else
		Task q = this.task;
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

		long[] l = d.deriver.timing.premise(d.what, task, beliefTerm);

		if (NAL.premise.PREMISE_FOCUS_TIME_DITHER && l[0] != ETERNAL)
			Tense.dither(l, d.ditherDT);

		return l;
	}

//    private void linkVariable(boolean unifiedBelief, NAR n, Concept beliefConcept) {
//        if (unifiedBelief) {
//            Concept originalBeliefConcept = n.conceptualize(beliefTerm());
//            if (originalBeliefConcept != null) {
//                Concept taskConcept = n.concept(task.target(), true);
//
//
//                float pri = termLink.priElseZero() * n.activateLinkRate.floatValue();
//
//
//                Term moreConstantTerm = beliefConcept.target();
//                Term lessConstantTerm = originalBeliefConcept.target();
//
//
//                beliefConcept.termlinks().putAsync(new PLink<>(lessConstantTerm, pri / 2f));
//
//                originalBeliefConcept.termlinks().putAsync(new PLink<>(moreConstantTerm, pri / 2f));
//
//
//                if (taskConcept != null)
//                    taskConcept.termlinks().putAsync(new PLink<>(moreConstantTerm, pri));
//
//
//            }
//        }
//    }

////    private boolean validMatch(@Nullable Task x) {
////        return x != null && !x.isDeleted() && !x.equals(task);
////    }
//
//    private Predicate<Task> stampFilter(Derivation d) {
//        ImmutableLongSet taskStamp =
//                Stamp.toSet(task);
//        return t -> !Stamp.overlapsAny(taskStamp, t.stamp());
//    }


	@Override
	public boolean equals(Object obj) {
		return this == obj || (
			//hashCode() == obj.hashCode() &&
			((Premise) obj).task.equals(task) && ((Premise) obj).beliefTerm.equals(beliefTerm));
	}

	@Override
	public final int hashCode() {
		Task b = belief();
		return Util.hashCombine(task.hashCode(), b ==null ? beliefTerm.hashCode() : b.hashCode());
		//return (int) (hash >> 10) /* shift down about 10 bits to capture all 3 elements in the hash otherwise the task hash is mostly excluded */;
		//throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return "Premise(" + task + " * " + beliefTerm + ')';
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
