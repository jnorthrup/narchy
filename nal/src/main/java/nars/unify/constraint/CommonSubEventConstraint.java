package nars.unify.constraint;

import nars.Op;
import nars.term.Term;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;
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

        //pre-filter
        int xxs = xx.subterms().structure();
        int yys = yy.subterms().structure();
        if (!(Op.hasAll(xxs, yys) || Op.hasAll(yys, xxs)))
            return true;

        //construct the set with the smaller of the two
        return !haveCommonEvents(xx, yy);
    }

    static boolean haveCommonEvents(Term xx, Term yy) {
        return xx.volume() < yy.volume() ? _haveCommonEvents(xx, yy) : _haveCommonEvents(yy, xx);
    }

    static boolean _haveCommonEvents(Term xx, Term yy) {

        Set<LongObjectPair<Term>> xe = new UnifiedSet<>(4);

        LongObjectPredicate<Term> adder = (when, what) -> {
            xe.add(PrimitiveTuples.pair(when, what));
            return true;
        };
        scanCommonEvents(xx, adder);

        final boolean[] common = {false};
        LongObjectPredicate<Term> remover = (when, what) -> {
            if (xe.remove(PrimitiveTuples.pair(when, what))) {
                common[0] = true;
                return false; //found at least 1, done
            }
            return true; //continue
        };

        scanCommonEvents(yy, remover);

        return common[0];
    }

    private static boolean scanCommonEvents(Term xx, LongObjectPredicate<Term> adder) {
        return xx.eventsWhile(adder, xx.dt() == DTERNAL ? ETERNAL : 0, true, xx.dt()==DTERNAL, false, 0);
    }

    @Override
    public float cost() {
        return 0.75f;
    }
}
