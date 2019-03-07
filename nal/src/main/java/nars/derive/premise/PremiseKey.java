package nars.derive.premise;

import jcog.memoize.byt.ByteKeyExternalWithParameter;
import nars.Task;
import nars.derive.Derivation;
import nars.derive.PreDerivation;
import nars.io.TermIO;

public class PremiseKey extends ByteKeyExternalWithParameter<PreDerivation> {

    public PremiseKey(PreDerivation d) {
        this(d, ((Derivation)d).ditherDT);
    }

    public PremiseKey(PreDerivation d, int ditherDT) {
        super(d);

        if (ditherDT > 0) {
            TermIO.DeferredTemporalTermIO io = new TermIO.DeferredTemporalTermIO();
            io.write(d.taskTerm, key);
            io.write(d.beliefTerm, key);
            io.writeDTs(ditherDT, key);
        } else {
            TermIO io = TermIO.the;
            io.write(d.taskTerm, key);
            io.write(d.beliefTerm, key);
        }

        key.writeByte(
          ((d.hasBeliefTruth() ? 1 : 0) << 3)
                    |
            Task.i(d.taskPunc)
        );

        commit();

        //System.out.println(d.taskTerm + " " + d.beliefTerm + " " + key.length() + " " + toString());
    }


}
