package nars.derive.premise;

import jcog.memoize.byt.ByteKeyExternalWithParameter;
import nars.NAL;
import nars.Task;
import nars.derive.model.Derivation;
import nars.derive.model.PreDerivation;
import nars.io.TermIO;
import nars.unify.Unify;

/** TODO use Gödel numbering to represent a term's unique dt's as independent but canonical numbers ? */
public class PremiseKey extends ByteKeyExternalWithParameter<Unify> {

    public PremiseKey(PreDerivation d) {
        this(d, ((Derivation)d).ditherDT);
    }

    public PremiseKey(PreDerivation d, int ditherDT) {
        super(d);

        /* TODO move dithering option to parameter */
        if (NAL.premise.PREMISE_KEY_DITHER && ditherDT > 1) {
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
