package nars.control;

import com.google.common.io.ByteArrayDataOutput;
import jcog.data.byt.DynBytes;
import nars.Op;
import nars.derive.DeriverRoot;
import nars.term.Term;
import nars.term.subst.Unify;
import nars.truth.Truth;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.RoaringBitmap;

import java.util.Arrays;
import java.util.Random;
import java.util.function.Consumer;

/** contains only information which depends on the premise itself (Task, Belief, BeliefTerm) */
public abstract class ProtoDerivation extends Unify {


    public Term taskTerm;
    public Term beliefTerm;
    public byte taskPunc;

    /* -1 = freq<0.5, 0 = null, +1 = freq>=0.5 */
    public int taskPolarity, beliefPolarity;





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


    static int polarity(Truth t) {
        return (t.isPositive() ? +1 : -1);
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


        public PremiseKey(Derivation d) {

            DynBytes k = new DynBytes(192);

            d.taskTerm.root().append((ByteArrayDataOutput)k);
            d.beliefTerm.root().append((ByteArrayDataOutput)k);
            k.writeByte(d.taskPunc);
            //2 bits for each polarity, each one offset by +1 (because it ranges from -1..+1)
            k.writeByte(((d.taskPolarity+1)<<2) | (d.beliefPolarity+1) );

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
        public int[] solve() {

            Derivation derivation  = Deriver.derivation.get();

            derivation.ttl = Integer.MAX_VALUE;

            assert(derivation.can.isEmpty()); //only place this is used

            derivation.deriver.what.test(derivation);

            int[] result = derivation.can.toArray();

            derivation.can.clear();

            //use the common zero array reference
            return result.length == 0 ? ArrayUtils.EMPTY_INT_ARRAY : result;

        }


        @Override
        public int hashCode() {
            return hash;
        }
    }
}
