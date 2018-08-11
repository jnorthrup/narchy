package nars.unify.constraint;

import nars.Op;
import nars.subterm.Subterms;
import nars.term.Term;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

import java.util.Set;

import static nars.Op.CONJ;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.ETERNAL;

public class CommonSubEventConstraint extends RelationConstraint {

    public CommonSubEventConstraint(Term x, Term y) {
        super(x, y, "eventCommon");
    }

    @Override
    public boolean invalid(Term xx, Term yy) {
        if (xx.op()!=CONJ || yy.op()!=CONJ)
            return true;

        if (!Term.commonStructure(xx.subterms(), yy.subterms()))
            return true;

        return !haveCommonEvents(xx, yy);
    }

    static boolean haveCommonEvents(Term xx, Term yy) {
        return xx.volume() < yy.volume() ? _haveCommonEvents(xx, yy) : _haveCommonEvents(yy, xx);
    }

    static boolean _haveCommonEvents(Term xx, Term yy) {
        if (Op.concurrent(xx.dt()) && Op.concurrent(yy.dt())) {
            Subterms xxx = xx.subterms();
            if (xx.subs() < 3) {
                return yy.subterms().OR(xxx::contains);
            } else {
                MutableSet<Term> xs = xxx.toSet();
                return yy.subterms().OR(xs::contains);
            }
        } else {

            Set<LongObjectPair<Term>> xe = new UnifiedSet<>(4);

            LongObjectPredicate<Term> adder = (when, what) -> {
                xe.add(PrimitiveTuples.pair(when, what));
                return true;
            };
            scan(xx, adder);

            return !scan(yy, (when, what) -> !xe.remove(PrimitiveTuples.pair(when, what)));
        }

    }

    private static boolean scan(Term xx, LongObjectPredicate<Term> adder) {
        return xx.eventsWhile(adder, xx.dt() == DTERNAL ? ETERNAL : 0, true, xx.dt()==DTERNAL, false, 0);
    }

    @Override
    public float cost() {
        return 0.75f;
    }
}
