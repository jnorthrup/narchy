package nars.control;

import jcog.Skill;
import jcog.TODO;
import jcog.Util;
import jcog.data.graph.MapNodeGraph;
import jcog.data.graph.NodeGraph;
import jcog.data.list.FasterList;
import jcog.func.IntIntToObjectFunction;
import jcog.learn.Agent;
import jcog.learn.AgentBuilder;
import jcog.learn.Agenterator;
import jcog.math.FloatRange;
import jcog.math.FloatSupplier;
import jcog.pri.PriMap;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.attention.AntistaticBag;
import nars.attention.PriNode;
import nars.control.channel.CauseChannel;
import nars.control.op.Remember;
import nars.task.ITask;
import nars.task.NALTask;
import nars.time.part.DurLoop;
import org.eclipse.collections.api.block.function.primitive.ShortToObjectFunction;

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
    public final FasterList<Why> why = new FasterList<>(512) {
        @Override
        protected Why[] newArray(int newCapacity) {

            if (newCapacity >= Short.MAX_VALUE)
                throw new TODO("all possible 16-bit cause ID's consumed");

            return new Why[newCapacity];
        }
    };


    /** hierarchical priority distribution DAG (TODO ensure acyclic) */
    public final MapNodeGraph<PriNode,Object> graph = new MapNodeGraph<>(
            PriMap.newMap(false)
            //new ConcurrentFastIteratingHashMap<>(new Node[0])
    );
    private final PriNode root = new PriNode.ConstPriNode("root", 1);
    private final NodeGraph.MutableNode<PriNode,Object> rootNode = graph.addNode(root);




    private final NAR nar;

    /**
     * proportion of time spent in forced curiosity
     * TODO move to its own control filter which ensures minimum fair priority among the causables
     */
    @Deprecated public final FloatRange explorationRate = new FloatRange(0.05f, 0, 1);

    private float updatePeriods =
            1;
            //2;

    private final DurLoop updater;

    public Control(NAR nar) {
        this.nar = nar;



        updater = nar.onDur(this::update);
        updater.durs(updatePeriods);
    }

    private void update() {

        schedule();

        prioritize();
    }





    private void schedule() {

        AntistaticBag<How> how = nar.how;
        int n = how.size();
        if (n == 0)
            return;

        float[] valMin = {Float.POSITIVE_INFINITY}, valMax = {Float.NEGATIVE_INFINITY};

        long now = nar.time();


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

                vr = v > Float.MIN_NORMAL ? (float) (v / ((1.0 + tUsed)/1.0E9)) : 0;
                assert (vr == vr);
            }

            c.valueRate = vr;

            if (vr > valMax[0]) valMax[0] = vr;
            if (vr < valMin[0]) valMin[0] = vr;

        });

        float valRange = valMax[0] - valMin[0];
        if (Float.isFinite(valRange) && Math.abs(valRange) > Float.MIN_NORMAL) {
            float exploreMargin = explorationRate.floatValue() * valRange;

            how.forEach(c -> {
                if (c.inactive()) {
                    c.pri(0);
                } else {
                    float vNorm = Util.normalize(c.valueRate, valMin[0] - exploreMargin, valMax[0]);
                    //pri(s, vNorm);
                    c.pri(vNorm);
                }
            });
        } else {
            //FLAT
            float pFlat = 1f / n;
            how.forEach(s -> s.pri(pFlat));
        }

    }

    private void prioritize() {
        root.pri(1);
        graph.forEachBF(root, x->x.update(graph));
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
        return MetaGoal.privaluate(why, effect);
    }


    /** attaches a priority node to the priority graph
     */
    public NodeGraph.MutableNode<PriNode, Object> add(PriNode p) {
        NodeGraph.MutableNode<PriNode, Object> a = graph.addNode(p);
        graph.addEdgeByNode(rootNode, "pri", a);
        return a;
    }

    public boolean remove(PriNode p) {
        return graph.removeNode(p);
    }


    public Why newCause(Object name) {
        return newCause((id) -> new Why(id, name));
    }

    /**
     * automatically adds the cause id to each input
     */
    public CauseChannel<ITask> newChannel(Object id) {
        return new TaskChannel(newCause(id));
    }

    public <C extends Why> C newCause(ShortToObjectFunction<C> idToChannel) {
        synchronized (why) {
            short next = (short) (why.size());
            C c = idToChannel.valueOf(next);
            why.add(c);
            return c;
        }
    }

    static final class TaskChannel extends CauseChannel<ITask> {

        private final short ci;
        final short[] uniqueCause;

        TaskChannel(Why why) {
            super(why);
            this.ci = why.id;
            uniqueCause = new short[]{ci};
        }

        @Override protected void preAccept(ITask x) {
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
            int cc = Param.causeCapacity.intValue();
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


    /** creates a base agent that can be used to interface with external controller
     *  it will be consistent as long as the NAR architecture remains the same.
     *  TODO kill signal notifying changed architecture and unwiring any created WiredAgent
     *  */
    public Agenterator agent(FloatSupplier reward, IntIntToObjectFunction<Agent> a) {
        AgentBuilder b = new AgentBuilder(reward);
        for (MetaGoal m : MetaGoal.values()) {
            b.out(5, i->{
                float w;
                switch(i) {
                    default:
                    case 0: w = -1; break;
                    case 1: w = -0.5f; break;
                    case 2: w = 0; break;
                    case 3: w = +0.5f; break;
                    case 4: w = +1; break;
                }
                nar.emotion.want(m, w);
            });
        }

        for (Why c : why) {

            b.in(() -> {
                float ca = c.amp();
                return ca==ca ? ca : 0;
            });

//            for (MetaGoal m : MetaGoal.values()) {
//                Traffic mm = c.credit[m.ordinal()];
//                b.in(()-> mm.current);
//            }
            //TODO other data
        }

        for (How c : nar.how) {
            b.in(() -> {
                PriNode cp = c.pri;
                return Util.unitize(cp.priElseZero());
            });
            //TODO other data
        }

        return b.get(a);
    }
}
