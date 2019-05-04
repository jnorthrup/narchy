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
    private final boolean yNeg;

    public RelationConstraint(Term id, Variable x, Variable y) {
        super(id, x);
        assert(!x.equals(y));
        this.y = y;
        this.yNeg = y.op()==NEG;
    }

    @Deprecated RelationConstraint(String func, Variable x, Variable y, Term... args) {
        this(Atomic.atom(func), x, y, args);
    }

    RelationConstraint(Atom func, Variable x, Variable y, Term... args) {
        this($.func(func, x, args.length > 0 ? $.p(y, $.p(args)) : y), x, y);
    }

    public final RelationConstraint mirror() {
        return newMirror(y, x);
    }

    /** provide the reversed (mirror) constraint */
    abstract protected RelationConstraint newMirror(Variable newX, Variable newY);

    public RelationConstraint neg() {
        return new NegRelationConstraint(this);
    }

    @Override
    public RelationConstraint negIf(boolean negate) {
        return negate ? neg() : this;
    }



    @Override
    public final boolean invalid(Term x, U f) {
        Term yy = f.resolve(y);
        return yy != y && invalid(x, yNeg ? yy.neg() : yy, f);
    }

    abstract public boolean invalid(Term xx, Term yy, U context);

    /** override to implement subsumption elimination */
    public boolean remainInAndWith(RelationConstraint c) {
        return true;
    }

    public static final class NegRelationConstraint extends RelationConstraint {

        public final RelationConstraint r;

        private NegRelationConstraint(RelationConstraint r) {
            super(r.ref.neg(), r.x, r.y);
            this.r = r;
            assert(!(r instanceof NegRelationConstraint) && (r.ref.op()!=NEG));
        }

        @Override
        public boolean remainInAndWith(RelationConstraint c) {
            if (c.equals(r))
                throw new WTF(this + " present in a rule with its opposite " + c);
            return true;
        }

        @Override
        protected @Nullable RelationConstraint newMirror(Variable newX, Variable newY) {
            return new NegRelationConstraint(r.mirror());
        }

        @Override
        public RelationConstraint neg() {
            return r;
        }

        @Override
        public boolean invalid(Term xx, Term yy, Unify context) {
            return !r.invalid(xx, yy, context);
        }

        @Override
        public float cost() {
            return r.cost() + 0.001f;
        }


    }
}
