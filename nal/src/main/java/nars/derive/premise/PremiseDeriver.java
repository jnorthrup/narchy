package nars.derive.premise;

import jcog.memoize.HijackMemoize;
import jcog.memoize.Memoize;
import nars.control.Cause;
import nars.derive.Derivation;
import nars.derive.control.ValueFork;
import nars.term.control.PrediTerm;

import java.io.PrintStream;

/**
 * compiled derivation rules
 * what -> can
 * */
public final class PremiseDeriver {

    public final PrediTerm<Derivation> what;
    public final ValueFork can;

    /**
     * TODO move this to a 'CachingDeriver' subclass
     */
    public final Memoize<PremiseKey, short[]> whats;

    public PremiseDeriver(PrediTerm<Derivation> what, ValueFork can) {
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


    public void print(PrintStream p) {
        print(p, 0);
    }

    public void print(PrintStream p, int indent) {
        PremiseDeriverCompiler.print(what, p, indent);
        PremiseDeriverCompiler.print(can, p, indent);
    }


    public boolean derivable(Derivation x) {
        return (x.will = whats.apply(new PremiseKey(x))).length > 0;
    }
}
