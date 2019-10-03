package nars.control;

import jcog.data.ShortBuffer;
import jcog.util.ArrayUtil;
import nars.$;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.atom.Int;
import org.eclipse.collections.api.block.procedure.primitive.ShortFloatProcedure;
import org.eclipse.collections.impl.set.mutable.primitive.ShortHashSet;
import org.roaringbitmap.RoaringBitmap;

import static nars.Op.CONJ;

public enum Why { ;

	public static Term why(short why) {
		return Int.the(why);
	}

	public static Term why(short[] why, int capacity) {
		assert(why.length > 0);
		if (why.length == 1)
			return why(why[0]);
		if (why.length > capacity) {
			why = sample(capacity, true, why);
		}
		return CONJ.the($.the(why));
	}

	public static Term why(Term whyA, short whyB, int capacity) {
		//TODO optimize
		return why(whyA, new short[] { whyB }, capacity);
	}

	public static Term why(Term whyA, short[] whyB, int capacity) {
		int wv = whyA.volume();
		if (wv + whyB.length + 1 > capacity) {

			//must reduce or sample
			int maxExistingSize = capacity - whyB.length - 1;
			if (maxExistingSize <= 0)
				return why(whyB, capacity); //can not save any existing

			ShortHashSet s = new ShortHashSet(wv);
			whyA.recurseTermsOrdered(x -> true, (e) -> {
				if (e instanceof Int)
					s.add(s(e));
				return true;
			}, null);
			if (s.size() > maxExistingSize-1) {
				//too many, must sample
				whyA = why(sample(capacity, true, s.toArray()), capacity);
			} else {
				//store linearized
				whyA = why(s.toArray(), capacity);
			}
		}
		return CONJ.the(whyA, why(whyB, capacity));
	}

	public static void eval(Term why, float pri, ShortFloatProcedure each) {
		if (why instanceof Int) {
			each.value(s(why), pri);
		} else {
			//split
			assert(why.opID()==CONJ.id);
			Subterms s = why.subterms();
			int n = s.subs();
			float priEach = pri/n;
			for (int i = 0; i < n; i++)
				eval(s.sub(i), priEach, each);
		}
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
