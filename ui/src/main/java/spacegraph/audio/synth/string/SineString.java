package spacegraph.audio.synth.string;

/**
 * Created by ericlgame on 24-Feb-16.
 */
public class SineString extends KarplusStrongString {

    /* tuned */
    public SineString (double frequency) {
        super(frequency, 1, .996);
        setMaxVolume(0.5);
    }

    public void pluck() {
        clear();
        var capacity = buffer.capacity();
        for (var i = 0; i < capacity; i++) {
            buffer.enqueue((Math.sin(i * 2 * Math.PI / capacity)) * getMaxVolume());
        }
    }

    public void tic() {
        double first = buffer.dequeue();
        double second = buffer.peek();
        var last = (first + second) * (deltaVolume / 2);
		buffer.enqueue(last * deltaVolume);
    }

    public void release() {

    }
}
