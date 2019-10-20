/*
 * This file is part of Beads. See http:
 */
package net.beadsproject.beads.ugens;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.UGen;

/**
 * Takes an incoming signal (or signals in the multi-channel case) and adds
 * something (either a float value or another signal) to it (them).
 *
 * @author ollie
 * @author Benito Crawford
 * @version 0.9.5
 * @beads.category utilities
 */
public class Add extends UGen {

    private UGen adderUGen;
    private float adder;

    /**
     * Constructor for an Add object that sets a UGen to control the value to
     * addAt.
     *
     * @param context   The audio context.
     * @param channels  The number of channels.
     * @param adderUGen The adder UGen controller.
     */
    public Add(AudioContext context, int channels, UGen adderUGen) {
        super(context, channels, channels);
        setAdder(adderUGen);
    }

    /**
     * Constructor for an Add object with a given UGen as input and another as adder.
     * i.e., use this as quickest way to add two UGens together.
     *
     * @param context   the AudioContext.
     * @param input     the input UGen.
     * @param adderUGen the adder UGen.
     */
    public Add(AudioContext context, UGen input, UGen adderUGen) {
        super(context, input.getOuts(), input.getOuts());
        setAdder(adderUGen);
        in(input);
    }

    /**
     * Constructor for an Add object that sets a static adder value.
     *
     * @param context  The audio context.
     * @param channels The number of channels.
     * @param adder    The value to addAt.
     */
    public Add(AudioContext context, int channels, float adder) {
        super(context, channels, channels);
        setAdder(adder);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.olliebown.beads.core.UGen#calculateBuffer()
     */
    @Override
    public void gen() {
        if (adderUGen == null) {
            for (var j = 0; j < outs; j++) {
                var bi = bufIn[j];
                var bo = bufOut[j];
                for (var i = 0; i < bufferSize; i++) {
                    bo[i] = bi[i] + adder;
                }
            }
        } else {
            adderUGen.update();
            if (outs == 1) {
                var bi = bufIn[0];
                var bo = bufOut[0];
                for (var i = 0; i < bufferSize; i++) {
                    adder = adderUGen.getValue(0, i);
                    bo[i] = bi[i] + adder;

                }
            } else {
                for (var i = 0; i < bufferSize; i++) {
                    for (var j = 0; j < outs; j++) {
                        adder = adderUGen.getValue(0, i);
                        bufOut[j][i] = bufIn[j][i] + adder;
                    }
                }
            }
        }
    }

    /**
     * Gets the current adder value.
     *
     * @return The adder value.
     */
    public float getAdder() {
        return adder;
    }

    /**
     * Sets the adder to a static float value.
     *
     * @param adder The new adder value.
     * @return This Add instance.
     */
    private Add setAdder(float adder) {
        this.adder = adder;
        adderUGen = null;
        return this;
    }

    /**
     * Sets a UGen to control the adder value.
     *
     * @param adderUGen The adder UGen controller.
     * @return This Add instance.
     */
    private Add setAdder(UGen adderUGen) {
        if (adderUGen == null) {
            setAdder(adder);
        } else {
            this.adderUGen = adderUGen;
            adderUGen.update();
            adder = adderUGen.getValue();
        }
        return this;
    }

    /**
     * Gets the adder UGen controller.
     *
     * @return The adder UGen controller.
     */
    public UGen getAdderUGen() {
        return adderUGen;
    }

}
