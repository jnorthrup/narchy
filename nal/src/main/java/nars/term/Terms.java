package nars.term;

import jcog.bloom.StableBloomFilter;
import jcog.bloom.hash.BytesHashProvider;
import jcog.sort.SortedList;
import jcog.util.ArrayUtils;
import nars.io.IO;
import nars.Op;
import nars.derive.premise.PatternIndex;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.term.atom.Atom;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.iterator.MutableIntIterator;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Random;
import java.util.SortedSet;
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


    /** sort and deduplicates the elements; returns new array if modifications to order or deduplication are necessary  */
    public static Term[] sorted(Term... t) {
        int len = t.length;
        switch (len) {
            case 0: return Op.EmptyTermArray;
            case 1: return t;
            case 2: return sorted2(t);
            case 3: return sorted3(t);
            default: {
                SortedList<Term> sl = new SortedList<>(t, new Term[t.length]);
                return sl.orderChangedOrDeduplicated ?
                        sl.toArrayRecycled(Term[]::new) : t;
            }
        }


    }

    public static Term[] sorted3(Term[] t) {
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
            return sorted(a, c); //a=b, so just combine a and c (recurse)
        } else if (ab > 0) {
            Term x = a;
            a = b;
            b = x;
        }
        int bc = b.compareTo(c);
        if (bc == 0) {
            //assert(a.compareTo(b) < 0); //temporary
            return new Term[]{a, b}; //b=c so just combine a and b
        } else if (bc > 0) {
            Term x = b;
            b = c;
            c = x;
            int ab2 = a.compareTo(b);
            if (ab2 == 0) {
                //assert(a.compareTo(c) < 0); //temporary
                return new Term[]{a, c};
            } else if (ab2 > 0) {
                Term y = a;
                a = b;
                b = y;
            }
        }
        if (t[0] == a && t[1] == b && t[2] == c)
            return t; //already sorted
        else {
            return new Term[]{a, b, c};
        }
    }

    @NotNull
    public static Term[] sorted2(Term[] t) {
        Term a = t[0], b = t[1];
        int ab = a.compareTo(b);
        if (ab < 0) return t;
        else if (ab > 0) return new Term[]{b, a};
        else /*if (c == 0)*/ return new Term[]{a};
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

        StringBuilder line = new StringBuilder(level*2 + 32);
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
        return subterms.hasAny(Op.NEG) && subterms.AND((Term t) -> t.op() == NEG);
    }

    public static int countNegated(Subterms subterms) {
        return subterms.hasAny(Op.NEG) ? subterms.subs(NEG) : 0;
    }

    public static int negatedNonConjCount(Subterms subterms) {
        return subterms.hasAny(Op.NEG) ? subterms.subs(x -> x.op() == NEG && !x.hasAny(CONJ)) : 0;
    }

    /**
     * returns the most optimal subterm that can be replaced with a variable, or null if one does not meet the criteria
     * when there is a chocie, it prefers least aggressive introduction. and then random choice if
     * multiple equals are introducible
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


        Term[] x = oi.keysView().toArray(Op.EmptyTermArray);
        return x;
    }


    /**
     * counts the repetition occurrence count of each subterm within a compound
     */
    @Nullable
    public static ObjectIntHashMap<Term> subtermScore(Subterms c, ToIntFunction<Term> score, int minTotalScore) {
        ObjectIntHashMap<Term> uniques = new ObjectIntHashMap(c.volume());

        c.forEach(cc -> {
            cc.recurseTerms((Term subterm) -> {
                if (subterm == c)
                    return;
                int s = score.applyAsInt(subterm);
                if (s > 0)
                    uniques.addToValue(subterm, s);
            });
        });

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
    public static Term[] sorted(Collection<Term> s) {
        Term[] x = s.toArray(Op.EmptyTermArray);
        if ((x.length >= 2) && (!(s instanceof SortedSet)))
            return sorted(x);
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


    public static Term[] dropRandom(Random random, Subterms t) {
        int size = t.subs();
        assert (size > 1);
        Term[] y = new Term[size - 1];
        int except = random.nextInt(size);
        for (int i = 0, j = 0; i < size; i++) {
            if (i != except) {
                y[j++] = t.sub(i);
            }
        }
        return y;
    }

    public static StableBloomFilter<Term> newTermBloomFilter(Random rng, int cells) {
        return new StableBloomFilter<>(
                cells, 2, 1f / cells, rng,
                new BytesHashProvider<>(IO::termToBytes));
    }

    //
//    public static boolean commonStructureExcept(Termlike x, Termlike y, int maskedBits) {
//        int xStruct = x.structure() & ~(maskedBits);
//        int yStruct = y.structure() & ~(maskedBits);
//        return (xStruct & yStruct) != 0;
//    }

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
                        p[0] = path.isEmpty() ? ArrayUtils.EMPTY_BYTE_ARRAY : path.toArray();
                    }
                    return true; //continue
                });
        return p[0];
    }

    private static boolean canExtractFixedPath(Term container) {
        return !(container instanceof PatternIndex.PremisePatternCompound.PremisePatternCompoundWithEllipsis)
                &&
               !container.isCommutative();
    }

    /** provides canonically sorted subterms of subterms */
    public static Subterms sorted(Subterms x) {
        if (x.isSorted())
            return x;
        return new TermList(Terms.sorted(x.arrayShared()));
    }

    public static boolean isSorted(Term[] s) {
        if (s.length < 2) return true;
        for (int i = 1; i < s.length; i++)
            if (s[(i - 1)].compareTo(s[i]) >= 0)
                return false;
        return true;
    }

    public static boolean possiblyUnifiable(Term x, Term y, boolean strict, int var) {

        boolean xEqY = x.equals(y);
        if (xEqY)
            return !strict;

        Op xo = x.op(), yo = y.op();

        int konst = ~var;
        if ((xo.bit & konst) == 0)
            return true; //variable, allow

        if ((yo.bit & konst) == 0)
            return true; //variable, allow

        if (xo != yo)
            return false;

        int xs = x.structure();
        int ys = y.structure();
        if (((xs & var)==0) && ((ys&var)==0)) //no variables
            return false;

        //TODO Conj Xternal allow

        Subterms xx = x.subterms(), yy = y.subterms();
        int xxs = xx.subs();
        if (xxs != yy.subs())
            return false;

        if (!Subterms.possiblyUnifiable(xx, yy, var))
            return false;

        if (!xo.commutative) {
            for (int i = 0; i < xxs; i++) {
                Term x0 = xx.sub(i), y0 = yy.sub(i);
                if (!possiblyUnifiable(x0, y0, false, var))
                    return false;
            }
        }

        return true;
    }

    /**
     * returns null if not found, and Null if no subterms remain after removal
     */
    @Nullable
    public static Term without(Term container, Predicate<Term> filter, Random rand) {


        Subterms cs = container.subterms();

        int i = cs.indexOf(filter, rand);
        if (i == -1)
            return Null;


        switch (cs.subs()) {
            case 1:
                return Null;
            case 2:

                Term remain = cs.sub(1 - i);
                Op o = container.op();
                return o.isSet() ? o.the(remain) : remain;
            default:
                return container.op().the(container.dt(), cs.subsExcluding(i));
        }

    }
}
























