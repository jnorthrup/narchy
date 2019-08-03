package nars.attention;

import jcog.Util;
import jcog.data.graph.MapNodeGraph;
import jcog.data.graph.Node;
import jcog.data.graph.NodeGraph;
import jcog.math.FloatRange;
import jcog.pri.PLink;
import nars.$;
import nars.term.Term;
import org.eclipse.collections.api.block.function.primitive.DoubleDoubleToDoubleFunction;

public class PriNode extends PLink<Term> {

    /**
     * amplitude, factor, boost, relative priority among peers
     * TODO use separate PriNode as the factor
     * */
    public final FloatRange amp = new FloatRange(1f, 0, 1f);

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

    @Override
    public float pri() {
        return super.pri() * amp.floatValue();
    }

    @Deprecated /* move to subclass */ protected final float priFraction() {
        int n = fanOut;
        return branch.priFraction(n); //TODO cache
//        if (n <= 1)
//            return 1;
//        //i = 1; //each component important as a top level concept
//        //i = (float) (1.0 / Math.sqrt((float)n)); //shared by sqrt of components
//        i = 1f / n; //shared by all components
//        return i;
    }

    public final float amp() {
        return amp.floatValue();
    }

    public enum Branch {
        Equal {
            @Override
            public float priFraction(int n) {
                return 1;
            }
        },
        One_Div_N {
            @Override
            public float priFraction(int n) {
                return 1f/n;
            }
        },
        One_div_sqrtN {
            @Override
            public float priFraction(int n) {
                return (float)(1f/Math.sqrt(n));
            }
        };

        abstract public float priFraction(int n);
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
                float c = nn.priComponent();
                if (c == c)
                    accum = f.applyAsDouble(accum, c);
            }
            return accum;
        }

        abstract public double merge(Iterable<? extends Node<PriNode, Object>> in);
    }

    Merge input = Merge.Plus;
    protected Branch branch = Branch.One_Div_N;

    /** how the incoming priority is combined from sources */
    public PriNode input(Merge m) {
        this.input = m;
        return this;
    }

    /** how the priority will be shared/distributed to children */
    public PriNode output(Branch b) {
        this.branch = b;
        return this;
    }

    public void update(MapNodeGraph<PriNode,Object> graph) {

        Node<PriNode, Object> node = node(graph);
        fanOut = node.edgeCount(false,true); //TODO cache

        float pri;
        if (node.edgeCount(true,false) > 0) {
            Iterable<? extends Node<PriNode, Object>> in = neighbors(graph, true, false);
            pri = (float) input.merge(in);
        } else {
            pri = 0; //disconnected
        }

        this.pri(pri );
    }

    public Iterable<? extends Node<PriNode, Object>> neighbors(MapNodeGraph<PriNode,Object> graph, boolean in, boolean out) {
        return node(graph).nodes(in, out);
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

    public final PriNode amp(float v) {
        amp.set(v);
        return this;
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

    private static class Mutable extends PriNode {

        private Mutable(Object id, float p) {
            super(id);
            pri(p);
        }
    }

    private static class Constant extends Mutable {
        private final float value;

        public Constant(String name, float value) {
            super(name, value);
            this.value = value;
            output(Branch.Equal);
        }

        @Override
        @Deprecated public float pri() {
            return value * amp.floatValue();
        }

    }
}
