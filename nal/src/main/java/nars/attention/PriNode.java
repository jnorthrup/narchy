package nars.attention;

import jcog.Util;
import jcog.data.graph.MapNodeGraph;
import jcog.data.graph.Node;
import jcog.data.graph.NodeGraph;
import jcog.math.FloatRange;
import jcog.pri.Prioritized;
import jcog.pri.UnitPri;
import nars.$;
import nars.term.Term;
import org.eclipse.collections.api.block.function.primitive.DoubleDoubleToDoubleFunction;

public class PriNode implements Prioritized {

    public final Term id;

    @Deprecated transient private Node<PriNode, Object> _node;

    protected Merge input = Merge.Plus;

    protected final UnitPri pri = new UnitPri(0);

    public PriNode(Object id) {
        this.id = $.identity(id);
    }

    @Override
    public String toString() {
        return id + " pri=" + pri();
    }

    @Override
    public final float pri() {
        return pri.pri();
    }


    public enum Merge {
        Plus {
            @Override public double merge(Iterable<? extends Node<PriNode, Object>> in) {
                return reduce(in, 0, Double::sum);
            }
        },
        And {
            @Override public double merge(Iterable<? extends Node<PriNode, Object>> in) {
                return reduce(in, 1, (p, c) -> p * c);
            }
        },
        Or {
            @Override public double merge(Iterable<? extends Node<PriNode, Object>> in) {
                return reduce(in, 0, Util::or);
            }
        }
        ;

        /** @param f f(accumulator, nodePri) */
        protected static double reduce(Iterable<? extends Node<PriNode, Object>> in, double accum, DoubleDoubleToDoubleFunction f) {
            for (Node<PriNode, Object> n : in) {
                PriNode nn = n.id();
                float np = nn.pri();
                if (np == np)
                    accum = f.applyAsDouble(accum, np);
            }
            return accum;
        }

        abstract public double merge(Iterable<? extends Node<PriNode, Object>> in);
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
            0;

        this.pri.pri( in(p) );
    }

    /** override to manipulate the incoming priority value (ex: transfer function) */
    protected float in(float p) {
        return p;
    }

    /** re-parent */
    public void parent(PriNode[] parent, MapNodeGraph<PriNode, Object> g, NodeGraph.MutableNode<PriNode, Object> thisNode) {


        assert(parent.length > 0);

        synchronized (g) {
            thisNode.edges(true, false).forEach(g::edgeRemove);

            for (PriNode p : parent) {
                assert(!this.equals(p));
                g.addEdge(p, "pri", thisNode);
            }
        }
    }

    public static PriNode.Source source(String name, float value) {
        return new Source(name, value);
    }

    /** cached
     * @param graph*/
    public Node<PriNode, Object> node(MapNodeGraph<PriNode, Object> graph) {
        if (_node == null) {
            _node = graph.node(this); //cache
        }
        return _node;
    }

    /** variably adjustable priority source */
    public static class Source extends PriNode {

        public final FloatRange in = new FloatRange(0.5f, 0, 1);

        public Source(Object id, float p) {
            super(id);
            this.pri.pri(p);
        }

        @Override
        public void update(MapNodeGraph<PriNode, Object> graph) {
            //assert(_node.edgeCount(true,false)==0);
            this.pri.pri(in.get());
        }

        public void pri(float p) {
            in.set(p);
        }
    }

}
