package nars.derive.op;

import nars.derive.Derivation;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.util.var.DepIndepVarIntroduction;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class IntroVars  {

    public static final Atomic VarIntro = Atomic.the("varIntro");

//    IntroVars() {
//        super(VarIntro);
//    }

//    @Override
    @Nullable public Term test(Term x, Derivation d) {

        if (x == null)
            return null;


        @Nullable Pair<Term, Map<Term, Term>> xy = DepIndepVarIntroduction.the.apply(x, d.random);
        if (xy == null)
            return null;

        Term y = xy.getOne();

        if (!y.unneg().op().conceptualizable ||
            y.equals(x)  /* keep only if it differs */
        )
            return null;

//        Map<Term, Term> changes = xy.getTwo();
//        changes.forEach((cx,cy)->d.untransform.put(cy, cx));

        return y;
    }
}
