package nars.term;

import jcog.bloom.StableBloomFilter;
import jcog.bloom.hash.BytesHasher;
import jcog.data.bit.MetalBitSet;
import jcog.util.ArrayUtil;
import nars.Op;
import nars.Task;
import nars.io.IO;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.task.util.TaskHasher;
import nars.term.atom.Atom;
import nars.term.compound.PatternCompound;
import nars.term.util.conj.Conj;
import nars.term.var.ellipsis.Ellipsislike;
import nars.unify.constraint.NotEqualConstraint;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.iterator.MutableIntIterator;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

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
			default: {
				return commuteN(x);
			}
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
			default:
				return ifDifferent(x, new TermList(x.clone()).sort());
		}
	}

	private static Term[] commuteN(Term[] x) {
		Term[] y = new TermList(x.clone()).sortAndDedup();
		return ifDifferent(x, y);
	}

	private static Term[] ifDifferent(Term[] a, Term[] b) {
		if (ArrayUtil.equalsIdentity(a, b))
			return a; //unchanged
		else
			return b;
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
		if (t[0] == a && t[1] == b)
			return t; //already sorted
		else {
			return new Term[]{a, b, c};
		}
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


		x.subterms().forEach(z -> printRecursive(out, z, level + 1));


	}

	/**
	 * for printing complex terms as a recursive tree
	 */
	public static void printRecursive(Term x, Consumer<String> c) {
		printRecursive(x, 0, c);
	}

	public static void printRecursive(Term x, int level, Consumer<String> c) {

		StringBuilder line = new StringBuilder(level * 2 + 32);
		line.append("  ".repeat(Math.max(0, level)));

		line.append(x);


		for (Term z : x.subterms())
			printRecursive(z, level + 1, c);


		c.accept(line.toString());
	}


	@Nullable
	public static Term[] concat(Term[] a, Term... b) {

		if (a.length == 0) return b;
		if (b.length == 0) return a;

		int L = a.length + b.length;

		Term[] arr = new Term[L];

		int l = a.length;
		System.arraycopy(a, 0, arr, 0, l);
		System.arraycopy(b, 0, arr, l, b.length);

		return arr;
	}


	@Nullable
	public static Atom atomOr(@Nullable Term possiblyCompound, Atom other) {
		return (possiblyCompound instanceof Atom) ? (Atom) possiblyCompound : other;
	}

	@Nullable
	public static Atom atomOrNull(@Nullable Term t) {
		return atomOr(t, null);
	}

	/**
	 * dangerous because some operations involving concepts can naturally reduce to atoms, and using this interprets them as non-existent
	 */
	@Nullable
	@Deprecated
	public static Compound compoundOrNull(@Nullable Term t) {
		return t instanceof Compound ? (Compound) t : null;
	}


	public static boolean allNegated(Subterms subterms) {
		return subterms.hasAny(Op.NEG) && subterms.AND((Term t) -> t instanceof Neg);
	}

	public static int countNegated(Subterms subterms) {
		return subterms.hasAny(Op.NEG) ? subterms.count(NEG) : 0;
	}

	public static int negatedCount(Subterms subterms) {
		return subterms.hasAny(Op.NEG) ? subterms.count(x -> x instanceof Neg) : 0;
	}

	public static int negatedNonConjCount(Subterms subterms) {
		return subterms.hasAny(Op.NEG) ? subterms.count(x -> x instanceof Neg && !x.subterms().hasAny(CONJ)) : 0;
	}

	/**
	 * returns the most optimal subterm that can be replaced with a variable, or null if one does not meet the criteria
	 * when there is a chocie, it prefers least aggressive introduction. and then random choice if
	 * multiple equals are introducible
	 *
	 * @param superterm filter applies to the immediate superterm of a potential subterm
	 */
	@Nullable
	public static Term[] nextRepeat(Subterms c, ToIntFunction<Term> countIf, int minCount) {
		ObjectIntHashMap<Term> oi = Terms.subtermScore(c, countIf, minCount);
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
	@Nullable
	public static ObjectIntHashMap<Term> subtermScore(Subterms c, ToIntFunction<Term> score, int minTotalScore) {
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
		if ((x.length >= 2) && (!(s instanceof SortedSet)))
			return commute(x);
		else
			return x;
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
			new BytesHasher<>(IO::termToBytes));
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
	@Nullable
	public static byte[] pathConstant(Term container, Term subterm) {
		if (!canExtractFixedPath(container))
			return null;

		final byte[][] p = new byte[1][];
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
		for (int i = 1; i < s.length; i++)
			if (s[(i - 1)].compareTo(s[i]) >= 0)
				return false;
		return true;
	}

	public static boolean possiblyUnifiable(Term x, Term y, boolean strict, int var) {

		//if (x.equals(y))
		if (x.equalsRoot(y))
			return !strict;

		Op xo = x.op(), yo = y.op();
		if (xo != yo) {

			int nonVarBits = ~var;
			if (x instanceof Variable && (xo.bit & nonVarBits) == 0) return true; //variable, allow
			if (y instanceof Variable && (yo.bit & nonVarBits) == 0) return true; //variable, allow

			return false; //op mismatch
		}

		if (!(x instanceof Compound))
			return false; //atomic non-var

		int varOrTemporal = var | Op.Temporal;
		int xxs = x.structure(), yys = y.structure();
		if (((xxs & varOrTemporal) == 0) && ((yys & varOrTemporal) == 0)) //no variables or temporals
			return false;

		//TODO Conj Xternal allow

		Subterms xx = x.subterms(), yy = y.subterms();
		int n = xx.subs();
		if ((n != yy.subs()) &&
			(!Terms.hasEllipsis(x, xxs) && !Terms.hasEllipsis(y, yys)) &&
			((xo != CONJ) || (!Conj.isSeq(x) && !Conj.isSeq(y)))
			//  && (xxs & Op.Temporal)==0 && (yys & Op.Temporal)==0)
		) {
			return false;
		}

		if (!Subterms.possiblyUnifiable(xx, yy, var))
			return false;

		if (!xo.commutative) {
			for (int i = n - 1; i >= 0; i--)
				if (!possiblyUnifiable(xx.sub(i), yy.sub(i), false, var))
					return false;
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
			if (remain == null)
				return Null;
			else {
				return container.op().the(container.dt(), remain);
			}
		}
	}


	private static boolean rCom(Term a, Term b, boolean recurse) {

		return recurse ?
			a.containsRecursively(b, NotEqualConstraint.NotEqualAndNotRecursiveSubtermOf.root, Op.statementLoopyContainer) :
			a.contains(NotEqualConstraint.NotEqualAndNotRecursiveSubtermOf.root ? b.root() : b);

	}

	public static boolean eqRCom(Term _x, Term _y) {
		if (_x == _y) return true;  //fast test
		Term x = _x.unneg(), y = _y.unneg();
		if (x.equals(y))
			return true;

		boolean xc = x instanceof Compound, yc = y instanceof Compound;
		if (!xc && !yc)
			return false; //done

//        if (Term.coRecursiveStructure(x.structure(), y.structure()) != Term.commonStructure(x, y))
//            Util.nop();

		if (!Term.commonStructure(x.structure() & ~(Op.CONJ.bit), y.structure() & ~(Op.CONJ.bit)))
			return false;

//		if (yc && y.op() == CONJ) {
//			return y.subterms().ORwith((Y, X) -> eqRCom(X, Y.unneg()), x); //AND?
//		} else if (xc && x.op() == CONJ) {
//			return x.subterms().ORwith((X, Y) -> eqRCom(X.unneg(), Y), y); //AND?
//		} else
		    {

                int av = x.volume(), bv = y.volume();

                //a > b |- a contains b?
                if (av < bv) {
                    Term c = x;
                    x = y;
                    y = c;
                }


                if (av == bv)
                    return false; //both atomic or same size (cant contain each other)

                return rCom(x, y, true);
            }


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

		if (Term.commonStructure(a, b)) {

			TreeSet<Term> ab = a.collect(b.subs() > 3 ? (b.toSet()::contains) : b::contains, new TreeSet());
			int ssi = ab == null ? 0 : ab.size();
			switch (ssi) {
				case 0:
					return Null;
				case 1:
					return ab.first();
				default:
					return o.the(ab);
			}

		} else
			return Null;
	}

	public static Term union(/*@NotNull*/ Op o, Subterms a, Subterms b) {
		boolean bothTerms = a instanceof Term && b instanceof Term;
		if (bothTerms && a.equals(b))
			return (Term) a;

		TreeSet<Term> t = new TreeSet<>();
		a.addAllTo(t);
		b.addAllTo(t);
		if (bothTerms) {
			int as = a.subs(), bs = b.subs();
			int maxSize = Math.max(as, bs);
			if (t.size() == maxSize) {
				return (Term) (as > bs ? a : b);
			}
		}
		return o.the(t);
	}
}























