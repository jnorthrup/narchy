package nars.unify.constraint;

import nars.term.Term;
import nars.term.control.AbstractPred;

import java.util.function.BiFunction;

public abstract class ConstraintAsPredicate<U, C extends UnifyConstraint> extends AbstractPred<U> {

    protected final C constraint;

    /** taskterm, beliefterm -> extracted */
    protected final BiFunction<Term,Term,Term> extractX;
    protected final BiFunction<Term,Term,Term> extractY;
    private final float cost;

    protected ConstraintAsPredicate(Term id, float cost, C m, BiFunction<Term, Term, Term> extractX, BiFunction<Term, Term, Term> extractY) {
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
