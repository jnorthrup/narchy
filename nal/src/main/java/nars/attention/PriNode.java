package nars.attention;

import jcog.data.graph.MapNodeGraph;
import jcog.data.graph.Node;
import jcog.data.graph.NodeGraph;
import jcog.util.Essence;
import jcog.math.FloatRange;
import jcog.pri.PLink;
import jcog.pri.ScalarValue;
import nars.$;
import nars.NAR;
import nars.term.Term;

public class PriNode extends PLink<Term> {


    /**
     * amplitude, factor, boost, relative priority among peers
     * TODO use separate PriNode as the factor
     * */
    public final FloatRange amp = new FloatRange(1f, 0.01f, 2f);

    /** cached */
    @Deprecated transient private Node<PriNode, Object> _node;

    private int fanOut;

    public PriNode(Object id) {
        super($.identity(id), 0);
    }

    @Override
    public String toString() {
        return id + " pri=" + pri();
    }

    @Deprecated /* move to subclass */ public final float priComponent() {
        return priFraction() * pri();
    }

    @Deprecated /* move to subclass */ protected float priFraction() {
        float i;
        int n = fanOut;
        if (n == 0)
            return 1;
        //i = 1; //each component important as a top level concept
        i = (float) (1.0 / Math.sqrt((float)n)); //shared by sqrt of components
        //i = 1f / n; //shared by all components
        return i;
    }

    public void update(MapNodeGraph<PriNode,Object> graph) {

        if (_node == null) {
            _node = graph.node(this); //cache
        }
        fanOut = _node.edgeCount(false,true); //TODO cache

        final double[] factor = {this.amp.floatValue()};

        if (_node.edgeCount(true,false) > 0) {

            _node.nodes(true, false).forEach((Node<PriNode, Object> n) -> {
                PriNode nn = n.id();
                float p = nn.priComponent();
                if (p == p) {
                    factor[0] *= p;
                }
            });
        }

        float pri = (float) (factor[0]);
        this.pri(pri);
    }


    public PriNode parent(NAR n, PriNode... parent) {
        MapNodeGraph<PriNode, Object> g = n.control.graph;

        NodeGraph.MutableNode<PriNode,Object> thisNode = g.addNode(this);
        parent(parent, g, thisNode);

        return this;
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

    public static class ConstPriNode extends PriNode {

        @Essence
        public final FloatRange f;

        public ConstPriNode(Object id, float initialValue) {
            this(id, new FloatRange(initialValue, ScalarValue.EPSILON, 1f));
        }

        public ConstPriNode(Object id, FloatRange f) {
            super(id);
            this.f = f;
        }

        @Override
        protected float priFraction() {
            return 1;
        }

        @Override
        public void update(MapNodeGraph<PriNode, Object> graph) {
            this.amp.set(f.floatValue());
            super.update(graph);
        }
    }
}
