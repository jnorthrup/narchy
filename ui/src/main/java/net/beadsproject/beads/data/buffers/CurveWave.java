/*
 * This file is part of Beads. See http:
 */
package net.beadsproject.beads.data.buffers;

import jcog.signal.tensor.ArrayTensor;
import net.beadsproject.beads.data.WaveFactory;

/**
 * Creates a {@link Buffer} of the function x^e^(-c) where x is in the range [0,1] and c is a curviness factor.
 */
public class CurveWave extends WaveFactory {

    /**
     * The curviness.
     */
    private final float curviness;

    /**
     * Instantiates a new curve buffer.
     *
     * @param curviness the curviness.
     */
    public CurveWave(float curviness) {
        this.curviness = Math.min(1.0F, Math.max(-1.0F, curviness));
    }

    /* (non-Javadoc)
     * @see net.beadsproject.beads.data.BufferFactory#generateBuffer(int)
     */
    @Override
    public ArrayTensor get(int bufferSize) {
        int size = bufferSize;
        ArrayTensor b = new ArrayTensor(size);
        double exponent = Math.exp((double) -curviness);
        for (int i = 0; i < bufferSize; i++) {
            b.data[i] = (float) Math.pow((double) (((float) i) / (float) bufferSize), exponent);
        }
        return b;
    }

    /* (non-Javadoc)
     * @see net.beadsproject.beads.data.BufferFactory#getName()
     */
    @Override
    public String getName() {
        return "Curve " + curviness;
    }

}
