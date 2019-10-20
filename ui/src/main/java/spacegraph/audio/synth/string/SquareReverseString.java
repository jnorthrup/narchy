package spacegraph.audio.synth.string;

/**
 * Created by ericlgame on 23-Feb-16.
 */
public class SquareReverseString extends KarplusStrongString {

    public SquareReverseString (double frequency) {
        super(frequency * 2, .996);
        setMaxVolume(0.2);
    }

    public void pluck() {
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
        var last = (first + second) * (deltaVolume / 2);
        buffer.enqueue(last * -1);
    }

    public void release() {

    }
}
