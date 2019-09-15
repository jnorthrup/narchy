package nars.derive.util;

import jcog.memoize.byt.ByteKeyExternalWithParameter;
import nars.NAL;
import nars.Task;
import nars.derive.Derivation;
import nars.derive.PreDerivation;
import nars.io.TermIO;
import nars.unify.Unify;

/** TODO use Gödel numbering to represent a term's unique dt's as independent but canonical numbers ? */
public class PremiseKey extends ByteKeyExternalWithParameter<Unify> {

    public PremiseKey(PreDerivation d) {
        this(d, ((Derivation)d).ditherDT);
    }

    public PremiseKey(PreDerivation d, int ditherDT) {
        super(d);

        //TODO if taskTerm or beliefTerm have #'s, apply full Anon intern that considers Int as Atom like before Int's had an Intern repr

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
            (Task.i(d.taskPunc) << 1)
                |
            (d.hasBeliefTruth() ? 1 : 0)
        );

        commit();

        //System.out.println(d.taskTerm + " " + d.beliefTerm + " " + key.length() + " " + toString());
    }


}
