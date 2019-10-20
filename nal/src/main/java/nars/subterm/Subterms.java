package nars.subterm;

import com.google.common.base.Joiner;
import com.google.common.collect.Streams;
import jcog.TODO;
import jcog.Util;
import jcog.data.bit.MetalBitSet;
import jcog.data.byt.DynBytes;
import jcog.data.list.FasterList;
import jcog.data.set.ArrayUnenforcedSortedSet;
import jcog.data.set.MetalTreeSet;
import jcog.decide.Roulette;
import jcog.sort.QuickSort;
import jcog.util.ArrayUtil;
import nars.NAL;
import nars.Op;
import nars.subterm.util.TermMetadata;
import nars.term.Variable;
import nars.term.*;
import nars.term.atom.IdempotentBool;
import nars.term.var.ellipsis.Fragment;
import nars.unify.Unify;
import nars.unify.mutate.CommutivePermutations;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntObjectPredicate;
import org.eclipse.collections.api.block.predicate.primitive.ObjectIntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Sets;
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
			boolean forward = v1 == v0 ? x0.volume() + y0.volume() <= x1.volume() + y1.volume() : v0 < v1;
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
				if (!x.sub((int) b).unify(y.sub((int) b), u))
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
		for (int i = 0; i < s; i++) {
			if (!x.sub(i).unify(y.sub(i), u)) {
				return false;
			}
		}
		return true;
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
			QuickSort.sort(c, cc -> (float) -(x.sub(cc).volume() + y.sub(cc).volume())); //sorts descending
			for (int cc : c) {
				if (!x.sub(cc).unify(y.sub(cc), u)) {
					return false;
				}
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
	private static boolean different(Term prev, Term next) {
		return prev != next;
	}

	/**
	 * determines if the two non-identical terms are actually equivalent or if y must be part of the output for some reason (special term, etc)
	 * TODO refine
	 */
	private static boolean differentlyTransformed(Term xi, Term yi) {
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

			if (k == IdempotentBool.Null) {
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
	default boolean hasXternal() {
		return hasAny(Op.Temporal) && OR(Term::hasXternal);
	}

	default Predicate<Term> containing() {
		switch (subs()) {
			case 0:
				return (x) -> false;
			case 1:
				return sub(0)::equals;
			default:
				return this::contains;
		}
	}

	@Override
	default boolean contains(Term x) {
		return indexOf(x) != -1;
	}

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

	default boolean containsInstance(Term t) {
		return ORwith((i, T) -> i == T, t);
	}

	default @Nullable Term subSub(byte[] path) {
		return subSub(0, path.length, path);
	}

	default @Nullable Term subSub(int start, int end, byte[] path) {
		Termlike ptr = this;
		for (int i = start; i < end; i++) {
			if ((ptr = ptr.subSafe((int) path[i])) == IdempotentBool.Null)
				return null;
		}
		return ptr != this ? (Term) ptr : null;
	}

	default @Nullable Term subSubUnsafe(int start, int end, byte[] path) {
		Termlike ptr = this;
		for (int i = start; i < end; )
			ptr = ptr.sub((int) path[i++]);
		return (Term) ptr;
	}

	default boolean containsAll(Subterms ofThese) {
		return this.equals(ofThese) || ofThese.AND(this::contains);
	}

	default boolean containsAny(Subterms ofThese) {
		return this.equals(ofThese) || ofThese.OR(this::contains);
	}

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

	default <X> X[] array(Function<Term, X> map, IntFunction<X[]> arrayizer) {
        int s = subs();
        X[] xx = arrayizer.apply(s);
		for (int i = 0; i < s; i++)
			xx[i] = map.apply(sub(i));
		return xx;
	}

	default int subEventRange(int i) {
		return sub(i).eventRange();
	}

	default @Nullable Term subRoulette(Random rng, FloatFunction<Term> subValue) {
        int s = subs();
		switch (s) {
			case 0:
				return null;
			case 1:
				return sub(0);
			default:
				return sub(Roulette.selectRoulette(s, i -> subValue.floatValueOf(sub(i)), rng));
		}
	}

	default @Nullable Term sub(Random rng) {
        int s = subs();
		switch (s) {
			case 0:
				return null;
			case 1:
				return sub(0);
			default:
				return sub(rng.nextInt(s));
		}
	}

	default Subterms remove(Term event) {
        Term[] t = removing(event);
		return (t == null || t.length == subs()) ? this : Op.terms.subterms(t);
	}

	@Override
	default int structure() {
		return intifyShallow(0, (s, x) -> s | x.structure());
	}

	default void forEachI(ObjectIntProcedure<Term> t) {
        int s = subs();
		for (int i = 0; i < s; i++)
			t.value(sub(i), i);
	}

	default <X> void forEachWith(BiConsumer<Term, X> t, X argConst) {
        int s = subs();
		for (int i = 0; i < s; i++)
			t.accept(sub(i), argConst);
	}

	/**
	 * sorted and deduplicated
	 */
	default Subterms commuted() {
		if (subs() <= 1) return this;
        Term[] x = arrayShared();
        Term[] y = Terms.commute(x);
		return x == y ? this : new TermList(y);
	}

	default boolean isSorted() {
        int s = subs();
		if (s < 2) return true;
        Term p = sub(0);
		for (int i = 1; i < s; i++) {
            Term n = sub(i);
			if (p.compareTo(n) > 0)
				return false;
			p = n;
		}
		return true;
	}

	default boolean isCommuted() {
        int s = subs();
		if (s < 2) return true;
        Term p = sub(0);
		for (int i = 1; i < s; i++) {
            Term n = sub(i);
			if (p.compareTo(n) >= 0)
				return false;
			p = n;
		}
		return true;
	}

	/**
	 * TODO constructing a Stream like this just for the Iterator is not very efficient
	 */
	@Override
	default Iterator<Term> iterator() {
		return IntStream.range(0, subs()).mapToObj(this::sub).iterator();
	}

	default boolean subEquals(int i, /*@NotNull*/ Term x) {
		return subs() > i && sub(i).equals(x);
	}

	default /*@NotNull*/ SortedSet<Term> toSetSorted() {
		SortedSet<nars.term.Term> u = new MetalTreeSet();
		addAllTo(u);
		return u;
	}

	@SuppressWarnings("LambdaUnfriendlyMethodOverload")
	default /*@NotNull*/ SortedSet<Term> toSetSorted(UnaryOperator<Term> map) {
		MetalTreeSet<Term> u = new MetalTreeSet();
		for (Term z : this)
			u.add(map.apply(z));
		return u;
	}


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

	default /*@NotNull*/ SortedSet<Term> toSetSorted(Predicate<Term> t) {
        int s = subs();
		switch (s) {
			case 1:
                Term the = sub(0);
				return t.test(the) ? ArrayUnenforcedSortedSet.the(the) : ArrayUnenforcedSortedSet.empty;
			case 2:
				Term a = sub(0), b = sub(1);
				boolean aok = t.test(a), bok = t.test(b);
				if (aok && bok) {
					return ArrayUnenforcedSortedSet.the(a, b);
				} else if (!aok && !bok) {
					return ArrayUnenforcedSortedSet.empty;
				} else if (!bok) {
					return ArrayUnenforcedSortedSet.the(a);
				} else
					return ArrayUnenforcedSortedSet.the(b);
			default:
				List<Term> u = new FasterList<>(s);
				for (Term x : this) {
					if (t.test(x)) u.add(x);
				}
                int us = u.size();
				if (us == s) {
					if (this instanceof TermVector)
						return ArrayUnenforcedSortedSet.the(this.arrayShared());
				}
				switch (us) {
					case 0:
						return ArrayUnenforcedSortedSet.empty;
					case 1:
						return ArrayUnenforcedSortedSet.the(u.get(0));
					case 2:
						return ArrayUnenforcedSortedSet.the(u.get(0), u.get(1));
					default:
						return ArrayUnenforcedSortedSet.the(u.toArray(Op.EmptyTermArray));
				}
		}

	}

	/**
	 * an array of the subterms, which an implementation may allow
	 * direct access to its internal array which if modified will
	 * lead to disaster. by default, it will call 'toArray' which
	 * guarantees a clone. override with caution
	 */
	default Term[] arrayShared() {
		return arrayClone();
	}

	/**
	 * an array of the subterms
	 * this is meant to be a clone always
	 */
	default Term[] arrayClone() {
        int s = subs();
		return s == 0 ? Op.EmptyTermArray : arrayClone(new Term[s], 0, s);
	}

	default Term[] arrayClone(Term[] target) {
		return arrayClone(target, 0, subs());
	}

	default Term[] arrayClone(Term[] target, int from, int to) {
		for (int i = from, j = 0; i < to; i++, j++)
			target[j] = this.sub(i);

		return target;
	}

	default /*@NotNull*/ TermList toList() {
		return new TermList(this);
	}

	/**
	 * @return a Mutable Set, unless empty
	 */
	default /*@NotNull*/ MutableSet<Term> toSet() {
        int s = subs();
		UnifiedSet<Term> u = new UnifiedSet(s, 0.99f);
		if (s > 0)
		    addAllTo(u);
		return u;
	}

	default @Nullable <C extends Collection<Term>> C collect(Predicate<Term> ifTrue, C c) {
        int s = subs();
		//UnifiedSet<Term> u = null;
		for (int i = 0; i < s; i++) {
			/*@NotNull*/
            Term x = sub(i);
			if (ifTrue.test(x)) {
//                    if (c == null)
//                        c = new UnifiedSet<>((s - i) * 2);
				c.add(x);
			}
		}

		return c;
	}

	/**
	 * by default this does not need to do anything
	 * but implementations can cache the normalization
	 * in a boolean because it only needs done once.
	 */
	default void setNormalized() {

	}

	/**
	 * assume its normalized if no variables are present
	 */
	default boolean isNormalized() {
		return TermMetadata.normalized(this);
	}

	/**
	 * gets the set of unique recursively contained terms of a specific type
	 * TODO generalize to a provided lambda predicate selector
	 */
	/*@NotNull*/
	default Set<Term> recurseSubtermsToSet(Op _onlyType) {
        int onlyType = _onlyType.bit;
		if (!hasAny(onlyType))
			return Sets.mutable.empty();

		Set<Term> t = new HashSet(volume());


		recurseTerms(
			tt -> tt.hasAny(onlyType),
			tt -> {
				if (tt.opBit() == onlyType)
					t.add(tt);
				return true;
			}, null);
		return t;
	}

	/*@NotNull*/
	default boolean recurseSubtermsToSet(int inStructure, /*@NotNull*/ Collection<Term> t, boolean untilAddedORwhileNotRemoved) {
		boolean[] r = {false};
		Predicate<Term> selector = s -> {

			if (!untilAddedORwhileNotRemoved && r[0])
				return false;

			if (s.hasAny(inStructure)) {
				r[0] |= (untilAddedORwhileNotRemoved) ? t.add(s) : t.remove(s);
			}

			return true;
		};


		recurseTerms(
			(inStructure != -1) ?
				p -> p.hasAny(inStructure)
				:
				any -> true,
			selector, null);


		return r[0];
	}

	default boolean containsRecursively(/*@NotNull*/ Term x, boolean root, Predicate<Term> subTermOf) {

		if (!impossibleSubTerm(x)) {
            int s = subs();
			Term prev = null;
			for (int i = 0; i < s; i++) {
                Term ii = sub(i);
				if (different(prev, ii)) {
					if (ii == x ||
						((root ? ii.equalsRoot(x) : ii.equals(x)) || ii.containsRecursively(x, root, subTermOf)))
						return true;
					prev = ii;
				}
			}
		}
		return false;
	}

	default boolean equalTerms(/*@NotNull*/ Subterms c) {
        int s = subs();
		if (s != c.subs())
			return false;
		for (int i = 0; i < s; i++) {
			if (!sub(i).equals(c.sub(i))) {
				return false;
			}
		}
		return true;
	}

	default boolean equalTerms(/*@NotNull*/ Term[] c) {
        int s = subs();
		if (s != c.length)
			return false;
		return ANDi((x,i) -> x.equals(c[i]));
	}

	default void addAllTo(Collection target) {
		for (Term term : this)
			target.add(term);
	}

	default void addAllTo(FasterList target) {
		target.ensureCapacity(subs());
		for (Term term : this)
			target.addFast(term);
	}

	default   boolean impossibleSubStructure(int structure) {
		//return !hasAll(structure);
		return !Op.has(subStructure(), structure, true);
	}

	default int subStructure() {
		assert (!(this instanceof Compound));
		return structure();
	}

	default Term[] terms(/*@NotNull*/ IntObjectPredicate<Term> filter) {
		TermList l = null;
        int s = subs();
		for (int i = 0; i < s; i++) {
            Term t = sub(i);
			if (filter.accept(i, t)) {
				if (l == null)
					l = new TermList(subs() - i);
				l.add(t);
			}
		}
		return l == null ? Op.EmptyTermArray : l.arrayKeep();
	}

	default void forEach(Consumer<? super Term> action, int start, int stop) {
		if (start < 0 || stop > subs())
			throw new ArrayIndexOutOfBoundsException();

		for (int i = start; i < stop; i++)
			action.accept(sub(i));
	}

	@Override
	default void forEach(Consumer<? super Term> action) {
		forEach(action, 0, subs());
	}

	/**
	 * return whether a subterm op at an index is an operator.
	 */
	default boolean subIs(int i, Op o) {
		return sub(i).opID() == (int) o.id;
	}

	/**
	 * counts subterms matching the predicate
	 */
	default int count(Predicate<Term> match) {
		return intifyShallow(0, (c, sub) -> match.test(sub) ? c + 1 : c);
	}

	default boolean countEquals(Predicate<Term> match, int n) {
        int s = subs();
		if (n > s) return false; //impossible
        int c = 0;
		for (int i = 0; i < s; i++) {
			if (match.test(sub(i))) {
				c++;
				if (c > n)
					return false;
			}
		}
		return c == n;
	}

	/**
	 * counts subterms matching the supplied op
	 */
	default int count(Op matchingOp) {
		int matchingOpID = (int) matchingOp.id;
		return count(x -> x.opID() == matchingOpID);
	}

	/**
	 * return whether a subterm op at an index is an operator.
	 * if there is no subterm or the index is out of bounds, returns false.
	 */
	default boolean subIsOrOOB(int i, Op o) {
        Term x = sub(i, null);
		return x != null && x.opID() == (int) o.id;
	}

	/**
	 * first index of; follows normal indexOf() semantics; -1 if not found
	 */
	default   int indexOf(/*@NotNull*/ Term t) {
		return indexOf(t, -1);
	}

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

	default int indexOf(/*@NotNull*/ Term t, int after) {
		return indexOf(t::equals, after);
	}

	/**
	 * return the first subterm matching the predicate, or null if none match
	 */
	default @Nullable Term subFirst(Predicate<Term> match) {
        int i = indexOf(match);
		return i != -1 ? sub(i) : null;
	}

	@Override
	default boolean impossibleSubTerm(Termlike target) {
		return impossibleSubVolume(target.volume()) || impossibleSubStructure(target.structure());
	}

	default boolean impossibleSubTerm(int structure, int volume) {
		return impossibleSubStructure(structure) || impossibleSubVolume(volume);
	}


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
	default Stream<Term> subStream() {
		return Streams.stream(this);
	}

	/**
	 * allows the subterms to hold a different hashcode than hashCode when comparing subterms
	 */
	default int hashCodeSubterms() {
		return Subterms.hash(this);
	}

	/**
	 * TODO write negating version of this that negates only up to subs() bits
	 */
	default MetalBitSet indicesOfBits(Predicate<Term> match) {
        int n = subs();
        MetalBitSet m = MetalBitSet.bits(n);
		for (int i = 0; i < n; i++) {
			if (match.test(sub(i)))
				m.set(i);
		}
		return m;
	}

	default Term[] subsIncluding(Predicate<Term> toKeep) {
		return subsIncExc(indicesOfBits(toKeep), true);
	}

	default Term[] subsIncluding(MetalBitSet toKeep) {
		return subsIncExc(toKeep, true);
	}

	default Term[] removing(MetalBitSet toRemove) {
		return subsIncExc(toRemove, false);
	}

	default @Nullable Term[] subsIncExc(MetalBitSet s, boolean includeOrExclude) {

        int c = s.cardinality();

		if (c == 0) {
//            if (!includeOrExclude)
//                throw new UnsupportedOperationException("should not reach here");
			return includeOrExclude ? Op.EmptyTermArray : arrayShared();
		}

        int size = subs();
		assert (c <= size) : "bitset has extra bits setAt beyond the range of subterms";

		if (includeOrExclude) {
			if (c == size) return arrayShared();
			if (c == 1) return new Term[]{sub(s.first(true))};
		} else {
			if (c == size) return EmptyTermArray;
			if (c == 1) return removing(s.first(true));
		}


        int newSize = includeOrExclude ? c : size - c;
        Term[] t = new Term[newSize];
        int j = 0;
		for (int i = 0; i < size; i++) {
			if (s.get(i) == includeOrExclude)
				t[j++] = sub(i);
		}
		return t;
	}

	/**
	 * match a range of subterms of Y.
	 * WARNING: provides a shared (non-cloned) copy if the entire range is selected
	 */
	/*@NotNull*/
	default Term[] subRangeArray(int from, int to) {
        int n = subs();
		if (from == Integer.MIN_VALUE) from = 0;
		if (to == Integer.MAX_VALUE) to = n;

		if (from == 0 && to == n) {
			return arrayShared();

		} else {

            int s = to - from;
			if (s == 0)
				return EmptyTermArray;
			else {
                Term[] l = new Term[s];
                int y = from;
				for (int i = 0; i < s; i++)
					l[i] = sub(y++);
				return l;
			}
		}
	}

	default int indexOf(/*@NotNull*/ Predicate<Term> p) {
		return indexOf(p, -1);
	}

	default int indexOf(/*@NotNull*/ Predicate<Term> p, int after) {
        int s = subs();
		Term prev = null;
		for (int i = after + 1; i < s; i++) {
            Term next = sub(i);
			if (different(prev, next)) {
				if (p.test(next))
					return i;
				prev = next;
			}
		}
		return -1;
	}

	/**
	 * returns true if evaluates true for all terms
	 * implementations are allowed to skip repeating subterms and visit out-of-order
	 *
	 * @param p
	 */
	default boolean AND(/*@NotNull*/ Predicate<Term> p) {
        int s = subs();
		Term prev = null;
		for (int i = 0; i < s; i++) {
            Term next = sub(i);
			if (different(prev, next)) {
				if (!p.test(next))
					return false;
				prev = next;
			}
		}
		return true;
	}

	/**
	 * returns true if evaluates true for any terms
	 * implementations are allowed to skip repeating subterms and visit out-of-order
	 *
	 * @param p
	 */
	default boolean OR(/*@NotNull*/ Predicate<Term> p) {
        int s = subs();
		Term prev = null;
		for (int i = 0; i < s; i++) {
            Term next = sub(i);
			if (different(prev, next)) {
				if (p.test(next))
					return true;
				prev = next;
			}
		}
		return false;
	}

	/**
	 * supplies the i'th index as 2nd lambda argument. all subterms traversed, incl repeats
	 */
	default boolean ANDi(/*@NotNull*/ ObjectIntPredicate<Term> p) {
        int s = subs();
		for (int i = 0; i < s; i++) {
			if (!p.accept(sub(i), i)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * supplies the i'th index as 2nd lambda argument. all subterms traversed, incl repeats
	 */
	default boolean ORi(/*@NotNull*/ ObjectIntPredicate<Term> p) {
        int s = subs();
		for (int i = 0; i < s; i++) {
			if (p.accept(sub(i), i)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * warning: elides test for repeated subterm
	 */
	default <X> boolean ORwith(/*@NotNull*/ BiPredicate<Term, X> p, X param) {
        int s = subs();
		Term prev = null;
		for (int i = 0; i < s; i++) {
            Term next = sub(i);
			if (different(prev, next)) {
				if (p.test(next, param))
					return true;
				prev = next;
			}
		}
		return false;
	}

	/**
	 * warning: elides test for repeated subterm
	 */
	default <X> boolean ANDwith(/*@NotNull*/ BiPredicate<Term, X> p, X param) {
        int s = subs();
		Term prev = null;
		for (int i = 0; i < s; i++) {
            Term next = sub(i);
			if (different(prev, next)) {
				if (!p.test(next, param))
					return false;
				prev = next;
			}
		}
		return true;
	}

	/**
	 * visits each, incl repeats
	 */
	default <X> boolean ANDwithOrdered(/*@NotNull*/ BiPredicate<Term, X> p, X param) {
        int s = subs();
		for (int i = 0; i < s; i++) {
			if (!p.test(sub(i), param)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * warning: elides test for repeated subterm
	 */
	default boolean ANDrecurse(Predicate<Term> p) {
        int s = subs();
		Term prev = null;
		for (int i = 0; i < s; i++) {
            Term next = sub(i);
			if (different(prev, next)) {
				if (!p.test(next) || (next instanceof Compound && !((Compound) next).ANDrecurse(p)))
					return false;
				prev = next;
			}
		}
		return true;
	}

	/**
	 * warning: elides test for repeated subterm
	 */
	default boolean ORrecurse(Predicate<Term> p) {
        int s = subs();
		Term prev = null;
		for (int i = 0; i < s; i++) {
            Term next = sub(i);
			if (different(prev, next)) {
				if (p.test(next) || (next instanceof Compound && ((Compound) next).ORrecurse(p)))
					return true;
				prev = next;
			}
		}
		return false;
	}

	/**
	 * must be overriden by any Compound subclasses
	 */
	@Override
	default boolean recurseTerms(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, Compound parent) {
		return AND(s -> s.recurseTerms(inSuperCompound, whileTrue, parent));
	}

	/**
	 * must be overriden by any Compound subclasses
	 */
	default boolean recurseTerms(Predicate<Compound> aSuperCompoundMust, BiPredicate<Term, Compound> whileTrue, Compound parent) {
		return AND(s -> s.recurseTerms(aSuperCompoundMust, whileTrue, parent));
	}

	/**
	 * incl repeats
	 */
	default boolean recurseTermsOrdered(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, Compound parent) {
		return AND(i -> i.recurseTermsOrdered(inSuperCompound, whileTrue, parent));
	}

	default Subterms reversed() {
		return RemappedSubterms.reverse(this);
	}

	/**
	 * removes first occurrence only
	 */
	default Term[] removing(int index) {
        int s = subs();
        Term[] x = new Term[s - 1];
        int k = 0;
		for (int j = 0; j < s; j++) {
			if (j != index)
				x[k++] = sub(j);
		}
		return x;

		//return ArrayUtils.remove(arrayShared(), Term[]::new, i);
	}

	default int hashWith(Op op) {
		return hashWith((int) op.id);
	}

	default int hashWith(/*byte*/int op) {
		return Util.hashCombine(this.hashCodeSubterms(), op);
	}

	default @Nullable Term[] removing(Term x) {
        MetalBitSet toRemove = indicesOfBits(x::equals);
		return toRemove.cardinality() == 0 ? null : removing(toRemove);
	}

//    /**
//     * dont override
//     */
//    default Subterms replaceSub(Term from, Term to) {
//        return !from.equals(to) && !impossibleSubTerm(from) ? transformSubs(MapSubst.replace(from, to), ATOM) : this;
//    }

	default Subterms transformSub(int which, UnaryOperator<Term> f) {
        Term x = sub(which);
        Term y = f.apply(x);
		//if (x == y)
		if (x.equals(y))
			return this;

        Term[] yy = arrayClone();
		yy[which] = y;
		return Op.terms.subterms(yy);
	}

	/**
	 * returns 'x' unchanged if no changes were applied,
	 * returns 'y' if changes
	 * returns null if untransformable
	 * <p>
	 * superOp is optional (use ATOM as the super-op to disable its use),
	 * providing a hint about the target operator the subterms is being constructed for
	 * this allows certain fail-fast cases
	 */
	default @Nullable Subterms transformSubs(UnaryOperator<Term> f, Op superOp) {

		TermList y = null;

        int s = subs();


		for (int i = 0; i < s; i++) {

            Term xi = sub(i);

            Term yi = f.apply(xi);

			//if (yi instanceof Bool) {
			if (yi == IdempotentBool.Null)
				return null; //short-circuit
			//}

			if (yi instanceof Fragment) {

                Subterms yy = yi.subterms();
				if (s == 1) {
					return (yy.subs() == 0) ?
						EmptySubterms //the empty ellipsis is the only subterm
						:
						yy.transformSubs(f, superOp); //it is only this ellipsis match so inline it by transforming directly and returning it (tail-call)
				} else {
					y = transformSubInline(yy, f, y, s, i);
					if (y == null)
						return null;
				}


			} else {


				if (y == null) {
					if (differentlyTransformed(xi, yi) /* special */)
						y = new DisposableTermList(s, i);
//                    else Util.nop(); ///why
				}

				if (y != null)
					y.addFast(yi);
			}
		}

		return y != null ? y.commit(this) : this;
	}

	@Override
	default boolean these() {
		return AND(Term::the);
	}

	default boolean containsPosOrNeg(Term x) {
		//TODO optimize
		return contains(x) || containsNeg(x);
	}

	default boolean containsNeg(Term x) {
		return x instanceof Neg ? contains(x.unneg()) : hasAny(NEG) && !impossibleSubTerm(x.structure() | NEG.bit, x.volume() + 1)
			&&
			ORwith((z, xx) -> z instanceof Neg && xx.equals(z.unneg()), x);
	}

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
