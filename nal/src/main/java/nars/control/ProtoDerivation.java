package nars.control;

import com.google.common.io.ByteArrayDataOutput;
import jcog.data.byt.DynBytes;
import nars.Op;
import nars.term.Term;
import nars.term.subst.Unify;
import nars.truth.Truth;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.RoaringBitmap;

import java.util.Arrays;
import java.util.Random;

/** contains only information which depends on the premise itself (Task, Belief, BeliefTerm) */
public abstract class ProtoDerivation extends Unify {


    public Term taskTerm;
    public Term beliefTerm;
    public byte taskPunc;

    /* -1 = freq<0.5, 0 = null, +1 = freq>=0.5 */
    public byte taskPolarity, beliefPolarity;





    public byte _taskOp;
    public byte _beliefOp;

    public int _taskStruct;
    public int _beliefStruct;


    /**
     * choices mapping the available post targets
     */
    public final RoaringBitmap can = new RoaringBitmap();
    public int[] will = ArrayUtils.EMPTY_INT_ARRAY;

    public ProtoDerivation(@Nullable Op type, Random random, int stackMax, int initialTTL) {
        super(type, random, stackMax, initialTTL);
    }


    static byte polarity(Truth t) {
        return (byte) (t.isPositive() ? +1 : -1);
    }

    public ProtoDerivation reset() {

        termutes.clear();

        this.taskTerm = this.beliefTerm = null;

        this.size = 0; //HACK instant revert to zero
        this.xy.map.clear(); //must also happen to be consistent

        return this;
    }

    public final static class PremiseKey {
        byte[] key;
        private final int hash;

        transient public Derivation derivation;


        public PremiseKey(Derivation d) {

            DynBytes k = new DynBytes(192);

            d.taskTerm.root().append((ByteArrayDataOutput)k);
            d.beliefTerm.root().append((ByteArrayDataOutput)k);
            k.writeByte(d.taskPunc);
            //2 bits for each polarity, each one offset by +1 (because it ranges from -1..+1)
            k.writeByte(((d.taskPolarity+1)<<2) | (d.beliefPolarity+1) );

            this.key = k.array();
            this.hash = k.hashCode();

            this.derivation = d;
        }

        @Override
        public boolean equals(Object o) {
            //if (this == o) return true;
            PremiseKey that = (PremiseKey) o;

            return hash == that.hash && Arrays.equals(key, that.key);
        }

        /** TODO this can safely return short[] results */
        public int[] solve() {

            Derivation derivation  = this.derivation;
            this.derivation = null; //dont retain references to the rules or the derivation if cached

            derivation.ttl = Integer.MAX_VALUE;
            derivation.deriver.what.test(derivation);

            int[] result = derivation.can.toArray();
            if (result.length == 0)
                return ArrayUtils.EMPTY_INT_ARRAY; //use the common zero array reference

            return result;
        }


        @Override
        public int hashCode() {
            return hash;
        }
    }
}
