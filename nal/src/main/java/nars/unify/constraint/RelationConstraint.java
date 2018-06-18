package nars.unify.constraint;

import nars.$;
import nars.derive.Derivation;
import nars.term.Term;
import nars.term.control.PrediTerm;
import nars.unify.Unify;
import org.jetbrains.annotations.Nullable;

import static nars.Op.NEG;

/** tests a relation between two terms which may be involved (and prevened) from unifying */
abstract public class RelationConstraint extends MatchConstraint {


    protected final Term y, yUnneg;
    protected final boolean yNeg;

    protected RelationConstraint(Term x, Term y, String func, Term... args) {
        super(x, func, args.length > 0 ? $.p(y, $.p(args)) : y);
        this.y = y;
        this.yUnneg = y.unneg();
        this.yNeg = y.op()==NEG;
    }

    @Override
    public @Nullable PrediTerm<Derivation> preFilter(Term taskPattern, Term beliefPattern) {
        
        if (x.equals(taskPattern) && y.equals(beliefPattern)) {
            return new ConstraintAsPredicate(this, true);
        }
        /*else if (y.equals(beliefPattern) && x.equals(beliefPattern)){
            return new ConstraintAsPredicate(this, false);
        }*/
        return null;
    }

    @Override
    public final boolean invalid(Term xx, Unify f) {
        Term yy = f.apply(yUnneg);
        return yy != yUnneg
                &&
               invalid(xx, yy.term().negIf(yNeg));
    }

    abstract public boolean invalid(Term xx, Term yy);

}
