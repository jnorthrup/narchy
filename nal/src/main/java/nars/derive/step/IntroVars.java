package nars.derive.step;

import nars.$;
import nars.Task;
import nars.derive.Derivation;
import nars.op.DepIndepVarIntroduction;
import nars.term.Term;
import nars.term.control.AbstractPred;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class IntroVars extends AbstractPred<Derivation> {

    public static final Term VAR_INTRO = $.the("varIntro");

    public IntroVars() {
        super(VAR_INTRO);
    }

    @Override
    public boolean test(Derivation d) {
        final Term x = d.derivedTerm;


        @Nullable Pair<Term, Map<Term, Term>> xy = DepIndepVarIntroduction.the.apply(x, d.random);
        if (xy == null)
            return false;

        final Term y = xy.getOne();

        if (!y.unneg().op().conceptualizable ||
            y.equals(x) || /* keep only if it differs */
            
            !Task.validTaskTerm(y, d.concPunc, true)
        )
            return false;


        Map<Term, Term> changes = xy.getTwo();
        changes.forEach((cx,cy)->d.untransform.put(cy, cx));
        d.derivedTerm = y;




        return true;
    }
}
