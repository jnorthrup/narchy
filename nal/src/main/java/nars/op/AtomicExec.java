package nars.op;

import jcog.bag.impl.ArrayBag;
import jcog.pri.PLink;
import jcog.pri.op.PriMerge;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.control.DurService;
import nars.control.MetaGoal;
import nars.term.Term;
import nars.truth.Truth;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static nars.time.Tense.ETERNAL;

/**
 * debounced and atomically/asynchronously executable operation
 */
public class AtomicExec implements BiFunction<Task, NAR, Task> {

    //    private final float minPeriod;
    private final MutableFloat desireThresh;

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

    private DurService onCycle;
    private long lastUpdated = ETERNAL;


    public AtomicExec(BiConsumer<Task, NAR> exe, float dThresh) {
        this(exe, new MutableFloat(dThresh));
    }

    public AtomicExec(BiConsumer<Task, NAR> exe, MutableFloat dThresh) {
        this.exe = exe;
        active.setCapacity(ACTIVE_CAPACITY);
        this.desireThresh = dThresh;
    }

    /**
     * implementations can override this to prefilter invalid operation patterns
     */
    protected boolean exePrefilter(Task x) {
        return true;
    }

    final AtomicBoolean busy = new AtomicBoolean(false);

    protected void update(NAR n) {

        long now = n.time();
        if (now!=lastUpdated) {
            busy.set(false); //force reset if new clock time occurrs.  the runLater may have been lost
        }

        if (!busy.compareAndSet(false, true))
            return; //in-progress

        lastUpdated = now;

        try {
            //probe all active concepts.
            //  remove any below desire threshold
            //  execute any above desire-belief threshold
            //  if no active remain, disable update service

            assert (!active.isEmpty());

            int dur = n.dur();
            long start = now - dur / 2;
            long end = now + dur / 2;
            List<Task> toInvoke = $.newArrayList(0);
            float desireThresh = this.desireThresh.floatValue();
            active.forEach(x -> {
                Term xx = x.get();
                Concept c = n.concept(xx);
                Task desire = c.goals().match(start, end, xx, n);
                Truth desireTruth;
                float d;
                if (desire == null
                        || (desireTruth = desire.truth(start, end, dur, Param.TRUTH_EPSILON)) == null
                        || (d = desireTruth.expectation()) < desireThresh) {
                    x.delete();
                    return;
                }
                Truth belief = c.beliefs().truth(start, end, n);
                float b = belief == null ? 0 /* assume false with no evidence */ :
                        belief.expectation();

                float delta = d - b;
                if (delta >= 0) {
                    toInvoke.add(desire);
                    MetaGoal.learn(MetaGoal.Action, desire.cause(), d, n);
                }
            });
            active.commit();
            if (active.isEmpty()) {
                onCycle.off();
                onCycle = null;
            }
            if (!toInvoke.isEmpty()) {
                n.runLater(() -> {
                    for (int i = 0, toInvokeSize = toInvoke.size(); i < toInvokeSize; i++) {
                        Task tt = toInvoke.get(i);
                        if (!tt.isDeleted()) {
                            exe.accept(tt, n);
                        }
                    }
                    busy.set(false);
                });
            } else {
                busy.set(false);
            }
        } finally {

        }
    }

    @Override
    public @Nullable Task apply(Task x, NAR n) {

        //TODO handle CMD's

        if (x.freq() <= 0.5f + Param.TRUTH_EPSILON)
            return x; //dont even think about executing it, but pass thru to reasoner

        if (!exePrefilter(x))
            return x; //pass thru to reasoner

        active.put(new PLink(x.term().root() /* incase it contains temporal, we will dynamically match task anyway on invocation */,
                x.priElseZero()
        ));

        if (onCycle == null) {
            synchronized (active) {
                if (onCycle==null) {
                    onCycle = DurService.on(n, this::update);
                }
            }
        }

        return x;
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
