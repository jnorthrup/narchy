package nars.attention;

import jcog.Util;
import jcog.data.graph.MapNodeGraph;
import jcog.data.graph.Node;
import jcog.data.graph.NodeGraph;
import jcog.data.graph.path.FromTo;
import jcog.pri.Prioritized;
import jcog.pri.UnitPri;
import nars.$;
import nars.term.Term;
import org.eclipse.collections.api.block.function.primitive.DoubleDoubleToDoubleFunction;

/** TODO make abstract, only use UnitPri pri in certain impl that actually need to store it and dont just copy an outside value like Source */
public class PriNode implements Prioritized {

    public final Term id;

    @Deprecated
    private transient Node<PriNode, Object> _node;

    protected Merge input = Merge.Plus;

    /** internal priority storage variable */
    public final UnitPri pri = new UnitPri((float) 0);

    public PriNode(Object id) {
        this.id = $.identity(id);
    }

    @Override
    public String toString() {
        return id.toString();
    }

    @Override
    public final float pri() {
        return pri.pri();
    }


    public enum Merge {
        Plus {
            @Override public double merge(Iterable<? extends Node<PriNode, Object>> in) {
                return reduce(in, (double) 0, Double::sum);
            }
        },
        And {
            @Override public double merge(Iterable<? extends Node<PriNode, Object>> in) {
                return reduce(in, 1.0, (p, c) -> p * c);
            }
        },
        Or {
            @Override public double merge(Iterable<? extends Node<PriNode, Object>> in) {
                return reduce(in, (double) 0, Util::or);
            }
        }
        ;

        /** @param f f(accumulator, nodePri) */
        protected static double reduce(Iterable<? extends Node<PriNode, Object>> in, double accum, DoubleDoubleToDoubleFunction f) {
            for (Node<PriNode, Object> n : in) {
                PriNode nn = n.id();
                float np = nn.pri();
                if (np == np)
                    accum = f.applyAsDouble(accum, (double) np);
            }
            return accum;
        }

        public abstract double merge(Iterable<? extends Node<PriNode, Object>> in);
    }

    /** how the incoming priority is combined from sources */
    public PriNode input(Merge m) {
        this.input = m;
        return this;
    }

    public void update(MapNodeGraph<PriNode,Object> graph) {

        Node<PriNode, Object> node = node(graph);
        //fanOut = node.edgeCount(false,true); //TODO cache

        float p = node.edgeCount(true, false) > 0 ?
            (float) input.merge(node.nodes(true, false))
            :
                (float) 0;

        this.pri.pri( in(p) );
    }

    /** override to manipulate the incoming priority value (ex: transfer function) */
    protected float in(float p) {
        return p;
    }

    /** re-parent */
    public void parent(PriNode[] parent, MapNodeGraph<PriNode, Object> g, NodeGraph.MutableNode<PriNode, Object> thisNode) {


        assert(parent.length > 0);


        for (FromTo<Node<PriNode, Object>, Object> nodeObjectFromTo : thisNode.edges(true, false)) {
            g.edgeRemove(nodeObjectFromTo);
        }

        for (PriNode p : parent) {
                assert(!this.equals(p));
                g.addEdge(p, "pri", thisNode);
            }

    }

    public static PriSource source(String name, float value) {
        return new PriSource(name, value);
    }

    /** cached
     * @param graph*/
    public Node<PriNode, Object> node(MapNodeGraph<PriNode, Object> graph) {
        if (_node == null) {
            _node = graph.node(this); //cache
        }
        return _node;
    }

}
