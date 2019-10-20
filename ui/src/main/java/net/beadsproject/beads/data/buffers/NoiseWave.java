/*
 * This file is part of Beads. See http:
 */
package net.beadsproject.beads.data.buffers;

import jcog.signal.tensor.ArrayTensor;
import net.beadsproject.beads.data.WaveFactory;

/**
 * Creates a {@link Buffer} of random floats.
 *
 * @author ben
 */
public class NoiseWave extends WaveFactory {
    @Override
    public ArrayTensor get(int bufferSize) {
        var size = bufferSize;
        var b = new ArrayTensor(size);
        for (var i = 0; i < bufferSize; i++) {
            b.data[i] = (float) (1. - 2. * Math.random());
        }
        return b;
    }

    @Override
    public String getName() {
        return "Noise";
    }

}