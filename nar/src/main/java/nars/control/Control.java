package nars.control;

import jcog.Skill;
import jcog.data.graph.MapNodeGraph;
import jcog.data.graph.Node;
import jcog.data.graph.NodeGraph;
import jcog.data.list.FasterList;
import jcog.data.map.ConcurrentFastIteratingHashMap;
import nars.NAR;
import nars.Task;
import nars.attention.PriAmp;
import nars.attention.PriNode;
import nars.control.channel.CauseChannel;
import nars.control.op.Remember;
import nars.task.AbstractTask;
import nars.term.Term;
import nars.time.part.DurLoop;
import org.eclipse.collections.api.block.function.primitive.ShortToObjectFunction;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;

/**
 *  NAR Control
 *  low-level system control interface
 *
 *  for guiding and specialize system activity and capabilities
 *  useful for emulating motivation, instincts, etc
 *
 *
 *  -- "activity equalizer": fixed-size bipolar control vetor for real-time behavior trajectory control
 *          (TODO dynamically pluggable) control metrics
 *  -- registers/unregisters plugins (causables). each is assigned a unique 16-bit (short) id.
 *  -- profiles and measures the performance of plugins when used, with respect to current system objectives
 *      --incl. rmeote logger, etc
 *  -- decides future prioritization (and other parametrs) of each plugin through an abstract
 *          reinforcement learning control interface
 *
 *  */
@Skill("Multi-armed_bandit") public final class Control {

    /**
     * a table of numerically (16-bit ID), individually registered reasons
     * why any mental activity could have happened.  for use in forming causal
     * traces
     *
     * raw cause id (short int) -> cause table
     *
     */
    public final FasterList<Cause> why = new FasterList<>(0, new Cause[512]);


    /** hierarchical priority distribution DAG (TODO ensure acyclic) */
    public final MapNodeGraph<PriNode,Object> graph = new MapNodeGraph<>(
            new ConcurrentFastIteratingHashMap<>(new Node[0])
            //PriMap.newMap(false)
    );





    private final NAR nar;

    public final DurLoop loop;

    public Control(NAR nar) {
        this.nar = nar;

        update();
        loop = nar.onDur(this::update);
    }

    private void update() {
        nar.what.commit(null);
        prioritize();
    }

    public MetaGoal.Report stats(PrintStream out) {
        MetaGoal.Report r = new MetaGoal.Report();
        r.add(why);
        r.print(out);
        return r;
    }

    private void prioritize() {


        //if (x.edgeCount(true, false)==0) { //root
        //}
        for (Node<PriNode, Object> x : graph.nodes()) {
            x.id().update(graph);
        }
    }

    /**
     * computes an evaluation amplifier factor, in range 0..2.0.
     * VALUE -> AMP
     * -Infinity -> amp=0
     * 0     -> amp=1
     * +Infinity -> amp=2
     */
//    public float amp(Term effect) {
//        return 1f + Util.tanhFast(value(effect));
//    }
//
//    public final float amp(Task task) {
//        return amp(task.why());
//    }

    public float value(short[] effect) {
        return MetaGoal.privaluate(why, effect);
    }

    public Node<nars.attention.PriNode, Object> add(PriNode p) {
        return graph.addNode(p);
    }

    public boolean remove(PriNode p) {
        return graph.removeNode(p);
    }
    public void removeAll(PriNode... p) {
        for (PriNode pp:p)
            remove(pp);
    }


    public Cause newCause(Object name) {
        return newCause(new ShortToObjectFunction<Cause>() {
            @Override
            public Cause valueOf(short id) {
                return new Cause(id, name);
            }
        });
    }

    /**
     * automatically adds the cause id to each input
     */
    public CauseChannel<Task> newChannel(Object id) {
        return new TaskChannel(newCause(id));
    }

    public <C extends Cause> C newCause(ShortToObjectFunction<C> idToChannel) {
        synchronized (why) {
            short next = (short) (why.size());
            C c = idToChannel.valueOf(next);
            why.add(c);
            return c;
        }
    }

    /** attach a target to a source directly */
    public final PriNode input(PriNode target, PriNode source) {
        return input(target, null, source);
    }

    /** attach a target to multiple source through a merge function */
    public final PriNode input(Term target, PriNode.Merge mode, PriNode... sources) {
        return input(new PriNode(target), mode, sources);
    }
    public final PriAmp amp(Term target, PriNode.Merge mode, PriNode... sources) {
        return input(new PriAmp(target), mode, sources);
    }

    private <P extends PriNode> P input(P target, @Nullable PriNode.Merge mode, PriNode... sources) {
        if (mode!=null)
            target.input(mode);

        MapNodeGraph<PriNode, Object> g = graph;
        NodeGraph.MutableNode<PriNode, Object> thisNode = g.addNode(target);
        synchronized (g) {
            target.parent(sources, g, thisNode);
        }
        
        return target;
    }





    static final class TaskChannel extends CauseChannel<Task> {

        final Term uniqueCause;

        TaskChannel(Cause cause) {
            super(cause);
            this.uniqueCause = cause.why;
        }

        @Override protected void preAccept(Task x) {
            if (x instanceof Task) {
                ((AbstractTask) x).why(uniqueCause);
            } else if (x instanceof Remember) {
                preAccept(((Remember) x).input); //HACK
            }
        }


    }


}
