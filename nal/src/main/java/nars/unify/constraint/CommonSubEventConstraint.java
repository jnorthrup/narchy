package nars.unify.constraint;

import nars.term.Term;
import nars.term.Variable;
import nars.term.util.conj.Conj;
import nars.term.util.conj.ConjList;
import nars.unify.Unify;
import org.jetbrains.annotations.Nullable;

import static nars.Op.CONJ;
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
        if (xx.op()!=CONJ || yy.op()!=CONJ || !Term.commonStructure(xx.subterms(), yy.subterms()))
            return true;

        return !haveCommonEvents(xx, yy);
    }

    private static boolean haveCommonEvents(Term xx, Term yy) {
        return xx.volume() <= yy.volume() ? _haveCommonEvents(xx, yy) : _haveCommonEvents(yy, xx);
    }

    /** xx smaller */
    private static boolean _haveCommonEvents(Term x, Term y) {
        if (Conj.concurrent(x.dt()) && Conj.concurrent(y.dt())) {
            return y.subterms().containsAny(x.subterms());
        } else {

            ConjList xe = ConjList.events(x);

            //return !scan(yy, (when, what) -> !xe.remove(when, what));
            return !y.eventsAND((when, what)->!xe.remove(when,what),
                    Conj.isSeq(y) ? ETERNAL : 0, true, false);
        }

    }
//
//    private static boolean scan(Term xx, LongObjectPredicate<Term> adder) {
//        return xx.eventsWhile(adder,
//                xx.dt() == DTERNAL ? ETERNAL : 0,
//                xx.dt()==DTERNAL,
//                false);
//    }

    @Override
    public float cost() {
        return 0.75f;
    }
}
