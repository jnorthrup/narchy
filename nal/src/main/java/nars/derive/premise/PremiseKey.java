package nars.derive.premise;

import com.google.common.io.ByteArrayDataOutput;
import jcog.Util;
import jcog.data.byt.DynBytes;
import jcog.memoize.byt.ByteKey;
import nars.derive.Derivation;
import nars.term.Term;
import nars.term.control.PrediTerm;

import static nars.Op.*;

public class PremiseKey extends ByteKey {

    public static PremiseKey get(Derivation d) {

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

        return new PremiseKey(d, k.array(), k.hashCode(), pri);
    }


    transient public final Derivation derivation;

    protected final float pri;

    protected PremiseKey(Derivation d, byte[] key, int hash, float pri) {
        super(key, hash);
        this.derivation = d;
        this.pri = pri;
    }


    /** TODO this can safely return short[] results
     * @param what*/
    public short[] solve(PrediTerm<Derivation> what) {

        Derivation derivation  = this.derivation;

        derivation.can.clear();

        what.test(derivation);

        return Util.toShort( derivation.can.toArray() );
    }


}
