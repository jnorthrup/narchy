package nars.unify.constraint;

import jcog.data.list.FasterList;
import nars.term.Term;
import nars.term.control.AbstractPred;
import nars.term.control.PREDICATE;
import nars.unify.Unify;

import java.util.function.BiFunction;

public abstract class ConstraintAsPredicate<U extends Unify, C extends UnifyConstraint<U>> extends AbstractPred<U> {

    protected final C constraint;

    /** taskterm, beliefterm -> extracted */
    protected final BiFunction<Term,Term,Term> extractX;
    protected final BiFunction<Term,Term,Term> extractY;
    private final float cost;

    protected ConstraintAsPredicate(Term id, C m, BiFunction<Term, Term, Term> extractX, BiFunction<Term, Term, Term> extractY, float cost) {
        super(id);
        this.constraint = m;
        this.cost = cost;
        this.extractX = extractX;
        this.extractY = extractY;
    }

    /** TODO generify further to U extends Unify */


    @Override
    public float cost() {
        return cost;
    }

}
