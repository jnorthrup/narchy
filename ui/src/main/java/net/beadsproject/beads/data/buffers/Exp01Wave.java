/*
 * This file is part of Beads. See http:
 */
package net.beadsproject.beads.data.buffers;

import jcog.signal.tensor.ArrayTensor;
import net.beadsproject.beads.data.WaveFactory;

/**
 * Creates a {@link Buffer} of the function exp(1 - 1 / x) over [0,1].
 */
public class Exp01Wave extends WaveFactory {

    /* (non-Javadoc)
     * @see net.beadsproject.beads.data.BufferFactory#generateBuffer(int)
     */
    @Override
    public ArrayTensor get(int bufferSize) {
        var size = bufferSize;
        var b = new ArrayTensor(size);
        for (var i = 0; i < bufferSize; i++) {
            var fract = (float) i / (bufferSize - 1);
            b.data[i] = (float) Math.exp(1f - 1f / fract);
        }
        return b;
    }

    /* (non-Javadoc)
     * @see net.beadsproject.beads.data.BufferFactory#getName()
     */
    @Override
    public String getName() {
        return "Exp01";
    }

}