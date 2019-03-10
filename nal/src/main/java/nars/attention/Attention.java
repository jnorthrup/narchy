package nars.attention;

import jcog.event.Offs;
import jcog.math.FloatRange;
import jcog.math.IntRange;
import jcog.pri.ScalarValue;
import jcog.pri.bag.Bag;
import jcog.pri.bag.Sampler;
import jcog.pri.bag.impl.ArrayBag;
import jcog.pri.bag.impl.hijack.PriHijackBag;
import jcog.pri.op.PriMerge;
import nars.NAR;
import nars.Param;
import nars.attention.derive.DefaultDerivePri;
import nars.control.DurService;
import nars.link.TaskLink;
import nars.link.TaskLinkBag;

import java.util.Random;
import java.util.function.Function;

import static jcog.pri.op.PriMerge.plus;

/** abstract attention economy model */
public abstract class Attention extends DurService implements Sampler<TaskLink> {

    public Forgetting forgetting;

    /**
     * short target memory, TODO abstract and remove
     */
    public TaskLinkBag active = null;

    /**
     * tasklink activation
     */
    @Deprecated
    public final FloatRange activationRate = new FloatRange(1f, Param.tasklinkMerge == plus ? ScalarValue.EPSILON : 1, 1);

    public final FloatRange forgetRate = new FloatRange(0.1f,  0, 1f /* 2f */);


    public final IntRange activeCapacity = new IntRange(256, 0, 2024) {
        @Override
        @Deprecated protected void changed() {
            TaskLinkBag a = active;
            if (a != null)
                a.setCapacity(intValue());
        }
    };

    /** default derivePri for derivers */
    public DerivePri derivePri =
            //new DirectDerivePri();
            new DefaultDerivePri();
    private Offs ons;
    //new DefaultPuncWeightedDerivePri();


    protected Attention(Forgetting forgetting) {
        super();
        this.forgetting = forgetting;
    }

    @Override
    protected void starting(NAR nar) {
        super.starting(nar);

        active = new TaskLinkBag(
                arrayBag(),
                //hijackBag(),
                forgetRate,
                nar.exe.concurrent());

        active.setCapacity(activeCapacity.intValue());

        ons = new Offs(
                nar.onCycle(active::forget),
                nar.eventClear.on(active::clear)
        );

        nar.on(this);
    }

    @Override
    protected void stopping(NAR nar) {
        ons.off();
        ons = null;
        super.stopping(nar);
    }

    @Override
    protected void run(NAR n, long dt) {

        forgetting.update(n);
        derivePri.update(n);

    }



    @Override
    public void clear() {
        active.clear();
    }

    @Override
    public void sample(Random rng, Function<? super TaskLink, SampleReaction> each) {
        active.sample(rng, each);
    }

    private static class TaskLinkArrayBag extends ArrayBag<TaskLink, TaskLink> {

        public TaskLinkArrayBag(int initialCapacity) {
            super(Param.tasklinkMerge, initialCapacity);
        }

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

    public PriHijackBag<TaskLink, TaskLink> hijackBag() {
        return new TaskLinkHijackBag(activeCapacity.intValue(), 3);
    }
    protected Bag<TaskLink, TaskLink> arrayBag() {
        int c = activeCapacity.intValue();
        return new TaskLinkArrayBag(c);
    }


}
