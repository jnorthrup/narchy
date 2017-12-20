package nars.task.signal;

public class ProxyTruthlet extends Truthlet {

    public Truthlet defined;

    public ProxyTruthlet(Truthlet defined) {
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
    public void truth(long when, float[] freqEvi) {
        defined.truth(when, freqEvi);
    }
}
