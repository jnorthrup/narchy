package nars.control;

import jcog.data.ShortBuffer;
import jcog.util.ArrayUtil;
import nars.$;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.atom.Int;
import org.eclipse.collections.api.block.procedure.primitive.ShortFloatProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;
import org.eclipse.collections.impl.set.mutable.primitive.ShortHashSet;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.RoaringBitmap;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import static nars.Op.SETe;

public enum Why { ;

	public static Term why(short why) {
		return Int.the(why);
	}

	public static Term why(short[] why, int capacity) {
		if (why.length == 0)
			return null; //TODO prevent from having to reach here
		if (why.length == 1)
			return why(why[0]);

		int excess = why.length - (capacity-1);
		if (excess > 0) {
			//TODO if (excess == 1) simple case

			//why = sample(capacity-1, true, why);
			why = why.clone();
			ArrayUtil.shuffle(why, ThreadLocalRandom.current());
			why = Arrays.copyOf(why, capacity-1);
		}
		return SETe.the($.the(why));
	}

	public static Term why(Term whyA, short whyB, int capacity) {
		//TODO optimize
		return why(whyA, new short[] { whyB }, capacity);
	}

	public static Term why(Term whyA, short[] _whyB, int capacity) {
		if (whyA == null)
			return why(_whyB, capacity);

		int wv = whyA.volume();

		Term whyB = why(_whyB, capacity);
		if (whyA.equals(whyB))
			return whyA;

		if (wv + _whyB.length + 1 > capacity) {

			//must reduce or sample
			int maxExistingSize = capacity - _whyB.length - 1;
			if (maxExistingSize <= 0)
				return whyB; //can not save any existing

			ShortHashSet s = new ShortHashSet(wv);
			toSet(whyA, s);
			whyA = why(s, maxExistingSize-1);
			if (whyA.equals(whyB))
				return whyA;
		}

		return SETe.the(whyA, whyB);
	}

	public static Term why(ShortHashSet s, int capacity) {
		if (s.isEmpty())
			return null; //TODO prevent from having to reach
		return why(s.toArray(), capacity);
//		if (s.size() > capacity-1) {
//			//too many, must sample
//			return why(sample(capacity-1, true, s), capacity);
//		} else {
//			//store linearized
//			return why(ss, capacity-1);
//		}
	}

	public static <C extends Caused> Term why(@Nullable C[] c, int capacity) {
		switch (c.length) {
			case 0: throw new UnsupportedOperationException();
			case 1: return c[0].why(); //TODO check capacity
			case 2: return why(c[0].why(), c[1].why(), capacity);
			default: {
				ShortHashSet s = new ShortHashSet(c.length * capacity);
				for (C cc : c) {
					if (cc!=null)
						toSet(cc.why(), s);
				}
				return why(s, capacity);
			}
		}
	}

	public static Term why(@Nullable Term whyA, @Nullable Term whyB, int capacity) {
		if (whyA == null)
			return whyB; //TODO check cap
		if (whyB == null)
			return whyA; //TODO check cap

		int wa = whyA.volume();
		if (whyA.equals(whyB) && wa <= capacity)
			return whyA; //same

		int wb = whyB.volume();
		if (wa + wb + 1 > capacity) {
			//must reduce or sample
			ShortHashSet s = new ShortHashSet(wa+wb);
			toSet(whyA, s);
			toSet(whyB, s);
			return why(s, capacity);
		} else
			return SETe.the(whyA, whyB);
	}

	public static void eval(@Nullable Term why, float pri, ShortFloatProcedure each) {
		if (why == null)
			return;

		if (why instanceof Int) {
			each.value(s(why), pri);
		} else {
			//split
			assert(why.opID()==SETe.id);
			Subterms s = why.subterms();
			int n = s.subs();
			float priEach = pri/n;
			for (int i = 0; i < n; i++)
				eval(s.sub(i), priEach, each);
		}
	}

	public static void forEachUnique(Term why, ShortProcedure s) {
		if (why instanceof Int) {
			//optimized case
			s.value(s(why));
		} else {
			//TODO optimized case of simple set with no recursive inner-sets
			ShortHashSet seen = Why.toSet(why);
			seen.forEach(s);
		}
	}

	public static ShortHashSet toSet(Term why) {
		ShortHashSet s = new ShortHashSet(why.volume() /* estimate */);
		toSet(why, s);
		return s;
	}

	private static void toSet(Term whyA, ShortHashSet s) {
		if (whyA==null)
			return;
		whyA.recurseTermsOrdered(x -> true, (e) -> {
			if (e instanceof Int)
				s.add(s(e));
			return true;
		}, null);
	}

	private static short s(Term why) {
		return (short)(((Int)why).i);
	}

	static short[] sample(int maxLen, boolean deduplicate, short[]... s) {
		int ss = s.length;
		int totalItems = 0;
		short[] lastNonEmpty = null;
		int nonEmpties = 0;
		for (short[] t : s) {
			int tl = t.length;
			totalItems += tl;
			if (tl > 0) {
				lastNonEmpty = t;
				nonEmpties++;
			}
		}
		if (nonEmpties == 1)
			return lastNonEmpty;
		if (totalItems == 0)
			return ArrayUtil.EMPTY_SHORT_ARRAY;


		ShortBuffer ll = new ShortBuffer(Math.min(maxLen, totalItems));
		RoaringBitmap r = deduplicate ? new RoaringBitmap() : null;
		int ls = 0;
		int n = 0;
		int done;
		main:
		do {
			done = 0;
			for (short[] c : s) {
				int cl = c.length;
				if (n < cl) {
					short next = c[cl - 1 - n];
					if (deduplicate)
						if (!r.checkedAdd(next))
							continue;

					ll.add/*adder.accept*/(next);
					if (++ls >= maxLen)
						break main;

				} else {
					done++;
				}
			}
			n++;
		} while (done < ss);

		//assert (ls > 0);
		short[] lll = ll.toArray();
		//assert (lll.length == ls);
		return lll;
	}
}
