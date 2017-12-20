package nars.task.signal;

import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.XTERNAL;

abstract public class RangeTruthlet extends Truthlet {

    public long start, end;

    public RangeTruthlet(long start, long end) {
        this.start = start;
        this.end = end;
        assert(start <= end);
    }

    @Override
    public long start() {
        return start;
    }

    @Override
    public long end() {
        return end;
    }

}
