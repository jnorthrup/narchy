package spacegraph.audio.synth.string;

/**
 * Created by ericlgame on 24-Feb-16.
 */
public class SquareViolinString extends VariableDeltaString{

    public SquareViolinString (double frequency) {
        super(frequency);
        setDeltaVolume(1.2);
        setMaxVolume(0.2);
        setInitialVolume(0.01);
    }

    public void pluck() {
        clear();
        var capacity = buffer.capacity();
        var half = capacity / 2;
        for (var i = 0; i < half; i++) {
            buffer.enqueue(getInitialVolume() * -1);
        }
        var otherHalf = capacity - half;
        for (var i = 0; i < otherHalf; i++) {
            buffer.enqueue(getInitialVolume());
        }
        resetTics();
        setDeltaVolume(1.2);
    }

    public void tic() {
        double first = buffer.dequeue();
        double second = buffer.peek();
        var last = (first + second) * (deltaVolume / 2);
        last = checkMax(last);
        buffer.enqueue(last);
        oneTic();
        calcDelta();
    }

    public void calcDelta() {
        if (getTics() < 10000) {
            setDeltaVolume(1.2);
        } else if (getTics() < 50000) {
            setDeltaVolume(1.0);
        } else {
            setDeltaVolume(0.99);
        }
    }

    public void release() {

    }
}
