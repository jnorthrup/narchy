package spacegraph.audio.synth.string;

/**
 * Created by ericlgame on 23-Feb-16.
 */

public class SquareString extends KarplusStrongString {

    private double pluckDelta;
    private double releaseDelta;
    private double filterIn;
    private double filterOut;

    public SquareString (double frequency) {
        super(frequency, 1);
        setMaxVolume(0.2);
        pluckDelta = .9998;
        releaseDelta = .98;
        filterIn = 0;
        filterOut = 0;
    }

    public void pluck() {
        setDeltaVolume(pluckDelta);
        clear();
        var capacity = buffer.capacity();
        var half = capacity / 2;
        for (var i = 0; i < half; i++) {
            buffer.enqueue(getMaxVolume() * -1);
        }
        var otherHalf = capacity - half;
        for (var i = 0; i < otherHalf; i++) {
            buffer.enqueue(getMaxVolume());
        }
    }

    public void tic() {
        double first = buffer.dequeue();
        double second = buffer.peek();
        var x = (first + second) / 2; // lowpass filter
		filterOut = C * x + filterIn - C * filterOut; // allpass tuning filter
        filterIn = x;
        buffer.enqueue(filterOut * deltaVolume);
    }

    public void release() {
        setDeltaVolume(releaseDelta);
    }

    public void setPluckDelta(double newPluckDelta) {
        pluckDelta = newPluckDelta;
    }

    public void setReleaseDelta(double newReleaseDelta) {
        releaseDelta = newReleaseDelta;
    }

    public double pluckDelta() {
        return pluckDelta;
    }

    public double releaseDelta() {
        return releaseDelta;
    }
}
