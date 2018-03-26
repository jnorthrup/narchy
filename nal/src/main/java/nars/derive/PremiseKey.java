package nars.derive;

import com.google.common.io.ByteArrayDataOutput;
import jcog.Util;
import jcog.data.byt.DynBytes;
import nars.term.pred.PrediTerm;

import java.util.Arrays;

public final class PremiseKey {

    final byte[] key;
    private final int hash;


    public PremiseKey(PreDerivation d) {

        DynBytes k = new DynBytes(64);

        k.writeByte(d.taskPunc);
        //2 bits for each polarity, each one offset by +1 (because it ranges from -1..+1)
        k.writeByte(((d.taskPolarity+1)<<2) | (d.beliefPolarity+1) );

        d.taskTerm.root().append((ByteArrayDataOutput)k);
        d.beliefTerm.root().append((ByteArrayDataOutput)k);

        this.key = k.array();
        this.hash = k.hashCode();

    }

    @Override
    public boolean equals(Object o) {
        //if (this == o) return true;
        PremiseKey that = (PremiseKey) o;

        return hash == that.hash && Arrays.equals(key, that.key);
    }

    /** TODO this can safely return short[] results */
    public short[] solve(PrediTerm<Derivation> what) {

        Derivation derivation  = Deriver.derivation.get();

//        derivation.ttl = Integer.MAX_VALUE;

//        assert(derivation.can.isEmpty()); //only place this is used

        what.test(derivation);

        int[] result = derivation.can.toArray();

        derivation.can.clear();

        return Util.toShort(result);
    }


    @Override
    public int hashCode() {
        return hash;
    }
}
