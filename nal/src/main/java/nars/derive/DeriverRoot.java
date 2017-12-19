package nars.derive;

import jcog.memoize.HijackMemoize;
import jcog.memoize.Memoize;
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

    Memoize<ProtoDerivation.PremiseKey,int[]> cache =
            new HijackMemoize<>(ProtoDerivation.PremiseKey::solve,
                    64 * 1024, 5);

    @Override
    public boolean test(Derivation x) {
        int ttl = x.ttl;


        int[] preToPost = cache.apply(new ProtoDerivation.PremiseKey(x));
        if (preToPost.length > 0) {

            x.preToPost.add(preToPost);

            x.ttl = ttl;  //HACK forward the specified TTL for use during the possibility phase

            can.test(x);
        }

        return true;
    }

}
