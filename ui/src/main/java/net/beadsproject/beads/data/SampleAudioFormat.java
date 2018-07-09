/*
 * This file is part of Beads. See http:
 */
package net.beadsproject.beads.data;

/**
 * Encapsulates data about audio format for Samples.
 * <p>
 * We have elected to use our own AudioFormat instead of
 * javax.sound.sampled.AudioFormat as javasound is not supported everywhere.
 *
 * @author ben
 */
public class SampleAudioFormat {

    private final int channels;
    public final int bitDepth;
    public final float sampleRate;
    private final boolean bigEndian;
    private final boolean signed;

    private SampleAudioFormat(float sampleRate, int bitDepth, int channels, boolean signed, boolean bigEndian) {
        this.sampleRate = sampleRate;
        this.bitDepth = bitDepth;
        this.signed = signed;
        this.bigEndian = bigEndian;
        this.channels = channels;
    }

    public SampleAudioFormat(float sampleRate, int bitDepth, int channels) {
        this(sampleRate, bitDepth, channels, true, true);
    }

}
