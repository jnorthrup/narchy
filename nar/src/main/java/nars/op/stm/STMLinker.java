package nars.op.stm;

import jcog.Util;
import jcog.data.list.MetalConcurrentQueue;
import jcog.math.FloatRange;
import jcog.pri.ScalarValue;
import nars.$;
import nars.NAL;
import nars.Task;
import nars.attention.TaskLinkWhat;
import nars.derive.Derivation;
import nars.derive.action.NativePremiseAction;
import nars.link.AtomicTaskLink;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.nio.BufferOverflowException;

public class STMLinker extends NativePremiseAction {

	public final FloatRange strength = new FloatRange(1f, 0f, 1f);

	@Override
	protected float pri(Derivation d) {
		return 1.0f;
	}

//    private final Cause cause;

	private final MetalConcurrentQueue<Task> stm;
	private final int capacity;

	{
		taskPattern($.varPattern(1));
		beliefPattern($.varPattern(1));
		taskPunc(true, true, true, true);
	}

	public STMLinker(int capacity) {
		super();

		this.capacity = capacity;
		stm = new MetalConcurrentQueue<>(capacity);
			//new MpmcArrayQueue<>(Math.max(2, capacity));
//		for (int i = 0; i < capacity; i++)
//			stm.offer(null);

	}

	private static boolean link(Task next, @Nullable Task prev, float factor, Derivation d) {
		if (prev == null)
			return true;
		if (next.equals(prev))
			return false;

		/* pri split bidirectionally so * 0.5 */
		float pri = 0.5f * factor * pri(next, prev);

		if (pri >= ScalarValue.EPSILON) {
			long dt = next.minTimeTo(prev);
			pri = (float) NAL.evi(pri, dt, d.dur);

			if (pri >= ScalarValue.EPSILON) {
				TaskLinkWhat w = (TaskLinkWhat) d.what;
				Term att = next.term().concept(), btt = prev.term().concept();
				if (!att.equals(btt)) {
					link(att, btt, next.punc(), pri, w);
					link(btt, att, prev.punc(), pri, w);
				}
			}
		}
		return true;
	}

	private static float pri(Task next, Task prev) {
		return Util.mean(next.priElseZero(), prev.priElseZero());
			//Util.or(next.priElseZero(), prev.priElseZero());
	}

	static void link(Term a, Term b, byte punc, float pri, TaskLinkWhat w) {
		w.links.link(AtomicTaskLink.link(a, b).priSet(punc, pri));
	}

	public boolean keep(Task x) {
		return x.isInput();
	}

	public boolean filter(Task x) {
		return x.isInput();
	}

	@Override
	protected void run(Derivation d) {

		if (filter(d._task))
			accept(d._task, d);
//		else if (d._belief != null && filter(d._belief)))
//			accept(d._belief, d)

	}

	private void accept(Task y, Derivation d) {


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
			novel = link(y, stm.peek(), factor, d);
		} else {
			//TODO test
			//int i = 0;
			novel = true;
			int h = stm.head();
			for (int i = 0; i < capacity; i++) {
				Task z = stm.peek(h, i);
				//for (Task z : stm) {
				novel &= link(y, z, factor, d);
				//if (++i == capacity) break;
				//}
			}
		}

		if (novel && keep(y)) {
			stm.poll();
			if (!stm.offer(y)) {
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
