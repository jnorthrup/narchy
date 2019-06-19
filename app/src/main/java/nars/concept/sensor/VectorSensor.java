package nars.concept.sensor;

import com.google.common.collect.Iterables;
import jcog.data.graph.MapNodeGraph;
import jcog.math.FloatSupplier;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.agent.Game;
import nars.attention.AttnBranch;
import nars.attention.PriNode;
import nars.concept.Concept;
import nars.control.channel.CauseChannel;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;

import static nars.Op.BELIEF;

/**
 * base class for a group of concepts representing a sensor 'vector'
 */
abstract public class VectorSensor extends AbstractSensor implements Iterable<Signal> {


    public final CauseChannel<Task> in;
    private final short[] causeArray;

    /** used and shared by all components */
    public final AttnBranch attn;


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
        attn = new AttnBranch(this.id, this) {

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
                priComponent = branch.priFraction(size) * pri();
            }

            @Override
            public float priComponent() {
                return priComponent;
            }
        };
        attn.branch(PriNode.Branch.One_div_sqrtN);

        this.in = n.newChannel(id != null ? id : this);
        this.causeArray = new short[] { in.id };
    }

    /**
     * best to override
     */
    abstract public int size();// {return Iterables.size(this);}


    @Override
    public void update(Game g) {

        float confDefault = nar.confDefault(BELIEF);
        float min = nar.confMin.floatValue();
        FloatFloatToObjectFunction<Truth> truther = (p, n) -> {
            float c = confDefault;// * Math.abs(n - 0.5f) * 2f;
            return c > min ? $.t(n, c) : null;
        };


        FloatSupplier aPri = attn::priComponent;
        //float quality = Util.sqrt(attn.amp.floatValue());
        //Random rng = g.random();
        for (Signal s : this) {
            //if (quality >= 1 || rng.nextFloat() < quality )
                s.update(truther, aPri, causeArray, g);
        }
    }

}
