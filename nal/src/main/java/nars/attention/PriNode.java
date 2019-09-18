package nars.attention;

import jcog.Util;
import jcog.data.graph.MapNodeGraph;
import jcog.data.graph.Node;
import jcog.data.graph.NodeGraph;
import jcog.pri.PLink;
import nars.$;
import nars.term.Term;
import org.eclipse.collections.api.block.function.primitive.DoubleDoubleToDoubleFunction;

public class PriNode extends PLink<Term> {

    @Deprecated transient private Node<PriNode, Object> _node;

    protected Merge input = Merge.Plus;

    public PriNode(Object id) {
        super($.identity(id), 0);
    }

    @Override
    public String toString() {
        return id + " pri=" + pri();
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

        this.pri(node.edgeCount(true, false) > 0 ?
            (float) input.merge(node.nodes( true, false))
            :
            0);
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

    public static PriNode source(String name, float value) {
        return new Mutable(name, value);
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
    public static class Mutable extends PriNode {

        public Mutable(Object id, float p) {
            super(id);
            pri(p);
        }

        @Override
        public void update(MapNodeGraph<PriNode, Object> graph) {
            //nothing
        }
    }

    private static final class Constant extends Mutable {
        private final float value;

        private Constant(String name, float value) {
            super(name, value);
            this.value = value;
        }
        @Override
        public void update(MapNodeGraph<PriNode, Object> graph) {
            //nothing
        }
        @Override
        public float pri() {
            return value;
        }

    }
}
