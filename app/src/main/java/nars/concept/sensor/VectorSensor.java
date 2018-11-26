package nars.concept.sensor;

import com.google.common.collect.Iterables;
import nars.NAR;
import nars.concept.NodeConcept;
import nars.control.channel.CauseChannel;
import nars.task.ITask;
import nars.term.Termed;

/** base class for a group of concepts representing a sensor 'vector'
 * */
abstract public class VectorSensor extends AbstractSensor implements Iterable<Signal> {


    public final CauseChannel<ITask> in;




    @Override
    public Iterable<Termed> components() {
        return Iterables.transform(this, NodeConcept::term);
    }

//    protected VectorSensor(@Nullable FloatSupplier input, @Nullable Term id, NAR nar) {
//        this(input, id, (prev, next) -> next==next ? $.t(Util.unitize(next), nar.confDefault(BELIEF)) : null, nar);
//    }

    protected VectorSensor(NAR nar) {
        super(nar);

        this.in = nar.newChannel(id);
    }

    /** best to override */
    public int size() {
        return Iterables.size(this);
    }



//
//    @Override
//    public void accept(NAR n) {
//        synchronized (this) {
//            long now = n.time();
//
//
//            update(last, now, n);
//
//            this.last = now;
//        }
//    }



//    @Override
//    public void setResolution(FloatRange r) {
//        super.setResolution(r);
//        forEach(s -> s.setResolution(r));
//    }

//    @Override
//    public void setPri(FloatRange p) {
//        super.setPri(p);
//        forEach(x -> x.setPri(p));
//    }
}
