package nars.derive.premise;

import com.google.common.io.ByteArrayDataOutput;
import jcog.Util;
import jcog.data.byt.DynBytes;
import nars.derive.Derivation;
import nars.derive.Deriver;
import nars.term.control.PrediTerm;

import java.util.Arrays;

import static nars.Op.*;

public final class PremiseKey {

    /*@Stable*/
    final byte[] key;
    private final int hash;


    public PremiseKey(PreDerivation d) {

        DynBytes k = new DynBytes(64);

        //task punc (2 bits) + whether belief is present (1 bit)
        byte puncPlusBeliefPresent;
        switch (d.taskPunc) {
            case BELIEF:  puncPlusBeliefPresent = 0; break;
            case GOAL:  puncPlusBeliefPresent = 1; break;
            case QUESTION:  puncPlusBeliefPresent = 2; break;
            case QUEST:  puncPlusBeliefPresent = 3; break;
            default:
                throw new UnsupportedOperationException();
        }
        puncPlusBeliefPresent |= (d.hasBeliefTruth() ? 1 : 0) << 3;
        k.writeByte(puncPlusBeliefPresent);

//        //2 bits for each polarity, each one offset by +1 (because it ranges from -1..+1)
//        k.writeByte(((d.taskPolarity+1)<<2) | (d.beliefPolarity+1) );
        //1 bit for whether the


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

        derivation.can.clear();

        what.test(derivation);

        int[] result = derivation.can.toArray();

        return Util.toShort(result);
    }


    @Override
    public int hashCode() {
        return hash;
    }
}
