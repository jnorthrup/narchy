package nars.unify.op;

import nars.$;
import nars.derive.Derivation;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;
import org.jetbrains.annotations.Nullable;

public class SubtermMatch extends AbstractPred<Derivation> {


    @Nullable
    public final byte[] pathInTask;
    @Nullable
    public final byte[] pathInBelief;

    public final boolean trueOrFalse;

    private final TermMatch match;

    SubtermMatch(boolean trueOrFalse, TermMatch m, byte[] pathInTask, byte[] pathInBelief) {
        super($.func(Atomic.the(m.getClass().getSimpleName()),m.param(),
                pathInBelief == null ? Derivation.Task : Derivation.Belief,
                $.pFast(pathInBelief == null ? pathInTask : pathInBelief)

        ).negIf(!trueOrFalse));
        assert(pathInBelief==null ^ pathInTask==null): "only one should be used the other remain null"; //HACK
        this.trueOrFalse = trueOrFalse;
        this.match = m;
        this.pathInTask = pathInTask;
        this.pathInBelief = pathInBelief;
    }

    @Override
    public float cost() {
        return 0.2f;
    }

    @Override
    public boolean test(Derivation d) {
        return (pathInTask == null || test(d.taskTerm, pathInTask)) &&
               (pathInBelief == null || test(d.beliefTerm, pathInBelief));
    }

    private boolean test(Term superTerm, byte[] subPath) {
        boolean superOK = match.testSuper(superTerm) == trueOrFalse;
        if (!superOK)
            return false;
        Term subTarget = superTerm.subPath(subPath);
        return subTarget!=null && (match.test(subTarget)) == trueOrFalse;
    }

}
