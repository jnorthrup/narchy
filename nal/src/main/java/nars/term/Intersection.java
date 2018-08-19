package nars.term;

import nars.Op;
import nars.op.SetFunc;
import nars.term.atom.Bool;
import nars.unify.match.EllipsisMatch;
import nars.unify.match.Ellipsislike;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.Set;
import java.util.TreeSet;

import static java.util.Arrays.copyOfRange;
import static nars.Op.Null;
import static nars.Op.Temporal;
import static nars.time.Tense.DTERNAL;

public class Intersection {

    public static Term intersect(Term[] t, /*@NotNull*/ Op intersection, /*@NotNull*/ Op setUnion, /*@NotNull*/ Op setIntersection) {

        int trues = 0, temporals = 0;
        for (Term x : t) {
            if (x instanceof Bool) {
                if (x == Op.True) {
                    trues++;
                } else if (x == Null || x == Op.False) {
                    return Null;
                }
            } else if (x instanceof Compound && x.hasAny(Temporal)) {
                temporals++;
            }
        }

        if (temporals > 1) {
            //repeat terms of the same root would collapse when conceptualized so this is prevented
            Set<Term> roots = new UnifiedSet(temporals);
            for (Term x : t) {
                if (x instanceof Compound && x.hasAny(Op.Temporal)) {
                    if (!roots.add(x.root())) {
                        //repeat detected
                        return Null;
                    }
                }
            }
        }

        if (trues > 0) {
            if (trues == t.length) {
                return Op.True;
            } else if (t.length - trues == 1) {

                for (Term x : t) {
                    if (x != Op.True)
                        return x;
                }
            } else {

                Term[] t2 = new Term[t.length - trues];
                int yy = 0;
                for (Term x : t) {
                    if (x != Op.True)
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
                if (single instanceof EllipsisMatch) {
                    return intersect(single.arrayShared(), intersection, setUnion, setIntersection);
                }
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

    /*@NotNull*/
    @Deprecated
    private static Term intersect2(Term term1, Term term2, /*@NotNull*/ Op intersection, /*@NotNull*/ Op setUnion, /*@NotNull*/ Op setIntersection) {

        if (term1.equals(term2))
            return term1;

        Op o1 = term1.op();
        Op o2 = term2.op();

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

}
