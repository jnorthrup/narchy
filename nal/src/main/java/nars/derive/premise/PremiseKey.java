package nars.derive.premise;

import com.google.common.io.ByteArrayDataOutput;
import jcog.Util;
import jcog.data.byt.DynBytes;
import jcog.pri.PriProxy;
import jcog.pri.Priority;
import nars.derive.Derivation;
import nars.term.Term;
import nars.term.control.PrediTerm;

import java.util.Arrays;

import static nars.Op.*;

abstract public class PremiseKey {

    /*@Stable*/
    protected final byte[] key;
    protected final int hash;

    private PremiseKey(byte[] key, int hash) {
        this.key = key;
        this.hash = hash;
    }

    /** final stored version of a key */
    public final static class PremiseKeyInternal extends PremiseKey implements Priority, PriProxy<PremiseKey,short[]> {
        final short[] result;
        private volatile float pri;

        public PremiseKeyInternal(byte[] premise, int hash, short[] result, float pri) {
            super(premise, hash);
            this.pri = pri;
            this.result = result;
        }

        @Override
        public PremiseKey x() {
            return this;
        }

        @Override
        public short[] get() {
            return result;
        }

        @Override
        public float priSet(float p) {
            return pri = p;
        }

        @Override
        public float pri() {
            return pri;
        }
    }

    /** temporary */
    public final static class PremiseKeyBuilder extends PremiseKey {

        transient public final Derivation derivation;

        protected final float pri;

        public static PremiseKeyBuilder get(Derivation d) {

            DynBytes k = new DynBytes(64);

            Term _t = d.taskTerm;
            Term t = _t.root();
            Term _b = d.beliefTerm;
            Term b = _b.root();

            float pri = 1f/(1+(t.volume() + b.volume()));
//            if (t.equals(b) && !_t.equals(_b)) {
//                //temporally different so store a distinction, otherwise the equal version will override it and neq() predicates will fail derivations
//                t = _t;
//                b = _b;
//                pri/=2; //store in cache with lower pri since temporal variations would clutter
//            }

            t.appendTo((ByteArrayDataOutput) k);

            k.writeByte(0); //delimeter, for safety

            b.appendTo((ByteArrayDataOutput) k);

            k.writeByte(0); //delimeter, for safety

            byte taskPuncAndIfDouble;
            switch (d.taskPunc) {
                case BELIEF:
                    taskPuncAndIfDouble = 0;
                    break;
                case GOAL:
                    taskPuncAndIfDouble = 1;
                    break;
                case QUESTION:
                    taskPuncAndIfDouble = 2;
                    break;
                case QUEST:
                    taskPuncAndIfDouble = 3;
                    break;
                default:
                    throw new UnsupportedOperationException();
            }

            taskPuncAndIfDouble |= (d.hasBeliefTruth() ? 1 : 0) << 3;
            k.writeByte(taskPuncAndIfDouble);

            return new PremiseKeyBuilder(d, k.array(), k.hashCode(), pri);
        }

        private PremiseKeyBuilder(Derivation d, byte[] key, int hash, float pri) {
            super(key, hash);
            this.derivation = d;
            this.pri = pri;
        }

        public PremiseKeyInternal key(short[] result) {
            return new PremiseKeyInternal(key, hash, result, pri);
        }
    }



    @Override
    public boolean equals(Object o) {
        
        PremiseKey that = (PremiseKey) o;

        return hash == that.hash && Arrays.equals(key, that.key);
    }

    /** TODO this can safely return short[] results
     * @param what*/
    public short[] solve(PrediTerm<Derivation> what) {

        Derivation derivation  = ((PremiseKeyBuilder)this).derivation;

        derivation.can.clear();

        what.test(derivation);

        return Util.toShort( derivation.can.toArray() );
    }


    @Override
    public int hashCode() {
        return hash;
    }
}
