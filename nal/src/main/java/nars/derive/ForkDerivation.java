package nars.derive;

import jcog.TODO;
import nars.term.pred.Fork;
import nars.term.pred.PrediTerm;

import java.util.function.Function;

public class ForkDerivation<D extends PreDerivation> extends Fork<D> {

    protected ForkDerivation(PrediTerm<D>[] actions) {
        super(actions);
    }

   @Override
    @Deprecated public PrediTerm<D> transform(Function<PrediTerm<D>, PrediTerm<D>> f) {
        //return fork(PrediTerm.transform(f, branches), ForkDerivation::new);
       throw new TODO();
    }

    /**
     * simple exhaustive impl
     */
    @Override
    public boolean test(PreDerivation m) {

        int before = m.now();

        for (PrediTerm c : branch) {

            c.test(m);

            if (!m.revertLive(before)) { //maybe possible to eliminate for pre's
                return false;
            }
        }

        return true;
    }

}
