package nars.term.util;

import jcog.data.bit.MetalBitSet;
import nars.Op;
import nars.op.SetFunc;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.unify.match.EllipsisMatch;
import nars.unify.match.Ellipsislike;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.Set;
import java.util.TreeSet;

import static java.util.Arrays.copyOfRange;
import static nars.term.atom.Bool.Null;
import static nars.time.Tense.DTERNAL;

/** NAL2/NAL3 intersection and difference functions */
public class SetSectDiff {

    public static Term intersect(Term[] t, /*@NotNull*/ Op intersection, /*@NotNull*/ Op setUnion, /*@NotNull*/ Op setIntersection) {

        int trues = 0;
        for (Term x : t) {
            if (x instanceof Bool) {
                if (x == Bool.True) {
                    trues++;
                } else if (x == Null || x == Bool.False) {
                    return Null;
                }
            }
        }

        if (t.length> 1) {
            if (!uniqueRoots(t))
                return Null;
        }

        if (trues > 0) {
            if (trues == t.length) {
                return Bool.True;
            } else if (t.length - trues == 1) {

                for (Term x : t) {
                    if (x != Bool.True)
                        return x;
                }
            } else {

                Term[] t2 = new Term[t.length - trues];
                int yy = 0;
                for (Term x : t) {
                    if (x != Bool.True)
                        t2[yy++] = x;
                }
                t = t2;
            }
        }


        switch (t.length) {

            case 0:
                throw new RuntimeException();

            case 1:

                Term single = t[0];
                if (single instanceof EllipsisMatch)
                    return intersect(
                            single.arrayShared(),
                            intersection, setUnion, setIntersection);

                return single instanceof Ellipsislike ?
                        Op.compound(intersection, DTERNAL, single) :
                        single;

            case 2:
                return intersect2(t[0], t[1], intersection, setUnion, setIntersection);
            default:

                Term a = intersect2(t[0], t[1], intersection, setUnion, setIntersection);

                Term b = intersect(copyOfRange(t, 2, t.length), intersection, setUnion, setIntersection);

                return intersect2(a, b,
                        intersection, setUnion, setIntersection
                );
        }

    }

    public static boolean uniqueRoots(Term... t) {
        if (t.length == 2) {
            if (t[0] instanceof Compound && t[1] instanceof Compound && t[0].hasAny(Op.Temporal) && t[1].hasAny(Op.Temporal))
                if (t[0].equalsRoot(t[1]))
                    return false;
        } else {
            //repeat terms of the same root would collapse when conceptualized so this is prevented
            Set<Term> roots = null;
            for (int i = t.length-1; i >= 0; i--) {
                Term x = t[i];
                if (x instanceof Compound && x.hasAny(Op.Temporal)) {
                    if (roots == null) {
                        if (i == 0)
                            break; //last one, dont need to check
                        roots = new UnifiedSet(i);
                    }
                    if (!roots.add(x.root())) {
                        //repeat detected
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /*@NotNull*/
    @Deprecated
    private static Term intersect2(Term term1, Term term2, /*@NotNull*/ Op intersection, /*@NotNull*/ Op setUnion, /*@NotNull*/ Op setIntersection) {

        if (term1.equals(term2))
            return term1;

        Op o1 = term1.op(), o2 = term2.op();

        if ((o1 == setUnion) && (o2 == setUnion)) {

            return SetFunc.union(setUnion, term1.subterms(), term2.subterms());
        }


        if ((o1 == setIntersection) && (o2 == setIntersection)) {

            return SetFunc.intersect(setIntersection, term1.subterms(), term2.subterms());
        }

        if (o2 == intersection && o1 != intersection) {

            Term x = term1;
            term1 = term2;
            term2 = x;
            o2 = o1;
            o1 = intersection;
        }


        TreeSet<Term> args = new TreeSet<>();
        if (o1 == intersection) {
            ((Iterable<Term>) term1).forEach(args::add);
            if (o2 == intersection)
                ((Iterable<Term>) term2).forEach(args::add);
            else
                args.add(term2);
        } else {
            args.add(term1);
            args.add(term2);
        }

        int aaa = args.size();
        if (aaa == 1)
            return args.first();
        else {
            return Op.compound(intersection, DTERNAL, args.toArray(Op.EmptyTermArray));
        }
    }

    public static Term differ(/*@NotNull*/ Op op, Term... t) {


        switch (t.length) {
            case 1:
                Term single = t[0];
                if (single instanceof EllipsisMatch) {
                    return differ(op, single.arrayShared());
                }
                return single instanceof Ellipsislike ?
                        Op.compound(op, DTERNAL, single) :
                        Null;
            case 2:
                Term et0 = t[0], et1 = t[1];

                if (et0 == Null || et1 == Null)
                    return Null;


                if (et0.equals(et1))
                    return Bool.False;

                //((--,X)~(--,Y)) reduces to (Y~X)
                if (et0.op() == Op.NEG && et1.op() == Op.NEG) {
                    //un-neg and swap order
                    Term x = et0.unneg();
                    et0 = et1.unneg();
                    et1 = x;
                }

                Op o0 = et0.op();
                if (et1.equalsNeg(et0)) {
                    return o0 == Op.NEG || et0 == Bool.False ? Bool.False : Bool.True;
                }


                /** non-bool vs. bool - invalid */
                if (Op.isTrueOrFalse(et0) || Op.isTrueOrFalse(et1)) {
                    return Null;
                }

                /* deny temporal terms which can collapse degeneratively on conceptualization
                *  TODO - for SET/SECT also? */
                if (!uniqueRoots(et0.unneg(), et1.unneg()))
                    return Null;

                Op o1 = et1.op();

                if (et0.containsRecursively(et1, true, Op.recursiveCommonalityDelimeterWeak)
                        || et1.containsRecursively(et0, true, Op.recursiveCommonalityDelimeterWeak))
                    return Null;


                Op set = op == Op.DIFFe ? Op.SETe : Op.SETi;
                if ((o0 == set && o1 == set)) {
                    return differenceSet(set, et0, et1);
                } else {
                    return differenceSect(op, et0, et1);
                }


        }

        throw new TermException(op, t, "diff requires 2 terms");

    }

    private static Term differenceSect(Op diffOp, Term a, Term b) {


        Op ao = a.op();
        if (((diffOp == Op.DIFFi && ao == Op.SECTe) || (diffOp == Op.DIFFe && ao == Op.SECTi)) && (b.op() == ao)) {
            Subterms aa = a.subterms();
            Subterms bb = b.subterms();
            MutableSet<Term> common = Subterms.intersect(aa, bb);
            if (common != null) {
                int cs = common.size();
                if (aa.subs() == cs || bb.subs() == cs)
                    return Null;
                return ao.the(common.with(
                        diffOp.the(ao.the(aa.subsExcept(common)), ao.the(bb.subsExcept(common)))
                ));
            }
        }


        if (((diffOp == Op.DIFFi && ao == Op.SECTi) || (diffOp == Op.DIFFe && ao == Op.SECTe)) && (b.op() == ao)) {
            Subterms aa = a.subterms(), bb = b.subterms();
            MutableSet<Term> common = Subterms.intersect(aa, bb);
            if (common != null) {
                int cs = common.size();
                if (aa.subs() == cs || bb.subs() == cs)
                    return Null;
                return ao.the(common.collect(Term::neg).with(
                        diffOp.the(ao.the(aa.subsExcept(common)), ao.the(bb.subsExcept(common)))
                ));
            }
        }

        return Op.compound(diffOp, DTERNAL, a, b);
    }

    /*@NotNull*/
    public static Term differenceSet(/*@NotNull*/ Op o, Term a, Term b) {


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
            return o.the(aa.subsExcept(removals));
        }

    }
}
