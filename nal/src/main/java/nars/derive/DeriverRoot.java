package nars.derive;

import nars.$;
import nars.control.Derivation;
import nars.control.ProtoDerivation;
import nars.term.pred.AbstractPred;
import nars.term.pred.PrediTerm;

public final class DeriverRoot extends AbstractPred<Derivation> {

    public final PrediTerm<Derivation> what;
    public final Try can;

    public DeriverRoot(PrediTerm<Derivation> what, Try can) {
        super($.p(what, can ));
        this.what = what;
        this.can = can;
    }

    @Override
    public boolean test(Derivation x) {
        int ttl = x.ttl;
        x.ttl = Integer.MAX_VALUE;

        what.test(x);

        x.ttl = ttl;  //HACK forward the specified TTL for use during the possibility phase

        can.test(x);

        return true;
    }
}
