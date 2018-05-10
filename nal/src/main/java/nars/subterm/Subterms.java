package nars.subterm;

import com.google.common.base.Joiner;
import com.google.common.io.ByteArrayDataOutput;
import jcog.TODO;
import jcog.Util;
import jcog.data.bit.MetalBitSet;
import jcog.list.FasterList;
import nars.$;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.Terms;
import nars.term.var.Variable;
import nars.unify.Unify;
import nars.unify.mutate.CommutivePermutations;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.predicate.primitive.IntObjectPredicate;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.RoaringBitmap;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;




/**
 * Methods common to both Term and Subterms
 * T = subterm type
 */
public interface Subterms extends Termlike, Iterable<Term> {

    /*@NotNull*/ TermVector Empty = new ArrayTermVector(Term.EmptyArray);


    static int hash(List<Term> term) {
        int n = term.size();
        int h = 1;
        for (int i = 0; i < n; i++)
            h = Util.hashCombine(h, term.get(i).hashCode());
        return h;
    }

    static int hash(Subterms container) {
//        int h = 1;
//        int s = container.subs();
//        for (int i = 0; i < s; i++) {
//            h = Util.hashCombine(h, container.sub(i).hashCode());
//        }
//        return h;
//
        return container.intifyShallow((h, x) -> Util.hashCombine(h, x.hashCode()), 1);
    }



//    //TODO optionally allow atomic structure positions to differ
//    default boolean equivalentStructures() {
//        int t0Struct = sub(0).structure();
//        int s = subs();
//        for (int i = 1; i < s; i++) {
//            if (sub(i).structure() != t0Struct)
//                return false;
//        }
//
//        ByteList structureKey = sub(0).structureKey();
//        ByteArrayList reuseKey = new ByteArrayList(structureKey.size());
//        for (int i = 1; i < s; i++) {
//            //all subterms must share the same structure
//            //TODO only needs to construct the key while comparing equality with the first
//            if (!sub(i).structureKey(reuseKey).equals(structureKey))
//                return false;
//            reuseKey.clear();
//        }
//        return true;
//    }


    /*@NotNull*/
    @Override
    default Iterator<Term> iterator() {
        throw new TODO();
    }


    @Override
    default Term sub(int i, Term ifOutOfBounds) {
        return subs() <= i ? ifOutOfBounds : sub(i);
    }

    default boolean subEquals(int i, /*@NotNull*/ Term x) {
        return subs() > i && sub(i).equals(x);
    }


//    @Override
//    default int subCount(Op o) {
//        if (!hasAll(o.bit))
//            return 0; //structure doesnt contain that op
//
//        switch (o) {
//            case VAR_DEP:
//                return varDep();
//            case VAR_INDEP:
//                return varIndep();
//            case VAR_QUERY:
//                return varQuery();
//            case VAR_PATTERN:
//                return varPattern();
//        }
//        return intValue(0, (sum, x) -> {
//            return (x.op() == o) ? (sum + 1) : sum;
//        });
//    }


    default /*@NotNull*/ SortedSet<Term> toSetSorted() {
        TreeSet u = new TreeSet();
        forEach(u::add);
        return u;
    }

    /**
     * @return a Mutable Set, unless empty
     */
    default /*@NotNull*/ MutableSet<Term> toSet() {
        int s = subs();
        UnifiedSet u = new UnifiedSet(s);
        if (s > 0) {
            forEach(u::add);
            //u.trimToSize();
        }
        return u;

        //        return new DirectArrayUnenforcedSet<Term>(Terms.sorted(toArray())) {
//            @Override
//            public boolean removeIf(Predicate<? super Term> filter) {
//
//                return false;
//            }
//        };
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
                        u = new UnifiedSet<>(s-i);
                    u.add(x);
                }
            }
            if (u!=null) {
                //u.trimToSize();
                return u;
            }
        }

        return null;

//        return new DirectArrayUnenforcedSet<Term>(Terms.sorted(toArray())) {
//            @Override
//            public boolean removeIf(Predicate<? super Term> filter) {
//
//                return false;
//            }
//        };
    }

    /** by default this does not need to do anything
     * but implementations can cache the normalization
     * in a boolean because it only needs done once.
      */
    default void setNormalized() {

    }

    /** assume its normalized if no variables are present */
    default boolean isNormalized() {
        return !hasVars() && !hasAny(Op.VAR_PATTERN);
    }

    /**
     * returns sorted ready for commutive; null if nothing in common
     */
    static @Nullable MutableSet<Term> intersect(/*@NotNull*/ Subterms a, /*@NotNull*/ Subterms b) {
        if ((a.structure() & b.structure()) > 0) {
            //TODO sort a and b so that less comparisons are made (ie. if b is smaller than a, compute a.toSet() first
            Set<Term> as = a.toSet();
            MutableSet<Term> ab = b.toSet(as::contains); //(MutableSet<Term>) as Sets.intersect(a.toSet(), b.toSet());
            if (ab!=null)
                return ab;
        }
        return null;
    }

//    Predicate2<Object, SetIterable> subtermIsCommon = (Object yy, SetIterable xx) -> {
//        return xx.contains(yy);
//    };
//    Predicate2<Object, SetIterable> nonVarSubtermIsCommon = (Object yy, SetIterable xx) -> {
//        return yy instanceof Variable ? false : xx.contains(yy);
//    };

    /**
     * recursively
     */
    /*@NotNull*/
    static boolean hasCommonSubtermsRecursive(/*@NotNull*/ Term a, /*@NotNull*/ Term b, boolean excludeVariables) {

        Subterms aa = a.subterms();
        Subterms bb = b.subterms();

        int commonStructure = aa.structure() & bb.structure();
        if (excludeVariables)
            commonStructure = commonStructure & ~(Op.VariableBits); //mask by variable bits since we do not want them

        if (commonStructure == 0)
            return false;

        Set<Term> scratch = new HashSet(/*a.size() + b.size()*/);
        aa.recurseTermsToSet(commonStructure, scratch, true);
        return bb.recurseTermsToSet(commonStructure, scratch, false);
    }


    /**
     * gets the set of unique recursively contained terms of a specific type
     * TODO generalize to a provided lambda predicate selector
     */
    /*@NotNull*/
    default Set<Term> recurseTermsToSet(Op onlyType) {
        if (onlyType != null && !hasAny(onlyType))
            return Sets.mutable.empty();

        Set<Term> t = new HashSet(volume());//$.newHashSet(volume() /* estimate */);

        //TODO use an additional predicate to cull subterms which don't contain the target type
        recurseTerms(
            tt->tt.hasAny(onlyType),
            tt -> {
                if (tt.op() == onlyType) //TODO make recurseTerms by Op then it can navigate to subterms using structure hash
                    t.add(tt);
                return true;
            }, null);
        return t;
    }


    /**
     * returns whether the set operation caused a change or not
     */
    /*@NotNull*/
    default boolean termsToSet(int inStructure, /*@NotNull*/ Collection<Term> t, boolean addOrRemoved) {
        boolean r = false;

        int l = subs();
        for (int i = 0; i < l; i++) {
            /*@NotNull*/
            Term s = sub(i);
            if (inStructure == -1 || ((s.structure() & inStructure) > 0)) {
                r |= (addOrRemoved) ? t.add(s) : t.remove(s);
                if (!addOrRemoved && r) //on removal we can exit early
                    return true;
            }
        }
        return r;
    }

    /*@NotNull*/
    default boolean recurseTermsToSet(int inStructure, /*@NotNull*/ Collection<Term> t, boolean addOrRemoved) {
        final boolean[] r = {false};
        Predicate<Term> selector = s -> {

            if (!addOrRemoved && r[0]) //on removal we can exit early
                return false;

            if (inStructure == -1 || ((s.structure() & inStructure) > 0)) {
                r[0] |= (addOrRemoved) ? t.add(s) : t.remove(s);
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
    default boolean containsRecursively(/*@NotNull*/ Term y, boolean root, Predicate<Term> subTermOf) {

        if (!impossibleSubTerm(y)) {
            int s = subs();
            for (int i = 0; i < s; i++) {
                Term x = sub(i);
                if (x == y || (root ? x.equalsRoot(y) : x.equals(y)) || x.containsRecursively(y, root, subTermOf))
                    return true;
            }
        }
        return false;
    }

    /*@NotNull*/
    static boolean commonSubterms(/*@NotNull*/ Compound a, /*@NotNull*/ Compound b, boolean excludeVariables) {

        int commonStructure = a.structure() & b.structure();
        if (excludeVariables)
            commonStructure = commonStructure & ~(Op.VariableBits); //mask by variable bits since we do not want them

        if (commonStructure == 0)
            return false;

        Set<Term> scratch = new HashSet(a.subs());
        a.termsToSet(commonStructure, scratch, true);
        return b.termsToSet(commonStructure, scratch, false);

    }


    //    /**
//     * scans first level only, not recursive
//     */
//    default boolean contains(Object o) {
//        return o instanceof Term && containsTerm((Term) o);
//    }


//    static boolean equals(/*@NotNull*/ TermContainer a, Object b) {
//        return b instanceof TermContainer && TermContainer.equals(a, (TermContainer)b);
//    }


//    boolean equalTerms(/*@NotNull*/ TermContainer c);
//    default boolean equalTerms(/*@NotNull*/ TermContainer c) {
//        int s = size();
//        if (s !=c.size())
//            return false;
//        for (int i = 0; i < s; i++) {
//            if (!sub(i).equals(c.sub(i))) {
//                sub(i).equals(c.sub(i));
//                return false;
//            }
//        }
//        return true;
//    }

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

    default void copyInto(Collection<Term> target) {
        forEach(target::add);
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
        switch (s) {
            case 0:
                return Term.EmptyArray;
            case 1:
                return new Term[]{sub(0)};
            case 2:
                return new Term[]{sub(0), sub(1)};
            default:
                return arrayClone(new Term[s], 0, s);
        }
    }

    default Term[] arrayClone(Term[] x, int from, int to) {

//        if (s == 0)
//            return Term.EmptyArray;
//
//        if (x == null || x.length!=s)
//            x = new Term[s];

        for (int i = from, j = 0; i < to; i++, j++)
            x[j] = this.sub(i);

        return x;
    }

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
        return added == 0 ? Term.EmptyArray : l.toArray(new Term[added]);
    }


    default void forEach(Consumer<? super Term> action, int start, int stop) {
        for (int i = start; i < stop; i++)
            action.accept(sub(i));
    }

    @Override
    default void forEach(Consumer<? super Term> action) {
        forEach(action, 0, subs());
    }


//    static Term[] copyByIndex(TermContainer c) {
//        int s = c.size();
//        Term[] x = new Term[s];
//        for (int i = 0; i < s; i++) {
//            x[i] = c.term(i);
//        }
//        return x;
//    }


    static String toString(/*@NotNull*/ Iterable<? extends Term> subterms) {
        return '(' + Joiner.on(',').join(subterms) + ')';
    }

//    /**
//     * extract a sublist of terms as an array
//     */
//    /*@NotNull*/
//    default Term[] terms(int start, int end) {
//        //TODO for TermVector, create an Array copy directly
//        //TODO for TermVector, if (start == 0) && end == just return its array
//
//        Term[] t = new Term[end - start];
//        int j = 0;
//        for (int i = start; i < end; i++)
//            t[j++] = sub(i);
//        return t;
//    }

    /**
     * follows normal indexOf() semantics; -1 if not found
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
        IntArrayList a = null; //lazily constructed
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


//    /** writes subterm bytes, including any attached metadata preceding or following it */
//    default void appendSubtermBytes(ByteBuf b) {
//
//        int n = size();
//
//        for (int i = 0; i < n; i++) {
//            Term t = term(i);
//
//            if (i != 0) {
//                b.add(ARGUMENT_SEPARATORbyte);
//            }
//
//            try {
//                byte[] bb = t.bytes();
//                if (bb.length!=t.bytesLength())
//                    System.err.println("wtf");
//                b.add(bb);
//            }
//            catch (ArrayIndexOutOfBoundsException a) {
//                System.err.println("Wtf");
//            }
//        }
//
//    }

//    @Override
//    default boolean containsTermRecursively(Term target) {
//        if (impossibleSubterm(target))
//            return false;
//
//        for (Term x : terms()) {
//            if (x.equals(target)) return true;
//            if (x instanceof Compound) {
//                if (x.containsTermRecursively(target)) {
//                    return true;
//                }
//            }
//        }
//        return false;
//
//    }

//    default boolean equivalent(/*@NotNull*/ List<Term> sub) {
//        int s = size();
//        if (s == sub.size()) {
//            for (int i = 0; i < s; i++) {
//                if (!term(i).equals(sub.get(i)))
//                    return false;
//            }
//            return true;
//        }
//        return false;
//    }
//
//    default boolean equivalent(/*@NotNull*/ Term[] sub) {
//        int s = size();
//        if (s == sub.length) {
//            for (int i = 0; i < s; i++) {
//                if (!term(i).equals(sub[i]))
//                    return false;
//            }
//            return true;
//        }
//        return false;
//    }


    /**
     * allows the subterms to hold a different hashcode than hashCode when comparing subterms
     */
    default int hashCodeSubterms() {
        return hashCode();
    }

    @Override
    default boolean hasVarQuery() {
        return hasAny(Op.VAR_QUERY);
    }

    @Override
    default boolean hasVarDep() {
        return hasAny(Op.VAR_DEP);
    }

    @Override
    default boolean hasVarIndep() {
        return hasAny(Op.VAR_INDEP);
    }

//    default int count(/*@NotNull*/ Predicate<Term> match) {
//        int s = subs();
//        int count = 0;
//        for (int i = 0; i < s; i++) {
//            if (match.test(sub(i))) {
//                count++;
//            }
//        }
//        return count;
//    }


    default boolean isTemporal() {
        return hasAny(Op.Temporal) && OR(Term::isTemporal);
    }


//    default int compareTo(/*@NotNull*/ Termlike o) {
//        return compareTo(this, o);
//    }

    static int compare(/*@NotNull*/ Subterms a, /*@NotNull*/ Subterms b) {

        if (a.equals(b)) return 0;

        int s;
        int diff;
        if ((diff = Integer.compare((s = a.subs()), b.subs())) != 0)
            return diff;
//        if ((diff = Integer.compare(a.volume(), b.volume())) != 0)
//            return diff;
        if ((diff = Integer.compare(a.structure(), b.structure())) != 0)
            return diff;

        //this inequalVariable stuff is so that the displayed order of variables is in increasing number.  HACK
        Term inequalVariableX = null, inequalVariableY = null;

        for (int i = 0; i < s; i++) {
            Term x = a.sub(i);
            Term y = b.sub(i);
            if (x instanceof Variable && y instanceof Variable) {
                if (inequalVariableX == null && !x.equals(y)) {
                    //test after; allow differing non-variable terms to determine sort order first
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

        //2nd-stage:
        if (inequalVariableX != null) {
            return inequalVariableX.compareTo(inequalVariableY);
        } else {
            return 0;
        }
    }


    /**
     * a and b must be instances of input, and output must be of size input.length-2
     */
    /*@NotNull*/
    static Term[] except(/*@NotNull*/ Subterms input, Term a, Term b, /*@NotNull*/ Term[] output) {
//        int targetLen = input.size() - 2;
//        if (output.length!= targetLen) {
//            throw new RuntimeException("wrong size");
//        }
        int j = 0;
        int l = input.subs();
        for (int i = 0; i < l; i++) {
            Term x = input.sub(i);
            if ((x != a) && (x != b))
                output[j++] = x;
        }

        if (j != output.length)
            throw new RuntimeException("permute underflow");

        return output;
    }

//    /**
//     * a must be in input, and output must be of size input.length-1
//     * equality is compared by instance for speed
//     */
//    /*@NotNull*/
//    static Term[] exceptThe(/*@NotNull*/ Term[] input, Term a, /*@NotNull*/ Term[] output) {
////        int targetLen = input.size() - 1;
////        if (output.length!= targetLen) {
////            throw new RuntimeException("wrong size");
////        }
//        int j = 0;
//        for (Term x : input) {
//            if (x != a)
//                output[j++] = x;
//        }
//
//        assert (j == output.length) : "permute underflow";
//
//
//        return output;
//    }


//    /*@NotNull*/
//    static Set<Term> toSetExcept(/*@NotNull*/ Subterms c, /*@NotNull*/ MutableSet<Term> except) {
//
////        return c.value(null, (x, s) -> {
////
////        });
//
//        int cs = c.subs();
//        Set<Term> s = null;
//        for (int i = 0; i < cs; i++) {
//            Term x = c.sub(i);
//            if (!except.contains(x)) {
//                if (s == null) s = new UnifiedSet(cs - i /* possible remaining items that could be added*/);
//                s.add(x);
//            }
//        }
//        return s == null ? Collections.emptySet() : s;
//    }

//    /**
//     * constructs a new container with the matching elements missing
//     * TODO elide creating a new vector if nothing would change
//     */
//    /*@NotNull*/
//    default TermContainer asFiltered(Predicate<Term> p) {
//        if (!(this instanceof TermContainer))
//            throw new UnsupportedOperationException("only implemented for TermVector instance currently");
//
//        List<Term> c = $.newArrayList(size());
//        if (OR(x -> p.test(x) && c.add(x)))
//            return TermVector.the(c);
//        else
//            return ZeroProduct;
//    }


    //TODO
//    default Stream<? extends Term> streamRecursive() {
//        return IntStream.range(0, size()).
//                mapToObj(x -> {
//                    Term s = sub(x);
//                    if (s instanceof TermContainer)
//                        return ((TermContainer) s).stream();
//                    else
//                        return Stream.empty();
//                }).collect(Collectors.);
//    }

//    /**
//     * matches in the correct ordering conditions for CONJ
//     */
//    static boolean unifyConj(Subterms X, int Xdt, Subterms Y, int Ydt, /*@NotNull*/ Unify u) {
//        boolean xCommutes = communify(Xdt);
//        boolean yCommutes = communify(Ydt);
//
//        if (!xCommutes && !yCommutes) {
//            //both temporal here, so compare in the right sequence:
//            boolean xReversed = (Xdt < 0);
//            boolean yReversed = (Ydt < 0);
//            if (xReversed ^ yReversed) {
//                Y = Y.reverse(); //just need to reverse one
//            }
//            return X.unifyLinear(Y, u);
//        }
//
//        return X.unifyCommute(Y, u);
//    }

//    /**
//     * commutes for unify
//     */
//    static boolean communify(int Xdt) {
//        return Xdt == XTERNAL || Xdt == DTERNAL || Xdt == 0;
//    }

    default boolean unifyLinear(Subterms Y, /*@NotNull*/ Unify u) {
        //return equals(Y) || ANDwith((xi,i)->xi.unify(Y.sub(i), u));
        //return ANDwith((xi,i)->xi.unify(Y.sub(i), u));

        int s = subs();
        for (int i = 0; i < s; i++) {
            if (!sub(i).unify(Y.sub(i), u))
                return false;
        }
        return true;

    }

    default boolean unifyCommute(Subterms y, /*@NotNull*/ Unify u) {


        if (u.constant(this, y))
            return y.equals(this);

        //if there are no variables of the matching type, then it seems CommutivePermutations wouldnt match anyway
//        if (!unifyPossible(subst.type)) {
//            return false; //TODO this still may not be the final answer
//        }

        //lexic sorted so that the formed termutator has a canonical representation, preventing permuted duplicates in the termute chain

        Collection<Term> yys = y.toSet();
        ////xs.removeIf(s -> !subst.matchType(s) && ys.remove(s));

        Map<Term, byte[]> constCommon = new LinkedHashMap<>(8);

        forEach(x -> {
            if (u.constant(x) && yys.contains(x)) { //attempt to eliminate a common constant term
                constCommon.compute(x, (k, vv) -> {
                    if (vv == null) {
                        vv = new byte[]{1, 0};
                    } else {
                        vv[0]++;
                    }
                    return vv;
                });
            }
        });

        int cc = constCommon.size();
        Term[] xs, ys;
        if (cc > 0) {

            y.forEach(yy -> {
                byte[] vv = constCommon.get(yy);
                if (vv != null)
                    vv[1]++;
            });
            constCommon.values().forEach(vv -> {
                vv[0] = vv[1] = (byte) Math.min(vv[0], vv[1]);
            });


            //filter out the common terms
            assert (cc != subs()); //otherwise equality test would have passed earlier

            xs = termsExcept(constCommon, true);
            ys = y.termsExcept(constCommon, false);

        } else {
            xs = arrayShared(); //already sorted
            ys = y.arrayShared();
        }

        //what remains are all variably-permutable terms and there must be an equal # of each
        int xss = xs.length;
        //assert (ys.length == xss);
        if (xss == 0 || xs.length != ys.length) {
            throw new RuntimeException("commutive unification mismatch");
        }


        //subst.termutes.add(new CommutivePermutations(TermVector.the(xs), TermVector.the(ys)));


        if (xss == 1) {
            //special case
            //  ex: {x,%1} vs. {x,z} --- there is actually no combination here
            //  Predicate<Term> notType = (x) -> !subst.matchType(x);
            //another case:
            //  xss > 1, yss=1: because of duplicates in ys that were removed; instead apply ys.first() to each of xs
            //  note for validation: the reverse will not work (trying to assign multiple different terms to the same variable in x)


            //return xs.allSatisfyWith((x,yy) -> x.unify(yy, u), ys.getOnly());
            return xs[0].unify(ys[0], u);

        } else /*if (xss == xss)*/ {

            boolean allEqual = true;
            for (int i = 1; i < ys.length; i++) {
                if (!ys[0].equals(ys[i])) {
                    allEqual = false;
                    break;
                }
            }
            if (allEqual) {
                //all equal, so the same value must unify to both X's subterms
                //no permutation necessary
                for (int i = 0; i < ys.length; i++) {
                    if (!xs[i].unify(ys[0], u))
                        return false;
                }
                return true;
            }

            u.termutes.add(new CommutivePermutations($.pFast(xs), $.pFast(ys)));
            return true;
        }
//        } else /* yss!=xss */ {
//            return false; //TODO this may possibly be handled
//        }
    }


    default Term[] termsExcept(RoaringBitmap toRemove) {
        int numRemoved = toRemove.getCardinality();
        int size = subs();
        int newSize = size - numRemoved;
        Term[] t = new Term[newSize];
        int j = 0;
        for (int i = 0; i < size; i++) {
            if (!toRemove.contains(i))
                t[j++] = sub(i);
        }
        return (t.length == 0) ? Compound.EmptyArray : t;
    }

    default Term[] termsExcept(MetalBitSet toRemove) {
        int numRemoved = toRemove.getCardinality();
        int size = subs();
        int newSize = size - numRemoved;
        Term[] t = new Term[newSize];
        int j = 0;
        for (int i = 0; i < size; i++) {
            if (!toRemove.get(i))
                t[j++] = sub(i);
        }
        return t.length == 0 ? Compound.EmptyArray : t;
    }

    default Term[] termsExcept(Collection<Term> except) {
        FasterList<Term> fxs = new FasterList<>(subs());
        forEach(t -> {
            if (!except.contains(t))
                fxs.add(t);
        });
        return fxs.toArrayRecycled(Term[]::new);
    }

    /**
     * extracts a certain subset of the terms according to a paired count map
     */
    default Term[] termsExcept(Map<Term, byte[]> except, boolean loOrHi) {
        FasterList<Term> fxs = new FasterList<>(subs());
        forEach(t -> {
            byte[] u = except.get(t);
            if (u != null) {
                byte remain = u[loOrHi ? 0 : 1];
                if (remain > 0) {
                    //decrement count and skip this one
                    u[loOrHi ? 0 : 1]--;
                    return;
                }
            }

            fxs.add(t);
        });
        return fxs.toArrayRecycled(Term[]::new);
    }
//    /** extracts a certain subset of the terms according to a paired count map */
//    default Term[] termsExcept(ObjectShortHashMap<Term> except, boolean loOrHi) {
//        FasterList<Term> fxs = new FasterList<>(subs());
//        forEach(t -> {
//            short u = except.getIfAbsent(t, Short.MAX_VALUE);
//            if (u != Short.MAX_VALUE) {
//                int b = (loOrHi ? u : (u >> 8)) & 0xff;
//                if (b > 0) {
//                    b--;
//                    //decrement count and skip this one
//                    //clear the active byte and update, dont erase the other byte
//                    except.put(t, (short) ((!loOrHi) ?
//                            ((u & 0x00ff) | (b << 8)) : ((u & 0xff00) | b)));
//                    return;
//                }
//            }
//
//            fxs.add(t);
//        });
//        return fxs.toArrayRecycled(Term[]::new);
//    }


    /**
     * match a range of subterms of Y.
     * WARNING: provides a shared (non-cloned) copy if the entire range is selected
     */
    /*@NotNull*/
    default Term[] toArraySubRange(int from, int to) {
        if (from == 0 && to == subs()) {
            return arrayShared();
            //arrayClone();
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

//    default boolean recurseSubTerms(BiPredicate<Term, Term> whileTrue, Compound parent) {
//        return AND(x -> x.recurseTerms(whileTrue, parent));
////        int s = subs();
////        for (int i = 0; i < s; i++) {
////            if (!sub(i).recurseTerms(whileTrue, parent))
////                return false;
////        }
////        return true;
//    }


    @Override
    default void recurseTerms(/*@NotNull*/ Consumer<Term> v) {
        forEach(s -> s.recurseTerms(v));
    }

    default boolean recurseTerms(Predicate<Term> aSuperCompoundMust, Predicate<Term> whileTrue, Term parent) {
        return AND(s -> whileTrue.test(s) && s.recurseTerms(aSuperCompoundMust, whileTrue, parent));
    }

    default Subterms reversed() {
        return subs() > 1 ? new ReverseSubterms(this) : this;
    }

    default Subterms sorted() {
        return isSorted() ? this : Op.terms.newSubterms(Terms.sorted(arrayShared()));
    }

    default Term[] termsExcept(int i) {
        return ArrayUtils.remove(arrayShared(), Term[]::new, i);
    }

    default int hashWith(Op op) {
        return Util.hashCombine(this.hashCodeSubterms(), op.id);
    }

    @Nullable default Term[] termsExcept(Term x) {
        int index = indexOf(x);
        return (index == -1) ? null : termsExcept(index);
    }

    default void append(ByteArrayDataOutput out) {
        out.writeByte(subs());
        forEach(t -> t.append(out));
    }


    //    /**
//     * returns a sorted and de-duplicated version of this container
//     */
//    default TermContainer sorted() {
//        int s = size();
//        if (s <= 1)
//            return this;
//
//
//        Term[] tt = Terms.sorted(toArray());
//        if (equalTerms(tt))
//            return this;
//        else
//            return Op.subterms(tt);
//    }

}
