package nars.term;

import jcog.Util;
import jcog.bloom.StableBloomFilter;
import jcog.bloom.hash.BytesHashProvider;
import jcog.decide.Roulette;
import jcog.sort.SortedList;
import nars.IO;
import nars.Op;
import nars.derive.premise.PremisePatternIndex;
import nars.subterm.Subterms;
import nars.term.atom.Atom;
import nars.unify.Unify;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import org.eclipse.collections.api.iterator.MutableIntIterator;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Random;
import java.util.SortedSet;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

import static nars.Op.NEG;

/**
 * Static utility class for static methods related to Terms
 * <p>
 * Also serves as stateless/memory-less transient static (on-heap) TermIndex
 *
 * @author me
 */
public enum Terms {
    ;


    public static Term[] sorted(Term... arg) {
        int len = arg.length;
        switch (len) {

            case 0:
                return Op.EmptyTermArray;

            case 1:
                return arg;

            case 2:
                Term a = arg[0];
                Term b = arg[1];
                int c = a.compareTo(b);
                if (c < 0) return arg;
                else if (c > 0) return new Term[]{b, a};
                else /*if (c == 0)*/ return new Term[]{a};


        }

        SortedList<Term> sl = new SortedList<>(arg, new Term[arg.length]);
        return sl.orderChangedOrDeduplicated ?
                sl.toArrayRecycled(Term[]::new) : arg;
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
        out.print("c" + x.complexity() + ",v" + x.volume() + ",dt=" + x.dt() + ",dtRange=" + x.dtRange() + " ");
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

        StringBuilder line = new StringBuilder();
        for (int i = 0; i < level; i++)
            line.append("  ");

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


    /**
     * returns the most optimal subterm that can be replaced with a variable, or null if one does not meet the criteria
     * when there is a chocie, it prefers least aggressive introduction. and then random choice if
     * multiple equals are introducible
     */
    public static Term nextRepeat(Term c, ToIntFunction<Term> countIf, int minCount, Random rng) {
        ObjectIntHashMap<Term> oi = Terms.subtermScore(c, countIf, minCount);
        if (oi == null)
            return null;

        LazyIterable<Term> ok = oi.keysView();
        switch (oi.size()) {
            case 0:
                return null;
            case 1:
                return ok.getFirst();
        }


        Term[] x = oi.keysView().toArray(Op.EmptyTermArray);
        IntToFloatFunction curve =
                //n -> 1f / (x[n].volume());
                n -> Util.sqr(1f / (x[n].volume()));
        return x[Roulette.selectRoulette(x.length, curve, rng)];
    }


    /**
     * counts the repetition occurrence count of each subterm within a compound
     */
    @Nullable
    public static ObjectIntHashMap<Term> subtermScore(Term c, ToIntFunction<Term> score, int minTotalScore) {
        ObjectIntHashMap<Term> uniques = new ObjectIntHashMap(c.volume());

        c.recurseTerms((Term subterm) -> {
            if (subterm == c)
                return;
            int s = score.applyAsInt(subterm);
            if (s > 0)
                uniques.addToValue(subterm, s);
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
        int xStruct = requirer.structure() & ~(maskedBits);
        return xStruct == 0 ||
               Op.hasAll(required.structure() & ~(maskedBits), xStruct);
    }

    public static boolean commonStructureTest(Termlike x, Termlike y, Unify u) {
        return u.symmetric || hasAllExcept(x, y, u.typeBits());
    }

    /** finds the shortest deterministic subterm path for extracting a subterm in a compound.
     *  paths in subterms of commutive terms are excluded also because the
     *  position is undeterministic. */
    @Nullable public static byte[] extractFixedPath(Term container, Term subterm) {
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
        return !container.isCommutative()
                && !(container instanceof PremisePatternIndex.PremisePatternCompound.PremisePatternCompoundWithEllipsisCommutive)
                && !(container instanceof PremisePatternIndex.PremisePatternCompound.PremisePatternCompoundWithEllipsisLinear);
    }
}
























