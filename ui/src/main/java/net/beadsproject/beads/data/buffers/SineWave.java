/*
 * This file is part of Beads. See http:
 */
package net.beadsproject.beads.data.buffers;

import jcog.signal.tensor.ArrayTensor;
import net.beadsproject.beads.data.WaveFactory;

/**
 * Creates a {@link Buffer} consisting of a sine wave in the range [-1,1].
 *
 * @author ollie
 * @see Buffer BufferFactory
 */
public class SineWave extends WaveFactory {


    /* (non-Javadoc)
     * @see net.beadsproject.beads.data.BufferFactory#generateBuffer(int)
     */
    @Override
    public ArrayTensor get(int bufferSize) {
        int size = bufferSize;
        ArrayTensor b = new ArrayTensor(size);
        float[] bd = b.data;
        for (int i = 0; i < bufferSize; i++) {
            bd[i] = (float) Math.sin(2.0 * Math.PI * (double) i / (double) bufferSize);
        }
        return b;
    }

    /* (non-Javadoc)
     * @see net.beadsproject.beads.data.BufferFactory#getName()
     */
    @Override
    public String getName() {
        return "Sine";
    }
}
