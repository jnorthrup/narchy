package nars.op.stm;

import jcog.Util;
import jcog.data.list.MetalConcurrentQueue;
import jcog.math.FloatRange;
import jcog.pri.ScalarValue;
import nars.NAL;
import nars.Task;
import nars.attention.TaskLinkWhat;
import nars.derive.Derivation;
import nars.derive.action.TaskAction;
import nars.derive.rule.RuleCause;
import nars.link.AbstractTaskLink;
import nars.link.AtomicTaskLink;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.Nullable;

import java.nio.BufferOverflowException;

public class STMLinker extends TaskAction {

	public final FloatRange strength = new FloatRange(1f, 0f, 1f);



	private final MetalConcurrentQueue<Task> stm;
	private final int capacity;


	public STMLinker(int capacity) {
		super();
		single();
		taskPunc(true,true,false,false);//all but questions and commands

		this.capacity = capacity;
		this.stm = new MetalConcurrentQueue<>(capacity);
	}

	private static boolean link(Task next, @Nullable Task prev, float factor, RuleCause why, Derivation d) {
		if (prev == null)
			return true;

		final Term att = next.term().concept(), btt = prev.term().concept();
		if (att.equals(btt))
			return false;

		/* pri split bidirectionally so * 0.5 */
		float pri = 0.5f * factor * pri(next, prev);

		if (pri >= ScalarValue.EPSILON) {
			long dt = next.minTimeTo(prev);
			if (dt > 0) {
				pri = (float) NAL.evi(pri, dt, d.dur); //fade by time distance
				if (pri < ScalarValue.EPSILON)
					return true;
			}

			TaskLinkWhat w = (TaskLinkWhat) d.x;

			Termed WHY = why.why(prev, next /*, d*/ );

			link(att, btt, next.punc(), pri, WHY, w);
			link(btt, att, prev.punc(), pri, WHY, w);
		}
		return true;
	}

	private static float pri(Task next, Task prev) {
		return
			Util.and(next.priElseZero(), prev.priElseZero());
			//Math.min(next.priElseZero(), prev.priElseZero());
			//Util.mean(next.priElseZero(), prev.priElseZero());
			//Util.or(next.priElseZero(), prev.priElseZero());
	}

	static void link(Term a, Term b, byte punc, float pri, Termed why, TaskLinkWhat w) {
		AbstractTaskLink l = AtomicTaskLink.link(a, b).priSet(punc, pri);
		l.why = why;
		w.links.link(l);
	}

	public boolean keep(Task x) {
		return x.isInput();
	}

	public boolean filter(Task x) {
		return x.isInput();
	}

	@Override
	protected void accept(Task x, RuleCause why, Derivation d) {
		if (!filter(x))
			return;

//            if (y.isEternal()) {
//                if (eternalize) {
//                    //project to present moment
//                    long now = nar.time();
//                    int dur = nar.dur();
//                    y = new SpecialOccurrenceTask(y, now - dur / 2, now + dur / 2);
//                } else {
//                    return;
//                }
//
//            }
		float factor = this.strength.floatValue();

		boolean novel;
		if (capacity == 1) {
			//optimized 1-ary case
			novel = link(x, stm.peek(), factor, why, d);
		} else {
			//TODO test
			//int i = 0;
			novel = true;
			int h = stm.head();
			for (int i = 0; i < capacity; i++) {
				Task z = stm.peek(h, i);
				//for (Task z : stm) {
				novel &= link(x, z, factor, why, d);
				//if (++i == capacity) break;
				//}
			}
		}

		if (novel && keep(x)) {
			stm.poll();
			if (!stm.offer(x)) {
				if (NAL.DEBUG)
					throw new BufferOverflowException();
			}
		}

	}


//
//		Task p = this.prev;
//		if (p == null) {
//			this.prev = n;
//		} else {
//			//if (prev.start() < n.start()) {
//				Term pt = p.term().concept();
//				Term nt = n.term().concept();
//				if (!pt.equals(nt)) {
//
//					float pri =
//						(float) NAL.evi(
//							Util.mean(n.priElseZero(), p.priElseZero()),
//							prev.minTimeTo(n.start(), n.end()),
//							d.dur);
//
//					if (pri > ScalarValue.EPSILON) {
//						d.what.link(AtomicTaskLink.link(pt, nt).priSet(p.punc(), pri));
//						d.what.link(AtomicTaskLink.link(nt, pt).priSet(n.punc(), pri));
//					}
//					this.prev = n;
//				}
//			//}
//		}
//	}

}
