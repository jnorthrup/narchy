/*
 * This file is part of Beads. See http:
 */
package net.beadsproject.beads.ugens;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.UGen;

/**
 * RangeLimiter forces a signal within the range [-1,1]. Use {@link Clip} for
 * constraining to other ranges.
 *
 * @author ollie
 * @author benito
 * @version 0.9.5
 * @beads.category utilities
 */
public class RangeLimiter extends UGen {

    /**
     * Instantiates a new RangeLimiter.
     *
     * @param context  The audio context.
     * @param channels The number of channels.
     */
    public RangeLimiter(AudioContext context, int channels) {
        super(context, channels, channels);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.olliebown.beads.core.UGen#calculateBuffer()
     */
    @Override
    public void gen() {

        for (var j = 0; j < ins; j++) {
            var bi = bufIn[j];
            var bo = bufOut[j];
            for (var i = 0; i < bufferSize; i++) {
                float y;
                if ((y = bi[i]) > 1.0f) {
                    bo[i] = 1f;
                } else bo[i] = Math.max(y, -1f);
            }
        }
    }

}
