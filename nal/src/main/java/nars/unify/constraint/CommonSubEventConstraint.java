package nars.unify.constraint;

import nars.term.Term;
import nars.term.Variable;
import nars.term.util.conj.ConjUnify;
import nars.unify.Unify;
import org.jetbrains.annotations.Nullable;

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
        return !ConjUnify.eventsCommon(xx, yy);
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
