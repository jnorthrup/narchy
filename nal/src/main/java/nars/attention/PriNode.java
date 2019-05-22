package nars.attention;

import jcog.data.graph.MapNodeGraph;
import jcog.data.graph.Node;
import jcog.data.graph.NodeGraph;
import jcog.math.FloatRange;
import jcog.math.FloatSupplier;
import jcog.pri.PLink;
import nars.$;
import nars.term.Term;

public class PriNode extends PLink<Term> {


    /**
     * amplitude, factor, boost, relative priority among peers
     * TODO use separate PriNode as the factor
     * */
    public final FloatRange amp = new FloatRange(1f, 0.01f, 1f /* 2f */);

    @Deprecated transient private Node<PriNode, Object> _node;

    private int fanOut;

    public PriNode(Object id) {
        super($.identity(id), 0);
    }

    @Override
    public String toString() {
        return id + " pri=" + pri();
    }

    @Deprecated /* move to subclass */ public float priComponent() {
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

    public enum Merge {
        Sum {
            @Override public double merge(Iterable<? extends Node<PriNode, Object>> in) {
                final double[] pSum = {0};

                in.forEach((Node<PriNode, Object> n) -> {
                    PriNode nn = n.id();
                    float p = nn.priComponent();
                    if (p == p) {
                        pSum[0] += p;
                    }
                });
                return pSum[0];
            }
        },
        Factor {
            @Override public double merge(Iterable<? extends Node<PriNode, Object>> in) {
                final double[] p = {1};

                in.forEach((Node<PriNode, Object> n) -> {
                    PriNode nn = n.id();
                    float c = nn.priComponent();
                    if (c == c) {
                        p[0] *= c;
                    }
                });
                return p[0];
            }
        };

        abstract public double merge(Iterable<? extends Node<PriNode, Object>> in);
    }

    Merge merge = Merge.Sum;

    public PriNode merge(Merge m) {
        this.merge = m;
        return this;
    }


    public void update(MapNodeGraph<PriNode,Object> graph) {

        Node<PriNode, Object> node = node(graph);
        fanOut = node.edgeCount(false,true); //TODO cache

        float pri;
        if (node.edgeCount(true,false) > 0) {
            Iterable<? extends Node<PriNode, Object>> in = neighbors(graph, true, false);
            pri = (float) (merge.merge(in) * amp.floatValue());
        } else {
            pri = 0; //disconnected
        }

        this.pri(pri );
    }



    public Iterable<? extends Node<PriNode, Object>> neighbors(MapNodeGraph<PriNode,Object> graph, boolean in, boolean out) {
        return node(graph).nodes(in, out);
    }

//
//    public PriNode parent(NAR n, PriNode... parent) {
//        MapNodeGraph<PriNode, Object> g = n.control.graph;
//
//        NodeGraph.MutableNode<PriNode,Object> thisNode = g.addNode(this);
//        parent(parent, g, thisNode);
//
//        return this;
//    }

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

    public final PriNode amp(float v) {
        amp.set(v);
        return this;
    }

    public static PriNode mutable(String name, float initialValue) {
        return new Mutable(name, initialValue);
    }
    public static PriNode constant(String name, float value) {
        return new Constant(name, value);
    }

    /** cached
     * @param graph*/
    public Node<PriNode, Object> node(MapNodeGraph<PriNode, Object> graph) {
        if (_node == null) {
            _node = graph.node(this); //cache
        }
        return _node;
    }

    public PriNode amp(FloatSupplier a) {
        return new PriNode($.p(this.id, $.func("amp", $.quote(a.toString())) )) {
            @Override
            public void update(MapNodeGraph<PriNode, Object> graph) {
                amp.set(a.asFloat());
                super.update(graph);
            }
        };
    }

    private static class Mutable extends PriNode {

//        @Essence
//        public final FloatRange f;

//        public ConstPriNode(Object id, float initialValue) {
//            this(id, new FloatRange(initialValue, ScalarValue.EPSILON, 1f));
//
//        }

        private Mutable(Object id, float p) {
            super(id);
            pri(p);
//            this.f = f;
//            pri(f.floatValue());
        }


        //
//        @Override
//        public float pri(float p) {
//            f.set(p);
//            return super.pri(p);
//        }

        @Override
        protected float priFraction() {
            return 1;
        }
//
//        @Override
//        public void update(MapNodeGraph<PriNode, Object> graph) {
////            pri(f.floatValue());
//            super.update(graph);
//        }
    }

    private static class Constant extends Mutable {
        private final float value;

        public Constant(String name, float value) {
            super(name, value);
            this.value = value;
        }

        @Override
        public float pri() {
            return value;
        }

        @Override
        public float priComponent() {
            return value;
        }
    }
}
