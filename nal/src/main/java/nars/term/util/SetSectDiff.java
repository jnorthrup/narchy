package nars.term.util;

import jcog.data.bit.MetalBitSet;
import jcog.data.iterator.ArrayIterator;
import nars.$;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.util.builder.HeapTermBuilder;
import nars.term.util.builder.TermBuilder;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.jetbrains.annotations.Nullable;

import static nars.Op.*;
import static nars.term.atom.Bool.Null;
import static nars.term.atom.Bool.True;

/**
 * NAL2/NAL3 setAt, intersection and difference functions
 */
public class SetSectDiff {

    /** high-level intersection/union build procedure.  handles:
     *      decoding op type
     *      whether a or b is negated
     *      whether a or b is set, sect, or product
     *
     *      Subterm params:
     *          2:              "&" or "|" indicating the SECT type
     *          3 (optional):   "*" enabling product splice
     */
    public static @Nullable Term sect(Term a, Term b, boolean union, Subterms s) {
        Op op = s.sub(2).equals(Op.SECTe.strAtom) ? Op.SECTe : Op.SECTi;

        boolean productSplice = s.subs() > 3 && s.sub(3).equals(Op.PROD.strAtom);

        if (productSplice && a.unneg().op()==PROD && b.unneg().op()==PROD && a.unneg().subs()==b.unneg().subs() /* && rng? */) {
            return SetSectDiff.intersectProd(op, union, a, b);
        } else {
            return SetSectDiff.intersect(op, union, a, b);
        }
    }

    public static Term intersect(TermBuilder b, Op o, Term... t) {
        return intersect(b, o, false, t);
    }

    public static Term intersect(Op o, boolean union, Term... t) {
        return intersect(HeapTermBuilder.the, o, union, t);
    }

    public static Term intersect(TermBuilder b, Op o, boolean union, Term... t) {

        switch (t.length) {
            case 0:
                return True;
            case 1:
                return t[0];
            case 2:
                Op o0 = t[0].op(), o1 = t[1].op();
                if (o0 == o1 && t[0].equals(t[1]))
                    return t[0];

                //fast eliminate contradiction

                if (o0 == NEG ^ o1 == NEG) {
                    if (o0 == NEG && t[0].unneg().equals(t[1])) {
                        return union ? t[1] : Null;
                    } else if (/*o1 == NEG && */t[1].unneg().equals(t[0])) {
                        return union ? t[0] : Null;
                    }
                }

                //continue below:
                break;
        }


        Op oSet = t[0].op();
        if ((o == SECTe && oSet == SETe) || (o == SECTi && oSet == SETi)) {
            boolean allSet = true;
            for (int i = 1, tLength = t.length; i < tLength; i++) {
                Term x = t[i];
                if (x.op() != oSet) {
                    allSet = false;
                    break;
                }

            }
            if (allSet) {
                o = oSet;
            }
        }

        /** if the boolean value of a key is false, then the entry is negated */
        ObjectByteHashMap<Term> y = intersect(o, o == SECTe || o == SECTi, union, ArrayIterator.iterable(t), new ObjectByteHashMap<>(t.length));
        if (y == null)
            return Null;
        int s = y.size();
        if (s == 0)
            return True;
        else if (s == 1)
            return y.keysView().getOnly();
        else {

            Term[] yyy = new Term[s];
            final int[] k = {0};
            y.keyValuesView().forEach(e ->
                    yyy[k[0]++] = e.getOne().negIf(e.getTwo() == -1)
            );
            return Op.compound(b, o, yyy);
        }


    }

    public static Term intersectProd(Op o, boolean union, Term x, Term y) {
        return intersectProd(HeapTermBuilder.the, o, union, x, y);
    }

    public static Term intersectProd(TermBuilder B, Op o, boolean union, Term x, Term y) {

        if (x.equals(y))
            return x;

        boolean xNeg = x.op() == NEG, yNeg = y.op() == NEG;
        if ((xNeg ^ yNeg) && x.unneg().equals(y.unneg()))
            return union ? x : Null;

        Term X = !xNeg ? x : x.unneg();
        if (X.op()!=PROD) return Null; //both must be PROD
        Term Y = !yNeg ? y : y.unneg();
        if (Y.op()!=PROD) return Null; //both must be PROD

        Subterms xx = X.subterms();
        int n = xx.subs();
        Subterms yy = Y.subterms();
        if (n != yy.subs())
            return Null;


        Term[] xy = new Term[n];
        for (int i = 0; i < n; i++) {
            Term a = xx.sub(i).negIf(xNeg);
            Term b = yy.sub(i).negIf(yNeg);
            Term ab;
            if (a.equals(b))
                ab = a;
            else {
                ab = a.unneg().op() == PROD && b.unneg().op() == PROD ?
                        intersectProd(B, o, union, a, b) //recurse
                        :
                        intersect(B, o, union, a, b);
                if (ab == Null)
                    return Null;
            }
            xy[i] = ab;
        }

        return $.pFast(xy);
    }

    /**
     * returns null to short-circuit failure
     */
    private static ObjectByteHashMap<Term> intersect(Op o, boolean sect, boolean union, Iterable<Term> t, ObjectByteHashMap<Term> y) {
        if (y == null)
            return null;


        for (Term x : t) {
            if (x instanceof Bool) {
                if (x == True)
                    continue;
                else
                    return null; //fail on null or false
            }

            Op xo = x.op();


            if (xo != o) {
                if (sect) {
                    byte p = (byte) (x.op() != NEG ? +1 : -1);
                    if (p == -1) x = x.unneg();
                    int existing = y.getIfAbsent(x, Byte.MIN_VALUE);
                    if (existing != Byte.MIN_VALUE) {
                        if (existing == p)
                            continue; //same exact target and polarity present
                        else {
                            if (!union) {
                                return null; //intersection of X and its opposite = contradiction
                            } else {
                                //union of X and its opposite = true, so ignore
                                y.remove(x);
                                continue;
                            }
                        }
                    } else {
                        y.put(x, p);
                    }
                } else {
                    y.put(x, (byte) 0); //as-is, doesnt matter
                }
            } else {
                //recurse
                if (intersect(o, sect, union, x.subterms(), y) == null)
                    return null;
            }
        }

        return y;
    }

//    private static boolean uniqueRoots(Term... t) {
//        if (t.length == 2) {
//            if (t[0] instanceof Compound && t[1] instanceof Compound && t[0].hasAny(Op.Temporal) && t[1].hasAny(Op.Temporal))
//                if (t[0].equalsRoot(t[1]))
//                    return false;
//        /*} else if (t.length == 3) {
//            //TODO
//*/
//        } else {
//                //repeat terms of the same root would collapse when conceptualized so this is prevented
//                Set<Term> roots = null;
//                for (int i = t.length-1; i >= 0; i--) {
//                    Term x = t[i];
//                    if (x instanceof Compound && x.hasAny(Op.Temporal)) {
//                        if (roots == null) {
//                            if (i == 0)
//                                break; //last one, dont need to check
//                            roots = new UnifiedSet<>(t.length-1 - i);
//                        }
//                        if (!roots.addAt(x.root())) {
//                            //repeat detected
//                            return false;
//                        }
//                    }
//                }
//            }
//
//
//        return true;
//    }

//    /*@NotNull*/
//    @Deprecated
//    private static Term intersect2(Term term1, Term term2, /*@NotNull*/ Op intersection, /*@NotNull*/ Op setUnion, /*@NotNull*/ Op setIntersection) {
//
//
//        Op o1 = term1.op(), o2 = term2.op();
//
//
//        if (o1 == o2 && o1.isSet()) {
//            if (term1.equals(term2))
//                return term1;
//
//            if (o1 == setUnion) {
//                return SetFunc.union(setUnion, term1.subterms(), term2.subterms());
//            } else if ((o1 == setIntersection) && (o2 == setIntersection)) {
//                return SetFunc.intersect(setIntersection, term1.subterms(), term2.subterms());
//            }
//        } else {
//            //SECT
//            if (term1.equals(term2))
//                return True;
//            if (o1==NEG && term1.unneg().equals(term2)) return False;
//            if (o2==NEG && term2.unneg().equals(term1)) return False;
//        }
//
////        if (o2 == intersection && o1 != intersection) {
////
////            Term x = term1;
////            term1 = term2;
////            term2 = x;
////            o2 = o1;
////            o1 = intersection;
////        }
////
////
////        if (o1 == intersection) {
////            UnifiedSet<Term> args = new UnifiedSet<>();
////
////            ((Iterable<Term>) term1).forEach(args::addAt);
////            if (o2 == intersection)
////                ((Iterable<Term>) term2).forEach(args::addAt);
////            else
////                args.addAt(term2);
////
////            int aaa = args.size();
////            if (aaa == 1)
////                return args.getOnly();
////            else {
////                return Op.compound(intersection, DTERNAL, Terms.sorted(args));
////            }
////
////        } else {
////            return Op.compound(intersection, DTERNAL, term1.compareTo(term2) < 0 ?
////                new Term[] { term1, term2 } : new Term[] { term2, term1 }
////            );
////        }
//
//    }

//    public static Term differ(/*@NotNull*/ Op op, Term... t) {
//
//
//        switch (t.length) {
//            case 1:
//                Term single = t[0];
//                if (single instanceof EllipsisMatch) {
//                    return differ(op, single.arrayShared());
//                }
//                return single instanceof Ellipsislike ?
//                        Op.compound(op, DTERNAL, single) :
//                        Null;
//            case 2:
//                Term et0 = t[0], et1 = t[1];
//
//                if (et0 == Null || et1 == Null)
//                    return Null;
//
//
//                if (et0.equals(et1))
//                    return Bool.False;
//
//                //((--,X)~(--,Y)) reduces to (Y~X)
//                if (et0.op() == Op.NEG && et1.op() == Op.NEG) {
//                    //un-neg and swap order
//                    Term x = et0.unneg();
//                    et0 = et1.unneg();
//                    et1 = x;
//                }
//
//                Op o0 = et0.op();
//                if (et1.equalsNeg(et0)) {
//                    return o0 == Op.NEG || et0 == Bool.False ? Bool.False : Bool.True;
//                }
//
//
//                /** non-bool vs. bool - invalid */
//                if (Op.isTrueOrFalse(et0) || Op.isTrueOrFalse(et1)) {
//                    return Null;
//                }
//
//                /* deny temporal terms which can collapse degeneratively on conceptualization
//                *  TODO - for SET/SECT also? */
//                if (!uniqueRoots(et0.unneg(), et1.unneg()))
//                    return Null;
//
//                Op o1 = et1.op();
//
//                if (et0.containsRecursively(et1, true, Op.recursiveCommonalityDelimeterWeak)
//                        || et1.containsRecursively(et0, true, Op.recursiveCommonalityDelimeterWeak))
//                    return Null;
//
//
//                Op set = op == Op.DIFFe ? Op.SETe : Op.SETi;
//                if ((o0 == set && o1 == setAt)) {
//                    return differenceSet(setAt, et0, et1);
//                } else {
//                    return differenceSect(op, et0, et1);
//                }
//
//
//        }
//
//        throw new TermException(op, t, "diff requires 2 terms");
//
//    }

//    private static Term differenceSect(Op diffOp, Term a, Term b) {
//
//
//        Op ao = a.op();
//        if (((diffOp == Op.DIFFi && ao == Op.SECTe) || (diffOp == Op.DIFFe && ao == Op.SECTi)) && (b.op() == ao)) {
//            Subterms aa = a.subterms();
//            Subterms bb = b.subterms();
//            MutableSet<Term> common = Subterms.intersect(aa, bb);
//            if (common != null) {
//                int cs = common.size();
//                if (aa.subs() == cs || bb.subs() == cs)
//                    return Null;
//                return ao.the(common.with(
//                        diffOp.the(ao.the(aa.subsExcept(common)), ao.the(bb.subsExcept(common)))
//                ));
//            }
//        }
//
//
//        if (((diffOp == Op.DIFFi && ao == Op.SECTi) || (diffOp == Op.DIFFe && ao == Op.SECTe)) && (b.op() == ao)) {
//            Subterms aa = a.subterms(), bb = b.subterms();
//            MutableSet<Term> common = Subterms.intersect(aa, bb);
//            if (common != null) {
//                int cs = common.size();
//                if (aa.subs() == cs || bb.subs() == cs)
//                    return Null;
//                return ao.the(common.collect(Term::neg).with(
//                        diffOp.the(ao.the(aa.subsExcept(common)), ao.the(bb.subsExcept(common)))
//                ));
//            }
//        }
//
//        return Op.compound(diffOp, DTERNAL, a, b);
//    }

    /*@NotNull*/
    public static Term differenceSet(/*@NotNull*/ Op o, Term a, Term b) {

        assert (o.isSet() && a.op() == o && b.op() == o);

        if (a.equals(b))
            return Null;

        Subterms aa = a.subterms();

        int size = aa.subs();
        MetalBitSet removals = MetalBitSet.bits(size);

        for (int i = 0; i < size; i++) {
            Term x = aa.sub(i);
            if (b.contains(x)) {
                removals.set(i);
            }
        }

        int retained = size - removals.cardinality();
        if (retained == size) {
            return a;
        } else if (retained == 0) {
            return Null;
        } else {
            return o.the(HeapTermBuilder.the, aa.subsExcluding(removals));
        }

    }

}
