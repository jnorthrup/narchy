package nars.subterm;

import com.google.common.base.Joiner;
import com.google.common.io.ByteArrayDataOutput;
import jcog.TODO;
import jcog.Util;
import jcog.data.bit.MetalBitSet;
import jcog.data.byt.DynBytes;
import jcog.data.list.FasterList;
import jcog.data.set.ArrayUnenforcedSortedSet;
import nars.$;
import nars.Op;
import nars.Param;
import nars.subterm.util.DisposableTermList;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.Variable;
import nars.term.atom.Bool;
import nars.term.util.Image;
import nars.term.util.transform.MapSubst;
import nars.term.util.transform.TermTransform;
import nars.unify.Unify;
import nars.unify.match.EllipsisMatch;
import nars.unify.mutate.CommutivePermutations;
import org.eclipse.collections.api.block.predicate.primitive.IntObjectPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static nars.Op.ATOM;


/**
 * Methods common to both Term and Subterms
 * T = subterm type
 */
public interface Subterms extends Termlike, Iterable<Term> {

    interface SubtermsBytesCached {
        void bytes(DynBytes builtWith);
    }

    static int hash(List<Term> term) {
        int h = 1;
        for (Term aTerm : term) h = Util.hashCombine(h, aTerm);
        return h;
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

    /**
     * returns sorted ready for commutive; null if nothing in common
     */
    static @Nullable MutableSet<Term> intersect(/*@NotNull*/ Subterms a, /*@NotNull*/ Subterms b) {
        if ((a.structure() & b.structure()) != 0) {

            Set<Term> as = a.toSet();
            MutableSet<Term> ab = b.toSet(as::contains);
            if (ab != null)
                return ab;
        }
        return null;
    }

    default void forEachWith(ObjectIntProcedure<Term> t) {
        int s = subs();
        for (int i = 0; i < s; i++)
            t.accept(sub(i), i);
    }

    default <X> void forEachWith(BiConsumer<Term, X> t, X argConst) {
        int s = subs();
        for (int i = 0; i < s; i++)
            t.accept(sub(i), argConst);
    }

    /**
     * recursively
     */
    /*@NotNull*/
    static boolean hasCommonSubtermsRecursive(/*@NotNull*/ Term a, /*@NotNull*/ Term b, boolean excludeVariables) {

        Subterms aa = a.subterms();
        Subterms bb = b.subterms();

        int commonStructure = aa.structure() & bb.structure();
        if (excludeVariables)
            commonStructure = commonStructure & ~(Op.Variable);

        if (commonStructure == 0)
            return false;

        Set<Term> scratch = new UnifiedSet<>(4);
        aa.recurseSubtermsToSet(commonStructure, scratch, true);
        return bb.recurseSubtermsToSet(commonStructure, scratch, false);
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

    static String toString(/*@NotNull*/ Iterable<? extends Term> subterms) {
        return '(' + Joiner.on(',').join(subterms) + ')';
    }

    static String toString(/*@NotNull*/ Term... subterms) {
        return '(' + Joiner.on(',').join(subterms) + ')';
    }

    static int compare(/*@NotNull*/ Subterms a, /*@NotNull*/ Subterms b) {

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

    default Op op() {
        return null;
    }

    default boolean isSorted() {
        int s = subs();
        if (s < 2) return true;


        for (int i = 1; i < s; i++) {
            if (sub(i - 1).compareTo(sub(i)) >= 0)
                return false;
        }
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

    /*@NotNull*/
    @Override
    default Iterator<Term> iterator() {
        throw new TODO();
    }


    default boolean subEquals(int i, /*@NotNull*/ Term x) {
        return subs() > i && sub(i).equals(x);
    }


    default /*@NotNull*/ SortedSet<Term> toSetSorted() {
        TreeSet u = new TreeSet();
        forEach(u::add);
        return u;
    }

    default /*@NotNull*/ SortedSet<Term> toSetSorted(Predicate<Term> t) {
        int s = subs();
        if (s == 1) {
            Term the = sub(0);
            if (t.test(the))
                return ArrayUnenforcedSortedSet.the(the);
            else
                return ArrayUnenforcedSortedSet.empty;
        } else if (s == 2) {
            Term a = sub(0);
            Term b = sub(1);
            boolean aok = t.test(a);
            boolean bok = t.test(b);
            if (aok && bok) {
                return ArrayUnenforcedSortedSet.the(a, b);
            } else if (!bok && !bok) {
                return ArrayUnenforcedSortedSet.empty;
            } else if (!bok) {
                return ArrayUnenforcedSortedSet.the(a);
            } else
                return ArrayUnenforcedSortedSet.the(b);
        } else {
            TreeSet<Term> u = new TreeSet<>();
            forEach(x -> {
                if (t.test(x)) u.add(x);
            });
            return u;
        }

    }


    default /*@NotNull*/ TermList toList() {
        TermList u = new TermList(subs());
        forEach(u::add);
        return u;
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
            if (u != null) {

                return u;
            }
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
        return !hasAny(Op.Variable) && !hasAll(Image.ImageBits);
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
//                r |= (addOrRemoved) ? t.add(s) : t.remove(s);
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

    default void addTo(Collection<Term> target) {
        forEach(target::add);
    }

    @Override
    default boolean impossibleSubStructure(int structure) {
        return !hasAll(structure);
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
        List<Term> l = $.newArrayList(subs());
        int s = subs();
        int added = 0;
        for (int i = 0; i < s; i++) {
            Term t = sub(i);
            if (filter.accept(i, t)) {
                l.add(t);
                added++;
            }
        }
        return added == 0 ? Op.EmptyTermArray : l.toArray(new Term[added]);
    }


    default void forEach(Consumer<? super Term> action, int start, int stop) {
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
        if (!impossibleSubTerm(t)) {
            int s = subs();
            for (int i = 0; i < s; i++) {
                if (t.equals(sub(i)))
                    return i;
            }
        }
        return -1;
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


    /**
     * of all the matches to the predicate, chooses one at random and returns its index
     */
    default int indexOf(Predicate<Term> t, Random r) {
        IntArrayList a = indicesOf(t);
        return (a == null) ? -1 :
                a.get(a.size() == 1 ? 0
                        : r.nextInt(a.size()));

    }

    @Nullable
    default IntArrayList indicesOf(Predicate<Term> t) {
        IntArrayList a = null;
        int s = subs();
        for (int i = 0; i < s; i++) {
            if (t.test(sub(i))) {
                if (a == null)
                    a = new IntArrayList(1);
                a.add(i);
            }
        }
        return a;
    }

    /**
     * allows the subterms to hold a different hashcode than hashCode when comparing subterms
     */
    default int hashCodeSubterms() {
        return hashCode();
    }


//    default boolean unifyLinearSimple(Subterms Y, /*@NotNull*/ Unify u) {
//
//
//        int s = subs();
//        for (int i = 0; i < s; i++) {
//            if (!sub(i).unify(Y.sub(i), u))
//                return false;
//        }
//        return true;
//
//    }

    /**
     * const/variable phase version
     */
    default boolean unifyLinear(Subterms Y, /*@NotNull*/ Unify u) {
        int s = subs();


        Term[] deferredPairs = null;
        int dynPairs = 0;
        for (int i = 0; i < s; i++) {
            Term xi = sub(i);
            Term yi = Y.sub(i);

            if (xi == yi)
                continue;

            boolean now = (i == s - 1) || (u.constant(xi) && u.constant(yi));

            if (now) {

                if (!xi.unify(yi, u))
                    return false;
            } else {
                if (deferredPairs == null)
                    deferredPairs = new Term[(s - i - 1) * 2];

                //backwards order
                deferredPairs[dynPairs++] = yi;
                deferredPairs[dynPairs++] = xi;
            }
        }


        if (deferredPairs != null) {

            do {
                if (!deferredPairs[--dynPairs].unify(deferredPairs[--dynPairs], u))
                    return false;
            } while (dynPairs > 0);
        }

        return true;
    }

    /**
     * first layer operator scan
     *
     * @return 0: must unify, -1: impossible, +1: unified already
     */
    private static int possiblyUnifiableWhileEliminatingEqual(TermList xx, TermList yy, Unify u) {
        int xs = 0, ys = 0;

        int xn = xx.size();
        for (int i = 0; i < xn; ) {
            Term xi = xx.get(i);
            if (yy.removeFirst(xi)) {
                xx.removeFast(i);
                xn--;
            } else {
                xs |= xi.op().bit;
                ys |= yy.get(i).op().bit;
                i++;
            }
        }

        assert (xx.size() == yy.size());

        int xxs = xx.size();
        if (xxs == 0)
            return +1; //all eliminated

        else if (!possiblyUnifiable(xs, ys, u.varBits))
            return -1; //first layer has no non-variable commonality, no way to unify

        else return 0;
    }
    /**
     * assume equality, structure commonality, and equal subterm count have been tested
     */
    default boolean unifyCommute(Subterms y, Unify u) {

        TermList xx = toList(), yy = y.toList();

        int i = possiblyUnifiableWhileEliminatingEqual(xx, yy, u);
        switch (i) {
            case -1:
                return false;
            case +1:
                return true;
            default: {
                if (xx.size() == 1) {
                    return xx.get(0).unify(yy.get(0), u);
                } else {
//                    int xs = xx.structure();
//                    if (!u.constant(xs) || !u.constant(yy)) {
//                        if (Terms.commonStructureTest(xs, yy, u)) {
                    if (xx==this || xx.subs()==subs()) {
                        //no change
                        u.termutes.add(new CommutivePermutations(this, y)); //use the original subs
                    } else {
                        u.termutes.add(new CommutivePermutations(xx, yy));
                    }
                    return true;
//                        }
//                    }
//                    return false;
                }
            }
        }

    }

    /**
     * xs and ys must have variable bits masked before calling, and xs!=0 and ys!=0
     */
    public static boolean possiblyUnifiable(int xs, int ys, int varBits) {
        int xxs = xs & (~varBits);
        if (xxs == 0)
            return true; //all var
        int yys = ys & (~varBits);
        if (yys == 0)
            return true; //all var

        return (xxs & yys) != 0; //some non-var match possibility
    }

    static boolean possiblyUnifiable(Subterms xx, Subterms yy, int varBits) {
        int XS = xx.structure();
        int XSc = XS & (~varBits);
        if (XSc == 0)
            return true; //all vars in X
        int YS = yy.structure();
        int YSc = YS & (~varBits);
        if (YSc == 0)
            return true; //all vars in Y
        if (XSc == XS && YSc == YS) {
            //cheap constant case invariant tests
            if (XS!=YS || xx.volume() != yy.volume())
                return false;
        } //TODO else test invariants of specifically the constant subterms (volume, structure, ..)

        if (XS==XSc && YS==YSc)
            return true; //done

        //finer-grained sub-constant test

        int xs = xx.structureConstant(varBits);
        if (xs == 0)
            return true;

        int ys = yy.structureConstant(varBits);
        if (ys == 0)
            return true;
        //int xs = XS;//xx.structureSurface();
//        int xxs = xs & (~varBits);
//        if (xxs == 0)
//            return true; //all var
//        int ys = YS;//yy.structureSurface();
//        int yys = ys & (~varBits);
//        if (yys == 0)
//            return true; //all var
//
        return (xs & ys) != 0; //any constant commonality
        //return true;
    }

    default int structureConstant(int varBits) {
        return intifyShallow((int v, Term z) -> {
            //int zs = z.structure();
            //return !Op.hasAny(zs, varBits) ? (v | zs) : v;
            return (z instanceof Variable && (z.op().bit & varBits)!=0) ? v : (v | z.structure());
        }, 0);
    }


    static boolean possiblyUnifiable(Subterms xx, Subterms yy, Unify u) {
        return possiblyUnifiable(xx, yy, u.varBits);
    }




    /**
     * structure of the first layer (surface) only
     */
    default int structureSurface() {
        return intifyShallow((s, x) -> s | x.op().bit, 0);
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

    default Term[] subsExcept(MetalBitSet toRemove) {
        int numRemoved = toRemove.cardinality();
        assert (numRemoved > 0);
        int size = subs();
        int newSize = size - numRemoved;
        Term[] t = new Term[newSize];
        int j = 0;
        for (int i = 0; i < size; i++) {
            if (!toRemove.get(i))
                t[j++] = sub(i);
        }
        return t.length == 0 ? Op.EmptyTermArray : t;
    }

    default Term[] subsExcept(Collection<Term> except) {
        assert (!except.isEmpty());
        FasterList<Term> fxs = new FasterList<>(subs());
        forEach(t -> {
            if (!except.contains(t))
                fxs.add(t);
        });
        if (fxs.isEmpty())
            return arrayShared();
        else
            return fxs.toArrayRecycled(Term[]::new);
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

            Term[] l = new Term[to - from];

            int y = from;
            for (int i = 0; i < s; i++) {
                l[i] = sub(y++);
            }

            return l;
        }
    }

    /**
     * must be overriden by any Compound subclasses
     */
    default boolean recurseTerms(Predicate<Term> aSuperCompoundMust, Predicate<Term> whileTrue, Term parent) {
        return AND(s -> s.recurseTerms(aSuperCompoundMust, whileTrue, parent));
    }

    /**
     * must be overriden by any Compound subclasses
     */
    default boolean recurseTerms(Predicate<Compound> aSuperCompoundMust, BiPredicate<Term, Compound> whileTrue, Compound parent) {
        return AND(s -> s.recurseTerms(aSuperCompoundMust, whileTrue, parent));
    }

    default Subterms reversed() {
        return subs() > 1 ? new ReverseSubterms(this) : this;
    }

//    default Subterms sorted() {
//        return isSorted() ? this : Op.terms.subterms(Terms.sorted(arrayShared()));
//    }

    /**
     * removes first occurrence only
     */
    default Term[] subsExcept(int index) {
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

    /**
     * only removes the next found item. if this is for use in non-commutive term, you may need to call this repeatedly
     */
    @Nullable
    default Term[] subsExcept(Term x) {
        int index = indexOf(x);
        return (index == -1) ? null : subsExcept(index);
    }

    default void appendTo(ByteArrayDataOutput out) {
        out.writeByte(subs());
        forEachWith(Term::appendTo, out);
    }

    default Subterms replaceSub(Term from, Term to, Op superOp) {
        if (containsRecursively(from)) {
            return transformSubs(MapSubst.replace(from, to), superOp);
        } else {
            return this;
        }
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
    default Subterms transformSubs(TermTransform f, Op superOp) {

        TermList y = null;

        int s = subs();

        for (int i = 0; i < s; i++) {

            Term xi = sub(i);

            Term yi = f.transform(xi);

            if (yi == null || yi == Bool.Null)
                return null;

            //these fail-fast cases must be consistent with the term construction process.
            //TODO add more
            switch (superOp) {
                case CONJ:
                    //cant skip the True yet
                    if (yi == Bool.False)
                        return Op.FalseSubterm;
                    break;
                case IMPL:
                    if (i == 0 && yi == Bool.False)
                        return null;
                    break;
//                case INH:
//                case SIM:
//                    //TODO when on 2nd term, compare with the first term (either from the source subterms, or the target if it was transformed)
//                    //then it can tell if it reduces to True, False etc
//                    break;
            }

            if (yi instanceof EllipsisMatch) {

                EllipsisMatch xe = (EllipsisMatch) yi;
                int xes = xe.subs();

                if (y == null)
                    y = new DisposableTermList(s - 1 + xes /*estimate */, i);
                else
                    y.ensureExtraCapacityExact(xes - 1);

                for (int j = 0; j < xes; j++) {

                    Term k = f.transform(xe.sub(j));

                    if (k == null || k == Bool.Null) {
                        return null;
                    } else if (k instanceof EllipsisMatch) {
                        if (Param.DEBUG)
                            throw new TODO("recursive EllipsisMatch unsupported");
                        else
                            return null;
                    } else {
                        y.addWithoutResizeCheck(k);
                    }
                }


            } else {

                if (xi != yi) {
                    if (y == null)
                        y = new DisposableTermList(s, i);
                }

                if (y != null)
                    y.addWithoutResizeCheck(yi);

            }

        }


        if (y != null) {
            return y.commit(this, superOp);
        } else
            return this;
    }

    default boolean these() {
        return AND(Term::the);
    }

}
