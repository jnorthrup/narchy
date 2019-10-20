package spacegraph.audio.synth.string;

/**
 * Created by ericlgame on 14-Mar-16.
 */

public class SquareWave extends KarplusStrongString {

    private final double pluckDelta;
    private final double releaseDelta;
    private double filterIn;
    private double filterOut;

    public SquareWave(double frequency) {
        super(frequency, 0);
        setMaxVolume(0.4);
        setInitialVolume(0.2);
        pluckDelta = .9998;
        releaseDelta = .9;
        filterIn = 0;
        filterOut = 0;
    }

    public void pluck() {
        setDeltaVolume(pluckDelta);
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
    }

    public void tic() {
        double first = buffer.dequeue();
        var x = first * deltaVolume;
		filterOut = C * x + filterIn - C * filterOut; // allpass tuning filter
        filterIn = x;
        buffer.enqueue(checkMax(filterOut * deltaVolume));
    }

    public void release() {
        setDeltaVolume(releaseDelta);
    }
}