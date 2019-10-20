package spacegraph.audio.synth.string;

/**
 * Created by ericlgame on 23-Feb-16.
 */
public class SawtoothString extends KarplusStrongString{

    public SawtoothString (double frequency) {
        super(frequency, .996);
        setMaxVolume(0.5);
    }

    public void pluck() {
        clear();
        int capacity = buffer.capacity();
        for (int i = 0; i < capacity; i++) {
            double sample = (-0.5 + (double) (i / (capacity - 1)) * 2.0 * getMaxVolume());
            buffer.enqueue(sample);
        }
    }

    public void tic() {
        double first = buffer.dequeue();
        double second = buffer.peek();
        double last = (first + second) * (deltaVolume / 2.0);
        buffer.enqueue(last);
    }

    public void release() {

    }
}
