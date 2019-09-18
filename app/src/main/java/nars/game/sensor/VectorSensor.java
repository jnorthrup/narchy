package nars.game.sensor;

import com.google.common.collect.Iterables;
import nars.NAR;
import nars.Task;
import nars.attention.AttnBranch;
import nars.concept.Concept;
import nars.control.channel.CauseChannel;
import nars.game.Game;
import nars.table.dynamic.SensorBeliefTables;
import nars.term.Term;
import nars.term.Termed;

/**
 * base class for a group of concepts representing a sensor 'vector'
 */
abstract public class VectorSensor extends AbstractSensor implements Iterable<Signal> {


    public final CauseChannel<Task> in;
    protected final short[] cause;

    /** used and shared by all components */
    public final AttnBranch pri;


    @Override
    public Iterable<Termed> components() {
        return Iterables.transform(this, Concept::term);
    }

//    protected VectorSensor(@Nullable FloatSupplier input, @Nullable Term id, NAR nar) {
//        this(input, id, (prev, next) -> next==next ? $.t(Util.unitize(next), nar.confDefault(BELIEF)) : null, nar);
//    }


    protected VectorSensor(NAR n) {
        this(null, n);
    }
    protected VectorSensor(Term rootID, NAR n) {
        super(rootID, n);

        this.pri = new AttnBranch(this.id, this);
        this.in = n.newChannel(id != null ? id : this);
        this.cause = new short[] { in.id };
    }

    /**
     * best to override
     */
    abstract public int size();// {return Iterables.size(this);}

    /** surPRIse */
    public double surprise() {
        double s = 0;
        for (Signal c : this)
            s += ((SensorBeliefTables)c.beliefs()).surprise();
        return s;
    }

    @Override
    public void update(Game g) {

        float aPri = pri.pri() / size();

        //float quality = Util.sqrt(attn.amp.floatValue());
        //Random rng = g.random();
        for (Signal s : this) {
            s.resolution().set(res);
            //if (quality >= 1 || rng.nextFloat() < quality )
            s.update(aPri, cause, g);
        }
    }

}
