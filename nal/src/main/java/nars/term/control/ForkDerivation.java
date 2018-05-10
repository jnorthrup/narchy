package nars.term.control;

import nars.derive.Derivation;

public class ForkDerivation extends Fork<Derivation> {

    public ForkDerivation(PrediTerm[] branches) {
        super(branches);
    }

    @Override
    public boolean test(Derivation x) {

        int before = x.now();

        for (PrediTerm c : branch) {
            if (!c.test(x) && x.revertLive(before))
                return false;
        }

        return true;
    }
}
