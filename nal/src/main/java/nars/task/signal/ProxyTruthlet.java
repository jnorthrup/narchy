package nars.task.signal;

public class ProxyTruthlet<T extends Truthlet> extends Truthlet {

    public T ref;

    public ProxyTruthlet(T ref) {
        this.ref = ref;
    }

    @Override
    public final long start() {
        return ref.start();
    }

    @Override
    public final long end() {
        return ref.end();
    }

    @Override
    public final boolean intersects(long start, long end) {
        return ref.intersects(start, end);
    }

    @Override
    public Truthlet stretch(long newStart, long newEnd) {
       return ref.stretch(newStart, newEnd);
    }

    @Override
    public void truth(long when, float[] freqEvi) {
        ref.truth(when, freqEvi);
    }
}
