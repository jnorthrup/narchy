package nars.attention;

import jcog.data.graph.MapNodeGraph;
import jcog.data.graph.Node;
import jcog.data.graph.NodeGraph;
import jcog.math.FloatRange;
import jcog.math.FloatSupplier;
import jcog.pri.PLink;
import nars.$;
import nars.NAR;
import nars.term.Term;

public class PriNode extends PLink<Term> {


    /** boost, relative among peers
     * TODO use separate PriNode as the factor
     * */
    public final FloatRange factor = new FloatRange(1f, 0.01f, 2f);

    /** cached */
    @Deprecated transient private Node<PriNode, Object> node;
    private int fanOut;

    public PriNode(Object id) {
        super($.identity(id), 0);
    }

    @Override
    public String toString() {
        return id + " pri=" + pri();
    }

    @Deprecated /* move to subclass */ protected float priFraction() {
        float i;
        int n = fanOut;
        if (n == 0)
            return 0;
        //i = 1; //each component important as a top level concept
        i = (float) (1f / Math.sqrt((float)n)); //shared by sqrt of components
        //i = 1f / n; //shared by all components
        return i;
    }

    public void update(MapNodeGraph<PriNode,Object> graph) {

        if (node == null) {
            node = graph.node(this); //cache
        }
        fanOut = node.edgeCount(false,true); //TODO cache

        final double[] factor = {this.factor.floatValue()};

        if (node.edgeCount(true,false) > 0) {

            node.nodes(true, false).forEach((Node<PriNode, Object> n) -> {
                PriNode nn = n.id();
                float np = nn.pri() * nn.priFraction();
//            if (Util.equals(0, np))
//                throw new WTF();
                factor[0] *= np;
            });
        }

        float pri = (float) (factor[0]);
        this.pri(pri);


//        int fanout = node.edgeCount(false, true);
//        if (fanout > 0) {
//            //TODO local boost's
//            //TODO p.nodes(..)
//            node.nodes(false, true).forEach(c -> c.id().update(graph));
//        }
    }


    public PriNode parent(NAR n, PriNode... parent) {
        MapNodeGraph<PriNode, Object> g = n.attn.graph;

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
        private final FloatSupplier f;

        public ConstPriNode(Object id, FloatSupplier f) {
            super(id);
            this.f = f;
        }

        @Override
        public void update(MapNodeGraph<PriNode, Object> graph) {
            this.factor.set(f.asFloat());
            super.update(graph);
        }
    }
}
