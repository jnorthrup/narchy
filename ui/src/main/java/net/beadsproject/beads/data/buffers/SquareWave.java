/*
 * This file is part of Beads. See http:
 */
package net.beadsproject.beads.data.buffers;

import jcog.signal.tensor.ArrayTensor;
import net.beadsproject.beads.data.WaveFactory;

/**
 * Creates a {@link Buffer} consisting of a square wave in the range [-1,1].
 *
 * @author ollie
 * @see Buffer BufferFactory
 */
public class SquareWave extends WaveFactory {

    /* (non-Javadoc)
     * @see net.beadsproject.beads.data.BufferFactory#generateBuffer(int)
     */
    @Override
    public ArrayTensor get(int bufferSize) {
        var size = bufferSize;
        var b = new ArrayTensor(size);
        var halfBufferSize = bufferSize / 2;
        for (var i = 0; i < halfBufferSize; i++) {
            b.data[i] = 1f;
        }
        for (var i = halfBufferSize; i < bufferSize; i++) {
            b.data[i] = -1f;
        }
        return b;
    }

    /* (non-Javadoc)
     * @see net.beadsproject.beads.data.BufferFactory#getName()
     */
    @Override
    public String getName() {
        return "Square";
    }


}