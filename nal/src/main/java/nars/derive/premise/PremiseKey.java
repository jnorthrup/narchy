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

        
        byte taskPuncAndIfDouble;
        switch (d.taskPunc) {
            case BELIEF:  taskPuncAndIfDouble = 0; break;
            case GOAL:  taskPuncAndIfDouble = 1; break;
            case QUESTION:  taskPuncAndIfDouble = 2; break;
            case QUEST:  taskPuncAndIfDouble = 3; break;
            default:
                throw new UnsupportedOperationException();
        }
        taskPuncAndIfDouble |= (d.hasBeliefTruth() ? 1 : 0) << 3;
        k.writeByte(taskPuncAndIfDouble);



        


        d.taskTerm.root().appendTo((ByteArrayDataOutput)k);
        d.beliefTerm.root().appendTo((ByteArrayDataOutput)k);



        this.key = k.array();
        this.hash = k.hashCode();

    }



    @Override
    public boolean equals(Object o) {
        
        PremiseKey that = (PremiseKey) o;

        return hash == that.hash && Arrays.equals(key, that.key);
    }

    /** TODO this can safely return short[] results */
    public static short[] solve(PrediTerm<Derivation> what) {

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
