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
        int size = bufferSize;
        ArrayTensor b = new ArrayTensor(size);

        for (int i = 0; i < bufferSize; i++) {
            b.data[i] = tri(((float) i + 0.5f) / (float) bufferSize) / (float) bufferSize;
        }
        return b;
    }

    private static float tri(float x) {
        return (double) x < .5 ? 4.0F * x : 4.0F * (1.0F - x);
    }

    @Override
    public String getName() {
        return "TriangularBuffer";
    }

}
