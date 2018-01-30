package nars.op;

import com.google.common.collect.Sets;
import jcog.bag.impl.ArrayBag;
import jcog.pri.PLink;
import jcog.pri.op.PriMerge;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.control.DurService;
import nars.task.signal.Truthlet;
import nars.task.signal.TruthletTask;
import nars.term.Term;
import nars.truth.Truth;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static jcog.Texts.n4;
import static nars.Op.GOAL;
import static nars.time.Tense.ETERNAL;
import static nars.truth.TruthFunctions.c2w;

/**
 * debounced and atomically/asynchronously executable operation
 */
public class AtomicExec implements BiFunction<Task, NAR, Task> {

    //    private final float minPeriod;
    public final MutableFloat exeThresh;

//    /**
//     * time of the current rising edge, or ETERNAL if not activated
//     */
//    final AtomicLong rise = new AtomicLong(ETERNAL);
//
//    /**
//     * how many durations before the current time in which a goal remains active in the present
//     */
//    final static float presentDurs = 0.5f;


    static final Logger logger = LoggerFactory.getLogger(AtomicExec.class);

    final BiConsumer<Task, NAR> exe;

    final static int ACTIVE_CAPACITY = 16;
    final ArrayBag<Term, PLink<Term>> active = new ArrayBag<Term, PLink<Term>>(PriMerge.max, new HashMap()) {
        @Nullable
        @Override
        public Term key(PLink<Term> l) {
            return l.get();
        }
    };

    /**
     * prevents repeated invocations while one is already in progress
     */
    final Set<Term> dispatched = Sets.newConcurrentHashSet();

    private DurService onCycle;
    private long lastUpdated = ETERNAL;


    public AtomicExec(BiConsumer<Task, NAR> exe, float dThresh) {
        this(exe, new MutableFloat(dThresh));
    }

    public AtomicExec(BiConsumer<Task, NAR> exe, MutableFloat dThresh) {
        this.exe = exe;
        active.setCapacity(ACTIVE_CAPACITY);
        this.exeThresh = dThresh;
    }

    /**
     * implementations can override this to prefilter invalid operation patterns
     */
    protected Task exePrefilter(Task x) {
        return x;
    }


    protected void update(NAR n) {

        long now = n.time();
//        if (now!=lastUpdated) {
//            busy.set(false); //force reset if new clock time occurrs.  the runLater may have been lost
//        }
//
//        if (!busy.compareAndSet(false, true))
//            return; //in-progress

        lastUpdated = now;


        //probe all active concepts.
        //  remove any below desire threshold
        //  execute any above desire-belief threshold
        //  if no active remain, disable update service

        int dur = n.dur();
        long start = now;
        long end = now + dur;
        List<Task> evoke = $.newArrayList(0);

        float exeThresh = this.exeThresh.floatValue();
        assert (exeThresh >= 0.5f);
        float goalDeltaThresh = exeThresh - 0.5f;

        active.forEach(x -> {
            Term xx = x.get();

            if (dispatched.contains(xx))
                return; //skip, already in progress

            Concept c = n.concept(xx);
            Truth goalTruth = c.goals().truth(start, end, n);

            float g;
            if (goalTruth == null || (g = goalTruth.expectation()) < exeThresh) {
                x.delete(); //delete the link
                return;
            }
            Truth belief = c.beliefs().truth(start, end, n);
            float b = belief == null ? 0 /* assume false with no evidence */ :
                    belief.expectation();

            float delta = g - b;
            if (delta >= goalDeltaThresh) {
                logger.info("{} EVOKE (b={},g={}) {}", n.time(),
                        n4(b), n4(g), xx);
                evoke.add(new TruthletTask(xx, GOAL, Truthlet
                        .impulse(now, now + dur, 1f, 0f, c2w(n.confDefault(GOAL))), n));
                dispatched.add(xx);
                //MetaGoal.learn(MetaGoal.Action, goal.cause(), g, n);
            }
        });
        active.commit();
        if (active.isEmpty()) {
            if (onCycle != null) {
                onCycle.off();
                onCycle = null;
            }
        }
        if (!evoke.isEmpty()) {
            //n.run(() -> {

            for (int i = 0, toInvokeSize = evoke.size(); i < toInvokeSize; i++) {
                Task tt = evoke.get(i);
                if (!tt.isDeleted()) {
                    exe.accept(tt, n);
                }
                boolean d = dispatched.remove(tt.term().conceptual());
                assert (d);
            }

            //});
        }

    }

    @Override
    public @Nullable Task apply(Task x, NAR n) {

        //TODO handle CMD's

        Task y = exePrefilter(x);
        if (y != x)
            return y; //transformed
        if (y == null)
            return x; //pass-through to reasoner

        x = y;

        if (x.isCommand()) {
            //immediately execute
            exe.accept(x, n);
            return null; //absorbed
        } else {

            active.put(new PLink(x.term().conceptual() /* incase it contains temporal, we will dynamically match task anyway on invocation */,
                    x.priElseZero()
            ));

            if (onCycle == null) {
                synchronized (active) {
                    if (onCycle == null) {
                        onCycle = DurService.on(n, this::update);
                    }
                }
            }

            return x;
        }
    }


//    public void tryInvoke(Task x, NAR n) {
//
//        long now = n.time();
//        if (!x.isDeleted() && lastActivity == ETERNAL || ((now) - lastActivity > minPeriod * n.dur()) && rise.compareAndSet(ETERNAL, now)) {
//            try {
//                @Nullable Concept cc = x.concept(n, true);
//                if (cc != null) {
//                    Truth desire = cc.goals().truth(now, n);
//                    if (desire != null && desire.expectation() >= desireThresh) {
//                        exe.accept(x, n);
//                    }
//                }
//            } catch (Throwable t) {
//                logger.info("{} {}", this, t);
//            } finally {
//                //end invocation
//                lastActivity = n.time();
//                rise.set(ETERNAL);
//            }
//        }
//    }

//    class FutureTask extends NativeTask.SchedTask {
//
//        private final Task task;
//
//        public FutureTask(long whenOrAfter, Task x) {
//            super(whenOrAfter, (NAR nn) -> tryInvoke(x, nn));
//            this.task = x;
//        }
//
//        @Override
//        public boolean equals(Object obj) {
//            return obj instanceof FutureTask && ((FutureTask)obj).task.equals(task) && ((SchedTask) obj).when == when;
//        }
//    }

}
