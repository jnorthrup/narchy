package nars.task.signal;

abstract public class RangeTruthlet extends Truthlet {

    public final long start, end;

    public RangeTruthlet(long start, long end) {
        assert(start <= end);
        this.start = start;
        this.end = end;
    }

    @Override
    abstract public RangeTruthlet stretch(long newStart, long newEnd);

    @Override
    public final long start() {
        return start;
    }

    @Override
    public final long end() {
        return end;
    }

}
