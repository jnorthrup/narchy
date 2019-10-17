/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package nars.derive.premise;

import jcog.Util;
import nars.Task;
import nars.derive.Derivation;
import nars.derive.rule.RuleCause;
import nars.term.Img;
import nars.term.Neg;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Bool;
import org.jetbrains.annotations.Nullable;

/**
 * immutable premise
 */
public class AbstractPremise implements Premise {

	public final Termed task, belief;

	private Termed why;

	public AbstractPremise(Task task, Termed why) {
		this(task, task.term(), why);
	}

	public AbstractPremise(Termed task, Termed belief, Termed why) {
		assert (valid(task) && task.term().op().taskable);
		assert (valid(belief));
		this.task = task;
		this.belief = belief;

//		boolean includeTaskAndBeliefAsWhy = true;
//		if (includeTaskAndBeliefAsWhy && task instanceof Task) {
//			Term taskWhy = ((Task) task).why();
//
//			this.why = Why.why(why,
//					belief instanceof Task ? Why.why(taskWhy, ((Task)belief).why(), Math.max(3, NAL.causeCapacity.intValue() - why.volume())) //double
//						:
//					taskWhy //single
//				);
//
//		} else {
		this.why = why;
//		}
	}

	public AbstractPremise(Termed task, Termed belief, RuleCause why, Derivation d) {
		this(task, belief, why.why(d));
	}

	private static boolean valid(Termed x) {
		return !(x instanceof Neg) && !(x instanceof Bool) && !(x instanceof Img);
	}

	@Override
	public final Term why() {
		Termed w = this.why; return w!=null ? w.term() : null;
	}


	@Override
	public final Term taskTerm() {
		return task instanceof Term ? (Term) task : task.term();
	}

	@Override
	public final Term beliefTerm() {
		return belief instanceof Term ? (Term) belief : belief.term();
	}

	@Override
	public final @Nullable Task task() {
		return task instanceof Task ? (Task) task : null;
	}

	@Override
	public final @Nullable Task belief() {
		return belief instanceof Task ? (Task) belief : null;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof AbstractPremise)) return false;
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
