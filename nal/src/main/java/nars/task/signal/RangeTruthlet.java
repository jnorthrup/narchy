package nars.task.signal;

abstract public class RangeTruthlet extends Truthlet {

    public long start, end;

    public RangeTruthlet(long start, long end) {
        this.start = start;
        this.end = end;
        assert(start <= end);
    }

    @Override
    public void setTime(long newStart, long newEnd) {
        this.start = newStart;
        this.end = newEnd;
    }

    @Override
    public final long start() {
        return start;
    }

    @Override
    public final long end() {
        return end;
    }

}
