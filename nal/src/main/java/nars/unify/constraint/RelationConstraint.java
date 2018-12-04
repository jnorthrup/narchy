package nars.unify.constraint;

import nars.$;
import nars.derive.premise.PreDerivation;
import nars.term.Term;
import nars.term.Terms;
import nars.term.control.PREDICATE;
import nars.unify.Unify;
import org.jetbrains.annotations.Nullable;

import static nars.Op.NEG;

/** tests a relation between two terms which may be involved (and prevened) from unifying */
abstract public class RelationConstraint extends UnifyConstraint {


    protected final Term y, yUnneg;
    protected final boolean yNeg;

    private RelationConstraint(Term id, Term x, Term y) {
        super(id, x);
        this.y = y;
        this.yUnneg = y.unneg();
        this.yNeg = y.op()==NEG;
    }

    protected RelationConstraint(String func, Term x, Term y, Term... args) {
        this($.func(func, x, args.length > 0 ? $.pFast(y, $.pFast(args)) : y), x, y);
        assert(!x.equals(y));
    }

    public final RelationConstraint mirror() {
        return newMirror(y, x);
    }

    /** provide the reversed (mirror) constraint */
    abstract protected RelationConstraint newMirror(Term newX, Term newY);

    public RelationConstraint neg() {
        return new NegRelationConstraint(this);
    }

    @Override
    public RelationConstraint negIf(boolean negate) {
        return negate ? neg() : this;
    }

    @Override
    public @Nullable PREDICATE<PreDerivation> preFilter(Term taskPattern, Term beliefPattern) {

//        //only test one of the directions
//        // because the opposite y->x will also be created so we only need one predicate filter for both
//        if (x.compareTo(y) > 0) {
//            RelationConstraint m = mirror();
//            if (m.x.compareTo(m.y) > 0)
//                throw new WTF();
//            return m.preFilter(taskPattern, beliefPattern); //use a canonical ordering to maximize chances of trie alignment
//        }


        byte[] xInTask = Terms.pathConstant(taskPattern, x);
        byte[] xInBelief = Terms.pathConstant(beliefPattern, x);
        if (xInTask!=null || xInBelief!=null) {
            byte[] yInTask = Terms.pathConstant(taskPattern, y);
            byte[] yInBelief = Terms.pathConstant(beliefPattern, y);
            if ((yInTask != null || yInBelief != null)) {
                return new ConstraintAsPredicate(this, xInTask, xInBelief, yInTask, yInBelief);
            }
        }

        return null;
    }

    @Override
    public boolean invalid(Term xx, Unify f) {
        Term yy = f.transform(yUnneg);
        return yy != yUnneg
                &&
               invalid(xx, yy.negIf(yNeg));
    }

    abstract public boolean invalid(Term xx, Term yy);

    /** override to implement subsumption elimination */
    public boolean remainInAndWith(RelationConstraint c) {
        return true;
    }

    static final class NegRelationConstraint extends RelationConstraint {

        private final RelationConstraint r;

        private NegRelationConstraint(RelationConstraint r) {
            super(r.ref.neg(), r.x, r.y);
            this.r = r;
            assert(!(r instanceof NegRelationConstraint) && (r.ref.op()!=NEG));
        }

        @Override
        protected @Nullable RelationConstraint newMirror(Term newX, Term newY) {
            return new NegRelationConstraint(r.mirror());
        }

        @Override
        public RelationConstraint neg() {
            return r;
        }

        @Override
        public boolean invalid(Term xx, Term yy) {
            return !r.invalid(xx, yy);
        }

        @Override
        public boolean invalid(Term xx, Unify f) {
            return !r.invalid(xx, f);
        }

        @Override
        public float cost() {
            return r.cost();
        }

        @Override
        public @Nullable PREDICATE<PreDerivation> preFilter(Term taskPattern, Term beliefPattern) {
            PREDICATE<PreDerivation> p = r.preFilter(taskPattern, beliefPattern);
            return p != null ? p.neg() : null;
        }
    }
}
