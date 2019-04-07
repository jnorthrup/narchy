package nars.op;

import jcog.sort.FloatRank;
import jcog.sort.RankedN;
import nars.NAR;
import nars.attention.What;
import nars.control.How;
import nars.link.TaskLink;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;

/** utility service for periodically ranking the
 * active tasklinks by some metric, and applying a procedure
 * based on the aggregate.
 */
public abstract class LinkRanker<Y> extends How {

    public LinkRanker() {
        super();
    }
    public LinkRanker(NAR n) {
        super(n);
    }

    @Override
    public void next(What w, BooleanSupplier kontinue) {


        RankedN<Y> best = new RankedN<>(newArray(cap()), score()); //TODO option for threadLocal

        //when = When.sinceAgo(nar.dur(), nar);

        w.forEach(x -> {
            Y y = apply(x);
            if (y!=null)
                best.add(y);
        });

        run(best, w);

    }


    protected abstract Y[] newArray(int size);

    /** here a prefilter can be implemented by returning null */
    @Nullable abstract protected Y apply(TaskLink x);

    abstract public FloatRank<Y> score();

    abstract public int cap();

    abstract protected void run(RankedN<Y> best, What w);

}
