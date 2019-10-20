package spacegraph.audio.synth.string;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ericlgame on 24-Feb-16.
 */
public class SquareHarmonicsString extends KarplusStrongString {

    Map<Integer, Double> harmonics = new HashMap<>();

    public SquareHarmonicsString (double frequency) {
        super(frequency, .996);
        harmonics.put(1, 0.2);
        harmonics.put(3, 0.1);
        harmonics.put(5, 0.1);
    }

    public void pluck() {
        clear();
        var capacity = buffer.capacity();
        for (var i = 0; i < capacity; i++) {
            buffer.enqueue(getSample(i));
        }
    }

    public void tic() {
        double first = buffer.dequeue();
        double second = buffer.peek();
        var last = (first + second) * (deltaVolume / 2);
        buffer.enqueue(last);
    }

    private double getSample(int index) {
        double position = index / buffer.capacity();
        double sample = 0;
        for (var integerDoubleEntry : harmonics.entrySet()) {
            var lowHigh = lowHigh(integerDoubleEntry.getKey(), index);
            double factor = integerDoubleEntry.getValue();
            if (lowHigh == 0) {
                sample -= factor;
            } else {
                sample += factor;
            }
        }
        return sample;
    }

    private int lowHigh(int harmonic, int index) {
        var position = (double) index / buffer.capacity();
        var relPos = position * 2 * harmonic;
        var floored = (int) Math.round(Math.floor(relPos));
        var modded = floored % 2;
        return modded;
    }

    public void release() {

    }
}
