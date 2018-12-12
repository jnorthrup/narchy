package nars.concept.sensor;

import jcog.math.FloatRange;
import nars.NAR;
import nars.Task;
import nars.control.NARService;
import nars.control.channel.CauseChannel;
import nars.table.dynamic.SeriesBeliefTable;
import nars.task.ITask;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;

abstract public class AbstractSensor extends NARService implements Sensor {

    public final FloatRange pri;
    private final FloatRange res;


    protected AbstractSensor(NAR nar) {
        this(null, nar);
    }

    protected AbstractSensor(Term id, NAR n) {
        super(id, n);

        //defaults
        pri = FloatRange.unit( n.beliefPriDefault );
        res = FloatRange.unit( n.freqResolution );
    }

    public <S extends AbstractSensor> S resolution(float v) {
        this.res.set(v);
        return (S) this;
    }


    @Override
    public final FloatRange resolution() {
        return res;
    }

    /** TODO abstract different priority distribution methods:
     *    uniform, divide equally, according to amount of value change, etc */
    protected void pri(Task x) {
        x.pri(pri);
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
