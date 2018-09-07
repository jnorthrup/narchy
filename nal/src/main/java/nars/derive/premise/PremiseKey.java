package nars.derive.premise;

import com.google.common.io.ByteArrayDataOutput;
import jcog.Util;
import jcog.data.byt.DynBytes;
import jcog.memoize.byt.ByteKey;
import nars.derive.Derivation;
import nars.term.control.PREDICATE;

import static nars.Op.*;

public class PremiseKey extends ByteKey.ByteKeyExternal {

    public static PremiseKey get(Derivation d) {

        DynBytes k = d.tmpPremiseKey;
        k.clear();

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

        if (d.hasBeliefTruth())
            taskPuncAndIfDouble |= 1 << 3;

        k.writeByte(taskPuncAndIfDouble);


        d.taskTerm.appendTo((ByteArrayDataOutput) k);

        //k.writeByte(0); //delimeter, for safety

        d.beliefTerm.appendTo((ByteArrayDataOutput) k);

        //k.writeByte(0); //delimeter, for safety



//        float pri = 1f/(1+(t.volume() + b.volume()));
////            if (t.equals(b) && !_t.equals(_b)) {
////                //temporally different so store a distinction, otherwise the equal version will override it and neq() predicates will fail derivations
////                t = _t;
////                b = _b;
////                pri/=2; //store in cache with lower pri since temporal variations would clutter
////            }

        return new PremiseKey(k, d);
    }


    transient public Derivation derivation;


    protected PremiseKey(DynBytes b, Derivation d) {
        super(b);
        this.derivation = d;
    }


    /** TODO this can safely return short[] results
     * @param what*/
    public short[] solve(PREDICATE<Derivation> what) {

        Derivation derivation  = this.derivation;

        this.derivation = null;

        derivation.can.clear();

        what.test(derivation);

        return Util.toShort( derivation.can.toArray() );
    }


}
