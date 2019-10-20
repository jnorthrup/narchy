package nars.link;

import jcog.TODO;
import jcog.Util;
import jcog.pri.op.PriMerge;
import jcog.pri.op.PriReturn;
import jcog.util.FloatFloatToFloatFunction;
import nars.Op;
import nars.Task;
import nars.control.Why;
import nars.task.util.TaskException;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.IdempotentBool;
import nars.term.util.TermException;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static jcog.Util.assertFinite;
import static jcog.pri.op.PriReturn.Changed;

public abstract class AbstractTaskLink implements TaskLink {

	//private static final AtomicFloatFieldUpdater<AbstractTaskLink> PRI = new AtomicFloatFieldUpdater<AbstractTaskLink>(AbstractTaskLink.class, "pri");

	static final FloatFloatToFloatFunction plus = PriMerge.plus::merge;
	static final FloatFloatToFloatFunction mult = PriMerge.and::merge;
	/**
	 * source,target as a 2-ary subterm
	 */
	public final Term from;
	public final Term to;
	public final int hash;
	public Termed why = null;

	/**
	 * cached; NaN means invalidated
	 */
	private volatile float pri = (float) 0;

	protected AbstractTaskLink(Term source, Term target, int hash) {
		this.from = source;
		this.to = target;
		this.hash = hash;
	}

	protected AbstractTaskLink(Term source, Term target) {
		this(source, Util.maybeEqual(source, target),
			//TODO construct hash as 16bit+16bit so that the short hash can be compared from external iterations
			hash(source, target)
		);

		if (source instanceof IdempotentBool)
			throw new TermException("source bool", source);
		if (target instanceof IdempotentBool)
			throw new TermException("target bool", target);

        Op so = source.op();
		if (!so.taskable)
			throw new TaskException("source term not taskable", source);
		if (!so.conceptualizable)
			throw new TaskException("source term not conceptualizable", source);
//        if (NAL.DEBUG) {
//            if (!source.isNormalized())
//                throw new TaskException(source, "source term not normalized and can not name a task");
//        }
	}

	private static int hash(Term source, Term target) {
        int s = source.hashCodeShort();
        int t = source != target ? target.hashCodeShort() : s;
		return (t << 16) | s;
//        if (t!=(hash >>> 16))
//            throw new WTF();
//        if (s!=(hash & 0xffff))
//            throw new WTF();
	}

	@Override
	public Term why() {
		return Optional.ofNullable(why).map(Termed::term).orElse(null);
	}

	@Override
	public @Nullable Term other(Term x, int xHashShort, boolean reverse) {
		@Nullable Term result = null;
		if (!isSelf() && xHashShort == (reverse ? toHash() : fromHash()) && x.equals(reverse ? to : from))
			result = reverse ? from : to;

		return result;
	}


	public int toHash() {
		return (hash >>> 16);
	}

	public int fromHash() {
		return (hash & 0xffff);
	}

	@Override
	public boolean isSelf() {
		return from == to;
	}

	@Override
	public TaskLink id() {
		return this;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj instanceof TaskLink && hash == obj.hashCode()) {
            TaskLink t = (TaskLink) obj;
			return from.equals(t.from()) && to.equals(t.to());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return hash;
	}

	@Override
	public Term from() {
		return from;
	}

	@Override
	public Term to() {
		return to;
	}

	@Override
	public float pri() {
        float p = this.pri;
		//update cached value
		return p != p ? (this.pri = priSum() / 4.0F) : p;
	}

	protected void invalidate() {
		this.pri = Float.NaN;
	}


//    @Override
//    public float take(byte punc, float howMuch) {
//        return Math.max(ScalarValue.EPSILON,
//                -priMergeGetDelta(punc, -howMuch, PriMerge.plus)
//        );
//    }

	@Override
	public void delete(byte punc) {
		priSet(punc, (float) 0);
	}

	@Override
	public boolean delete() {
		fill((float) 0);
		return true;
	}

	@Override
	public boolean isDeleted() {
		return false;
	}

	protected abstract float priSum();

	/**
	 * merge a component; used internally.  does not invalidate so use the high-level methods like merge()
	 */
	protected abstract float apply(int ith, float pri, FloatFloatToFloatFunction componentMerge, PriReturn returning);

	protected abstract void fill(float pri);


	@Override
	public abstract String toString();


	public AbstractTaskLink priSet(byte punc, float puncPri) {
		mergeComponent(punc, puncPri, PriMerge.replace, null);
		return this;
	}

	@Override
	public float merge(TaskLink incoming, PriMerge merge, PriReturn returning) {

		why = Why.why(why(), incoming.why()); //TODO priority proportional Why merge

		FloatFloatToFloatFunction m = merge::mergeUnitize;
		float o;
		switch (returning) {
			case Overflow:
			case Delta:
				o = (float) 0;
				for (byte i = (byte) 0; (int) i < 4; i++)
					o += mergeDirect((int) i, incoming.priIndex(i), m, returning);
				o/= 4.0F;
				break;
			case Void:
				for (byte i = (byte) 0; (int) i < 4; i++)
					mergeDirect((int) i, incoming.priIndex(i), m, null);
				o = Float.NaN;
				break;
            default:
                throw new UnsupportedOperationException();
		}

		invalidate();

		return o;
	}

//    protected void mergeComponent(byte punc, float pri, PriMerge merge) {
//        mergeComponent(punc, pri, merge, PriReturn.Void);
//    }

	public float mergeComponent(byte punc, float pri, PriMerge merge, @Nullable PriReturn returning) {
		return merge((int) Task.i(punc), pri, merge, returning);
	}

	/** does not invalidate */
	protected float mergeDirect(int ith, float pri, FloatFloatToFloatFunction merge, @Nullable PriReturn returning) {
		//assertFinite(pri);
		return apply(ith, pri, merge, returning);
	}

	protected float merge(int ith, float pri, PriMerge merge, @Nullable PriReturn returning) {

        float y = mergeDirect(ith, pri, merge::mergeUnitize, returning);

		if (returning != PriReturn.Delta || y != (float) 0) //delta==0 on individual component = unchanged
			invalidate();

		return y/ 4.0F;
	}


	@Override
	public AbstractTaskLink pri(float p) {
//        //TODO fully atomic
//        float e = pri();
//        priMult(  p/ e); } else { fill(p);  }
//        return this;
		throw new UnsupportedOperationException();
	}

	@Override
	public void priAdd(float a) {
		throw new TODO();
	}


	@Override
	public float priMult(float X) {
		assertFinite(X);
		if (!Util.equals(X, 1.0F)) {


            boolean acc = false;
			for (int i = 0; i < 4; i++) {
                boolean aBoolean = apply(i, X, mult, Changed) != (float) 0;
				acc = acc || aBoolean;
			}
            boolean changed = acc;
			//HACK not fully atomic but at least consistent

            if (changed)
				invalidate();
		}
		return pri();
	}

//	@Override
//	public void priMult(float belief, float goal, float question, float quest) {
//		boolean changed;
//		changed = apply(0, belief, mult, Changed) != 0;
//		changed |= apply(1, goal, mult, Changed) != 0;
//		changed |= apply(2, question, mult, Changed) != 0;
//		changed |= apply(3, quest, mult, Changed) != 0;
//		if (changed)
//			invalidate();
//	}


	public @Nullable Term matchReverse(Term from, int fromHash, Term to, int toHash) {
        int f = fromHash();
		return f != fromHash &&
			f != toHash &&
			toHash() == toHash &&
			to.equals(this.to) &&
			!from.equals(this.from) ? this.from : null;
	}

//    @Override
//    public float pri(FloatFloatToFloatFunction update, float scalar) {
//        throw new UnsupportedOperationException();
//
//        //TODO make fully atomic:
////            float prev = this.pri();
////            float next = update.apply(prev, x);
////            if (next == next) {
////                next = Util.unitizeSafe(next);
////                if (prev != prev) {
////                    punc.fill(next); //flat
////                } else {
////                    if (!Util.equals(next, prev)) {
////                        //renormalize
////                        renormalize(next);
////                    }
////                }
////            } else {
////                punc.fill(0);
////                next = 0;
////            }
////            invalidate();
////            return next;
//
////            }, _x);
//
////            return y;
//    }

//    /** holds value for one punctuation only */
//    public static class SimpleTaskLink extends AbstractTaskLink {
//
//        @Override
//        protected float priSum() {
//            return 0;
//        }
//
//        @Override
//        protected float merge(int ith, float pri, FloatFloatToFloatFunction componentMerge, PriReturn returning) {
//            return 0;
//        }
//
//        @Override
//        protected void fill(float pri) {
//
//        }
//
//        @Override
//        public String toString() {
//            return null;
//        }
//
//        @Override
//        public float priIndex(byte index) {
//            return 0;
//        }
//    }

}
