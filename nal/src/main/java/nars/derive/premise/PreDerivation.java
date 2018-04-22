package nars.derive.premise;

import jcog.version.Versioned;
import nars.Op;
import nars.term.Term;
import nars.unify.Unify;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.RoaringBitmap;

import java.util.Map;
import java.util.Random;

/** contains only information which depends on the premise itself (Task, Belief, BeliefTerm) */
public abstract class PreDerivation extends Unify {


    public Term taskTerm;
    public Term beliefTerm;
    public byte taskPunc;

//    /* -1 = freq<0.5, 0 = null, +1 = freq>=0.5 */
//    public int taskPolarity, beliefPolarity;

    public byte _taskOp;
    public byte _beliefOp;

    public int _taskStruct;
    public int _beliefStruct;


    /**
     * choices mapping the available post targets
     */
    public final RoaringBitmap can = new RoaringBitmap();
    public short[] will = ArrayUtils.EMPTY_SHORT_ARRAY;

    public PreDerivation(@Nullable Op type, Random random, int stackMax, int initialTTL, Map<Term, Versioned<Term>> termMap) {
        super(type, random, stackMax, initialTTL, termMap);
    }


//    static int polarity(Truth t) {
//        return (t.isPositive() ? +1 : -1);
//    }

    protected void reset() {


        termutes.clear();

        this.size = 0; //HACK instant revert to zero
        this.xy.map.clear(); //must also happen to be consistent


    }

}
