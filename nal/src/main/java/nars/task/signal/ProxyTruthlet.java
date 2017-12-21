package nars.task.signal;

public class ProxyTruthlet<T extends Truthlet> extends Truthlet {

    public T defined;

    public ProxyTruthlet(T defined) {
        this.defined = defined;
    }

    @Override
    public long start() {
        return defined.start();
    }

    @Override
    public long end() {
        return defined.end();
    }

    @Override
    public void setTime(long newStart, long newEnd) {
       defined.setTime(newStart, newEnd);
    }

    @Override
    public void truth(long when, float[] freqEvi) {
        defined.truth(when, freqEvi);
    }
}
