package nars.unify.constraint;

import nars.subterm.util.SubtermCondition;
import nars.term.Term;
import nars.term.Variable;
import org.jetbrains.annotations.Nullable;

public class SubOfConstraint extends RelationConstraint {
    private final boolean forward;

    private final SubtermCondition containment;


    /**
     * containment of the term positively (normal), negatively (negated), or either (must test both)
     */
    private final int polarityCompare;

    public SubOfConstraint(Variable x, Variable y, SubtermCondition contains) {
        this(x, y, contains, +1);
    }

    public SubOfConstraint(Variable x, Variable y, SubtermCondition contains, int polarityCompare) {
        this(x, y, false, contains, polarityCompare);
    }

    @Override
    protected @Nullable RelationConstraint newMirror(Variable newX, Variable newY) {
        return new SubOfConstraint(newX, newY, true, containment, polarityCompare);
    }

    private SubOfConstraint(Variable x, Variable y, /* HACK change to forward semantics */ boolean reverse, SubtermCondition contains, int polarityCompare) {
        super(contains.name() +
                (!reverse ? "->" : "<-") +
                (polarityCompare != 0 ? (polarityCompare == -1 ? "(-)" : "(+)") : "(+|-)"), x, y
        );


        this.forward = !reverse;
        this.containment = contains;
        this.polarityCompare = polarityCompare;
    }


    @Override
    public float cost() {
        return containment.cost();
    }

    public final boolean invalid(Term xx, Term yy) {
        /** x polarized */
        Term contentP = (forward ? yy : xx).negIf(polarityCompare < 0);
        Term container = forward ? xx : yy;

        boolean posAndNeg = polarityCompare==0;

        return !containment.test(container, contentP, posAndNeg);
    }

    public final boolean valid(Term x, Term y) {
        return !invalid(x, y);
    }

}
