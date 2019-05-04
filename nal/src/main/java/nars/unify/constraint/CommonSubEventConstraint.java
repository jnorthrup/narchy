package nars.unify.constraint;

import nars.term.Term;
import nars.term.Variable;
import nars.term.util.conj.Conj;
import nars.unify.Unify;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

import static nars.Op.CONJ;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.ETERNAL;

public final class CommonSubEventConstraint extends RelationConstraint {

    public CommonSubEventConstraint(Variable x, Variable y) {
        super("eventCommon", x, y);
    }

    @Override
    protected @Nullable RelationConstraint newMirror(Variable newX, Variable newY) {
        return new CommonSubEventConstraint(newX, newY);
    }

    @Override
    public boolean invalid(Term xx, Term yy, Unify context) {
        if (xx.op()!=CONJ || yy.op()!=CONJ)
            return true;

        if (!Term.commonStructure(xx.subterms(), yy.subterms()))
            return true;

        return !haveCommonEvents(xx, yy);
    }

    private static boolean haveCommonEvents(Term xx, Term yy) {
        return xx.volume() < yy.volume() ? _haveCommonEvents(xx, yy) : _haveCommonEvents(yy, xx);
    }

    /** xx smaller */
    private static boolean _haveCommonEvents(Term xx, Term yy) {
        if (Conj.concurrent(xx.dt()) && Conj.concurrent(yy.dt())) {
            return yy.subterms().containsAny(xx.subterms());
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
        return xx.eventsWhile(adder, xx.dt() == DTERNAL ? ETERNAL : 0, true, xx.dt()==DTERNAL, false);
    }

    @Override
    public float cost() {
        return 0.75f;
    }
}
