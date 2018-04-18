package nars.util;

import jcog.bag.impl.ArrayBag;
import jcog.bag.impl.PLinkArrayBag;
import jcog.list.FasterList;
import jcog.math.FloatRange;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import jcog.pri.op.PriMerge;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.concept.Concept;
import nars.concept.Operator;
import nars.control.DurService;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * debounced and atomically/asynchronously executable operation
 * TODO support asych executions that delay or stretch feedback until they complete
 */
public class AtomicExec implements BiFunction<Task, NAR, Task> {

    /**
     * expectation threhold (in range 0.5..1.0 indicating the minimum expectation minimum
     * for desire, and the 1-exp expectation maximum of belief necessary to invoke the action.
     *
     *      1.00 - impossible
     *      0.75 - mid-range
     *      0.50 - hair-trigger hysterisis
     */
    public final FloatRange exeThresh;

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

    final BiConsumer<Term, NAR> exe;

    final static int ACTIVE_CAPACITY = 16;
    final ArrayBag<Term, PriReference<Term>> active = new PLinkArrayBag<>(PriMerge.max, ACTIVE_CAPACITY);


    private DurService onCycle;



    public AtomicExec(@Nullable BiConsumer<Term, NAR> exe, float exeThresh) {
        this(exe, new FloatRange(exeThresh, 0.5f, 1f));
    }

    public AtomicExec(@Nullable BiConsumer<Term, NAR> exe, FloatRange exeThresh) {
        this.exe = exe == null ? ((BiConsumer) this) : exe;
        active.setCapacity(ACTIVE_CAPACITY);
        this.exeThresh = exeThresh;
    }

    /**
     * implementations can override this to prefilter invalid operation patterns
     */
    protected Task exePrefilter(Task x) {
        Subterms a = Operator.args(x);
        return a.hasAny(Op.ConstantAtomics) ? x : null; //avoid purely variable args
    }


    public void update(NAR n) {

//        if (!busy.compareAndSet(false, true))
//            return; //in-progress


        //probe all active concepts.
        //  remove any below desire threshold
        //  execute any above desire-belief threshold
        //  if no active remain, disable update service


        long[] focus = n.timeFocus();
        //List<Task> evoke = $.newArrayList(0);

        float exeThresh = this.exeThresh.floatValue();

        List<Term> dispatch = new FasterList(active.size());

        active.forEach(x -> {
            Term xx = x.get();

            Concept c = n.concept(xx);
            if (c == null) {
                //concept disappeared
                x.delete();
                return;
            }

            long start = focus[0];
            long end = focus[1];
            Truth goalTruth = c.goals().truth(start, end, n);
            if (goalTruth == null || goalTruth.expectation() < exeThresh) {
                x.delete();
                return; //undesired
            }

            Truth beliefTruth = c.beliefs().truth(start, end, n); /* assume false with no evidence */
            if (beliefTruth != null && beliefTruth.expectation() > exeThresh) {
                return; //satisfied
            }

            logger.info("{} EVOKE (b={},g={}) {}", n.time(), beliefTruth, goalTruth, xx);
            dispatch.add(xx);
            x.delete();
            //MetaGoal.learn(MetaGoal.Action, goal.cause(), g, n);
        });

        dispatch.forEach(tt -> {
            exe.accept(tt, n);
        });

        active.commit();
        if (active.isEmpty()) {
            disable();
        }
    }

    @Override
    public @Nullable Task apply(Task x, NAR n) {

        //TODO handle CMD's

        Task y = exePrefilter(x);
        if (y == null)
            return x; //pass-through to reasoner
        if (y != x)
            return y; //transformed

        x = y;

        if (x.isCommand()) {
            //immediately execute
            exe.accept(x.term(), n);
            return null; //absorbed
        } else {

            active.put(new PLink(x.term().concept() /* incase it contains temporal, we will dynamically match task anyway on invocation */,
                    x.priElseZero()
            ));


            enable(n);


            return x;
        }
    }

    /** operator goes into active probing mode */
    protected void enable(NAR n) {
        synchronized (this) {
            if (onCycle == null) {
                onCycle = DurService.on(n, this::update);
            }
        }
    }
    /** operator leaves active probing mode */
    protected void disable() {
        synchronized (this) {
            if (onCycle != null) {
                onCycle.off();
                onCycle = null;
            }
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
