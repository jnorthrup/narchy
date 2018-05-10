package nars.unify.constraint;

import nars.Op;
import nars.term.Term;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

import java.util.Set;

import static nars.Op.CONJ;
import static nars.util.time.Tense.DTERNAL;

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
        if (xx.volume() < yy.volume())
            return commonEvents(xx, yy);
        else
            return commonEvents(yy, xx);
    }

    public boolean commonEvents(Term xx, Term yy) {
        Set<LongObjectPair<Term>> xe = new UnifiedSet(8);
        xx.eventsWhile((when,what)->{
            xe.add(PrimitiveTuples.pair(when, what));
            return true;
        }, 0, true, xx.dt()==DTERNAL, false, 0);

        final boolean[] common = {false};
        yy.eventsWhile((when,what)->{
            if (xe.remove(PrimitiveTuples.pair(when, what))) {
                common[0] = true;
                return false;
            }
            return true; //continue
        }, 0, true, yy.dt()==DTERNAL, false, 0);
        return common[0];
    }

    @Override
    public float cost() {
        return 0.75f;
    }
}
