package nars.util;

import jcog.data.list.FasterList;
import jcog.math.FloatRange;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import jcog.pri.bag.impl.ArrayBag;
import jcog.pri.bag.impl.PLinkArrayBag;
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
     * <p>
     * 1.00 - impossible
     * 0.75 - mid-range
     * 0.50 - hair-trigger hysterisis
     */
    public final FloatRange exeThresh;


    static final Logger logger = LoggerFactory.getLogger(AtomicExec.class);

    final BiConsumer<Term, TimeAware> exe;

    final static int ACTIVE_CAPACITY = 16;
    final ArrayBag<Term, PriReference<Term>> active = new PLinkArrayBag<>(PriMerge.max, ACTIVE_CAPACITY);

    private DurService onCycle;

    public AtomicExec(@Nullable BiConsumer<Term, TimeAware> exe, float exeThresh) {
        this(exe, new FloatRange(exeThresh, 0.5f, 1f));
    }

    public AtomicExec(@Nullable BiConsumer<Term, TimeAware> exe, FloatRange exeThresh) {
        this.exe = exe == null ? ((BiConsumer) this) : exe;
        active.setCapacity(ACTIVE_CAPACITY);
        this.exeThresh = exeThresh;
    }

    /**
     * implementations can override this to prefilter invalid operation patterns
     */
    protected Task exePrefilter(Task x) {
        Subterms a = Operator.args(x);
        return a.hasAny(Op.AtomicConstant) ? x : null;
    }


    public void update(NAR n) {


        long now = n.time();
        long end = now;
        long start = now - n.dur();

        float exeThresh = this.exeThresh.floatValue();

        List<Term> dispatch = new FasterList(active.size());

        active.forEach((PriReference<Term> x) -> {
            Term xx = x.get();

            Concept c = n.concept(xx);
            if (c == null) {
                return;
            }


            Truth goalTruth = c.goals().truth(start, end, n);
            if (goalTruth == null || goalTruth.expectation() <= exeThresh) {
                return; //it may not have been input to the belief table yet so dont delete
            }

            Truth beliefTruth = c.beliefs().truth(start, end, n); /* assume false with no evidence */
            if (beliefTruth != null && beliefTruth.expectation() >= exeThresh) {
                return;
            }

            logger.info("{} EVOKE (b={},g={}) {}", n.time(), beliefTruth, goalTruth, xx);
            dispatch.add(xx);

            x.delete();
        });

        dispatch.forEach(tt -> exe.accept(tt, n));

        active.commit();
        if (active.isEmpty()) {
            disable();
        }
    }

    @Override
    public @Nullable Task apply(Task x, NAR n) {


        Task y = exePrefilter(x);
        if (y == null)
            return x;
        if (y != x)
            return y;

        x = y;

        if (x.isCommand()) {

            exe.accept(x.term(), n);
            return null;
        } else {

            active.put(new PLink(x.term().concept() /* incase it contains temporal, we will dynamically match task anyway on invocation */,
                    x.priElseZero()
            ));


            enable(n);


            return x;
        }
    }

    /**
     * operator goes into active probing mode
     */
    protected void enable(NAR n) {
        synchronized (this) {
            if (onCycle == null) {
                onCycle = DurService.on(n, this::update);
            }
        }
    }

    /**
     * operator leaves active probing mode
     */
    protected void disable() {
        synchronized (this) {
            if (onCycle != null) {
                onCycle.off();
                onCycle = null;
            }
        }
    }


}
