package nars.derive.op;

import nars.derive.Derivation;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.util.var.DepIndepVarIntroduction;
import org.jetbrains.annotations.Nullable;

public final class IntroVars  {

    public static final Atomic VarIntro = Atomic.the("varIntro");

//    IntroVars() {
//        super(VarIntro);
//    }

//    @Override
    @Nullable public Term test(Term x, Derivation d) {

        if (x == null)
            return null;


        Term y = DepIndepVarIntroduction.the.apply(x, d.random, null);
        if (y == null || !y.unneg().op().conceptualizable || y.equals(x)  /* keep only if it differs */)
            return null;

//        Map<Term, Term> changes = xy.getTwo();
//        changes.forEach((cx,cy)->d.untransform.put(cy, cx));

        return y;
    }
}
