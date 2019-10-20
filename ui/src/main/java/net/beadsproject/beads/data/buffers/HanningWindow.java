/*
 * This file is part of Beads. See http:
 */
package net.beadsproject.beads.data.buffers;

import jcog.signal.tensor.ArrayTensor;
import net.beadsproject.beads.data.WaveFactory;

/**
 * Creates a {@link Buffer} of a Hanning window.
 *
 * @author ollie
 * @see Buffer BufferFactory
 */
public class HanningWindow extends WaveFactory {

    /* (non-Javadoc)
     * @see net.beadsproject.beads.data.BufferFactory#generateBuffer(int)
     */
    @Override
    public ArrayTensor get(int bufferSize) {
        var size = bufferSize;
        var b = new ArrayTensor(size);
        var lowerThresh = bufferSize / 4;
        var upperThresh = bufferSize - lowerThresh;
        for (var i = 0; i < bufferSize; i++) {
            b.data[i] = i < lowerThresh || i > upperThresh ? 0.5f * (1.0f + (float) Math.cos((Math.PI + Math.PI * 4.0f * i / (float) (bufferSize - 1)))) : 1.0f;
        }
        return b;
    }

    /* (non-Javadoc)
     * @see net.beadsproject.beads.data.BufferFactory#getName()
     */
    @Override
    public String getName() {
        return "Hanning";
    }


}
