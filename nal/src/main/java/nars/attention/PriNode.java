package nars.attention;

import jcog.data.graph.MapNodeGraph;
import jcog.data.graph.Node;
import jcog.data.graph.NodeGraph;
import jcog.math.FloatRange;
import jcog.pri.PLink;
import nars.$;
import nars.NAR;
import nars.term.Term;

public class PriNode extends PLink<Term> {

    /** boost, relative among peers
     * TODO use separate PriNode as the factor
     * */
    @Deprecated public final FloatRange factor = new FloatRange(1f, 0.01f, 2f);

    public PriNode(Object id) {
        super($.identity(id), 0);
    }

    @Override
    public String toString() {
        return id + " pri=" + pri() + " boost=" + factor;
    }

//    public Stream<Concept> concepts(NAR nar) {
//        return childrenStream().map(x -> nar.concept(x.id)).filter(Objects::nonNull);
//    }

//    static public void ensure(Prioritizable c, float pri) {
//        pri -= c.priElseZero();
//        if (pri > 0) {
//            //c.take(supply, pri, true, false); //RESTRICTED
//            c.priAdd(pri);
//        }
//    }

    public final float elementPri() {
        return this.priElseZero();
        //return nar.priDefault(BELIEF);
    }
    protected float elementFraction(int n) {
        float i;
        if (n == 0)
            return 0;
        //i = 1; //each component important as a top level concept
        i = (float) (1f / Math.sqrt((float)n)); //shared by sqrt of components
        //i = 1f / n; //shared by all components
        return i;
    }

    public void update(float pri, MapNodeGraph<PriNode,Object> graph) {
        Node<PriNode, Object> p = graph.node(this);
        this.pri(pri);
        int fanout = p.edgeCount(false, true);
        float priEach = pri * elementFraction(fanout) * factor.floatValue();
        //TODO local boost's
        //TODO p.nodes(..)
        p.nodes(false,true).forEach(c -> c.id().update(priEach, graph));
    }

    public void outs(MapNodeGraph<PriNode, Object> g, PriNode... p) {
        NodeGraph.MutableNode<PriNode,Object> thisNode = g.addNode(this);
        for (PriNode pp : p) {
            g.addEdge(thisNode, "pri", pp);
        }
    }

    public void parent(PriNode parent, NAR n) {
        MapNodeGraph<PriNode, Object> g = n.attn.graph;

        NodeGraph.MutableNode<PriNode,Object> thisNode = g.addNode(this);
        parent(parent, g, thisNode);
    }

    /** re-parent */
    public void parent(PriNode parent, MapNodeGraph<PriNode, Object> g, NodeGraph.MutableNode<PriNode, Object> thisNode) {
        assert(!this.equals(parent));
        synchronized (g) {
            thisNode.edges(true, false).forEach(g::edgeRemove);

            g.addEdge(parent, "pri", thisNode);
        }
    }

    public void factor(float f) {
        factor.set(f);
    }
}
