package nars.op;

import jcog.sort.FloatRank;
import jcog.sort.RankedN;
import nars.NAR;
import nars.link.TaskLink;
import nars.time.event.DurService;
import org.jetbrains.annotations.Nullable;

/** utility service for periodically ranking the
 * active tasklinks by some metric, and applying a procedure
 * based on the aggregate.
 */
public abstract class LinkRanker<Y> extends DurService {

    public LinkRanker() {
        super();
    }
    public LinkRanker(NAR n) {
        super(n);
    }

    @Override
    protected final void run(NAR n, long dt) {

        beforeRun(n, dt);

        RankedN<Y> best = new RankedN<>(newArray(cap()), score()); //TODO option for threadLocal

        n.attn.links.forEach(x -> {
            Y y = apply(x);
            if (y!=null)
                best.add(y);
        });

        run(best, n, dt);

    }

    protected void beforeRun(NAR n, long dt) {

    }

    protected abstract Y[] newArray(int size);

    /** here a prefilter can be implemented by returning null */
    @Nullable abstract protected Y apply(TaskLink x);

    abstract public FloatRank<Y> score();

    abstract public int cap();

    abstract protected void run(RankedN<Y> best, NAR n, long dt);

}
