package nars.derive.op;

import nars.derive.Derivation;
import nars.op.DepIndepVarIntroduction;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class IntroVars extends AbstractPred<Derivation> {

    public static final Atomic VarIntro = Atomic.the("varIntro");

    public IntroVars() {
        super(VarIntro);
    }

    @Override
    public boolean test(Derivation d) {
        final Term x = d.concTerm;
        if (x == null)
            return false;


        @Nullable Pair<Term, Map<Term, Term>> xy = DepIndepVarIntroduction.the.apply(x, d.random);
        if (xy == null)
            return false;

        Term y = xy.getOne();

        if (!y.unneg().op().conceptualizable ||
            y.equals(x)  /* keep only if it differs */
        )
            return false;

//        Map<Term, Term> changes = xy.getTwo();
//        changes.forEach((cx,cy)->d.untransform.put(cy, cx));
        d.concTerm = y;

        return true;
    }
}
