/*
 * This file is part of Beads. See http:
 */
package net.beadsproject.beads.data.buffers;

import jcog.signal.tensor.ArrayTensor;
import net.beadsproject.beads.data.WaveFactory;

/**
 * A filter used for smoothing data.
 *
 * @author ben
 */
public class TriangularWindow extends WaveFactory {

    @Override
    public ArrayTensor get(int bufferSize) {
        var size = bufferSize;
        var b = new ArrayTensor(size);

        for (var i = 0; i < bufferSize; i++) {
            b.data[i] = tri((i + 0.5f) / bufferSize) / bufferSize;
        }
        return b;
    }

    private static float tri(float x) {
        return x < .5 ? 4 * x : 4 * (1 - x);
    }

    @Override
    public String getName() {
        return "TriangularBuffer";
    }

}
