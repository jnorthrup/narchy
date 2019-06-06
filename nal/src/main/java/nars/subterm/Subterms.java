package nars.subterm;

import com.google.common.base.Joiner;
import jcog.TODO;
import jcog.Util;
import jcog.data.bit.MetalBitSet;
import jcog.data.byt.DynBytes;
import jcog.data.list.FasterList;
import jcog.data.set.ArrayUnenforcedSortedSet;
import jcog.util.ArrayUtil;
import nars.NAL;
import nars.Op;
import nars.subterm.util.TermMetadata;
import nars.term.Variable;
import nars.term.*;
import nars.term.atom.Bool;
import nars.term.util.transform.MapSubst;
import nars.unify.Unify;
import nars.unify.mutate.CommutivePermutations;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntObjectPredicate;
import org.eclipse.collections.api.block.predicate.primitive.ObjectIntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
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



    @Override
    default boolean hasXternal() {
        return hasAny(Op.Temporal) && OR(Term::hasXternal);
    }

    default Predicate<Term> containing() {
        switch(subs()) {
            case 0: return (x)->false;
            case 1: return sub(0)::equals;
            default: return this::contains;
        }
    }

    default boolean contains(Term t) {
        return ORwith(Term::equals, t);
    }
    default boolean containsInstance(Term t) {
        return ORwith((u, tt) -> tt == u, t);
    }


    @Nullable default Term subSub(byte[] path) {
        return subSub(0, path.length, path);
    }

    @Nullable default Term subSub(int start, int end, byte[] path) {
        Termlike ptr = this;
        for (int i = start; i < end; i++) {
            byte b = path[i];
            if ((ptr = ptr.subSafe(b)) == Bool.Null)
                return null;
        }
        return ptr != this ? (Term) ptr : null;
    }

    @Nullable default Term subSubUnsafe(int start, int end, byte[] path) {
        Termlike ptr = this;
        for (int i = start; i < end; )
            ptr = ptr.sub(path[i++]);
        return (Term) ptr;
    }

    default boolean containsAll(Subterms ofThese) {
        return this.equals(ofThese) || (subs() >= ofThese.subs() && volume() >= ofThese.volume() && ofThese.AND(this::contains));
    }

    default boolean containsAny(Subterms ofThese) {
        return this.equals(ofThese) || (ofThese.OR(this::contains));
    }

    default <X> X[] array(Function<Term,X> map, IntFunction<X[]> arrayizer) {
        int s = subs();
        X[] xx = arrayizer.apply(s);
        for (int i = 0; i < s; i++) {
            xx[i] = map.apply(sub(i));
        }
        return xx;
    }

    default int subEventRange(int i) {
        return sub(i).eventRange();
    }

    @Override
    default int structure() {
        return intifyShallow((s, x) -> s | x.structure(), 0);
    }




    //    default boolean equalsRoot(Subterms y) {
//        return equals(y) ||
//                (y.hasAny(Op.Temporal) && y.subs() == subs() && y.structure()==structure() && ANDith((x, i)-> x.equalsRoot(y.sub(i))));
//    }

    /** allows a Subterms implementation to accept the byte[] key that was used in constructing it,
     *  allowing it to cache it for fast serialization.  typically it will want to keep:
     *
     *      byte[] cached = builtWith.arrayCopy(1) //skip prefix op byte
     */
    interface SubtermsBytesCached {
        void acceptBytes(DynBytes constructedWith);
    }

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
        return container.intifyShallow(Util::hashCombine, 1);
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
    default void forEachWith(IntObjectToIntFunction<Term> t, int x) {
        int s = subs();
        for (int i = 0; i < s; i++)
            x = t.intValueOf(x, sub(i));
    }

//    /**
//     * recursively
//     */
//    /*@NotNull*/
//    static boolean hasCommonSubtermsRecursive(/*@NotNull*/ Term a, /*@NotNull*/ Term b, boolean excludeVariables) {
//
//        Subterms aa = a.subterms();
//        Subterms bb = b.subterms();
//
//        int commonStructure = aa.structure() & bb.structure();
//        if (excludeVariables)
//            commonStructure = commonStructure & ~(Op.Variable);
//
//        if (commonStructure == 0)
//            return false;
//
//        Set<Term> scratch = new UnifiedSet<>(4);
//        aa.recurseSubtermsToSet(commonStructure, scratch, true);
//        return bb.recurseSubtermsToSet(commonStructure, scratch, false);
//    }

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

    static String toString(/*@NotNull*/ Iterable<? extends Term> subterms) {
        return '(' + Joiner.on(',').join(subterms) + ')';
    }

    static String toString(/*@NotNull*/ Term... subterms) {
        return '(' + Joiner.on(',').join(subterms) + ')';
    }

    static int compare(/*@NotNull*/ Termlike a, /*@NotNull*/ Termlike b) {

        if (a.equals(b)) return 0;

        int s;
        int diff;
        if ((diff = Integer.compare(s = a.subs(), b.subs())) != 0)
            return diff;

        if (s == 1) {
            return a.sub(0).compareTo(b.sub(0));
        } else {

            Term inequalVariableX = null, inequalVariableY = null;

            for (int i = 0; i < s; i++) {
                Term x = a.sub(i);
                Term y = b.sub(i);
                if (x instanceof Variable && y instanceof Variable) {
                    if (inequalVariableX == null && !x.equals(y)) {

                        inequalVariableX = x;
                        inequalVariableY = y;
                    }
                } else {
                    int d = x.compareTo(y);
                    if (d != 0) {
                        return d;
                    }
                }
            }


            return inequalVariableX != null ? inequalVariableX.compareTo(inequalVariableY) : 0;
        }
    }


    /** sorted and deduplicated */
    default Subterms commuted() {
        if (isSorted())
            return this;
        else {
            return new TermList(Terms.commute(arrayShared()));
        }
    }

    default boolean isSorted() {
        int s = subs();
        if (s < 2) return true;
        for (int i = 1; i < s; i++)
            if (sub(i - 1).compareTo(sub(i)) >= 0)
                return false;
        return true;
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

    /** TODO constructing a Stream like this just for the Iterator is not very efficient */
    @Override
    default Iterator<Term> iterator() {
        return IntStream.range(0, subs()).mapToObj(this::sub).iterator();
    }


    default boolean subEquals(int i, /*@NotNull*/ Term x) {
        return subs() > i && sub(i).equals(x);
    }


    default /*@NotNull*/ SortedSet<Term> toSetSorted() {
        TreeSet u = new TreeSet();
        forEach(u::add);
        return u;
    }

    default /*@NotNull*/ SortedSet<Term> toSetSorted(Function<Term,Term> map) {
        TreeSet u = new TreeSet();
        forEach(z -> u.add(map.apply(z)));
        return u;
    }

    default /*@NotNull*/ SortedSet<Term> toSetSorted(Predicate<Term> t) {
        int s = subs();
        if (s == 1) {
            Term the = sub(0);
            return t.test(the) ? ArrayUnenforcedSortedSet.the(the) : ArrayUnenforcedSortedSet.empty;
        } else if (s == 2) {
            Term a = sub(0);
            Term b = sub(1);
            boolean aok = t.test(a);
            boolean bok = t.test(b);
            if (aok && bok) {
                return ArrayUnenforcedSortedSet.the(a, b);
            } else if (!aok && !bok) {
                return ArrayUnenforcedSortedSet.empty;
            } else if (!bok) {
                return ArrayUnenforcedSortedSet.the(a);
            } else
                return ArrayUnenforcedSortedSet.the(b);
        } else {


            List<Term> u = new FasterList<>(s);
            forEach(x -> {
                if (t.test(x)) u.add(x);
            });
            int us = u.size();
            if (us == s) {
                if (this instanceof TermVector)
                    return ArrayUnenforcedSortedSet.the(this.arrayShared());
            }
            switch (us) {
                case 0: return ArrayUnenforcedSortedSet.empty;
                case 1: return ArrayUnenforcedSortedSet.the(u.get(0));
                case 2: return ArrayUnenforcedSortedSet.the(u.get(0), u.get(1));
                default: return ArrayUnenforcedSortedSet.the(u.toArray(Op.EmptyTermArray));
            }
        }

    }


    default /*@NotNull*/ TermList toList() {
        return new TermList(this);
    }

    /**
     * @return a Mutable Set, unless empty
     */
    default /*@NotNull*/ MutableSet<Term> toSet() {
        int s = subs();
        UnifiedSet u = new UnifiedSet(s, 0.99f);
        if (s > 0) {
            forEach(u::add);
        }
        return u;
    }

    default @Nullable MutableSet<Term> toSet(Predicate<Term> ifTrue) {
        int s = subs();
        if (s > 0) {
            UnifiedSet<Term> u = null;
            for (int i = 0; i < s; i++) {
                /*@NotNull*/
                Term x = sub(i);
                if (ifTrue.test(x)) {
                    if (u == null)
                        u = new UnifiedSet<>((s - i) * 2);
                    u.add(x);
                }
            }
            return u;
        }

        return null;


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
    default Set<Term> recurseSubtermsToSet(Op onlyType) {
        if (onlyType != null && !hasAny(onlyType))
            return Sets.mutable.empty();

        Set<Term> t = new HashSet(volume());


        recurseTerms(
                tt -> tt.hasAny(onlyType),
                tt -> {
                    if (tt.op() == onlyType)
                        t.add(tt);
                    return true;
                }, null);
        return t;
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

    /*@NotNull*/
    default boolean recurseSubtermsToSet(int inStructure, /*@NotNull*/ Collection<Term> t, boolean untilAddedORwhileNotRemoved) {
        final boolean[] r = {false};
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


    @Override
    default boolean containsRecursively(/*@NotNull*/ Term x, boolean root, Predicate<Term> subTermOf) {

        if (!impossibleSubTerm(x)) {
            int s = subs();
            for (int i = 0; i < s; i++) {
                Term ii = sub(i);
                if (ii == x || (root ? ii.equalsRoot(x) : ii.equals(x)) || ii.containsRecursively(x, root, subTermOf))
                    return true;
            }
        }
        return false;
    }

    default boolean equalTerms(/*@NotNull*/ Subterms c) {
        int s = subs();
        if (s != c.subs())
            return false;
        for (int i = 0; i < s; i++) {
            if (!sub(i).equals(c.sub(i)))
                return false;
        }
        return true;
    }

    default boolean equalTerms(/*@NotNull*/ Term[] c) {
        int s = subs();
        if (s != c.length)
            return false;
        for (int i = 0; i < s; i++) {
            if (!sub(i).equals(c[i]))
                return false;
        }
        return true;
    }

    default void addAllTo(Collection<Term> target) {
        forEach(target::add);
    }

    default /* final */ boolean impossibleSubStructure(int structure) {
        //return !hasAll(structure);
        return !Op.has(structureSub(), structure, true);
    }

    default int structureSub() {
        assert(!(this instanceof Compound));
        return structure();
    }

//
//    /**
//     * if subterms are already sorted, returns arrayShared().
//     * otherwise a sorted clone is returned.
//     */
//    default Term[] arraySharedSorted(boolean dedup) {
//        Term[] aa = arrayShared();
//        if (dedup) {
//            Term[] ss = Terms.sorted();
//            if (ss == aa)
//                return aa;
//            else
//                return ss;
//        } else {
//            if (Util.isSorted(aa))
//                return aa;
//            else {
//                Term[] ss = aa.clone();
//                Arrays.sort(ss);
//                return ss;
//            }
//        }
//    }


    /*@NotNull*/
    default Term[] terms(/*@NotNull*/ IntObjectPredicate<Term> filter) {
        TermList l = new TermList(subs());
        int s = subs();
        int added = 0;
        for (int i = 0; i < s; i++) {
            Term t = sub(i);
            if (filter.accept(i, t)) {
                l.add(t);
                added++;
            }
        }
        return added == 0 ? Op.EmptyTermArray : l.arrayKeep();
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
     * first index of; follows normal indexOf() semantics; -1 if not found
     */
    default int indexOf(/*@NotNull*/ Term t) {
        return first(t::equals);
    }

    default int indexOf(/*@NotNull*/ Term t, int after) {

        int s = subs();
        int i = after + 1;
        if (i >= s)
            return -1;

        for (; i < s; i++) {
            if (t.equals(sub(i)))
                return i;
        }

        return -1;
    }
    default boolean impossibleSubTerm(Termlike target) {
        return impossibleSubStructure(target.structure()) || impossibleSubVolume(target.volume());
    }


//    /**
//     * if it's larger than this target it can not be equal to this.
//     * if it's larger than some number less than that, it can't be a subterm.
//     */
//    default boolean impossibleSubTermOrEqualityVolume(int otherTermsVolume) {
//        return otherTermsVolume > volume();
//    }

//    default boolean impossibleSubTermOrEquality(/*@NotNull*/Term target) {
//        return ((!hasAll(target.structure())) ||
//                (impossibleSubTermOrEqualityVolume(target.volume())));
//    }


    /**
     * stream of each subterm
     */
    default Stream<Term> subStream() {
        int subs = subs();
        switch (subs) {
            case 0:
                return Stream.empty();
            case 1:
                return Stream.of(sub(0));
            case 2:
                return Stream.of(sub(0), sub(1));
            case 3:
                return Stream.of(sub(0), sub(1), sub(2));
            default:
                return IntStream.range(0, subs).mapToObj(this::sub);
        }
    }

    /**
     * of all the matches to the predicate, chooses one at random and returns its index
     */
    default int indexOf(Predicate<Term> t, Random r) {
        int[] a = indicesOf(t);
        int l = a.length;
        return (l == 0) ? -1 :
                a[l == 1 ? 0 : r.nextInt(l)];

    }

    @Nullable
    default int[] indicesOf(Predicate<Term> t) {
        IntArrayList a = new IntArrayList(1);
        int s = subs();
        for (int i = 0; i < s; i++) {
            if (t.test(sub(i))) {
                a.add(i);
            }
        }
        return a.isEmpty() ? ArrayUtil.EMPTY_INT_ARRAY : a.toArray();
    }

    /**
     * allows the subterms to hold a different hashcode than hashCode when comparing subterms
     */
    default int hashCodeSubterms() {
        return Subterms.hash(this);
    }

    static boolean unifyLinear(Subterms x, Subterms y, Unify u) {
        int n;
        switch(n = x.subs()) {
            case 0:
                return true;
            case 1:
                return x.sub(0).unify(y.sub(0), u);
            case 2:
                return unifyLinear2_complexityHeuristic(x, y, u);
            default:
                return unifyLinearN_TwoPhase(x, y, n, u);
        }
    }

    static boolean unifyLinear2_complexityHeuristic(Subterms x, Subterms y, Unify u) {
        Term x0 = x.sub(0), x1 = x.sub(1);
        Term y0 = y.sub(0), y1 = y.sub(1);
        boolean xv = u.var(x0), yv = u.var(x1);
        boolean forward;
        if (xv == yv) {
            if (!xv) {
                boolean dx = !u.var(y0), dy = !u.var(y1);
                if (dx && dy)
                    forward = choose(1f/y0.volume(), 1f/y1.volume(), u);
                else
                    forward = dx;
            } else {
                forward = choose(
                        1f/(x0.voluplexity() + y0.voluplexity()),
                        1f/(x1.voluplexity() + y1.voluplexity()),
                        u);
            }
        } else {
            forward = xv;
        }

        return forward ?
                x0.unify(y0, u) && x1.unify(y1, u) :
                x1.unify(y1, u) && x0.unify(y0, u);
    }


    static boolean unifyLinearN_Simple(Subterms x, Subterms y, /*@NotNull*/ Unify u) {
        int s = x.subs();
        for (int i = 0; i < s; i++) {
            if (!x.sub(i).unify(y.sub(i), u))
                return false;
        }
        return true;
    }

    static boolean unifyLinearN_TwoPhase(Subterms x, Subterms y, int n, Unify u) {
        Term[] p = null;
        int dynPairs = 0;
        for (int i = 0; i < n; i++) {
            Term xi = x.sub(i);
            Term yi = y.sub(i);

            if (xi == yi)
                continue;

            boolean now = (i == n - 1) || ((u.var(xi) && u.var(yi)));

            if (now) {

                if (!xi.unify(yi, u))
                    return false;
            } else {
                if (p == null)
                    p = new Term[(n - i - 1) * 2];

                //backwards order
                p[dynPairs++] = yi;
                p[dynPairs++] = xi;
            }
        }


        if (p != null) {
            int pairs = dynPairs/2;
            if (pairs == 1) {
                return p[1].unify(p[0], u);
            } else {

                //TODO sort deferredPairs so that smaller non-commutive subterms are tried first
                if (pairs ==2 ) {
                    boolean forward = choose(1f/(p[0].voluplexity() + p[1].voluplexity()), 1f/(p[2].voluplexity() + p[3].voluplexity()), u
                    );

                    if (forward) {
                        return p[1].unify(p[0], u) && p[3].unify(p[2], u);
                    } else {
                        return p[3].unify(p[2], u) && p[1].unify(p[0], u);
                    }
                } else {

                    do {
                        if (!p[--dynPairs].unify(p[--dynPairs], u))
                            return false;
                    } while (dynPairs > 0);
                }
            }
        }

        return true;
    }

    static boolean choose(float forwardWeight, float reverseWeight, Unify u) {
        return u.random.nextFloat() < (forwardWeight /(forwardWeight + reverseWeight));

//                    if (v01 < v23) {
//                        //try 01 first
//                        forward = true;
//                    } else if (v01 > v23) {
//                        forward = false;
//                    } else {
//                        forward = u.random.nextBoolean();
//                    }
    }

    /**
     * first layer operator scan
     * TODO check for obvious constant target mismatch
     * @return 0: must unify, -1: impossible, +1: unified already
     */
    private static int possiblyUnifiableWhileEliminatingEqualAndConstants(TermList xx, TermList yy, Unify u) {

        int n = xx.size(); assert(yy.size()==n);




        for (int i = 0; i < n; ) {
            Term xi = u.resolvePosNeg(xx.get(i));
            if (yy.removeFirst(xi)) {
                xx.removeFast(i);
                n--;
            } else {
                i++;
            }
        }

        int xxs = xx.size();
        if (xxs == 0)
            return +1; //all eliminated


        if (xxs == n || possiblyUnifiable(xx, yy, u.varBits)) {
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

            return 0;
        } else
            return -1; //first layer has no non-variable commonality, no way to unify

    }


    /**
     * assumes that equality, structure commonality, and equal subterm count have been tested
     */
    static boolean unifyCommute(Subterms x, Subterms y, Unify u) {
        TermList xx = x.toList(), yy = y.toList();

        int i = possiblyUnifiableWhileEliminatingEqualAndConstants(xx, yy, u);
        switch (i) {
            case -1:
                return false;
            case +1:
                return true;
            default: {
                if (xx.size() == 1) {
                    return xx.getFirstFast().unify(yy.getFirstFast(), u);
                } else {
                    if (xx==x || xx.subs()==x.subs()) {
                        u.termutes.add(new CommutivePermutations(x, y)); //use the original subs since nothing was removed from either
                    } else {
                        u.termutes.add(new CommutivePermutations(xx, yy));
                    }
                    return true;
                }
            }
        }

    }


    static boolean possiblyUnifiable(Termlike xx, Termlike yy, int varBits) {
        int XS = xx.structure();
        int YS = yy.structure();
        int XSc = XS & (~varBits);
        if (XSc == 0)
            return true; //X contains only vars
        int YSc = YS & (~varBits);
        if (YSc == 0)
            return true; //Y contains only vars

        if (XSc == XS && YSc == YS) {
            //no variables:
            //cheap constant case invariant tests

            //differing constant structure (excluding CONJ)
            if (XS != YS || ((XS & Op.CONJ.bit) == 0) && xx.volume()!=yy.volume())
                return false;

            return ((XS & Op.Temporal) != 0) /*&& ((YS & Op.Temporal) == 0)*/ || xx.equals(yy); //constant, inequal

//            //if volume differs (and no recursive conjunction subterms)
//            if ((xx.volume() != yy.volume()) &&
//                    (((XS & CONJ.bit) == 0) || !xx.hasXternal()) &&
//                    (((YS & CONJ.bit) == 0) || !yy.hasXternal())
//            )
//                return false;
        }

        return true;

        //finer-grained sub-constant test

//        int xs = xx.structureConstant(varBits);
//        int ys = yy.structureConstant(varBits);
//        return (xs & ys) != 0; //any constant subterm commonality
    }


    static boolean possiblyUnifiable(Subterms xx, Subterms yy, Unify u) {
        return possiblyUnifiable(xx, yy, u.varBits);
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

    /** TODO write negating version of this that negates only up to subs() bits */
    default MetalBitSet indicesOfBits(Predicate<Term> match) {
        int n = subs();
        MetalBitSet m = MetalBitSet.bits(n);
        for (int i = 0; i < n; i++) {
            if (match.test(sub(i)))
                m.set(i);
        }
        return m;
    }


    default Term[] subsIncluding(MetalBitSet toKeep) {
        return subsIncExc(toKeep, true);
    }
    default Term[] removing(MetalBitSet toRemove) {
        return subsIncExc(toRemove, false);
    }
    default Term[] subsIncExc(MetalBitSet s, boolean includeOrExclude) {

        int c = s.cardinality();

        if (c == 0) return includeOrExclude ? Op.EmptyTermArray : arrayShared();

        int size = subs();
        assert(c <= size): "bitset has extra bits setAt beyond the range of subterms";

        if (includeOrExclude) {
            if (c == size) return arrayShared();
            if (c == 1) return new Term[] { sub(s.first(true))};
        } else {
            if (c == size) return EmptyTermArray;
            if (c == 1) return removing(s.first(true));
        }


        int newSize = includeOrExclude ? c : size - c;
        Term[] t = new Term[newSize];
        int j = 0;
        for (int i = 0; i < size; i++) {
            if (s.get(i)==includeOrExclude)
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
        if (from == 0 && to == subs()) {
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
            if (prev!=next && p.test(next))
                return true;
            prev = next;
        }
        return false;
    }

    default int first(/*@NotNull*/ Predicate<Term> p) {
        int s = subs();
        Term prev = null;
        for (int i = 0; i < s; i++) {
            Term next = sub(i);
            if (prev!=next && p.test(next))
                return i;
            prev = next;
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
            if (prev!=next && !p.test(next))
                return false;
            prev = next;
        }
        return true;
    }

    /** supplies the i'th index as 2nd lambda argument. all subterms traversed, even repeats */
    default boolean ANDith(/*@NotNull*/ ObjectIntPredicate<Term> p) {
        int s = subs();
        for (int i = 0; i < s; i++)
            if (!p.accept(sub(i), i))
                return false;
        return true;
    }

    /** supplies the i'th index as 2nd lambda argument. all subterms traversed, even repeats */
    default boolean ORith(/*@NotNull*/ ObjectIntPredicate<Term> p) {
        int s = subs();
        for (int i = 0; i < s; i++)
            if (p.accept(sub(i), i))
                return true;
        return false;
    }

    /** warning: elides test for repeated subterm */
    default <X> boolean ORwith(/*@NotNull*/ BiPredicate<Term,X> p, X param) {
        int s = subs();
        Term prev = null;
        for (int i = 0; i < s; i++) {
            Term next = sub(i);
            if (prev!=next && p.test(next, param))
                return true;
            prev = next;
        }
        return false;
    }
    /** warning: elides test for repeated subterm */
    default <X> boolean ANDwith(/*@NotNull*/ BiPredicate<Term,X> p, X param) {
        int s = subs();
        Term prev = null;
        for (int i = 0; i < s; i++) {
            Term next = sub(i);
            if (prev!=next && !p.test(next, param))
                return false;
            prev = next;
        }
        return true;
    }
    /** visits each, does not deduplciate or elide in case of repeat */
    default <X> boolean ANDwithOrdered(/*@NotNull*/ BiPredicate<Term,X> p, X param) {
        int s = subs();
        for (int i = 0; i < s; i++) {
            Term ii = sub(i);
            if (!p.test(ii, param))
                return false;
        }
        return true;
    }

    /** implementations are allowed to skip repeating subterms and visit out of order */
    default boolean ANDrecurse(Predicate<Term> p) {
        int s = subs();
        Term prev = null;
        for (int i = 0; i < s; i++) {
            Term next = sub(i);
            if (prev!=next && !next.ANDrecurse(p))
                return false;
            prev = next;
        }
        return true;
    }

    /** implementations are allowed to skip repeating subterms and visit out of order */
    default boolean ORrecurse(Predicate<Term> p) {
        int s = subs();
        Term prev = null;
        for (int i = 0; i < s; i++) {
            Term next = sub(i);
            if (prev!=next && next.ORrecurse(p))
                return true;
            prev = next;
        }
        return false;
    }




    /**
     * must be overriden by any Compound subclasses
     */
    @Override default boolean recurseTerms(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, Compound parent) {
        return AND(s -> s.recurseTerms(inSuperCompound, whileTrue, parent));
    }

    /**
     * must be overriden by any Compound subclasses
     */
    default boolean recurseTerms(Predicate<Compound> aSuperCompoundMust, BiPredicate<Term, Compound> whileTrue, Compound parent) {
        return AND(s -> s.recurseTerms(aSuperCompoundMust, whileTrue, parent));
    }

    default boolean recurseTermsOrdered(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, Compound parent) {
        int s = subs();
        for (int i = 0; i < s; i++)
            if (!sub(i).recurseTermsOrdered(inSuperCompound, whileTrue, parent))
                return false;
        return true;
    }

    default Subterms reversed() {
        return subs() > 1 ? RemappedSubterms.reverse(this) : this;
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
        return hashWith(op.id);
    }

    default int hashWith(byte op) {
        return Util.hashCombine(this.hashCodeSubterms(), op);
    }

    @Nullable
    default Term[] removing(Term x) {
        return removing(indicesOfBits(x::equals));
    }

    default Subterms replaceSub(Term from, Term to, Op superOp) {
        if (containsRecursively(from)) {
            return transformSubs(MapSubst.replace(from, to), superOp);
        } else {
            return this;
        }
    }

    default Subterms transformSub(int which, Function<Term,Term> f) {
        Term x = sub(which);
        Term y = f.apply(x);
        if (x!=y) {
            Term[] xx = arrayClone();
            xx[which] = y;
            return Op.terms.subterms(xx);
        }
        else
            return this;
    }

    /**
     * dont override
     */
    default Subterms replaceSub(Term from, Term to) {
        return replaceSub(from, to, ATOM);
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
    @Nullable default Subterms transformSubs(Function<Term,Term> f, Op superOp) {

        TermList y = null;

        int s = subs();

        for (int i = 0; i < s; i++) {

            Term xi = sub(i);

            Term yi = f.apply(xi);

            if (yi == null || yi == Bool.Null)
                return null;

            //these fail-fast cases must be consistent with the target construction process.
            if (yi == Bool.False && superOp == CONJ)
                return Op.FalseSubterm;

            if (yi.op() == FRAG) {

                Subterms ee = yi.subterms();
                if (ee.subs()==0) {
                    if (s == 1)
                        return EmptySubterms; //the empty ellipsis is the only subterm
                }

                if (s == 1) {
                    //it is only this ellipsis match so inline it by transforming directly and returning it (tail-call)
                    return ee.transformSubs(f, superOp);
                } else {
                    y = transformSubInline(ee, f, y, s, i);
                    if (y == null)
                        return null;
                }


            } else {

                if (xi != yi) {
                    if (y == null)
                        y = new DisposableTermList(s, i);
                }

                if (y != null)
                    y.addFast(yi);

            }
        }

        return y != null ? y.commit(this, superOp) : this;
    }

    static TermList transformSubInline(Subterms e, Function<Term,Term> f, TermList out, int subsTotal, int i) {
        int xes = e.subs();


        if (out == null)
            out = new DisposableTermList(subsTotal - 1 + xes /*estimate */, i);
        else
            out.ensureExtraCapacityExact(xes - 1);

        for (int j = 0; j < xes; j++) {

            Term k = f.apply(e.sub(j));

            if (k == null || k == Bool.Null) {
                return null;
            } else if (k.op()==FRAG) {
                if (NAL.DEBUG)
                    throw new TODO("recursive EllipsisMatch unsupported");
                else
                    return null;
            } else {
                out.addFast(k);
            }
        }
        return out;
    }

    @Override default boolean these() {
        return AND(Term::the);
    }

}
