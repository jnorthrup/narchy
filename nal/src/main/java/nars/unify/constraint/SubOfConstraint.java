package nars.unify.constraint;

import nars.subterm.util.SubtermCondition;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Variable;
import nars.unify.Unify;

public class SubOfConstraint<U extends Unify> extends RelationConstraint<U> {
    private final boolean forward;

    private final SubtermCondition containment;


    /**
     * containment of the target positively (normal), negatively (negated), or either (must test both)
     */
    private final int polarityCompare;

    public SubOfConstraint(Variable x, Variable y, SubtermCondition contains) {
        this(x, y, contains, +1);
    }

    public SubOfConstraint(Variable x, Variable y, SubtermCondition contains, int polarityCompare) {
        this(x, y, false, contains, polarityCompare);
    }

    @Override
    protected RelationConstraint newMirror(Variable newX, Variable newY) {
        return new SubOfConstraint(newX, newY, true, containment, polarityCompare);
    }

    private SubOfConstraint(Variable x, Variable y, /* HACK change to forward semantics */ boolean reverse, SubtermCondition contains, int polarityCompare) {
        super(contains.name() +
                (!reverse ? "->" : "<-") +
                (polarityCompare != 0 ? (polarityCompare == -1 ? "(-)" : "(+)") : "(+|-)"), x, y
        );

        assert(!x.equals(y));

        this.forward = !reverse;
        this.containment = contains;
        this.polarityCompare = polarityCompare;
    }


    @Override
    public float cost() {
        var baseCost = containment.cost();
        switch (polarityCompare) {
            case 1: return baseCost;
            case 0: return 1.9f * baseCost;
            case -1: return 1.1f * baseCost;
            default:
                throw new UnsupportedOperationException();
        }
    }

    public final boolean invalid(Term x, Term y, Unify context) {

        var _container = forward ? x : y;
        if (!(_container instanceof Compound))
            return true;
        var container = (Compound)_container;


        var content = forward ? y : x;
//        if (container.volume() <= content.volume())
//            return true; //doesnt seem to happen, probably eliminated by bigger-than constraint

        var c = this.containment;
        switch (polarityCompare) {
            case +1: return !c.test(container, content);
            case -1: return !c.test(container, content.neg());
            case  0: return !c.test(container, content) && !c.test(container, content.neg());
            default:
                throw new UnsupportedOperationException();
        }

    }



}
