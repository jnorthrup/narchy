package nars.control;

import jcog.util.ArrayUtil;
import nars.$;
import nars.NAL;
import nars.subterm.Subterms;
import nars.term.LazyTerm;
import nars.term.Term;
import nars.term.Termed;
import nars.term.Terms;
import nars.term.atom.Int;
import nars.term.util.TermException;
import org.eclipse.collections.api.block.procedure.primitive.ShortFloatProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;
import org.eclipse.collections.impl.set.mutable.primitive.ShortHashSet;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.IntConsumer;
import org.roaringbitmap.PeekableIntIterator;
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

//	public static Term why(Term whyA, short whyB, int capacity) {
//		//TODO optimize
//		return why(whyA, new short[] { whyB }, capacity);
//	}

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

			RoaringBitmap s = new RoaringBitmap();
			toSet(whyA, s::add);
			whyA = why(s, maxExistingSize-1);
			if (whyA.equals(whyB))
				return whyA;
		}

		return SETe.the(whyA, whyB);
	}

	@Nullable public static Term why(RoaringBitmap s, int capacity) {
		int ss = s.getCardinality();
		if (ss == 0)
			return null;
		short[] i = new short[ss];
		int in = 0;
		PeekableIntIterator ii = s.getIntIterator();
		while (ii.hasNext())
			i[in++] = (short) ii.next();

		return why(i, capacity);
	}

	@Nullable public static Term why(ShortHashSet s, int capacity) {
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

	@Nullable public static <C extends Caused> Termed whyLazy(@Nullable C... c) {
		return whyLazy(c, NAL.causeCapacity.intValue());
	}

	@Nullable public static <C extends Caused> Termed whyLazy(@Nullable C[] c, int capacity) {
		switch (c.length) {
			case 0: return null;
			case 1: return c[0].why();
			case 2: if (c[0]==c[1] || c[1] == null) return c[0].why();  if (c[0] == null) return c[1].why(); break;
		}
		return new LazyTerm(()-> why(c, capacity)); //TODO custom LazyTerm impl
	}

	@Deprecated public static <C extends Caused> Term why(@Nullable C... c) {
		return why(c, NAL.causeCapacity.intValue());
	}


	public static <C extends Caused> Term why(@Nullable C[] c, int capacity) {
		switch (c.length) {
			case 0: throw new UnsupportedOperationException();
			case 1: return c[0].why(); //TODO check capacity
			case 2: return why(c[0].why(), c[1].why(), capacity);
			default: {
				Term[] ct = new Term[c.length];
				int vt = 0;
				boolean nulls = false;
				for (int i = 0, cLength = c.length; i < cLength; i++) {
					C ci = c[i];
					if (ci!=null) {
						Term cti = ct[i] = ci.why();
						if (cti!=null)
							vt += cti.volume();
						else
							nulls = true;
					} else
						nulls = true;
				}
				if (vt == 0)
					return null;
				if (nulls)
					ct = ArrayUtil.removeNulls(ct);

				if (vt < capacity - 1) {
					ct = Terms.commute(ct);
					if (ct.length == 1)
						return ct[0];
					else
						return SETe.the(ct);
				} else {
					//flatten and sample
					//ShortHashSet s = new ShortHashSet(ct.length * capacity);
					RoaringBitmap s = new RoaringBitmap();
					IntConsumer addToS = s::add;
					for (Term cc : ct)
						toSet(cc, addToS);

					return why(s, capacity);
				}
			}
		}
	}

	@Deprecated public static Term why(@Nullable Term whyA, @Nullable Term whyB) {
		return why(whyA, whyB, NAL.causeCapacity.intValue());
	}

	public static Term why(@Nullable Term whyA, @Nullable Term whyB, int capacity) {
		if (capacity <= 0)
			return null;

		if (whyA == null)
			return whyB; //TODO check cap
		if (whyB == null)
			return whyA; //TODO check cap

		int wa = whyA.volume();
		if (whyA.equals(whyB) && wa <= capacity)
			return whyA; //same

		int wb = whyB.volume();
		if (wa + wb + 1 <= capacity) {
			return SETe.the(whyA, whyB);
		} else {
			//must reduce or sample
			RoaringBitmap s = new RoaringBitmap();
			IntConsumer addToS = s::add;
			toSet(whyA, addToS);
			toSet(whyB, addToS);
			return why(s, capacity);
		}
	}

	@FunctionalInterface interface Evaluator<X> {
		void value(X[] x, short cause, float pri);
	}
	public static <X> void eval(@Nullable Term why, float pri, X[] causes, Evaluator<X> each) {
		if (why == null)
			return;

		if (why instanceof Int) {
			each.value(causes, s(why), pri);
		} else {
			//split
			Subterms s = why.subterms();
			int n = s.subs();

			assert (why.opID() == SETe.id  && n > 1);

			float priEach = pri/n;
			for (int i = 0; i < n; i++)
				eval(s.sub(i), priEach, causes, each);
		}
	}

	@Deprecated public static void eval(@Nullable Term why, float pri, ShortFloatProcedure each) {
		if (why == null)
			return;

		if (why instanceof Int) {
			each.value(s(why), pri);
		} else {
			//split
			assert(why.opID()==SETe.id);
			Subterms s = why.subterms();
			int n = s.subs();
			if (n <= 1)
				throw new TermException("Malformed Why", why);
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
			ShortHashSet seen = Why.toSet(why); //TODO RoaringBitmap
			seen.forEach(s);
		}
	}

	@Deprecated public static ShortHashSet toSet(Term why) {
		ShortHashSet s = new ShortHashSet(why.volume() /* estimate */);
		toSet(why, s);
		return s;
	}

	@Deprecated private static void toSet(Term whyA, ShortHashSet s) {
		if (whyA==null)
			return;
		whyA.recurseTermsOrdered(x -> true, (e) -> {
			if (e instanceof Int)
				s.add(s(e));
			return true;
		}, null);
	}
	private static void toSet(Term w, IntConsumer each) {
		if (w instanceof Int) {
			each.accept(s(w));
		} else {
			Subterms ww = w.subterms();
			int wn = ww.subs();
			for (int i = 0; i < wn; i++)
				toSet(ww.sub(i), each);
		}
	}

	private static short s(Term why) {
		return (short)(((Int)why).i);
	}

//	static short[] sample(int maxLen, boolean deduplicate, short[]... s) {
//		int ss = s.length;
//		int totalItems = 0;
//		short[] lastNonEmpty = null;
//		int nonEmpties = 0;
//		for (short[] t : s) {
//			int tl = t.length;
//			totalItems += tl;
//			if (tl > 0) {
//				lastNonEmpty = t;
//				nonEmpties++;
//			}
//		}
//		if (nonEmpties == 1)
//			return lastNonEmpty;
//		if (totalItems == 0)
//			return ArrayUtil.EMPTY_SHORT_ARRAY;
//
//
//		ShortBuffer ll = new ShortBuffer(Math.min(maxLen, totalItems));
//		RoaringBitmap r = deduplicate ? new RoaringBitmap() : null;
//		int ls = 0;
//		int n = 0;
//		int done;
//		main:
//		do {
//			done = 0;
//			for (short[] c : s) {
//				int cl = c.length;
//				if (n < cl) {
//					short next = c[cl - 1 - n];
//					if (deduplicate)
//						if (!r.checkedAdd(next))
//							continue;
//
//					ll.add/*adder.accept*/(next);
//					if (++ls >= maxLen)
//						break main;
//
//				} else {
//					done++;
//				}
//			}
//			n++;
//		} while (done < ss);
//
//		//assert (ls > 0);
//		short[] lll = ll.toArray();
//		//assert (lll.length == ls);
//		return lll;
//	}
}
