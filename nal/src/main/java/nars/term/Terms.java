package nars.term;

import jcog.bloom.StableBloomFilter;
import jcog.data.bit.MetalBitSet;
import jcog.data.set.MetalTreeSet;
import jcog.sort.QuickSort;
import jcog.util.ArrayUtil;
import nars.Op;
import nars.Task;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.task.util.TaskHasher;
import nars.term.atom.Atom;
import nars.term.compound.PatternCompound;
import nars.term.util.TermHasher;
import nars.term.util.conj.Conj;
import nars.term.var.ellipsis.Ellipsislike;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.iterator.MutableIntIterator;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;

import static nars.Op.CONJ;
import static nars.Op.NEG;
import static nars.term.atom.Bool.Null;

/**
 * Static utility class for static methods related to Terms
 * <p>
 * Also serves as stateless/memory-less transient static (on-heap) TermIndex
 *
 * @author me
 */
public enum Terms {
	;



	/**
	 * sort and deduplicates the elements; returns new array if modifications to order or deduplication are necessary
	 */
	public static Term[] commute(Term... x) {
		int len = x.length;
		switch (len) {
			case 0:
				return Op.EmptyTermArray;
			case 1:
				return x;
			case 2:
				return commute2(x);
			case 3:
				return commute3(x);
			default:
				return commuteN(x);
		}


	}

	/**
	 * doesnt deduplicate
	 */
	public static Term[] sort(Term[] x) {
		switch (x.length) {
			case 0:
				return Op.EmptyTermArray;
			case 1:
				return x;
			case 2:
				return sort2(x);
			//TODO optimized sort3
			default:
				return ifDifferent(x, new TermList(x.clone()).sort());
		}
	}

	private static Term[] commuteN(Term[] x) {
		return ifDifferent(x,
			commuteTerms(x, false)
			//commuteTermsTermList(x)
			//new MetalTreeSet<>(x).toArray(Op.EmptyTermArray)
			//new SortedList<>(x, new Term[x.length]).toArrayRecycled(Term[]::new) //slow
		);
	}

	static Term[] commuteTermsTermList(Term[] x, boolean modifyInputArray) {
		return new TermList(modifyInputArray ? x : x.clone()).sortAndDedup();
	}

	/** n-array commute function optimized to avoid unnecessary comparisons by grouping by term properties like volume first.
	 * TODO use op if that is the 2nd comparison property
	 * */
	static Term[] commuteTerms(Term[] x, boolean modifyInputArray) {
		short[] volumes = new short[x.length];
		int volMin = Integer.MAX_VALUE, volMax = Integer.MIN_VALUE;
		boolean allDecreasing = true;
		for (int i = 0, xLength = x.length; i < xLength; i++) {

			short v = (short) x[i].volume();

			volumes[i] = v;

			volMin = Math.min(volMin, v); volMax = Math.max(volMax, v);

			if (allDecreasing && i > 1 && volumes[i-1] <= v)
				allDecreasing = false;
		}
		if (allDecreasing) {
			//already in sorted order guaranteed by the volume being strictly decreasing
			return x;
		}

		Term[] y = modifyInputArray ? x : x.clone();

		int nulls;
		if (volMax <= volMin) {
			//flat
			Arrays.sort(y);
			//TODO maybe use ArrayUtil.quickSort and nullify as soon as a .compare returns 0.  avoids the subsequent .equals tests
			nulls = nullDups(y, 0, y.length);
		} else {
			//sort within the spans where the terms have equal volumes (divide & conquer)

			int n = y.length;
			QuickSort.quickSort(0, n,
				(a, b) -> Integer.compare(volumes[b], volumes[a]),
				(a, b) -> ArrayUtil.swapObjShort(y, volumes, a, b));

			int vs = volumes[0];
			nulls = 0;
			//span start
			int s = 0;
			for (int i = 1; i <= n; i++) {
				int vi = i < n ? volumes[i] : -1;
				if (vi != vs) {
					if (i - s > 1) {
						//sort span
						//TODO optimized 2-array compare swap
						Arrays.sort(y, s, i);
						nulls += nullDups(y, s, i);
					}
					//next span
					s = i;
					vs = vi;
				}
			}

		}

		return nulls == 0 ? y : ArrayUtil.removeNulls(y, nulls);
	}

	private static int nullDups(Term[] y, int start, int end) {
		Term prev = y[start];
		int nulls = 0;
		for (int i = start+1; i < end; i++) {
			Term next = y[i];
			if (prev.equals(next)) {
				y[i] = null;
				nulls++;
			} else {
				prev = next;
			}
		}
		return nulls;
	}

	private static Term[] ifDifferent(Term[] a, Term[] b) {
		return ArrayUtil.equalsIdentity(a, b) ? a : b;
	}

	private static Term[] commute3(Term[] t) {
    /*
    //https://stackoverflow.com/a/16612345
    if (el1 > el2) Swap(el1, el2)
    if (el2 > el3) {
        Swap(el2, el3)
        if (el1 > el2) Swap(el1, el2)
    }*/

		Term a = t[0], b = t[1], c = t[2];
		int ab = a.compareTo(b);
		if (ab == 0) {
			return commute(a, c); //a=b, so just combine a and c (recurse)
		} else if (ab > 0) {
			Term x = a;
			a = b;
			b = x;
		}
		int bc = b.compareTo(c);
		if (bc == 0) {
			return new Term[]{a, b}; //b=c so just combine a and b
		} else if (bc > 0) {
			Term x = b;
			b = c;
			c = x;
			int ab2 = a.compareTo(b);
			if (ab2 == 0) {
				return new Term[]{a, c};
			} else if (ab2 > 0) {
				Term y = a;
				a = b;
				b = y;
			}
		}
		//already sorted
		return t[0] == a && t[1] == b ? t : new Term[]{a, b, c};
	}


	private static Term[] commute2(Term[] t) {
		Term a = t[0], b = t[1];
		int ab = a.compareTo(b);
		if (ab < 0) return t;
		else if (ab > 0) return new Term[]{b, a};
		else /*if (c == 0)*/ return new Term[]{a};
	}

	private static Term[] sort2(Term[] t) {
		Term a = t[0], b = t[1];
		int ab = a.compareTo(b);
		if (ab < 0) return t;
		else if (ab > 0) return new Term[]{b, a};
		else /*if (c == 0)*/ return new Term[]{a, a};
	}

	public static void printRecursive(PrintStream out, Term x) {
		printRecursive(out, x, 0);
	}

	static void printRecursive(PrintStream out, Term x, int level) {

		for (int i = 0; i < level; i++)
			out.print("  ");

		out.print(x);
		out.print(" (");
		out.print(x.op() + "[" + x.getClass().getSimpleName() + "] ");
		out.print("c" + x.complexity() + ",v" + x.volume() + ",dt=" + x.dt() + ",dtRange=" + x.eventRange() + ' ');
		out.print(Integer.toBinaryString(x.structure()) + ')');
		out.println();


		for (Term z : x.subterms()) {
			printRecursive(out, z, level + 1);
		}


	}

	/**
	 * for printing complex terms as a recursive tree
	 */
	public static void printRecursive(Term x, Consumer<String> c) {
		printRecursive(x, 0, c);
	}

	public static void printRecursive(Term x, int level, Consumer<String> c) {


        for (Term z : x.subterms())
			printRecursive(z, level + 1, c);


        String line = "  ".repeat(Math.max(0, level)) +
                x;
        c.accept(line);
	}


	public static @Nullable Term[] concat(Term[] a, Term... b) {

		if (a.length == 0) return b;
		if (b.length == 0) return a;

		int L = a.length + b.length;

		Term[] arr = new Term[L];

		int l = a.length;
		System.arraycopy(a, 0, arr, 0, l);
		System.arraycopy(b, 0, arr, l, b.length);

		return arr;
	}


	public static @Nullable Atom atomOr(@Nullable Term possiblyCompound, Atom other) {
		return (possiblyCompound instanceof Atom) ? (Atom) possiblyCompound : other;
	}

	public static @Nullable Atom atomOrNull(@Nullable Term t) {
		return atomOr(t, null);
	}

	/**
	 * dangerous because some operations involving concepts can naturally reduce to atoms, and using this interprets them as non-existent
	 */
	@Deprecated
	public static @Nullable Compound compoundOrNull(@Nullable Term t) {
		return t instanceof Compound ? (Compound) t : null;
	}


	public static boolean allNegated(Subterms subterms) {
		return subterms.hasAny(NEG) && subterms.AND(t -> t instanceof Neg);
	}

//	public static int countNegated(Subterms subterms) {
//		return subterms.hasAny(Op.NEG) ? subterms.count(NEG) : 0;
//	}
//
//	public static int negatedCount(Subterms subterms) {
//		return subterms.hasAny(Op.NEG) ? subterms.count(x -> x instanceof Neg) : 0;
//	}

	/**
	 * returns the most optimal subterm that can be replaced with a variable, or null if one does not meet the criteria
	 * when there is a chocie, it prefers least aggressive introduction. and then random choice if
	 * multiple equals are introducible
	 *
	 * @param superterm filter applies to the immediate superterm of a potential subterm
	 */
	public static @Nullable Term[] nextRepeat(Subterms c, int minCount, ToIntFunction<Term> countIf) {
		ObjectIntHashMap<Term> oi = Terms.subtermScore(c, minCount, countIf);
		if (oi == null)
			return null;

		LazyIterable<Term> ok = oi.keysView();
		switch (oi.size()) {
			case 0:
				return null;
			case 1:
				return new Term[]{ok.getFirst()};
		}


		return ok.toArray(Op.EmptyTermArray);
	}


	/**
	 * counts the repetition occurrence count of each subterm within a compound
	 */
	public static @Nullable ObjectIntHashMap<Term> subtermScore(Subterms c, int minTotalScore, ToIntFunction<Term> score) {
		ObjectIntHashMap<Term> uniques = new ObjectIntHashMap(8); //c.volume());


		c.recurseTermsOrdered(z -> true, subterm -> {


			//c.forEach(cc ->
			//cc.recurseTermsOrdered(z -> true, (subterm) -> {
			int s = score.applyAsInt(subterm);
			if (s > 0)
				uniques.addToValue(subterm, s);
			return true;
			//}, null);
			//});
		}, null);

		int total = uniques.size();
		if (total == 0) return null;

		MutableIntIterator uu = uniques.intIterator();
		while (uu.hasNext()) {
			if (uu.next() < minTotalScore)
				uu.remove();
		}

		return uniques.isEmpty() ? null : uniques;

	}

	/**
	 * a Set is already duplicate free, so just sort it
	 */
	public static Term[] commute(Collection<Term> s) {
		Term[] x = s.toArray(Op.EmptyTermArray);
		return (x.length >= 2) && (!(s instanceof SortedSet)) ? commute(x) : x;
	}

//
//    public static Term[] neg(Term... modified) {
//        int l = modified.length;
//        Term[] u = new Term[l];
//        for (int i = 0; i < l; i++) {
//            u[i] = modified[i].neg();
//        }
//        return u;
//    }


//    public static Term[] dropRandom(Random random, Subterms t) {
//        int size = t.subs();
//        assert (size > 1);
//        Term[] y = new Term[size - 1];
//        int except = random.nextInt(size);
//        for (int i = 0, j = 0; i < size; i++) {
//            if (i != except) {
//                y[j++] = t.sub(i);
//            }
//        }
//        return y;
//    }

	public static StableBloomFilter<Term> newTermBloomFilter(Random rng, int cells) {
		return new StableBloomFilter<>(
			cells, 2, 1f / cells, rng,
			new TermHasher());
	}

	public static StableBloomFilter<Task> newTaskBloomFilter(Random rng, int cells) {
		return new StableBloomFilter<>(
			cells, 2, 1f / cells, rng,
			new TaskHasher());
	}


	/**
	 * non-symmetric use only
	 */
	public static boolean hasAllExcept(Termlike requirer, Termlike required, int maskedBits) {
		return hasAllExcept(requirer.structure(), required.structure(), maskedBits);
	}

	public static boolean hasAllExcept(int requirer, int required, int maskedBits) {
		int xStruct = requirer & ~(maskedBits);
		int yStruct = required & ~(maskedBits);
		return xStruct == 0 || yStruct == 0 ||
			Op.hasAll(yStruct, xStruct);
	}

	/**
	 * finds the shortest deterministic subterm path for extracting a subterm in a compound.
	 * paths in subterms of commutive terms are excluded also because the
	 * position is undeterministic.
	 */
	public static @Nullable byte[] pathConstant(Term container, Term subterm) {
		if (!canExtractFixedPath(container))
			return null;

		byte[][] p = new byte[1][];
		container.pathsTo(subterm,

			Terms::canExtractFixedPath,

			(path, xx) -> {
				if (p[0] == null || p[0].length > path.size()) {
					//found first or shorter
					p[0] = path.isEmpty() ? ArrayUtil.EMPTY_BYTE_ARRAY : path.toArray();
				}
				return true; //continue
			});
		return p[0];
	}

	private static boolean canExtractFixedPath(Term container) {
		return !(container instanceof PatternCompound.PatternCompoundWithEllipsis)
			&&
			!container.isCommutative();
	}


	public static boolean isSorted(Term[] s) {
		if (s.length < 2) return true;
		return IntStream.range(1, s.length).noneMatch(i -> s[(i - 1)].compareTo(s[i]) >= 0);
	}

	public static boolean possiblyUnifiable(Term x, Term y, boolean strict, int var) {

		Op xo = x.op(), yo = y.op();
		if (xo == NEG && yo == NEG) {
			x = x.unneg(); y = y.unneg();
			xo = x.op(); yo = y.op();
		}
		if (xo != yo) {
			//op mismatch, only allow if either is variable
			return (xo.bit & var) != 0 || (yo.bit & var) != 0;
		}


		//if (x.equals(y))
		if (x.equalsRoot(y))
			return !strict;

		if (!(x instanceof Compound))
			return false; //atomic non-var

		Subterms xx = x.subterms(), yy = y.subterms(); //subtermsDirect not possible because possiblyUnifiable tests equality HACK

		int n = xx.subs();
		if (n != yy.subs() &&
////			(!Terms.hasEllipsis(x, xxs) && !Terms.hasEllipsis(y, yys)) &&
			(xo != CONJ) || (Conj.isSeq(x) || Conj.isSeq(y))
//			//  && (xxs & Op.Temporal)==0 && (yys & Op.Temporal)==0)
		) {
			return false;
		}

		if (!Subterms.possiblyUnifiableAssumingNotEqual(xx, yy, var))
			return false;

		//TODO Conj Xternal allow


		if (!xo.commutative) {
            //reverse since smallest terms are last
            return IntStream.iterate(n - 1, i -> i >= 0, i -> i - 1).allMatch(i -> possiblyUnifiable(xx.sub(i), yy.sub(i), false, var));
		}

		return true;
	}


	public static Term withoutAll(Term container, Predicate<Term> filter) {
		Subterms cs = container.subterms();
		MetalBitSet match = cs.indicesOfBits(filter);
		int n = match.cardinality();
		if (n == 0) {
			return container; //no matches
		} else {
			Term[] remain = cs.removing(match);
			return remain == null ? Null : container.op().the(container.dt(), remain);
		}
	}


	public static boolean eqRCom(Term x, Term y) {
		if (x.equalsPosOrNeg(y))
			return true;

		if (x instanceof Neg && y instanceof Neg) {
			x = x.unneg(); y = y.unneg();
		}

		boolean xc = x instanceof Compound, yc = y instanceof Compound;
		if (!xc && !yc)
			return false; //both atomics, done



		if (!Term.commonStructure(x.structure(), y.structure()))
			return false;


		int xv = x.volume(), yv = y.volume();
		if (xv == yv)
			return false; //both atomic or same size (cant contain each other)

		//a > b |- a contains b?
		if (xv < yv) {
			Term c = x;
			x = y;
			y = c;
		}

//		Term finalX = x;
//		return !y.recurseTerms(yy -> statementLoopyContainer.test(yy), yy -> {
//			if (finalX.containsRecursively(yy, false, Op.statementLoopyContainer ))
//				return false;
//			return true;
//		}, null);
		return x.containsRecursively(y, false, Op.statementLoopyContainer);

	}

	public static boolean hasEllipsisRecurse(Term x) {
		return x instanceof Compound && x.hasVarPattern() && ((Compound) x).ORrecurse(t -> t instanceof Ellipsislike);
	}

	public static boolean hasEllipsis(Term x, int xs) {
		return x instanceof Compound && ((xs & Op.VAR_PATTERN.bit) != 0) && ((Compound) x).OR(t -> t instanceof Ellipsislike);
	}

	public static Term intersect(/*@NotNull*/ Op o, Subterms a, Subterms b) {
		if (a instanceof Term && a.equals(b))
			return (Term) a;

		if (!Term.commonStructure(a, b))
			return Null;


		SortedSet<nars.term.Term> ab = a.collect(b.subs() > 3 ? b.toSet()::contains : b::contains, new MetalTreeSet());
		int ssi = ab == null ? 0 : ab.size();
		switch (ssi) {
			case 0:
				return Null;
			case 1:
				return ab.first();
			default:
				return o.the(ab);
		}


	}

	public static Term union(/*@NotNull*/ Op o, Subterms a, Subterms b) {
		if (a == b)
			return a instanceof Term && ((Term)a).op()==o ? (Term)a : o.the(a);

		boolean bothTerms = a instanceof Term && b instanceof Term;
		if (bothTerms && a.equals(b))
			return (Term) a;

		int as = a.subs(), bs = b.subs();
		Set<Term> t = new UnifiedSet<>(as+bs);
		a.addAllTo(t);
		b.addAllTo(t);
		if (bothTerms) {
			int maxSize = Math.max(as, bs);
			if (t.size() == maxSize) {
				return (Term) (as > bs ? a : b);
			}
		}
		return o.the(t);
	}
}























