package nars.op;

import jcog.sort.FloatRank;
import nars.attention.What;
import nars.control.How;
import nars.link.TaskLink;
import nars.time.When;
import nars.time.event.WhenTimeIs;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;

/** utility service for periodically ranking the
 * active tasklinks by some metric, and applying a procedure
 * based on the aggregate.
 */
public abstract class LinkProcessor<Y> extends How {


    protected LinkProcessor() {
        super();
    }

    @Override
    public void next(What w, BooleanSupplier kontinue) {


//        RankedN<Y> best = new RankedN<>(newArray(cap()), score()); //TODO option for threadLocal
//        //when = When.sinceAgo(nar.dur(), nar);

//        w.forEach(x -> {
//            Y y = apply(x, when);
//            if (y!=null)
//                best.add(y);
//        });
//
//        if (!best.isEmpty())
//            run(best, w);

        do {
            When when = WhenTimeIs.now(w);
            TaskLink x = w.sample();
            if (x != null) {
                Y y = apply(x, when);
                if (y != null)
                    run(y, w);
            }
        } while (kontinue.getAsBoolean());

    }


    protected abstract Y[] newArray(int size);

    /** here a prefilter can be implemented by returning null */
    @Nullable abstract protected Y apply(TaskLink x, When when);

    abstract public FloatRank<Y> score();

    abstract public int cap();

    abstract protected void run(Y l, What w);

}
