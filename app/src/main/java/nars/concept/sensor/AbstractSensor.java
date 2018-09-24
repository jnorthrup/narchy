package nars.concept.sensor;

import jcog.Util;
import jcog.math.FloatRange;
import nars.$;
import nars.NAR;
import nars.control.NARService;
import nars.control.channel.CauseChannel;
import nars.task.ITask;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;

import static nars.Op.BELIEF;

abstract public class AbstractSensor extends NARService implements Sensor {

    private FloatRange pri, res;


    public AbstractSensor(NAR nar) {
        this(null, nar);
    }

    public AbstractSensor(Term id, NAR n) {
        super(id, n);

        //defaults
        pri = FloatRange.unit( n.beliefPriDefault );
        res = FloatRange.unit( n.freqResolution );
    }

    public AbstractSensor pri(float v) {
        pri.set(v);
        return this;
    }

//    public void setPri(FloatRange p) {
//        this.pri = p;
//    }

    public AbstractSensor resolution(float v) {
        this.res.set(v);
        return this;
    }

//    public void setResolution(FloatRange r) {
//        this.res = r;
//    }

    @Override
    public final FloatRange resolution() {
        return res;
    }

    @Override
    public final FloatRange pri() {
        return pri;
    }



    /** default truther */
    final private FloatFloatToObjectFunction<Truth> truther = (prev, next) -> $.t(Util.unitize(next), nar.confDefault(BELIEF));

    /** convenience class for updating a set of signals */
    protected void update(long last, long now, Iterable<Signal> signals, CauseChannel<ITask> in) {
        signals.forEach(s -> in.input(s.update(last, now, truther, now - last, nar)));
    }

}
