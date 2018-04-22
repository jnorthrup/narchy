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
        if ((x.will = whats.apply(new PremiseKey(x))).length > 0) {
            //filterByTruthAndPunctuation

//            byte taskPunc = x.taskPunc;
//            ///boolean cyclic = x...
//            boolean hasBelief = x.belief!=null;
//
//            RoaringBitmap toRemove = new RoaringBitmap();
//            short[] will = x.will;
//            for (int i = 0, willLength = will.length; i < willLength; i++) {
//                int w = will[i];
//                PrediTerm<Derivation> b = can.branch[w];
//                if (b instanceof Truthify) {
//                    if (!((Truthify) b).test(taskPunc, hasBelief)) {
//                        toRemove.add(i);
//                    }
//                }
//            }
//            int numToRemove = toRemove.getCardinality();
//            if (numToRemove > 0) {
//                if (numToRemove == will.length)
//                    return false; //not derivable
//
//                short newWill[] = x.will = new short[will.length - numToRemove];
//                int j = 0;
//                for (int i = 0, willLength = will.length; i < willLength; i++) {
//                    if (!toRemove.contains(i)) //TODO use the RoaringBitmap's iterator to copy ranges without lookup each time
//                        newWill[j++] = will[i];
//                }
//                assert(j == newWill.length);
//                x.will = newWill;
//            }
            return true;
        }
        return false;
    }
}
