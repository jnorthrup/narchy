package nars.game.sensor;

import jcog.math.FloatRange;
import nars.NAR;
import nars.control.NARPart;
import nars.term.Term;

public abstract class AbstractSensor extends NARPart implements GameLoop {

    protected final FloatRange res;

    protected AbstractSensor(NAR nar) {
        this(null, nar);
    }

    protected AbstractSensor(Term id, NAR n) {
        super(id);

        this.nar = n; //HACK

        //defaults
        res = FloatRange.unit( n.freqResolution );

    }

    public <S extends AbstractSensor> S resolution(float v) {
        this.res.set(v);
        return (S) this;
    }

    public final FloatRange resolution() {
        return res;
    }


//    private float pri(float pri, Truth prev, Truth next, float fRes) {
//        float priIfNoChange =
//                //ScalarValue.EPSILON; //min pri used if signal value remains the same
//                pri * pri;
//
//        if (prev == null)
//            return pri;
//        else {
////            float fDiff = next!=null ? Math.abs(next.freq() - prev.freq()) : 1f;
////            return Util.lerp(fDiff, priIfNoChange, pri);
//            if (next == null || Math.abs(next.freq()-prev.freq()) > fRes)
//                return pri;
//            else
//                return priIfNoChange;
//        }
//    }

}
