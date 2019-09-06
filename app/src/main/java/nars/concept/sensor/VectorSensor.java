package nars.concept.sensor;

import com.google.common.collect.Iterables;
import jcog.data.graph.MapNodeGraph;
import jcog.math.FloatSupplier;
import nars.NAR;
import nars.Task;
import nars.agent.Game;
import nars.attention.AttnBranch;
import nars.attention.PriNode;
import nars.concept.Concept;
import nars.control.channel.CauseChannel;
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

        //HACK
        pri = new AttnBranch(this.id, this) {

            int size = -1;

            float priComponent;

            @Override
            public void update(MapNodeGraph<PriNode, Object> graph) {
                if (size <= 0) {
                    //init size
                    try {
                        size = Iterables.size(components());
                    } catch (Throwable t) {
                        size = 0; //HACK
                    }
                }

                super.update(graph);
                priComponent = super.pri();
            }

            @Override
            public float pri() {
                return super.pri() * size;
            }

            @Override
            public float priComponent() {
                return priComponent;
            }
        };
        pri.output(PriNode.Branch.One_Div_N);

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

        FloatSupplier aPri = pri::priComponent;
        //float quality = Util.sqrt(attn.amp.floatValue());
        //Random rng = g.random();
        for (Signal s : this) {
            s.resolution().set(res);
            //if (quality >= 1 || rng.nextFloat() < quality )
            s.update(aPri, cause, g);
        }
    }

}
