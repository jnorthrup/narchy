package nars.control;

import jcog.Util;
import jcog.data.graph.MapNodeGraph;
import jcog.data.graph.NodeGraph;
import jcog.data.list.FastCoWList;
import jcog.data.list.FasterList;
import jcog.pri.PriBuffer;
import jcog.service.Service;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.attention.PriNode;
import nars.control.channel.CauseChannel;
import nars.control.op.Remember;
import nars.task.ITask;
import nars.task.NALTask;
import nars.term.Term;
import nars.time.event.DurService;
import org.eclipse.collections.api.block.function.primitive.ShortToObjectFunction;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;

import java.util.Arrays;
import java.util.function.Consumer;

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
public final class Control {

    /**
     * raw cause id (short int) -> cause table
     */
    public final FasterList<Cause> causes = new FasterList<>(512) {
        @Override
        protected Cause[] newArray(int newCapacity) {
            return new Cause[newCapacity];
        }
    };


    /** hierarchical priority distribution digraph (TODO ensure acyclic) */
    public final MapNodeGraph<PriNode,Object> graph = new MapNodeGraph<>(
            PriBuffer.newMap(false)
            //new ConcurrentFastIteratingHashMap<>(new Node[0])
    );
    private final PriNode root = new PriNode.ConstPriNode("root", ()->1);
    private final NodeGraph.MutableNode<PriNode,Object> rootNode = graph.addNode(root);


    /** ready queue. updated as necessary */
    public final FastCoWList<Causable> active = new FastCoWList(Causable[]::new);


    private final NAR nar;

    /**
     * proportion of time spent in forced curiosity
     * TODO move to its own control filter which ensures minimum fair priority among the causables
     */
    @Deprecated private final float explorationRate = 0.05f;

    public Control(NAR nar) {
        this.nar = nar;

        Consumer<ObjectBooleanPair<Service<NAR>>> serviceChange = (xb) -> {
            Service<NAR> s = xb.getOne();
            if (s instanceof Causable) {
                if (xb.getTwo())
                    add((Causable) s);
                else
                    remove((Causable) s);
            }
        };
        refreshServices();
        nar.plugin.change.on(serviceChange);
        refreshServices(); //to be sure

        DurService.on(nar, this::update);
    }

    private void update() {
        value();

        prioritize();

        root.pri(1);
        graph.forEachBF(root, (PriNode x)->x.update(graph));
    }

    protected void value() {

//        long time = nar.time();
//        if (lastUpdate == ETERNAL)
//            lastUpdate = time;
//        dt = (time - lastUpdate);


//        lastUpdate = time;



        int cc = causes.size();
        if (cc == 0)
            return;

//        if (cur.length != cc) {
//            resize(cc);
//        }

        Cause[] ccc = causes.array();

        float[] want = nar.feel.want;

        for (int i = 0; i < cc; i++) {

            Cause ci = ccc[i];

            ci.commit();

            float v = 0;
            Traffic[] cg = ci.credit;
            for (int j = 0; j < want.length; j++) {
                v += want[j] * cg[j].current;
            }

            ccc[i].setValue(v);

//            prev[i] = cur[i];
//            cur[i] = v;
        }


//        process();

//        for (int i = 0; i < cc; i++)
//            ccc[i].setValue(out[i]);
    }


    private void prioritize() {

        FastCoWList<Causable> cpu = active;
        int n = cpu.size();
        if (n == 0)
            return;

        float[] valMin = {Float.POSITIVE_INFINITY}, valMax = {Float.NEGATIVE_INFINITY};

        long now = nar.time();


        cpu.forEach(c -> {

            boolean sleeping = c.sleeping(now);
            if (sleeping)
                return;

            float vr;
            long tUsed = c.used();
            if (tUsed <= 0) {
                c.value = Float.NaN;
                vr = 0;
            } else {
                double v = Math.max(0, c.value = c.value());
                //double cyclesUsed = ((double) tUsed) / cycleIdealNS;
                //vr = (float) (v / (1 + cyclesUsed));

                vr = v > Float.MIN_NORMAL ? (float) (v / ((1 + tUsed)/1.0E-9)) : 0;
                assert (vr == vr);
            }

            c.valueRate = vr;

            if (vr > valMax[0]) valMax[0] = vr;
            if (vr < valMin[0]) valMin[0] = vr;

        });

        float valRange = valMax[0] - valMin[0];
        if (Float.isFinite(valRange) && Math.abs(valRange) > Float.MIN_NORMAL) {
            float exploreMargin = explorationRate * valRange;

            cpu.forEach(c -> {
                if (c.sleeping()) {
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
            cpu.forEach(s -> s.pri(pFlat));
        }

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
        return amp(task.cause());
    }

    public float value(short[] effect) {
        return MetaGoal.privaluate(causes, effect);
    }


    /** attaches a priority node to the priority graph
     * @return*/
    public NodeGraph.MutableNode<PriNode, Object> add(PriNode p) {
        NodeGraph.MutableNode<PriNode, Object> a = graph.addNode(p);
        graph.addEdgeByNode(rootNode, "pri", a);
        return a;
    }

    public boolean remove(PriNode p) {
        return graph.removeNode(p);
    }


    public Cause newCause(Object name) {
        return newCause((id) -> new Cause(id, name));
    }

    /**
     * automatically adds the cause id to each input
     */
    public CauseChannel<ITask> newChannel(Object id) {
        return new TaskChannel(newCause(id));
    }

    public <C extends Cause> C newCause(ShortToObjectFunction<C> idToChannel) {
        synchronized (causes) {
            short next = (short) (causes.size());
            C c = idToChannel.valueOf(next);
            causes.add(c);
            return c;
        }
    }

    /** registers a new pri node */
    public PriNode newPri(Term id) {
        PriNode p = new PriNode(id);
        add(p);
        return p;
    }

    private class TaskChannel extends CauseChannel<ITask> {

        private final short ci;
        final short[] uniqueCause;

        TaskChannel(Cause cause) {
            super(cause);
            this.ci = cause.id;
            uniqueCause = new short[]{ci};
        }

        @Override
        public void input(ITask x) {
            if (process(x))
                nar.in.put(x);
        }

        protected boolean process(Object x) {
            if (x instanceof NALTask) {
                NALTask t = (NALTask) x;
                short[] currentCause = t.cause();
                int tcl = currentCause.length;
                switch (tcl) {
                    case 0:
                        //shared one-element cause
                        //assert (uniqueCause[0] == ci);
                        t.cause(uniqueCause);
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
                return process(((Remember) x).input);
            } else
                return x != null;

            return true;
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


    private boolean remove(Causable s) {
        return active.removeIf(x->x==s);
    }

    private void add(Causable s) {
        //InstrumentedCausable r = can.computeIfAbsent(s, InstrumentedCausable::new);
        Term sid = s.id;
        if (active.containsInstance(s))
            throw new RuntimeException("causable " + s + " already present");
        if (active.OR(x->sid.equals(x.id)))
            throw new RuntimeException("causable " + s + " name collision");

        active.add(s);
    }

    private void refreshServices() {
        nar.plugins().filter(x -> x instanceof Causable).forEach(x -> add((Causable) x));
    }

}
