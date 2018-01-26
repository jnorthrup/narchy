package nars.derive;

import nars.$;
import nars.Op;
import nars.control.Derivation;
import nars.op.DepIndepVarIntroduction;
import nars.term.Term;
import nars.term.pred.AbstractPred;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class IntroVars extends AbstractPred<Derivation> {

    protected IntroVars() {
        super($.the("varIntro"));
    }

    @Override
    public boolean test(Derivation p) {
        Term x = p.derivedTerm.get();

        @Nullable Pair<Term, Map<Term, Term>> xy = DepIndepVarIntroduction.varIntroX(x, p.random);
        if (xy == null)
            return false;

        Term y = xy.getOne();
        if (!(y.op().conceptualizable) || (y.equals(x) /* keep only if it differs */)) {
            return false;
        } else {
            if (!y.hasAny(Op.ConstantAtomics)) {
                return false; //entirely variablized
            } else {
                //Map<Term, Term> changes = xy.getTwo();
                //p.xy.putAll(changes);
                p.derivedTerm.set(y);
                return true;
            }
        }
    }
}
