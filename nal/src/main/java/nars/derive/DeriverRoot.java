package nars.derive;

import nars.derive.value.Try;
import nars.term.pred.PrediTerm;

import java.io.PrintStream;

/** what -> can */
public final class DeriverRoot {

    public final PrediTerm<Derivation> what;
    public final Try can;



    public DeriverRoot(PrediTerm<Derivation> what, Try can) {
        this.what = what;
        this.can = can;
    }

    public void printRecursive() {
        printRecursive(System.out);
    }
    public void printRecursive(PrintStream out) {
        TrieDeriver.print(this, out);
    }
}
