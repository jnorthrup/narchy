package nars.derive;

import jcog.memoize.HijackMemoize;
import jcog.memoize.Memoize;
import nars.control.Derivation;
import nars.control.ProtoDerivation;
import nars.term.pred.PrediTerm;

/** what -> can */
public final class DeriverRoot {

    public final PrediTerm<Derivation> what;
    public final Try can;

    public DeriverRoot(PrediTerm<Derivation> what, Try can) {
        //this.id = ($.p(what, can ));
        this.what = what;
        this.can = can;
    }

    Memoize<ProtoDerivation.PremiseKey,int[]> whatCached =
            new HijackMemoize<>(ProtoDerivation.PremiseKey::solve,
                    64 * 1024, 5, false);

    /** 1. CAN (proto) stage */
    public boolean proto(Derivation x) {
        x.can.clear();
        int[] trys = x.will = whatCached.apply(new ProtoDerivation.PremiseKey(x));
        return trys.length > 0 ? true : false;
    }

    /** 2. TRY stage */
    public void derive(Derivation x, int ttl) {
        if (x.derive()) {
            x.setTTL(ttl);
            can.test(x);
        }
    }

    public void printRecursive() {

        what.printRecursive();
        can.printRecursive();

    }
}
