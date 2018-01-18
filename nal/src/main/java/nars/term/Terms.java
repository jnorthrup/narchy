package nars.term;

import jcog.Texts;
import jcog.Util;
import jcog.sort.SortedList;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.atom.Atom;
import nars.term.atom.Bool;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.primitive.ObjectIntPair;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

import static nars.Op.*;
import static nars.time.Tense.DTERNAL;

/**
 * Static utility class for static methods related to Terms
 * <p>
 * Also serves as stateless/memory-less transient static (on-heap) TermIndex
 *
 * @author me
 */
public enum Terms {
    ;


//    /**
//     * computes the content hash while accumulating subterm metadata summary fields into int[] meta
//     */
//    public static int hashSubterms(Term[] term, int[] meta) {
//
//        int result = 1;
//        for (int i = 0; i < term.length; i++) {
//            Term t = term[i];
//            t.init(meta);
//            result = Util.hashCombine(t.hashCode(), result);
//        }
//        return result;
//    }

    public static int hashSubterms(Term[] term) {
        int h = 1;
        for (int i = 0; i < term.length; i++)
            h = Util.hashCombine(h, term[i].hashCode());
        return h;
    }


    //    @Deprecated
//    public static boolean equalSubTermsInRespectToImageAndProduct(@Nullable Term a, @Nullable Term b) {
//
//        /*if (a == null || b == null) {
//            return false;
//        } else {*/
//        Op o = a.op();
//        boolean equalOps = (o == b.op());
//
//        if (equalOps) {
//
//            switch (o) {
//                case INH:
//                    return equalSubjectPredicateInRespectToImageAndProduct((Compound) a, (Compound) b);
//
//                case SIM:
//                    //only half seems necessary:
//                    //boolean y = equalSubjectPredicateInRespectToImageAndProduct((Compound) b, (Compound) a);
//                    return equalSubjectPredicateInRespectToImageAndProduct((Compound) a, (Compound) b);
//
//                default:
//                    if (Terms.equalAtemporally(a, b))
//                        return false;
//                    break;
//            }
//        }
//
//
//        if ((a instanceof Compound) && (b instanceof Compound)) {
//            //both are compounds
//
//
//            Compound A = ((Compound) a);
//            Compound B = ((Compound) b);
//            int aLen = A.size();
//            if (aLen != B.size()) {
//                return false;
//            } else {
//
//                //match all subterms
//                for (int i = 0; i < aLen; i++) {
//                    if (!equalSubTermsInRespectToImageAndProduct(A.get(i), B.get(i)))
//                        return false;
//                }
//                return true;
//            }
//        }
//
//        return false;
//
//
//    }
//
//
//    public static boolean equalSubjectPredicateInRespectToImageAndProduct(Compound A, Compound B) {
//
//        if (A.equals(B)) {
//            return true;
//        }
//
//
//        if (/*!hasAny(as, Op.PRODUCT) || */!A.hasAny(Op.ImageBits))
//            return false;
//
//        if (/*!hasAny(bs, Op.PRODUCT) || */!B.hasAny(Op.ImageBits))
//            return false;
//
////        if (!A.hasAny(Op.PRODUCT) || !B.hasAny(Op.PRODUCT) || !A.hasAny(Op.ImageBits) || !B.hasAny(Op.ImageBits)) {
////            //product and one of either image types
////            return false; //the remaining comparisons are unnecessary
////        }
//
//        Term subjA = subj(A);
//        Term predA = pred(A);
//        Term subjB = subj(B);
//        Term predB = pred(B);
//
//        Term ta = null, tb = null; //the compound term to put itself in the comparison set
//        Term sa = null, sb = null; //the compound term to put its components in the comparison set
//
//        Op sao = subjA.op();
//        Op sbo = subjB.op();
//        Op pao = predA.op();
//        Op pbo = predB.op();
//
//
//        if ((sao == PROD) && (pbo == IMGe)) {
//            ta = predA;
//            sa = subjA;
//            tb = subjB;
//            sb = predB;
//        }
//
//        if ((sbo == PROD) && (pao == IMGe)) {
//            ta = subjA;
//            sa = predA;
//            tb = predB;
//            sb = subjB;
//        }
//
//        if ((pao == IMGe) && (pbo == IMGe)) {
//            ta = subjA;
//            sa = predA;
//            tb = subjB;
//            sb = predB;
//        }
//
//        if ((sao == IMGi) && (sbo == IMGi)) {
//            ta = predA;
//            sa = subjA;
//            tb = predB;
//            sb = subjB;
//        }
//
//        if ((pao == PROD) && (sbo == IMGi)) {
//            ta = subjA;
//            sa = predA;
//            tb = predB;
//            sb = subjB;
//        }
//
//        if ((pbo == PROD) && (sao == IMGi)) {
//            ta = predA;
//            sa = subjA;
//            tb = subjB;
//            sb = predB;
//        }
//
//        if (ta != null) {
//            //original code did not check relation index equality
//            //https://code.google.com/p/open-nars/source/browse/trunk/nars_core_java/nars/language/CompoundTerm.java
//            //if (requireEqualImageRelation) {
//            //if (sa.op().isImage() && sb.op().isImage()) {
//            Compound csa = (Compound) sa;
//            Compound csb = (Compound) sb;
//
//            return csa.dt() == csb.dt() && containsAll(csa, ta, csb, tb);
//        } else {
//            return false;
//        }
//
//    }
//
//    private static boolean containsAll(TermContainer sat, Term ta, TermContainer sbt, Term tb) {
//        //set for fast containment check
//        Set<Term> componentsA = sat.toSet();
//        componentsA.add(ta);
//
//        //test A contains B
//        if (!componentsA.contains(tb))
//            return false;
//
//        Term[] sbtt = sbt.toArray();
//        for (Term x : sbtt) {
//            if (!componentsA.contains(x))
//                return false;
//        }
//
//        return true;
//    }


    @NotNull
    public static Term[] reverse(Term[] arg) {
        int l = arg.length;
        Term[] r = new Term[l];
        for (int i = 0; i < l; i++) {
            r[i] = arg[l - i - 1];
        }
        return r;
    }


    /**
     * warning may rearrange items in the input
     */
    public static Term[] sorted(Term... arg) {
        int len = arg.length;
        switch (len) {

            case 0:
                return Term.EmptyArray;

            case 1:
                return arg;

            case 2:
                Term a = arg[0];
                Term b = arg[1];
                int c = a.compareTo(b);
                if (c < 0) return arg; //same as input //new Term[]{a, b};
                else if (c > 0) return new Term[]{b, a};
                else /*if (c == 0)*/ return new Term[]{a}; //equal


                //TODO fast sorted array for arg.length == 3 ?

            default: {
                SortedList<Term> sl = new SortedList<>(arg, new Term[arg.length]);
                if (sl.orderChangedOrDeduplicated)
                    return sl.toArrayRecycled(Term[]::new);
                else
                    return arg; //input is already sorted and de-duplicated
            }

            //return sortUniquely(arg); //<- may also work but seems slower

        }
    }

    public static void printRecursive(PrintStream out, Term x) {
        printRecursive(out, x, 0);
    }

    static void printRecursive(PrintStream out, Term x, int level) {
        //indent
        for (int i = 0; i < level; i++)
            out.print("  ");

        out.print(x);
        out.print(" (");
        out.print(x.op() + "[" + x.getClass().getSimpleName() + "] ");
        out.print("c" + x.complexity() + ",v" + x.volume() + ",dt=" + x.dt() + ",dtRange=" + x.dtRange() + " ");
        out.print(Integer.toBinaryString(x.structure()) + ')');
        out.println();


        for (Term z : x.subterms())
            printRecursive(out, z, level + 1);

    }

    /**
     * for printing complex terms as a recursive tree
     */
    public static void printRecursive(Term x, Consumer<String> c) {
        printRecursive(x, 0, c);
    }

    public static void printRecursive(Term x, int level, Consumer<String> c) {
        //indent
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < level; i++)
            line.append("  ");

        line.append(x);


        for (Term z : x.subterms())
            printRecursive(z, level + 1, c);


        c.accept(line.toString());
    }


    public static int maxLevel(Term term) {
        int[] max = {0};
        term.recurseTerms((t) -> {
            int m = t.op().minLevel;
            if (m > max[0])
                max[0] = m;
        });
        return max[0];
    }

    @Nullable
    public static Term[] concat(@Nullable Term[] a, Term... b) {

        if (a == null) {
            return null;
        }

        if (a.length == 0) return b;
        if (b.length == 0) return a;

        int L = a.length + b.length;

        //TODO apply preventUnnecessaryDeepCopy to more cases

        Term[] arr = new Term[L];

        int l = a.length;
        System.arraycopy(a, 0, arr, 0, l);
        System.arraycopy(b, 0, arr, l, b.length);

        return arr;
    }


    /**
     * returns lev distance divided by max(a.length(), b.length()
     */
    public static float levenshteinDistancePercent(CharSequence a, CharSequence b) {
        int len = Math.max(a.length(), b.length());
        if (len == 0) return 0f;
        return Texts.levenshteinDistance(a, b) / ((float) len);
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
        if (t instanceof Compound) return (Compound) t;
        else
            return null;
    }


    public static boolean allNegated(Subterms subterms) {
        return subterms.hasAny(Op.NEG) && subterms.AND((Term t) -> t.op() == NEG);
    }


//    @Nullable
//    public static Term atemporalize(Term c) {
//        if (c instanceof Compound)
//            return atemporalize((Compound)c);
//        return c;
//    }


//    /**
//     * returns the most optimal subterm that can be replaced with a variable, or null if one does not meet the criteria
//     */
//    @Nullable
//    public static Term[] substMaximal(Compound c, Predicate<Term> include, int minCount, int minScore) {
//        HashBag<Term> uniques = subtermScore(c,
//                t -> include.test(t) ? t.volume() : 0 //sum by complexity if passes include filter
//        );
//
//        int s = uniques.size();
//        if (s > 0) {
//            MutableList<ObjectIntPair<Term>> u = uniques.topOccurrences(s);
//            for (ObjectIntPair<Term> p : u) {
//                int score = p.getTwo();
//                if (score >= minScore) {
//                    Term subterm = p.getOne();
//                    int count = score / subterm.complexity(); //should be a whole number according to the above scoring policy
//                    if (count >= minCount) {
//                        return new Term[]{subterm};
//                    }
//                }
//            }
//
//        }
//
//        return null;
//    }
//
//    /**
//     * returns the most optimal subterm that can be replaced with a variable, or null if one does not meet the criteria
//     */
//    @Nullable
//    public static Term[] substRoulette(Compound c, Predicate<Term> include, int minCount, Random rng) {
//        HashBag<Term> uniques = subtermScore(c,
//                t -> include.test(t) ? 1 : 0 //sum by complexity if passes include filter
//        );
//
//        int s = uniques.size();
//        if (s > 0) {
//            ObjectIntPair<Term>[] oi = new ObjectIntPair[s];
//            final int[] j = {0};
//            final int[] sum = {0};
//            uniques.forEachWithOccurrences((Term t, int count) -> {
//                if (count >= minCount) {
//                    int score = count * t.volume();
//                    oi[j[0]++] = PrimitiveTuples.pair(t, score);
//                    sum[0] += score;
//                }
//            });
//
//            int available = j[0];
//            if (available == 1) {
//                return new Term[]{oi[0].getOne()};
//            } else if (available > 1) {
//                int selected = DecideRoulette.decideRoulette(j[0], (i) -> oi[i].getTwo(), sum[0], rng);
//                return new Term[]{oi[selected].getOne()};
//            }
//        }
//
//        return null;
//    }

    /**
     * returns the most optimal subterm that can be replaced with a variable, or null if one does not meet the criteria
     * when there is a chocie, it prefers least aggressive introduction. and then random choice if
     * multiple equals are introducible
     */
    public static Term nextRepeat(Term c, ToIntFunction<Term> countIf, int minCount, @Nullable Random rng) {
        //FasterList<Term> oi = uniqueRepeats(c, countIf, minCount);
        MutableList<ObjectIntPair<Term>> oi = Terms.subtermScore(c, countIf, minCount).toList();
        switch (oi.size()) {
            case 0: return null;
            case 1: return oi.get(0).getOne();
        }

        //keep only the unique subterms which are not contained by other terms in the list
        //terms which are contained by other terms in the list
        final int[] minScore = {Integer.MAX_VALUE};
        oi.removeIf(bb -> {
            if (oi.anySatisfyWith((a,b) -> (a != b) && a.getOne().containsRecursively(b.getOne()), bb)) {
                return true;
            } else {
                if (bb.getTwo() < minScore[0])
                    minScore[0] = bb.getTwo();
                return false;
            }
        });

        switch (oi.size()) {
            case 0: throw new RuntimeException("shouldnt happen");
            case 1:
                return oi.get(0).getOne();
        }

        //prefer least aggressive options to gradually introduce variables rather than destroy the most information first, prefer to destroy small amounts first
        oi.removeIf(bb -> bb.getTwo() > minScore[0]);
        switch (oi.size()) {
            case 0: throw new RuntimeException("shouldnt happen");
            case 1:
                return oi.get(0).getOne();
            default:
                return oi.get(rng.nextInt(oi.size())).getOne();
        }
    }

//    /**
//     * returns a list but its contents will be unique
//     */
//    @Nullable
//    static FasterList<Term> uniqueRepeats(Term c, ToIntFunction<Term> termScore, int minTotalScore) {
//        RichIterable<ObjectIntPair<Term>> uniques = Terms.subtermScore(c, termScore, minTotalScore);
//        int us = uniques.size();
//        if (us == 0)
//            return null;
//
//        FasterList<Term> oi = new FasterList(0);
//
//        uniques.forEachWithOccurrences((Term t, int count) -> {
//            if (count >= minTotalScore)
//                oi.add(t);
//        });
//
//        return oi;
//    }


    /**
     * counts the repetition occurrence count of each subterm within a compound
     */
    public static RichIterable<ObjectIntPair<Term>> subtermScore(Term c, ToIntFunction<Term> score, int minTotalScore) {
        ObjectIntHashMap<Term> uniques = new ObjectIntHashMap(c.volume() / 2);

        c.recurseTerms((Term subterm) -> {
            int s = score.applyAsInt(subterm);
            if (s > 0)
                uniques.addToValue(subterm, s);
        });

        return uniques.keyValuesView().select((oi)->oi.getTwo()>=minTotalScore);
    }

    /**
     * a Set is already duplicate free, so just sort it
     */
    public static Term[] sorted(Collection<Term> s) {

        Term[] x = s.toArray(new Term[s.size()]);

        //2. sorted
        if ((x.length >= 2) && (!(s instanceof SortedSet)))
            return sorted(x);
        else
            return x;
    }

    @NotNull
    public static Term[] neg(Term... modified) {
        int l = modified.length;
        Term[] u = new Term[l];
        for (int i = 0; i < l; i++) {
            u[i] = modified[i].neg();
        }
        return u;
    }

    static final Comparator<Term> volumeComparator = Comparator.comparingInt(Term::volume);

    /**
     * for commutive conjunction
     *
     * @param dt will be either 0 or DTERNAL (commutive relation)
     * @return
     *      True   -- short-circuit to True
     *      False  -- short-circuit to False
     *      Null   -- short-circuit to Null (failure)
     *      null   -- ok
     */
    public static Term flatten(/*@NotNull*/ Op op, Term[] u, int dt, ObjectByteHashMap<Term> s) {

        assert (u.length > 1);

        //sort by volume, decreasing first. necessary for proper subsumption of events into sibling sequence compounds that may contain them
        //may also provide some performance benefit for accelerated early termination in case of invalid construction attempts (ex: co-negation)

        //u = u.clone(); //dont modify input, it will confuse callee's
        //Arrays.sort(u, volumeComparator);

        byte trues = 0, falses = 0;
        for (Term x : u) {
            Term xx = flatten(op, dt, x, s);
            if (xx != null) {
                if (xx == True) {
                    if (falses > 0)
                        return False;
                    trues++;
                }else if (xx == False) {
                    if (trues > 0)
                        return False;
                    falses++;
                } else if (xx == Null)
                    return Null;
            }
        }
        /*if (trues > 0 && falses > 0)
            return Null;
        else */if (trues > 0)
            return True;
        else if (falses > 0)
            return False;
        else
            return null;
    }



    public static boolean flattenMatchDT(int candidate, int target) {
        return (candidate == target) || ((target == 0) && (candidate == DTERNAL));
    }

    public static Term flatten(/*@NotNull*/ Op op, int dt, Term x, ObjectByteHashMap<Term> s) {
        if (x instanceof Bool) {
            return x;
        }

        Op xo = x.op();

        if ((xo == op) && (flattenMatchDT(x.dt(), dt))) {
            return flatten(op, x.subterms().arrayShared(), dt, s);

        } else {
            if (!testCoNegate(x, s))
                return False;

            if (x.op() == CONJ) {
                int xdt = x.dt();
                if (xdt != 0) {
                    //test for x's early subterm (left aligned t=0) in case it matches with a term in the superconjunction that x is a subterm of
                    Term early = x.sub(xdt > 0 ? 0 : 1);

                    //check if the early event is present, and if it is (with correct polarity) then include x without the early event
                    Term earlyUnneg = early.unneg();
                    byte earlyExisting = s.getIfAbsent(earlyUnneg, (byte) 0);
                    if (earlyExisting != 0) {
                        if (early.op() == NEG ^ (earlyExisting == -1))
                            return False; //wrong polarity
                        else {
                            //subsume the existing term by removing it from the list, since it is part of 'x' which has been added in entirity already
                            s.remove(earlyUnneg);
                        }

                    }

                }
            }

            return null; //ok
        }

    }

    static boolean testCoNegate(Term x, ObjectByteHashMap<Term> s) {

        assert (x != Null);

        byte polarity;
        Term t;
        if (x.op() == NEG || x == False) {
            polarity = -1;
            t = x.unneg();
        } else {
            polarity = +1;
            t = x;
        }
        return s.getIfAbsentPut(t, polarity) == polarity;
    }

    @NotNull
    public static Term intersect(/*@NotNull*/ Op o, Subterms a, Subterms b) {
        if (a instanceof Term && a.equals(b))
            return (Term) a;


        Term[] c = Subterms.intersect(a, b);
        return (c == null || c.length == 0) ? Null : (Compound) (o.the(c));
    }

    @NotNull
    public static Term union(/*@NotNull*/ Op o, Subterms a, Subterms b) {
        boolean bothTerms = a instanceof Term && b instanceof Term;
        if (bothTerms && a.equals(b))
            return (Term) a;

        TreeSet<Term> t = new TreeSet<>();
        a.copyInto(t);
        b.copyInto(t);
        if (bothTerms) {
            int as = a.subs();
            int bs = b.subs();
            int maxSize = Math.max(as, bs);
            if (t.size() == maxSize) {
                //the smaller is contained by the larger other
                //so return an input value rather than constructing a duplicate
                return (Term) (as > bs ? a : b);
            }
        }
        return o.the(DTERNAL, t);
    }


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
}


//    private static boolean equalsAnonymous(TermContainer a, TermContainer b) {
//        if (a.volume() == b.volume()) {
//            int n = a.size();
//            if (n == b.size()) {
//                for (int i = 0; i < n; i++) {
//                    Term as = a.term(i);
//                    Term bs = b.term(i);
//                    //        if (as == bs) {
////            return true;
////        } else if (as instanceof Compound && bs instanceof Compound) {
////            return equalsAnonymous((Compound) as, (Compound) bs);
////        } else {
////            return as.equals(bs);
////        }
//                    if (!Terms.equalAtemporally(as, bs))
//                        return false;
//                }
//                return true;
//            }
//        }
//        return false;
//    }
