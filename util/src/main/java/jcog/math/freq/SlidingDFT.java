/*
 *  SlidingDFT.java
 *  Eisenkraut
 *
 *  Copyright (c) 2004-2017 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */
package jcog.math.freq;

/**
 * For the sliding DFT algorithm, see for example
 * Bradford/Dobson/ffitch "Sliding is smoother than jumping".
 */
public class SlidingDFT {
    private final int fftSize;
    private final int fftSizeP2;
    private final int bins;


    private final double[] cos;
    private final double[] sin;
    private final float[][] timeBuf;
    private final double[][] fftBufD;
    private final int[] timeBufIdx;

    public SlidingDFT(int fftSize, int numChannels) {
        this.fftSize = fftSize;

        final double d1;
        final int binsH;
        double d2;

        bins = fftSize >> 1;
        fftSizeP2 = fftSize + 2;
        fftBufD = new double[numChannels][fftSizeP2];
        d1 = Math.PI * 2 / fftSize;


        binsH = bins >> 1;


        cos = new double[bins + 1];
        sin = new double[bins + 1];
        timeBuf = new float[numChannels][fftSize];
        timeBufIdx = new int[numChannels];


        for (int i = 0, j = bins, k = binsH, m = binsH; i < binsH; i++, j--, k--, m++) {
            d2 = Math.cos(d1 * i);
            cos[i] = d2;
            cos[j] = -d2;
            sin[k] = d2;
            sin[m] = d2;
        }
    }

    public void next(float[] inBuf, int chan, float[] fftBuf) {
        next(inBuf, 0, fftSize, chan, fftBuf);
    }

    public void next(float[] inBuf, int inOff, int len, int chan, float[] fftBuf) {


        final double[] fftBufDC = fftBufD[chan];
        final float[] timeBufC = timeBuf[chan];
        int timeBufIdxC = timeBufIdx[chan];
        double delta, re1, im1, re2, im2;
        float f1;

        for (int i = 0, j = inOff; i < len; i++, j++) {
            f1 = inBuf[j];
            delta = (double) f1 - timeBufC[timeBufIdxC];

            timeBufC[timeBufIdxC] = f1;
            for (int k = 0, m = 0; m < fftSizeP2; k++) {


                if ((k & 1) == 0) {
                    re1 = fftBufDC[m] + delta;
                } else {
                    re1 = fftBufDC[m] - delta;
                }
                im1 = fftBufDC[m + 1];

                re2 = cos[k];
                im2 = sin[k];


                fftBufDC[m++] = re1 * re2 - im1 * im2;
                fftBufDC[m++] = re1 * im2 + re2 * im1;
            }
            if (++timeBufIdxC == fftSize) timeBufIdxC = 0;
        }
        timeBufIdx[chan] = timeBufIdxC;

        for (int i = 0; i < fftSizeP2; i++) {
            fftBuf[i] = (float) fftBufDC[i];
        }


    }
}