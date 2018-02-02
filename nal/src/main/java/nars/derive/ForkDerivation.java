package nars.derive;

import nars.term.pred.Fork;
import nars.term.pred.PrediTerm;

import java.util.function.Function;

public class ForkDerivation<D extends ProtoDerivation> extends Fork<D> {

    protected ForkDerivation(PrediTerm<D>[] actions) {
        super(actions);
    }

   @Override
    public PrediTerm<D> transform(Function<PrediTerm<D>, PrediTerm<D>> f) {
        return fork(PrediTerm.transform(f, branches), x -> new ForkDerivation(x));
    }

    /**
     * simple exhaustive impl
     */
    @Override
    public boolean test(ProtoDerivation m) {

        int before = m.now();

        for (PrediTerm c : branches) {

            c.test(m);

            if (!m.revertLive(before)) { //maybe possible to eliminate for pre's
                return false;
            }
        }

        return true;
    }

}
