package nars.derive.premise;

import jcog.memoize.HijackMemoize;
import jcog.memoize.Memoize;
import nars.control.Cause;
import nars.derive.Derivation;
import nars.derive.control.Try;
import nars.term.control.PrediTerm;

import java.io.PrintStream;

/**
 * compiled derivation rules
 * what -> can
 * */
public final class PremiseDeriver {

    public final PrediTerm<Derivation> what;
    protected final Try can;

    /**
     * TODO move this to a 'CachingDeriver' subclass
     */
    final Memoize<PremiseKey, short[]> whats;

    public PremiseDeriver(PrediTerm<Derivation> what, Try can) {
        this.what = what;
        this.can = can;
        this.whats = new HijackMemoize<>(k->k.solve(what),
                64 * 1024, 4, false);
    }

    /**
     * the conclusions that in which this deriver can result
     */
    public Cause[] causes() { return can.causes; }

    public void printRecursive() {
        printRecursive(System.out);
    }
    public void printRecursive(PrintStream out) {
        PremiseDeriverCompiler.print(this, out);
    }

    /**
     * 1. CAN (proto) stage
     */
    public boolean derivable(PreDerivation x) {
        return (x.will = whats.apply(new PremiseKey(x))).length > 0;
    }

    public void print(PrintStream p) {
        print(p, 0);
    }

    public void print(PrintStream p, int indent) {
        PremiseDeriverCompiler.print(what, p, indent);
        PremiseDeriverCompiler.print(can, p, indent);
    }

    public final void run(Derivation d) {
        can.accept(d);
    }

}
