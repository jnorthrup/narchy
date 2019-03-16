package nars.attention;

import jcog.data.graph.MapNodeGraph;
import jcog.data.graph.Node;
import jcog.data.graph.NodeGraph;
import jcog.data.graph.path.FromTo;
import jcog.data.graph.search.Search;
import jcog.data.list.FasterList;
import jcog.math.FloatRange;
import jcog.math.IntRange;
import jcog.pri.PriBuffer;
import jcog.pri.bag.Sampler;
import jcog.pri.bag.impl.ArrayBag;
import jcog.pri.bag.impl.hijack.PriHijackBag;
import jcog.pri.op.PriMerge;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.attention.derive.DefaultDerivePri;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.derive.Derivation;
import nars.link.TaskLink;
import nars.link.TaskLinkBag;
import nars.link.TermLinker;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atom;
import nars.time.event.DurService;
import org.eclipse.collections.api.tuple.primitive.BooleanObjectPair;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;

/** abstract attention economy model */
public class Attention extends DurService implements Sampler<TaskLink> {

    public Forgetting forgetting = new Forgetting.AsyncForgetting();
    /**
     * short target memory, TODO abstract and remove
     */
    public TaskLinkBag links = null;

//    /**
//     * tasklink activation
//     */
//    @Deprecated
//    public final FloatRange activationRate = new FloatRange(1f, Param.tasklinkMerge == plus ? ScalarValue.EPSILON : 1, 1);

    public final FloatRange forgetRate = new FloatRange(0.1f,  0, 1f /* 2f */);

    /** propagation (decay+growth) rate */
    public final FloatRange amp = new FloatRange(0.5f,  0, 2f /* 2f */);
    //0.25f;
    //(float) (1f/(1 + Math.sqrt(t.volume())));
    //1f/(1 + t.volume());
    //1f/((s.volume() + t.volume())/2f); //1/vol_mean
    //1f/(s.volume() + t.volume()); //1/vol_sum


    public final MapNodeGraph<PriNode,Object> graph = new MapNodeGraph<>(PriBuffer.newMap());
    public PriNode root = new PriNode.ConstPriNode("root", ()->1);
    private final NodeGraph.MutableNode<PriNode,Object> rootNode = graph.addNode(root);



    public final IntRange linksCapacity = new IntRange(256, 0, 8192) {
        @Override
        @Deprecated protected void changed() {
            TaskLinkBag a = links;
            if (a != null)
                a.setCapacity(intValue());
        }
    };

    /** default derivePri for derivers */
    public DerivePri derivePri =
            //new DirectDerivePri();
            new DefaultDerivePri();
            //new DefaultPuncWeightedDerivePri();


    @Override
    protected void starting(NAR nar) {
        int c = linksCapacity.intValue();
        links = new TaskLinkBag(
                new TaskLinkArrayBag(c)
                //new TaskLinkHijackBag(c, 7)
        );

        links.setCapacity(linksCapacity.intValue());




        on(
                nar.eventClear.on(links::clear),
                nar.onCycle(this::onCycle)
        );

        super.starting(nar);

    }

    protected final void onCycle() {
        links.commit(
            forgetting.forget(links, 1f, forgetRate.floatValue())
        );
    }


    @Override
    protected void run(NAR n, long dt) {
        forgetting.update(n);
        derivePri.update(n);
        root.pri(1);

        //iterate, in topologically sorted order
        root.update( graph);
        graph.bfs(root, new Search<PriNode,Object>() {
            @Override
            protected boolean next(BooleanObjectPair<FromTo<Node<PriNode, Object>, Object>> move, Node<PriNode, Object> next) {
                next.id().update(graph);
                return true;
            }
        });
    }

    @Override
    public void sample(Random rng, Function<? super TaskLink, SampleReaction> each) {
        links.sample(rng, each);
    }

    /** resolves and possibly sub-links a link target */
    @Nullable public Term term(TaskLink link, Task task, Derivation d) {

        Term t = link.target();

        NAR nar = d.nar;
        Random rng = d.random;
        final Term s = link.source();
        byte punc = task.punc();



        float p =
                link.priPunc(punc);
        //task.priElseZero();

        ///* HACK */ getAndSetPriPunc(punc, p*0.9f /* decay */); //spend


        //Math.max(ScalarValue.EPSILON, task.priElseZero() - priPunc(punc));

//        float pDown = 1*p, pUp = Float.NaN;

        Concept ct;
        if (t.op().conceptualizable) {



            Term u = null;

            boolean self = s.equals(t);

            ct = nar.conceptualize(t);
            if (ct != null) {
                t = ct.term();
                TermLinker linker = ct.linker();
                if (linker != TermLinker.NullLinker && !((FasterList) linker).isEmpty())
                    //grow-ahead: s -> t -> u
                    u = linker.sample(rng);
                else {

                    if (t instanceof Atom) {
                        //why is this necessary
                        if (d.random.nextFloat() < 0.5f) {
                            //if (self || d.random.nextFloat() > 1f/(1+s.complexity())) {
                            //sample active tasklinks for a tangent match to the atom
//                            Atom tt = (Atom) t;
                            Predicate<TaskLink> filter =
                                    x -> !link.equals(x);
                            //x -> !link.equals(x) && !link.other(tt).equals(s);

                            u = links.atomTangent(ct, filter, d.time, 1, d.random);
//                        if (u!=null && u.equals(s)) {
////                            u = links.atomTangent(ct, ((TaskLink x)->!link.equals(x)), d.time, 1, d.random);//TEMPORARY
//                            throw new WTF();
//                        }

//                        } else {
//
//
//                            //link(t, s, punc, p*subRate); //reverse echo
//                        }
                        }
                    }

                }


            }


            if (u != null && !t.equals(u)) {


//                //TODO abstact activation parameter object
//                float subRate =
//                        1f;
//                //1f/(t.volume());
//                //(float) (1f/(Math.sqrt(s.volume())));
//
//
//                float inflation = 1; //TODO test inflation<1
//                float want = p * subRate / 2;
//                float p =
//                        inflation < 1 ? Util.lerp(inflation, link.take(punc, want*inflation), want) : want;

                int n = 2;
                float pp = p * amp.floatValue() / n;

                //link.take(punc, pp*n);

                //CHAIN
                link(s, u, punc, pp); //forward (hop)
                //link(u, s, punc, pp); //reverse (hop)
                //link(t, u, punc, pp); //forward (adjacent)
                link(u, t, punc, pp); //reverse (adjacent)




                //link(s, t, punc, ); //redundant
                //link(t, s, punc, pp); //reverse echo

//                if (self)
//                    t = u;

            } else {
//                int n = 1;
//                float pp = p * conductance / n;
//
//                link(t, s, punc, pp); //reverse echo

            }
        }


        return t;
    }

    void link(Term s, Term u, byte punc, float p) {
        Op o = s.op();
        if (o.taskable) {
            linkSafe(s, u, punc, p);
        }
    }

    private TaskLink linkSafe(Term src, Term tgt, byte punc, float pri) {
        TaskLink t = TaskLink.tasklink(src, tgt, punc, pri);
        link(t);
        return t;
    }

    public void link(TaskLink x) {
        links.putAsync(x);
    }
    public void link(TaskLink... xx) {
        for (TaskLink x : xx)
            link(x);
    }

    /** initial tasklink activation for an input task
     * @return*/
    public boolean link(Task task, @Nullable Concept taskConcept, NAR n) {

        Termed cc = taskConcept == null ? task : taskConcept;
        Concept c =
                n.conceptualize(cc);
                //n.activate(cc, pri, true);
        if (c == null)
            return false;

        float pri = task.pri();
        if (pri!=pri)
            return false;


        link(new TaskLink.GeneralTaskLink(c.term()).priMerge(task.punc(), pri));


        ((TaskConcept) c).value(task, n);

        return true;
    }

    /** attaches a priority node to the priority graph
     * @return*/
    public NodeGraph.MutableNode<PriNode, Object> add(PriNode p) {
        NodeGraph.MutableNode<PriNode, Object> a = graph.addNode(p);
        graph.addEdgeByNode(rootNode, "pri", a);
        return a;
    }

    private static class TaskLinkArrayBag extends ArrayBag<TaskLink, TaskLink> {

        public TaskLinkArrayBag(int initialCapacity) {
            super(Param.tasklinkMerge, initialCapacity, PriBuffer.newMap());
        }

//        @Override
//        protected float sortedness() {
//            return 0.33f;
//        }

        @Override
        public TaskLink key(TaskLink value) {
            return value;
        }

        @Override
        protected float merge(TaskLink existing, TaskLink incoming) {
            return existing.merge(incoming, Param.tasklinkMerge);
        }
    }

    private static class TaskLinkHijackBag extends PriHijackBag<TaskLink, TaskLink> {

        public TaskLinkHijackBag(int initialCap, int reprobes) {
            super(initialCap, reprobes);
        }

        @Override
        public TaskLink key(TaskLink value) {
            return value;
        }

        @Override
        protected PriMerge merge() {
            return Param.tasklinkMerge;
        }
    }


}
