package nars.derive.premise;

import jcog.memoize.byt.ByteKeyExternal;
import nars.Task;
import nars.derive.Derivation;
import nars.io.TermIO;

public class PremiseKey extends ByteKeyExternal {

    transient public Derivation d;

    public PremiseKey(Derivation d) {
        super();

        this.d = d;

        TermIO.DeferredTemporalTermIO io = new TermIO.DeferredTemporalTermIO();
        io.write(d.taskTerm, key);
        io.write(d.beliefTerm, key);
        io.writeDTs(d.ditherDT, key);

        int p = Task.i(d.taskPunc);
        int s = d.hasBeliefTruth() ? 1 : 0;
        key.writeByte((s << 3) | p);

        commit();

        //System.out.println(d.taskTerm + " " + d.beliefTerm + " " + key.length() + " " + toString());
    }

    @Override
    public void close() {
        d = null;
        super.close();
    }
}
