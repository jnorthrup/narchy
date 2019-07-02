package nars.unify.constraint;

import jcog.WTF;
import nars.$;
import nars.term.Term;
import nars.term.Variable;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.unify.Unify;
import org.jetbrains.annotations.Nullable;

import static nars.Op.NEG;

/** tests a relation between two terms which may be involved (and prevened) from unifying */
abstract public class RelationConstraint<U extends Unify> extends UnifyConstraint<U> {


    public final Variable y;

    private RelationConstraint(Term id, Variable x, Variable y) {
        super(id, x);
        assert(!x.equals(y));
        this.y = y;
    }

    @Deprecated
    protected RelationConstraint(String func, Variable x, Variable y, Term... args) {
        this(Atomic.atom(func), x, y, args);
    }

    protected RelationConstraint(Atom func, Variable x, Variable y, Term... args) {
        this($.func(func, x, args.length > 0 ? $.p(y, $.p(args)) : y), x, y);
    }

    public final RelationConstraint<U> mirror() {
        return newMirror(y, x);
    }

    /** provide the reversed (mirror) constraint */
    abstract protected RelationConstraint<U> newMirror(Variable newX, Variable newY);

    public RelationConstraint<U> neg() {
        return new NegRelationConstraint(this);
    }

    @Override
    public final RelationConstraint<U> negIf(boolean negate) {
        return negate ? neg() : this;
    }

    @Override
    public final boolean invalid(Term x, U f) {
        Term yy = f.resolveVar(y);
        return yy != y && invalid(x, yy, f);
    }

    abstract public boolean invalid(Term x, Term y, U context);

    /** override to implement subsumption elimination */
    public boolean remainInAndWith(RelationConstraint<U> c) {
        return true;
    }

    public static final class NegRelationConstraint<U extends Unify> extends RelationConstraint<U> {

        public final RelationConstraint<U> r;

        private NegRelationConstraint(RelationConstraint<U> r) {
            super(r.ref.neg(), r.x, r.y);
            this.r = r;
            assert(!(r instanceof NegRelationConstraint) && (r.ref.op()!=NEG));
        }

        @Override
        public boolean remainInAndWith(RelationConstraint<U> c) {
            if (c.equals(r))
                throw new WTF(this + " present in a rule with its opposite " + c);
            return true;
        }

        @Override
        protected @Nullable RelationConstraint newMirror(Variable newX, Variable newY) {
            return new NegRelationConstraint(this);
            //return new NegRelationConstraint(r.mirror());
        }

        @Override
        public RelationConstraint neg() {
            return r;
        }

        @Override
        public boolean invalid(Term x, Term y, U context) {
            return !r.invalid(x, y, context);
        }

        @Override
        public float cost() {
            return r.cost() + 0.001f;
        }


    }
}
