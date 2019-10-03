package nars.control;

import jcog.Skill;
import jcog.Util;
import jcog.data.graph.MapNodeGraph;
import jcog.data.graph.Node;
import jcog.data.graph.NodeGraph;
import jcog.data.list.FasterList;
import jcog.data.map.ConcurrentFastIteratingHashMap;
import nars.NAL;
import nars.NAR;
import nars.Task;
import nars.attention.AntistaticBag;
import nars.attention.PriAmp;
import nars.attention.PriNode;
import nars.control.channel.CauseChannel;
import nars.control.op.Remember;
import nars.task.NALTask;
import nars.term.Term;
import nars.time.part.DurLoop;
import org.eclipse.collections.api.block.function.primitive.ShortToObjectFunction;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Arrays;

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
    public final FasterList<Cause> cause = new FasterList<>(0, new Cause[512]);


    /** hierarchical priority distribution DAG (TODO ensure acyclic) */
    public final MapNodeGraph<PriNode,Object> graph = new MapNodeGraph<>(
            new ConcurrentFastIteratingHashMap<>(new Node[0])
            //PriMap.newMap(false)
    );





    private final NAR nar;

    public final DurLoop loop;

    public Control(NAR nar) {
        this.nar = nar;

        loop = nar.onDur(this::update);
    }

    private void update() {
        nar.what.commit(null);
        nar.how.commit(null);
        prioritize();
        schedule();
    }

    public MetaGoal.Report stats(PrintStream out) {
        MetaGoal.Report r = new MetaGoal.Report();
        r.add(cause);
        r.print(out);
        return r;
    }

    private void prioritize() {


        graph.nodes().forEach(x->{
            //if (x.edgeCount(true, false)==0) { //root

                x.id().update(graph);
            //}
        });
    }

    /**
     * computes an evaluation amplifier factor, in range 0..2.0.
     * VALUE -> AMP
     * -Infinity -> amp=0
     * 0     -> amp=1
     * +Infinity -> amp=2
     */
    public float amp(short[] effect) {
        return 1f + Util.tanhFast(value(effect));
    }

    public final float amp(Task task) {
        return amp(task.why());
    }

    public float value(short[] effect) {
        return MetaGoal.privaluate(cause, effect);
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
        return newCause((id) -> new Cause(id, name));
    }

    /**
     * automatically adds the cause id to each input
     */
    public CauseChannel<Task> newChannel(Object id) {
        return new TaskChannel(newCause(id));
    }

    public <C extends Cause> C newCause(ShortToObjectFunction<C> idToChannel) {
        synchronized (cause) {
            short next = (short) (cause.size());
            C c = idToChannel.valueOf(next);
            cause.add(c);
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
        NodeGraph.MutableNode<PriNode,Object> thisNode = g.addNode(target);
        target.parent(sources, g, thisNode);
        return target;
    }


    private void schedule() {

        AntistaticBag<How> how = nar.how;
        int n = how.size();
        if (n == 0)
            return;

        float[] valMin = {Float.POSITIVE_INFINITY}, valMax = {Float.NEGATIVE_INFINITY};

        long now = nar.time();


        final double[] valueRateSumPos = {0}, valueRateSumNeg = {0};
        how.forEach(c -> {

            boolean sleeping = c.inactive(now);
            if (sleeping)
                return;

            float vr;
            long tUsed = c.used();
            if (tUsed <= 0) {
                c.value = Float.NaN;
                vr = 0;
            } else {
                double v = c.value();
                //double v = Math.max(0, c.value = c.value());
                //double cyclesUsed = ((double) tUsed) / cycleIdealNS;
                //vr = (float) (v / (1 + cyclesUsed));

                vr = (float) (v / ((1.0 + tUsed)/1.0E9));
                assert (vr == vr);
            }

            c.valueRate = vr;
            if (vr >= 0)
                valueRateSumPos[0] += vr;
            else
                valueRateSumNeg[0] += vr;
            if (vr > valMax[0]) valMax[0] = vr;
            if (vr < valMin[0]) valMin[0] = vr;

        });

        float valRange = valMax[0] - valMin[0];
        if (Float.isFinite(valRange) && Math.abs(valRange) > Float.MIN_NORMAL) {
//            /**
//             * proportion of time spent in forced curiosity
//             * TODO move to its own control filter which ensures minimum fair priority among the causables
//             */
//            @Deprecated public final FloatRange explorationRate = new FloatRange(0.05f, 0, 1);
//            float exploreMargin = explorationRate.floatValue() * valRange;

            how.forEach(c -> {
//                if (c.inactive()) {
//                    c.pri(0);
//                } else {
                float vNorm = (float) Util.normalize(c.valueRate, valueRateSumNeg[0], +valueRateSumPos[0]);
                c.valueRateNormalized = vNorm;
//                }
            });
        } else {
            //FLAT
            float pFlat = 1f / n;
            how.forEach(s -> s.valueRateNormalized = pFlat);
        }


    }


    static final class TaskChannel extends CauseChannel<Task> {

        private final short ci;
        final short[] uniqueCause;

        TaskChannel(Cause cause) {
            super(cause);
            this.ci = cause.id;
            uniqueCause = new short[]{ci};
        }

        @Override protected void preAccept(Task x) {
            if (x instanceof NALTask) {
                NALTask t = (NALTask) x;
                short[] currentCause = t.why();
                int tcl = currentCause.length;
                switch (tcl) {
                    case 0:
                        //shared one-element cause
                        t.cause(uniqueCause); //assert (uniqueCause[0] == ci);
                        break;
                    case 1:
                        if (currentCause == uniqueCause) {
                            /* same instance */
                        } else if (currentCause[0] == ci) {
                            //replace with shared instance
                            t.cause(uniqueCause);
                        } else {
                            t.cause(append(currentCause, tcl));
                        }
                        break;
                    default:
                        t.cause(append(currentCause, tcl));
                        break;
                }
            } else if (x instanceof Remember) {
                preAccept(((Remember) x).input); //HACK
            }

        }

        private short[] append(short[] currentCause, int tcl) {
            int cc = NAL.causeCapacity.intValue();
            short[] tc = Arrays.copyOf(currentCause, Math.min(cc, tcl + 1));
            int target;
            if (tc.length == cc) {
                //shift
                System.arraycopy(tc, 1, tc, 0, tc.length - 1);
                target = tc.length-1;
            } else {
                target = tcl;
            }
            tc[target] = ci;
            return tc;
        }

    }


}
