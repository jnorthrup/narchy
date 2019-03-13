package nars.attention;

import jcog.math.FloatRange;
import jcog.math.IntRange;
import jcog.pri.PriBuffer;
import jcog.pri.bag.Sampler;
import jcog.pri.bag.impl.ArrayBag;
import jcog.pri.bag.impl.hijack.PriHijackBag;
import jcog.pri.op.PriMerge;
import nars.NAR;
import nars.Param;
import nars.attention.derive.DefaultDerivePri;
import nars.link.TaskLink;
import nars.link.TaskLinkBag;
import nars.time.event.DurService;

import java.util.Random;
import java.util.function.Function;

/** abstract attention economy model */
public class Attention extends DurService implements Sampler<TaskLink> {

    public Forgetting forgetting = new Forgetting.AsyncForgetting();
    /**
     * short target memory, TODO abstract and remove
     */
    public TaskLinkBag active = null;

//    /**
//     * tasklink activation
//     */
//    @Deprecated
//    public final FloatRange activationRate = new FloatRange(1f, Param.tasklinkMerge == plus ? ScalarValue.EPSILON : 1, 1);

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
            //new DefaultPuncWeightedDerivePri();


    @Override
    protected void starting(NAR nar) {
        super.starting(nar);

        int c = activeCapacity.intValue();
        active = new TaskLinkBag(
                new TaskLinkArrayBag(c)
                //new TaskLinkHijackBag(c, 5),
        );

        active.setCapacity(activeCapacity.intValue());

        on(
                nar.eventClear.on(active::clear),
                nar.onCycle(() -> {
//                    System.out.println(nar.time());
//                    active.pre.items.forEach((k,v)->System.out.println(v));
                            active.commit(
                                    forgetting.forget(active, 1f, forgetRate.floatValue()));
                        }
                )
        );

    }

    @Override
    protected void run(NAR n, long dt) {
        forgetting.update(n);
        derivePri.update(n);
    }

    @Override
    public void sample(Random rng, Function<? super TaskLink, SampleReaction> each) {
        active.sample(rng, each);
    }

    private static class TaskLinkArrayBag extends ArrayBag<TaskLink, TaskLink> {

        public TaskLinkArrayBag(int initialCapacity) {
            super(Param.tasklinkMerge, initialCapacity, PriBuffer.newConcurrentMap());
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


}
