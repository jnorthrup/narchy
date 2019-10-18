package nars.task.util.signal;

public abstract class RangeTruthlet extends Truthlet {

    public final long start;
    public final long end;

    public RangeTruthlet(long start, long end) {
        assert(start <= end);
        this.start = start;
        this.end = end;
    }

    @Override
    public abstract RangeTruthlet stretch(long newStart, long newEnd);

    @Override
    public final long start() {
        return start;
    }

    @Override
    public final long end() {
        return end;
    }

}
