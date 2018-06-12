package nars.unify.op;

import nars.$;
import nars.Op;
import nars.derive.Derivation;
import nars.term.Term;
import nars.term.control.AbstractPred;
import org.jetbrains.annotations.Nullable;

public class PathStructure extends AbstractPred<Derivation> {

    private final int struct;

    @Nullable
    private final byte[] pathInTask;
    @Nullable
    private final byte[] pathInBelief;
    private final boolean isOrIsnt;

    PathStructure(boolean isOrIsnt, int struct, byte[] pathInTask, byte[] pathInBelief) {
        super($.func(TaskBeliefHas.has,
                pathInBelief == null ? Derivation.Task : Derivation.Belief,
                $.pFast(pathInBelief == null ? pathInTask : pathInBelief),
                Op.strucTerm(struct)
        ).negIf(!isOrIsnt));
        assert(pathInBelief==null ^ pathInTask==null): "only one should be used the other remain null"; //HACK
        this.isOrIsnt = isOrIsnt;
        this.struct = struct;
        this.pathInTask = pathInTask;
        this.pathInBelief = pathInBelief;
    }


    @Override
    public float cost() {
        return 0.2f;
    }

    @Override
    public boolean test(Derivation o) {
        if (pathInTask != null) {
            Term T = o.taskTerm;
            boolean Thas = T.hasAny(struct);
            if (isOrIsnt && !Thas)
                return false;
            if (isOrIsnt || Thas) {
                Term s = T.subPath(pathInTask);
                if (s == null || isOrIsnt != s.isAny(struct))
                    return false;
            }
        }
        if (pathInBelief != null) {
            Term B = o.beliefTerm;
            boolean Bhas = B.hasAny(struct);
            if (isOrIsnt && !Bhas)
                return false;
            if (isOrIsnt || Bhas) {
                Term s = B.subPath(pathInBelief);
                if (s == null || isOrIsnt != s.isAny(struct))
                    return false;
            }
        }
        return true;
    }
}
