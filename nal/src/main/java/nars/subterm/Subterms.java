package nars.subterm;

import com.google.common.base.Joiner;
import jcog.TODO;
import jcog.Util;
import jcog.data.bit.MetalBitSet;
import jcog.data.byt.DynBytes;
import jcog.data.list.FasterList;
import jcog.sort.QuickSort;
import jcog.util.ArrayUtil;
import nars.NAL;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.Variable;
import nars.term.atom.Bool;
import nars.unify.Unify;
import nars.unify.mutate.CommutivePermutations;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntObjectPredicate;
import org.eclipse.collections.api.block.predicate.primitive.ObjectIntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.*;
import java.util.function.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static nars.Op.*;


/**
 * Methods common to both Term and Subterms
 * T = subterm type
 */
public interface Subterms extends Termlike, Iterable<Term> {


	static int hash(Term onlySub) {
		return Util.hashCombine1(onlySub);
	}

	static int hash(Term[] term) {
		return hash(term, term.length);
	}

	static int hash(Term[] term, int n) {
		int h = 1;
		for (int i = 0; i < n; i++)
			h = Util.hashCombine(h, term[i]);
		return h;
	}

	static int hash(Subterms container) {
		return container.intifyShallow(1, Util::hashCombine);
	}

	static boolean commonSubtermsRecursive(/*@NotNull*/ Term a, /*@NotNull*/ Term b, boolean excludeVariables) {

		Subterms aa = a.subterms(), bb = b.subterms();

		int commonStructure = aa.structure() & bb.structure();
		if (excludeVariables)
			commonStructure &= ~(Op.Variable) & AtomicConstant;

		if (commonStructure == 0)
			return false;

		Set<Term> scratch = new UnifiedSet<>(0);
		aa.recurseSubtermsToSet(commonStructure, scratch, true);
		return bb.recurseSubtermsToSet(commonStructure, scratch, false);
	}

	static String toString(/*@NotNull*/ Iterable<? extends Term> subterms) {
		return '(' + Joiner.on(',').join(subterms) + ')';
	}

	static String toString(/*@NotNull*/ Term... subterms) {
		return '(' + Joiner.on(',').join(subterms) + ')';
	}

	static int compare(/*@NotNull*/ Subterms a, /*@NotNull*/ Subterms b) {
		if (a == b)
			return 0;

		int s;
		int diff;
		if ((diff = Integer.compare(s = a.subs(), b.subs())) != 0)
			return diff;

		if (s == 1) {
			return a.sub(0).compareTo(b.sub(0));
		} else {

			Term inequalVariableX = null, inequalVariableY = null;

			for (int i = 0; i < s; i++) {
				Term x = a.sub(i), y = b.sub(i);
				if (x instanceof Variable && y instanceof Variable) {
					if (inequalVariableX == null && !x.equals(y)) {
						inequalVariableX = x;
						inequalVariableY = y;
					}
				} else {
					int d = x.compareTo(y);
					if (d != 0)
						return d;
				}
			}


			return inequalVariableX != null ? inequalVariableX.compareTo(inequalVariableY) : 0;
		}
	}

	static boolean unifyLinear(Subterms x, Subterms y, Unify u) {
		int n = x.subs();
		assert (y.subs() == n);
		switch (n) {
			case 0:
				return true;
			case 1:
				return x.sub(0).unify(y.sub(0), u);
			default:
				if (//(/* flat: */ x instanceof IntrinSubterms && y instanceof IntrinSubterms) ||
					u.random.nextFloat() < NAL.SUBTERM_UNIFY_ORDER_RANDOM_PROBABILITY)
					return unifyRandom(x, y, n, u);
				return n == 2 ? unifyLinear2_complexityHeuristic(x, y, u) : unifyLinearN_TwoPhase(x, y, n, u);
		}
	}

	static boolean unifyLinear2_complexityHeuristic(Subterms x, Subterms y, Unify u) {
		Term x0 = x.sub(0), y0 = y.sub(0);
		if (x0 == y0)
			return x.sub(1).unify(y.sub(1), u);

		int v0 = u.vars(x0) + u.vars(y0);
		if (v0 == 0) {
			return x0.unify(y0, u) && x.sub(1).unify(y.sub(1), u);
		} else {
			Term x1 = x.sub(1), y1 = y.sub(1);
			if (x1 == y1)
				return x0.unify(y0, u);

			int v1 = u.vars(x1) + u.vars(y1);
			boolean forward;
			forward = v1 == v0 ? x0.volume() + y0.volume() <= x1.volume() + y1.volume() : v0 < v1;
			return forward ?
				x0.unify(y0, u) && x1.unify(y1, u) :
				x1.unify(y1, u) && x0.unify(y0, u);
		}
	}

	static boolean unifyRandom(Subterms x, Subterms y, int n, Unify u) {
		if (n == 2) {
			int s = u.random.nextBoolean() ? 0 : 1;
			return x.sub(s).unify(y.sub(s), u) && x.sub(1 - s).unify(y.sub(1 - s), u);
		} else {
			byte[] order = new byte[n];
			for (int i = 0; i < n; i++)
				order[i] = (byte) i;
			ArrayUtil.shuffle(order, u.random);
			for (byte b : order) {
				if (!x.sub(b).unify(y.sub(b), u))
					return false;
			}
			return true;
		}
	}

//    default boolean equalTermsIdentical(Subterms x) {
//        if (this == x) return true;
//        int n = subs();
//        if (x.subs()!=n) return false;
//        return IntStream.range(0, n).noneMatch(i -> sub(i) != x.sub(i));
//    }

	static boolean unifyLinearN_Forward(Subterms x, Subterms y, /*@NotNull*/ Unify u) {
		int s = x.subs();
		return IntStream.range(0, s).allMatch(i -> x.sub(i).unify(y.sub(i), u));
	}

	static boolean unifyLinearN_TwoPhase(Subterms x, Subterms y, int n, Unify u) {
		//TODO elide subsequent repeats
		MetalBitSet m = null;
		for (int i = 0; i < n; i++) {
			Term xi = x.sub(i), yi = y.sub(i);

			if (xi.equals(yi))
				continue;

			boolean now = (i == n - 1 && m == null /* last one anyway so just do it */) || (!u.varIn(xi) && !u.varIn(yi));
			if (now) {
				if (!xi.unify(yi, u))
					return false;
			} else {
				if (m == null) m = MetalBitSet.bits(n);
				m.set(i);
			}
		}
		if (m == null)
			return true;

		//process remaining non-constant subterms

		int nonconst = m.cardinality();
		if (nonconst == 1) {
			int which = m.next(true, -1, n);
			return x.sub(which).unify(y.sub(which), u);
		} else {

			int[] c = new int[nonconst];
			int k = 0;
			//sort based on heuristic of estimated simplicity
			for (int i = 0; i < n && k < nonconst; i++) {
				if (m.get(i))
					c[k++] = i;
			}
			QuickSort.sort(c, cc -> -(x.sub(cc).volume() + y.sub(cc).volume())); //sorts descending
			for (int cc : c) {
				if (!x.sub(cc).unify(y.sub(cc), u))
					return false;
			}
			return true;
		}
	}

	/**
	 * first layer operator scan
	 * TODO check for obvious constant target mismatch
	 *
	 * @return 0: must unify, -1: impossible, +1: unified already
	 */
	static int possiblyUnifiableWhileEliminatingEqualAndConstants(TermList xx, TermList yy, Unify u) {

		int xxs = xx.size();

		//assert(yy.size()==n);
		if (yy.size() != xxs)
			return -1;

		for (int i = 0; i < xxs; ) {
//            Term xi = u.resolvePosNeg(xx.get(i));
			Term xi = xx.get(i);
			if (yy.removeFirst(xi)) {
				xx.removeFast(i);
				xxs--;
			} else {
				i++;
			}
		}

		if (xxs == 0)
			return +1; //all eliminated

		//            if (xxs == 1)
		//                return 0; //one subterm remaining, direct match will be tested by callee
		//            Set<Term> xConst = null;
		//            for (int i = 0; i < xxs; i++) {
		//                Term xxx = xx.get(i);
		//                if (u.constant(xxx)) {
		//                    if (xConst == null) xConst = new UnifiedSet(xxs-i);
		//                    xConst.addAt(xxx);
		//                }
		//            }
		//            if (xConst!=null) {
		//                Set<Term> yConst = null;
		//                for (int i = 0; i < xxs; i++) {
		//                    Term yyy = yy.get(i);
		//                    if (u.constant(yyy)) {
		//                        if (yConst == null) yConst = new UnifiedSet(xxs-i);
		//                        yConst.addAt(yyy);
		//                    }
		//                }
		//                if (yConst!=null) {
		//                    if (xConst.size() == yConst.size()) {
		//                        if (!xConst.equals(yConst))
		//                            return -1; //constant mismatch
		//                    } else {
		//                        //can this be tested
		//                    }
		//                }
		//            }
		//first layer has no non-variable commonality, no way to unify
		return possiblyUnifiable(xx, yy, u.varBits) ? 0 : -1;

	}

	/**
	 * assumes that equality, structure commonality, and equal subterm count have been tested
	 */
	static boolean unifyCommute(Subterms x, Subterms y, Unify u) {
		TermList xx = u.resolveListIfChanged(x, true);
		if (xx == null) xx = x.toList();

		TermList yy = u.resolveListIfChanged(y, true);
		if (yy == null) yy = y.toList();

		//TermList xx = x.toList(), yy = y.toList();

		int i = possiblyUnifiableWhileEliminatingEqualAndConstants(xx, yy, u);
		switch (i) {
			case -1:
				return false;
			case +1:
				return true;
		}

		if (xx.subs() == 1) {
//                    Term x0 = xx.getFirstFast();
//                    if (x0.equals(yy))
//                        return false; //this is a cyclical case that has been detected?
//                    return x0.unify(yy.getFirstFast(), u);
			return xx.getFirstFast().unify(yy.getFirstFast(), u);
		} else {
			u.termute(new CommutivePermutations(x.equals(xx) ? x : xx, y.equals(yy) ? y : yy));
			return true;
		}


	}

	static boolean possiblyUnifiable(Termlike xx, Termlike yy, int var) {
		return xx.equals(yy) || possiblyUnifiableAssumingNotEqual(xx, yy, var);
	}

	static boolean possiblyUnifiableAssumingNotEqual(Termlike xx, Termlike yy, int var) {

		int varOrTemporal = var | CONJ.bit; //Op.Temporal;
		int XS = xx.structure(), YS = yy.structure();
		if (((XS & varOrTemporal) == 0) && ((YS & varOrTemporal) == 0)) //no variables or temporals
			return false;
		if (XS != YS && (XS & var) == 0 && (YS & var) == 0)
			return false; //differing structure and both constant


//        int XS = xx.structure(), YS = yy.structure();
		int XSc = XS & (~var);
		if (XSc == 0)
			return true; //X contains only vars
		int YSc = YS & (~var);
		if (YSc == 0)
			return true; //Y contains only vars

		if (XSc == XS && YSc == YS) {
			boolean noTemporal = (XS & Op.Temporal) == 0 && ((YS & Op.Temporal) == 0);
			return noTemporal ? xx.equals(yy) : ((XS & CONJ.bit) != 0 || ((YS & CONJ.bit) != 0)) || (XS == YS && xx.volume() == yy.volume());

		}

		return true;

		//finer-grained sub-constant test

//        int xs = xx.structureConstant(varBits);
//        int ys = yy.structureConstant(varBits);
//        return (xs & ys) != 0; //any constant subterm commonality
	}

	/**
	 * test for eliding repeats in visitors
	 */
	public  static boolean different(Term prev, Term next) {
		return prev != next;
	}

	/**
	 * determines if the two non-identical terms are actually equivalent or if y must be part of the output for some reason (special term, etc)
	 * TODO refine
	 */
	  static boolean differentlyTransformed(Term xi, Term yi) {
		return !xi.equals(yi) || xi.unneg().getClass() != yi.unneg().getClass();// || !yi.the();
//        if (!xi.equals(yi)) return true;
//        Term xxi, yyi;
//        if (xi instanceof Neg) {
//            xxi = xi.unneg(); yyi = yi.unneg();
//        } else {
//            xxi = xi; yyi = yi;
//        }
//        return xxi.getClass() != yyi.getClass()
//            ||
//            !yi.the();

//            || !xi.the()
//            || !yi.the()
//        );
	}

	static @Nullable TermList transformSubInline(Subterms e, UnaryOperator<Term> f, TermList out, int subsTotal, int i) {
		int xes = e.subs();

		if (out == null)
			out = new DisposableTermList(subsTotal - 1 + xes /*estimate */, i);
		else
			out.ensureExtraCapacityExact(xes - 1);

		for (int j = 0; j < xes; j++) {

			Term k = f.apply(e.sub(j)); //assert(k!=null);

			if (k == Bool.Null) {
				return null;
			} else if (k.op() == FRAG) {
				if (NAL.DEBUG)
					throw new TODO("recursive EllipsisMatch unsupported");
				else
					return null;
			} else {

				out.ensureExtraCapacityExact(xes - 1);
				out.addFast(k);
			}
		}
		return out;
	}

//    /**
//     * returns sorted ready for commutive; null if nothing in common
//     */
//    static @Nullable MutableSet<Term> intersect(/*@NotNull*/ Subterms a, /*@NotNull*/ Subterms b) {
//        if ((a.structure() & b.structure()) != 0) {
//
//            Set<Term> as = a.toSet();
//            MutableSet<Term> ab = b.toSet(as::contains);
//            if (ab != null)
//                return ab;
//        }
//        return null;
//    }

	@Override
	boolean hasXternal();

	Predicate<Term> containing();

	@Override
	boolean contains(Term x);

//    /*@NotNull*/
//    static boolean commonSubterms(/*@NotNull*/ Compound a, /*@NotNull*/ Compound b, boolean excludeVariables) {
//
//        int commonStructure = a.structure() & b.structure();
//        if (excludeVariables)
//            commonStructure = commonStructure & ~(Op.Variable);
//
//        if (commonStructure == 0)
//            return false;
//
//        Set<Term> scratch = new UnifiedSet(a.subs());
//        Subterms.subtermsToSet(a, commonStructure, scratch, true);
//        return Subterms.subtermsToSet(b, commonStructure, scratch, false);
//
//    }

	boolean containsInstance(Term t);

	@Nullable Term subSub(byte[] path);

	@Nullable Term subSub(int start, int end, byte[] path);

	@Nullable Term subSubUnsafe(int start, int end, byte[] path);

	boolean containsAll(Subterms ofThese);

	boolean containsAny(Subterms ofThese);

	//    /**
//     * a and b must be instances of input, and output must be of size input.length-2
//     */
//    /*@NotNull*/
//    static Term[] except(/*@NotNull*/ Subterms input, Term a, Term b, /*@NotNull*/ Term[] output) {
//
//
//        int j = 0;
//        int l = input.subs();
//        for (int i = 0; i < l; i++) {
//            Term x = input.sub(i);
//            if ((x != a) && (x != b))
//                output[j++] = x;
//        }
//
//        if (j != output.length)
//            throw new RuntimeException("permute underflow");
//
//        return output;
//    }

	<X> X[] array(Function<Term, X> map, IntFunction<X[]> arrayizer);

	int subEventRange(int i);

	@Nullable Term subRoulette(FloatFunction<Term> subValue, Random rng);

	@Nullable Term sub(Random rng);

	Subterms remove(Term event);

	void forEachI(ObjectIntProcedure<Term> t);

	<X> void forEachWith(BiConsumer<Term, X> t, X argConst);

	/**
	 * sorted and deduplicated
	 */
	Subterms commuted();

	boolean isSorted();

	boolean isCommuted();


	final class SubtermsIterator implements Iterator<Term> {
		final int s;
		private final Subterms terms;
		int i = 0;

		public SubtermsIterator(Subterms terms) {
			this.terms = terms;
			this.s = terms.subs();
		}

		@Override
		public boolean hasNext() {
			return i < s;
		}

		@Override
		public Term next() {
			return terms.sub(i++);
		}
	}

	boolean subEquals(int i, /*@NotNull*/ Term x);

	/*@NotNull*/ SortedSet<Term> toSetSorted();

	@SuppressWarnings("LambdaUnfriendlyMethodOverload") /*@NotNull*/ SortedSet<Term> toSetSorted(UnaryOperator<Term> map);


//    /**
//     * returns whether the set operation caused a change or not
//     */
//    /*@NotNull*/
//    private static boolean subtermsToSet(Subterms ss, int inStructure, /*@NotNull*/ Collection<Term> t, boolean addOrRemoved) {
//        boolean r = false;
//
//        int l = ss.subs();
//        for (int i = 0; i < l; i++) {
//            /*@NotNull*/
//            Term s = ss.sub(i);
//            if (inStructure == -1 || ((s.structure() & inStructure) > 0)) {
//                r |= (addOrRemoved) ? t.addAt(s) : t.remove(s);
//                if (!addOrRemoved && r)
//                    return true;
//            }
//        }
//        return r;
//    }

	/*@NotNull*/ SortedSet<Term> toSetSorted(Predicate<Term> t);

	/**
	 * an array of the subterms, which an implementation may allow
	 * direct access to its internal array which if modified will
	 * lead to disaster. by default, it will call 'toArray' which
	 * guarantees a clone. override with caution
	 */
	Term[] arrayShared();

	/**
	 * an array of the subterms
	 * this is meant to be a clone always
	 */
	Term[] arrayClone();

	Term[] arrayClone(Term[] target);

	Term[] arrayClone(Term[] target, int from, int to);

	/*@NotNull*/ TermList toList();

	/**
	 * @return a Mutable Set, unless empty
	 */
	/*@NotNull*/ MutableSet<Term> toSet();

	@Nullable <C extends Collection<Term>> C collect(Predicate<Term> ifTrue, C c);

	/**
	 * by default this does not need to do anything
	 * but implementations can cache the normalization
	 * in a boolean because it only needs done once.
	 */
	void setNormalized();

	/**
	 * assume its normalized if no variables are present
	 */
	boolean isNormalized();

	/**
	 * gets the set of unique recursively contained terms of a specific type
	 * TODO generalize to a provided lambda predicate selector
	 */
	/*@NotNull*/
	Set<Term> recurseSubtermsToSet(Op _onlyType);

	/*@NotNull*/
	boolean recurseSubtermsToSet(int inStructure, /*@NotNull*/ Collection<Term> t, boolean untilAddedORwhileNotRemoved);

	boolean containsRecursively(/*@NotNull*/ Term x, boolean root, Predicate<Term> subTermOf);

	boolean equalTerms(/*@NotNull*/ Subterms c);

	void addAllTo(Collection target);

	void addAllTo(FasterList target);

	/* final */ boolean impossibleSubStructure(int structure);

	int subStructure();

	Term[] terms(/*@NotNull*/ IntObjectPredicate<Term> filter);

	void forEach(Consumer<? super Term> action, int start, int stop);


	/**
	 * return whether a subterm op at an index is an operator.
	 */
	boolean subIs(int i, Op o);

	/**
	 * counts subterms matching the predicate
	 */
	int count(Predicate<Term> match);

	boolean countEquals(Predicate<Term> match, int n);

	/**
	 * counts subterms matching the supplied op
	 */
	int count(Op matchingOp);

	/**
	 * return whether a subterm op at an index is an operator.
	 * if there is no subterm or the index is out of bounds, returns false.
	 */
	boolean subIsOrOOB(int i, Op o);

	/**
	 * first index of; follows normal indexOf() semantics; -1 if not found
	 */
	/* final */ int indexOf(/*@NotNull*/ Term t);

//    static boolean unifyLinearN_TwoPhase0(Subterms x, Subterms y, int n, Unify u) {
//        Term[] p = null;
//        int dynPairs = 0;
//        for (int i = 0; i < n; i++) {
//            Term xi = x.sub(i);
//            Term yi = y.sub(i);
//
//            if (xi == yi)
//                continue;
//
//            boolean now = (i == n - 1) || ((u.var(xi) && u.var(yi)));
//
//            if (now) {
//
//                if (!xi.unify(yi, u))
//                    return false;
//            } else {
//                if (p == null)
//                    p = new Term[(n - i - 1) * 2];
//
//                //backwards order
//                p[dynPairs++] = yi;
//                p[dynPairs++] = xi;
//            }
//        }
//
//
//        if (p != null) {
//            int pairs = dynPairs/2;
//            if (pairs == 1) {
//                return p[1].unify(p[0], u);
//            } else {
//
//                //TODO sort deferredPairs so that smaller non-commutive subterms are tried first
//                if (pairs ==2 ) {
//                    boolean forward = choose(1f/(p[0].voluplexity() + p[1].voluplexity()), 1f/(p[2].voluplexity() + p[3].voluplexity()), u
//                    );
//
//                    if (forward) {
//                        return p[1].unify(p[0], u) && p[3].unify(p[2], u);
//                    } else {
//                        return p[3].unify(p[2], u) && p[1].unify(p[0], u);
//                    }
//                } else {
//
//                    do {
//                        if (!p[--dynPairs].unify(p[--dynPairs], u))
//                            return false;
//                    } while (dynPairs > 0);
//                }
//            }
//        }
//
//        return true;
//    }

//    static boolean choose(float forwardWeight, float reverseWeight, Unify u) {
//        return u.random.nextFloat() < (forwardWeight /(forwardWeight + reverseWeight));
//
////                    if (v01 < v23) {
////                        //try 01 first
////                        forward = true;
////                    } else if (v01 > v23) {
////                        forward = false;
////                    } else {
////                        forward = u.random.nextBoolean();
////                    }
//    }

	int indexOf(/*@NotNull*/ Term t, int after);

	/**
	 * return the first subterm matching the predicate, or null if none match
	 */
	@Nullable Term subFirst(Predicate<Term> match);

	boolean impossibleSubTerm(int structure, int volume);


//    default Term[] termsExcept(RoaringBitmap toRemove) {
//        int numRemoved = toRemove.getCardinality();
//        int size = subs();
//        int newSize = size - numRemoved;
//        Term[] t = new Term[newSize];
//        int j = 0;
//        for (int i = 0; i < size; i++) {
//            if (!toRemove.contains(i))
//                t[j++] = sub(i);
//        }
//        return (t.length == 0) ? Op.EmptyTermArray : t;
//    }

	/**
	 * stream of each subterm
	 */
	Stream<Term> subStream();

	/**
	 * allows the subterms to hold a different hashcode than hashCode when comparing subterms
	 */
	int hashCodeSubterms();

	/**
	 * TODO write negating version of this that negates only up to subs() bits
	 */
	MetalBitSet indicesOfBits(Predicate<Term> match);

	Term[] subsIncluding(Predicate<Term> toKeep);

	Term[] subsIncluding(MetalBitSet toKeep);

	Term[] removing(MetalBitSet toRemove);

	@Nullable Term[] subsIncExc(MetalBitSet s, boolean includeOrExclude);

	/**
	 * match a range of subterms of Y.
	 * WARNING: provides a shared (non-cloned) copy if the entire range is selected
	 */
	/*@NotNull*/
	Term[] subRangeArray(int from, int to);

	int indexOf(/*@NotNull*/ Predicate<Term> p);

	int indexOf(/*@NotNull*/ Predicate<Term> p, int after);

	/**
	 * returns true if evaluates true for all terms
	 * implementations are allowed to skip repeating subterms and visit out-of-order
	 *
	 * @param p
	 */
	boolean AND(/*@NotNull*/ Predicate<Term> p);

	/**
	 * returns true if evaluates true for any terms
	 * implementations are allowed to skip repeating subterms and visit out-of-order
	 *
	 * @param p
	 */
	boolean OR(/*@NotNull*/ Predicate<Term> p);

	/**
	 * supplies the i'th index as 2nd lambda argument. all subterms traversed, incl repeats
	 */
	boolean ANDi(/*@NotNull*/ ObjectIntPredicate<Term> p);

	/**
	 * supplies the i'th index as 2nd lambda argument. all subterms traversed, incl repeats
	 */
	boolean ORi(/*@NotNull*/ ObjectIntPredicate<Term> p);

	/**
	 * warning: elides test for repeated subterm
	 */
	<X> boolean ORwith(/*@NotNull*/ BiPredicate<Term, X> p, X param);

	/**
	 * warning: elides test for repeated subterm
	 */
	<X> boolean ANDwith(/*@NotNull*/ BiPredicate<Term, X> p, X param);

	/**
	 * visits each, incl repeats
	 */
	<X> boolean ANDwithOrdered(/*@NotNull*/ BiPredicate<Term, X> p, X param);

	/**
	 * warning: elides test for repeated subterm
	 */
	boolean ANDrecurse(Predicate<Term> p);

	/**
	 * warning: elides test for repeated subterm
	 */
	boolean ORrecurse(Predicate<Term> p);

	/**
	 * must be overriden by any Compound subclasses
	 */
	boolean recurseTerms(Predicate<Compound> aSuperCompoundMust, BiPredicate<Term, Compound> whileTrue, Compound parent);

	/**
	 * incl repeats
	 */
	boolean recurseTermsOrdered(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, Compound parent);

	Subterms reversed();

	/**
	 * removes first occurrence only
	 */
	Term[] removing(int index);

	int hashWith(Op op);

	int hashWith(/*byte*/int op);

	@Nullable Term[] removing(Term x);

//    /**
//     * dont override
//     */
//    default Subterms replaceSub(Term from, Term to) {
//        return !from.equals(to) && !impossibleSubTerm(from) ? transformSubs(MapSubst.replace(from, to), ATOM) : this;
//    }

	Subterms transformSub(int which, UnaryOperator<Term> f);

	/**
	 * returns 'x' unchanged if no changes were applied,
	 * returns 'y' if changes
	 * returns null if untransformable
	 * <p>
	 * superOp is optional (use ATOM as the super-op to disable its use),
	 * providing a hint about the target operator the subterms is being constructed for
	 * this allows certain fail-fast cases
	 */
	@Nullable Subterms transformSubs(UnaryOperator<Term> f, Op superOp);


	boolean containsPosOrNeg(Term x);

	boolean containsNeg(Term x);

	/**
	 * allows a Subterms implementation to accept the byte[] key that was used in constructing it,
	 * allowing it to cache it for fast serialization.  typically it will want to keep:
	 * <p>
	 * byte[] cached = builtWith.arrayCopy(1) //skip prefix op byte
	 */
	@FunctionalInterface
	interface SubtermsBytesCached {
		void acceptBytes(DynBytes constructedWith);
	}

}
