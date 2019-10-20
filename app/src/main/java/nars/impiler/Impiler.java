package nars.impiler;

import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import jcog.TODO;
import jcog.data.graph.AdjGraph;
import jcog.data.graph.GraphIO;
import jcog.data.graph.Node;
import jcog.data.graph.NodeGraph;
import jcog.data.graph.path.FromTo;
import jcog.pri.PLink;
import jcog.pri.bag.Bag;
import jcog.pri.bag.impl.PriReferenceArrayBag;
import jcog.pri.op.PriMerge;
import nars.NAR;
import nars.Task;
import nars.attention.What;
import nars.concept.Concept;
import nars.control.channel.CauseChannel;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static java.util.Optional.ofNullable;
import static nars.Op.IMPL;

/**
 * Implication Graph / Compiler
 * a set of plugins that empower and accelerate causal reasoning
 * <p>
 * see: https:
 * see: https:
 */
public class Impiler {

    /**
     * concept metadata field key storing impiler node instances
     */
    private static final String IMPILER_NODE = ImplGrapher.class.getSimpleName();


    public static @Nullable ImplNode node(Termed x, boolean createIfMissing, NAR nar) {
        return ofNullable(createIfMissing ? nar.conceptualize(x) : nar.concept(x)).map(concept -> node(concept, createIfMissing)).orElse(null);
    }

    private static @Nullable ImplNode node(Concept sc, boolean createIfMissing) {
        return createIfMissing ? sc.meta(IMPILER_NODE, s -> new ImplNode(sc.term())) : sc.meta(IMPILER_NODE);
    }

    private static boolean filter(Term next) {
        return next instanceof Compound && next.op() == IMPL && !next.hasVars();
    }

    private static float implValue(Task t) {
        return t.priElseZero();


    }

    public static void graphGML(Iterable<? extends Concept> concepts, PrintStream out) {
        GraphIO.writeGML(graph(concepts), out);
    }


    public static AdjGraph<Term, Task> graph(NAR nar) {
        return graph(() -> nar.concepts().iterator());
    }

    public static AdjGraph<Term, Task> graph(What w) {
        throw new TODO();
    }

    /**
     * creates graph snapshot
     */
    private static AdjGraph<Term, Task> graph(Iterable<? extends Concept> concepts) {
        AdjGraph<Term, Task> g = new AdjGraph<Term, Task>(true);


        Streams.stream(concepts)
                .map(c -> node(c, false))
                .filter(Objects::nonNull)
                .forEach(m -> {
                    for (FromTo<Node<Term, Task>, Task> e : m.edges(false, true)) {
                        Term s = e.from().id();
                        g.addNode(s);
                        Term t = e.to().id();
                        g.addNode(t);
                        g.setEdge(s, t, e.id());
                    }
                });
        return g;        //unstreamable monster


    }

    /**
     * all tasks in NAR memory
     */
    public static void impile(NAR n) {

        n.tasks().forEach(t -> impile(t, n));
    }


    /**
     * try to add/update a task in the graph
     */
    public static boolean impile(Task i, NAR n) {
        boolean result = false;
        if (i.isBelief() && filter(i.term())) {
            _impile(i, n);
            result = true;
        }
        return result;
    }

    private static void _impile(Task t, NAR nar) {
        Term i = t.term();
        Subterms ii = i.subterms();


        Term subj = ii.sub(0).concept(), pred = ii.sub(1).concept();
        if (!subj.equals(pred)) {


            Concept sc =
                    nar.conceptualize(subj.unneg());

            if (sc != null) {
                Concept pc =
                        nar.conceptualize(pred);

                if (pc != null) {
                    edge(t, sc, pc);
                }
            }
        }
    }

    private static void edge(Task t, Concept sc, Concept pc) {
        node(sc, true).add(true, t, pc);
        node(pc, true).add(false, t, sc);
    }


    /**
     * searches forward, discovering shortcuts in the downstream impl graph
     * TODO configurable trigger, dont just extend TaskLeak
     */
    static class ImpilerDeducer {

        ImpilerDeducer(NAR n) {
            CauseChannel<Task> in = n.newChannel(this);
        }


        protected static boolean filter(Term term) {
            return !term.hasVars();
        }

        protected static float leak(Task next, What what) {
            return deduce(next, what, true);
        }

        static float deduce(Task task, What what, boolean forward) {
            float res;
            Term target = task.term();

            ImpilerDeduction x = new ImpilerDeduction(what.nar);

            List<Task> result = x.get(target, task.isEternal() ? what.time() : task.start(), forward);
            if (result.isEmpty()) res = (float) 0;
            else {
                what.acceptAll(result);
                res = 1.0F;
            }


            return res;
        }


    }

    /**
     * builds the implication graph in concept metadata fields
     * TODO configurable trigger, dont just extend TaskLeak
     */
    public static class ImplGrapher {
        protected static boolean filter(Term term) {
            return filter(term);
        }

        protected static float leak(Task t, What what) {
            _impile(t, what.nar);
            return 1.0F;
        }


    }

    static class ImplNode extends NodeGraph.AbstractNode<Term, Task> {

        static final int CAP = 8;
        final Bag<Task, ImplPLink> tasks = new PriReferenceArrayBag<>(PriMerge.max, CAP);

        ImplNode(Term id) {
            super(id);
        }

        final void add(boolean direction, Task e, Concept target) {
            tasks.commit();
            tasks.put(new ImplPLink(e, implValue(e), direction, target));
        }


        @Override
        public String toString() {
            return id + ":" + tasks.toString();
        }

        @Override
        public Iterable<FromTo<Node<Term, Task>, Task>> edges(boolean in, boolean out) {
            assert (in ^ out);
            return tasks.isEmpty() ? Collections.EMPTY_LIST : Iterables.filter(Iterables.transform(tasks, (tLink) -> {
                boolean td = tLink.direction;
                if ((out && td) || (in && !td)) {
                    Task tt = tLink.get();


                    Node otherNode = node(tLink.target, false);
                    if (otherNode != null) {
                        return out ? Node.edge(this, tt, otherNode) : Node.edge(otherNode, tt, this);
                    } else {
                        tLink.delete();
                    }
                }
                return null;
            }), Objects::nonNull);
        }

        private static final class ImplPLink extends PLink<Task> {

            /**
             * true = out, false = in
             */
            private final boolean direction;

            /**
             * TODO weakref?
             */
            private final Concept target;

            ImplPLink(Task task, float p, boolean direction, Concept target) {
                super(task, p);
                this.direction = direction;
                this.target = target;
            }
        }

    }
}
