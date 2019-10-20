/*
 *      _______                       _____   _____ _____
 *     |__   __|                     |  __ \ / ____|  __ \
 *        | | __ _ _ __ ___  ___  ___| |  | | (___ | |__) |
 *        | |/ _` | '__/ __|/ _ \/ __| |  | |\___ \|  ___/
 *        | | (_| | |  \__ \ (_) \__ \ |__| |____) | |
 *        |_|\__,_|_|  |___/\___/|___/_____/|_____/|_|
 *
 * -------------------------------------------------------------
 *
 * TarsosDSP is developed by Joren Six at IPEM, University Ghent
 *
 * -------------------------------------------------------------
 *
 *  Info: http://0110.be/tag/TarsosDSP
 *  Github: https://github.com/JorenSix/TarsosDSP
 *  Releases: http://0110.be/releases/TarsosDSP/
 *
 *  TarsosDSP includes modified source code by various authors,
 *  for credits and info, see README.
 *
 */


/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is JTransforms.
 *
 * The Initial Developer of the Original Code is
 * Piotr Wendykier, Emory University.
 * Portions created by the Initial Developer are Copyright (C) 2007-2009
 * the Initial Developer. All Rights Reserved.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package spacegraph.audio.modem.chirp.transceive.util;

import java.util.concurrent.Future;


/**
 * Computes 1D Discrete Fourier Transform (DFT) of complex and real, single
 * precision data. The size of the data can be an arbitrary number. This is a
 * parallel implementation of split-radix and mixed-radix algorithms optimized
 * for SMP systems. <br>
 * <br>
 * This code is derived from General Purpose FFT Package written by Takuya Ooura
 * (http://www.kurims.kyoto-u.ac.jp/~ooura/fft.html) and from JFFTPack written
 * by Baoshe Zhang (http://jfftpack.sourceforge.net/)
 *
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 */
public strictfp class FloatFFT {

    private static final int[] factors = {4, 2, 3, 5};
    private static final float PI = 3.14159265358979311599796346854418516f;
    private static final float TWO_PI = 6.28318530717958623199592693708837032f;
    private final int n;
    private final Plans plan;
    private int nBluestein;
    private int[] ip;
    private float[] w;
    private int nw;
    private int nc;
    private float[] wtable;
    private float[] wtable_r;
    private float[] bk1;
    private float[] bk2;

    /**
     * Creates new instance of FloatFFT.
     *
     * @param n size of data
     */
    public FloatFFT(int n) {
        if (n < 1) {
            throw new IllegalArgumentException("n must be greater than 0");
        }
        this.n = n;

        if (!ConcurrencyUtils.isPowerOf2(n)) {
            if (getReminder(n, factors) >= 211) {
                plan = Plans.BLUESTEIN;
                nBluestein = ConcurrencyUtils.nextPow2(n * 2 - 1);
                bk1 = new float[2 * nBluestein];
                bk2 = new float[2 * nBluestein];
                this.ip = new int[2 + (int) Math.ceil(2 + (1 << (int) (Math.log(nBluestein + 0.5) / Math.log(2)) / 2))];
                this.w = new float[nBluestein];
                var twon = 2 * nBluestein;
                nw = ip[0];
                if (twon > (nw << 2)) {
                    nw = twon >> 2;
                    makewt(nw);
                }
                nc = ip[1];
                if (nBluestein > (nc << 2)) {
                    nc = nBluestein >> 2;
                    makect(nc, w, nw);
                }
                bluesteini();
            } else {
                plan = Plans.MIXED_RADIX;
                wtable = new float[4 * n + 15];
                wtable_r = new float[2 * n + 15];
                cffti();
                rffti();
            }
        } else {
            plan = Plans.SPLIT_RADIX;
            this.ip = new int[2 + (int) Math.ceil(2 + (1 << (int) (Math.log(n + 0.5) / Math.log(2)) / 2))];
            this.w = new float[n];
            nw = ip[0];
            var twon = 2 * n;
            if (twon > (nw << 2)) {
                nw = twon >> 2;
                makewt(nw);
            }
            nc = ip[1];
            if (n > (nc << 2)) {
                nc = n >> 2;
                makect(nc, w, nw);
            }
        }
    }

    private static int getReminder(int n, int[] factors) {

        if (n <= 0) {
            throw new IllegalArgumentException("n must be positive integer");
        }

        var reminder = n;
        for (var i = 0; i < factors.length && reminder != 1; i++) {
            var factor = factors[i];
            while ((reminder % factor) == 0) {
                reminder /= factor;
            }
        }
        return reminder;
    }

    private static void bitrv2(int n, int[] ip, float[] a, int offa) {
        int l;

        var m = 1;
        for (l = n >> 2; l > 8; l >>= 2) {
            m <<= 1;
        }
        var nh = n >> 1;
        var nm = 4 * m;
        int idx2;
        int idx1;
        int idx0;
        float yi;
        float yr;
        float xi;
        float xr;
        int k1;
        int j1;
        if (l == 8) {
            for (var k = 0; k < m; k++) {
                idx0 = 4 * k;
                for (var j = 0; j < k; j++) {
                    j1 = 4 * j + 2 * ip[m + k];
                    k1 = idx0 + 2 * ip[m + j];
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += nm;
                    k1 += 2 * nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += nm;
                    k1 -= nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += nm;
                    k1 += 2 * nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += nh;
                    k1 += 2;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 -= nm;
                    k1 -= 2 * nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 -= nm;
                    k1 += nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 -= nm;
                    k1 -= 2 * nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += 2;
                    k1 += nh;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += nm;
                    k1 += 2 * nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += nm;
                    k1 -= nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += nm;
                    k1 += 2 * nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 -= nh;
                    k1 -= 2;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 -= nm;
                    k1 -= 2 * nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 -= nm;
                    k1 += nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 -= nm;
                    k1 -= 2 * nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                }
                k1 = idx0 + 2 * ip[m + k];
                j1 = k1 + 2;
                k1 += nh;
                idx1 = offa + j1;
                idx2 = offa + k1;
                xr = a[idx1];
                xi = a[idx1 + 1];
                yr = a[idx2];
                yi = a[idx2 + 1];
                a[idx1] = yr;
                a[idx1 + 1] = yi;
                a[idx2] = xr;
                a[idx2 + 1] = xi;
                j1 += nm;
                k1 += 2 * nm;
                idx1 = offa + j1;
                idx2 = offa + k1;
                xr = a[idx1];
                xi = a[idx1 + 1];
                yr = a[idx2];
                yi = a[idx2 + 1];
                a[idx1] = yr;
                a[idx1 + 1] = yi;
                a[idx2] = xr;
                a[idx2 + 1] = xi;
                j1 += nm;
                k1 -= nm;
                idx1 = offa + j1;
                idx2 = offa + k1;
                xr = a[idx1];
                xi = a[idx1 + 1];
                yr = a[idx2];
                yi = a[idx2 + 1];
                a[idx1] = yr;
                a[idx1 + 1] = yi;
                a[idx2] = xr;
                a[idx2 + 1] = xi;
                j1 -= 2;
                k1 -= nh;
                idx1 = offa + j1;
                idx2 = offa + k1;
                xr = a[idx1];
                xi = a[idx1 + 1];
                yr = a[idx2];
                yi = a[idx2 + 1];
                a[idx1] = yr;
                a[idx1 + 1] = yi;
                a[idx2] = xr;
                a[idx2 + 1] = xi;
                j1 += nh + 2;
                k1 += nh + 2;
                idx1 = offa + j1;
                idx2 = offa + k1;
                xr = a[idx1];
                xi = a[idx1 + 1];
                yr = a[idx2];
                yi = a[idx2 + 1];
                a[idx1] = yr;
                a[idx1 + 1] = yi;
                a[idx2] = xr;
                a[idx2 + 1] = xi;
                j1 -= nh - nm;
                k1 += 2 * nm - 2;
                idx1 = offa + j1;
                idx2 = offa + k1;
                xr = a[idx1];
                xi = a[idx1 + 1];
                yr = a[idx2];
                yi = a[idx2 + 1];
                a[idx1] = yr;
                a[idx1 + 1] = yi;
                a[idx2] = xr;
                a[idx2 + 1] = xi;
            }
        } else {
            for (var k = 0; k < m; k++) {
                idx0 = 4 * k;
                for (var j = 0; j < k; j++) {
                    j1 = 4 * j + ip[m + k];
                    k1 = idx0 + ip[m + j];
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += nm;
                    k1 += nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += nh;
                    k1 += 2;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 -= nm;
                    k1 -= nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += 2;
                    k1 += nh;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += nm;
                    k1 += nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 -= nh;
                    k1 -= 2;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 -= nm;
                    k1 -= nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = a[idx1 + 1];
                    yr = a[idx2];
                    yi = a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                }
                k1 = idx0 + ip[m + k];
                j1 = k1 + 2;
                k1 += nh;
                idx1 = offa + j1;
                idx2 = offa + k1;
                xr = a[idx1];
                xi = a[idx1 + 1];
                yr = a[idx2];
                yi = a[idx2 + 1];
                a[idx1] = yr;
                a[idx1 + 1] = yi;
                a[idx2] = xr;
                a[idx2 + 1] = xi;
                j1 += nm;
                k1 += nm;
                idx1 = offa + j1;
                idx2 = offa + k1;
                xr = a[idx1];
                xi = a[idx1 + 1];
                yr = a[idx2];
                yi = a[idx2 + 1];
                a[idx1] = yr;
                a[idx1 + 1] = yi;
                a[idx2] = xr;
                a[idx2 + 1] = xi;
            }
        }
    }

    private static void bitrv2conj(int n, int[] ip, float[] a, int offa) {
        int l;

        var m = 1;
        for (l = n >> 2; l > 8; l >>= 2) {
            m <<= 1;
        }
        var nh = n >> 1;
        var nm = 4 * m;
        int idx2;
        int idx1;
        int idx0;
        float yi;
        float yr;
        float xi;
        float xr;
        int k1;
        int j1;
        if (l == 8) {
            for (var k = 0; k < m; k++) {
                idx0 = 4 * k;
                for (var j = 0; j < k; j++) {
                    j1 = 4 * j + 2 * ip[m + k];
                    k1 = idx0 + 2 * ip[m + j];
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += nm;
                    k1 += 2 * nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += nm;
                    k1 -= nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += nm;
                    k1 += 2 * nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += nh;
                    k1 += 2;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 -= nm;
                    k1 -= 2 * nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 -= nm;
                    k1 += nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 -= nm;
                    k1 -= 2 * nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += 2;
                    k1 += nh;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += nm;
                    k1 += 2 * nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += nm;
                    k1 -= nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += nm;
                    k1 += 2 * nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 -= nh;
                    k1 -= 2;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 -= nm;
                    k1 -= 2 * nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 -= nm;
                    k1 += nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 -= nm;
                    k1 -= 2 * nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                }
                k1 = idx0 + 2 * ip[m + k];
                j1 = k1 + 2;
                k1 += nh;
                idx1 = offa + j1;
                idx2 = offa + k1;
                a[idx1 - 1] = -a[idx1 - 1];
                xr = a[idx1];
                xi = -a[idx1 + 1];
                yr = a[idx2];
                yi = -a[idx2 + 1];
                a[idx1] = yr;
                a[idx1 + 1] = yi;
                a[idx2] = xr;
                a[idx2 + 1] = xi;
                a[idx2 + 3] = -a[idx2 + 3];
                j1 += nm;
                k1 += 2 * nm;
                idx1 = offa + j1;
                idx2 = offa + k1;
                xr = a[idx1];
                xi = -a[idx1 + 1];
                yr = a[idx2];
                yi = -a[idx2 + 1];
                a[idx1] = yr;
                a[idx1 + 1] = yi;
                a[idx2] = xr;
                a[idx2 + 1] = xi;
                j1 += nm;
                k1 -= nm;
                idx1 = offa + j1;
                idx2 = offa + k1;
                xr = a[idx1];
                xi = -a[idx1 + 1];
                yr = a[idx2];
                yi = -a[idx2 + 1];
                a[idx1] = yr;
                a[idx1 + 1] = yi;
                a[idx2] = xr;
                a[idx2 + 1] = xi;
                j1 -= 2;
                k1 -= nh;
                idx1 = offa + j1;
                idx2 = offa + k1;
                xr = a[idx1];
                xi = -a[idx1 + 1];
                yr = a[idx2];
                yi = -a[idx2 + 1];
                a[idx1] = yr;
                a[idx1 + 1] = yi;
                a[idx2] = xr;
                a[idx2 + 1] = xi;
                j1 += nh + 2;
                k1 += nh + 2;
                idx1 = offa + j1;
                idx2 = offa + k1;
                xr = a[idx1];
                xi = -a[idx1 + 1];
                yr = a[idx2];
                yi = -a[idx2 + 1];
                a[idx1] = yr;
                a[idx1 + 1] = yi;
                a[idx2] = xr;
                a[idx2 + 1] = xi;
                j1 -= nh - nm;
                k1 += 2 * nm - 2;
                idx1 = offa + j1;
                idx2 = offa + k1;
                a[idx1 - 1] = -a[idx1 - 1];
                xr = a[idx1];
                xi = -a[idx1 + 1];
                yr = a[idx2];
                yi = -a[idx2 + 1];
                a[idx1] = yr;
                a[idx1 + 1] = yi;
                a[idx2] = xr;
                a[idx2 + 1] = xi;
                a[idx2 + 3] = -a[idx2 + 3];
            }
        } else {
            for (var k = 0; k < m; k++) {
                idx0 = 4 * k;
                for (var j = 0; j < k; j++) {
                    j1 = 4 * j + ip[m + k];
                    k1 = idx0 + ip[m + j];
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += nm;
                    k1 += nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += nh;
                    k1 += 2;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 -= nm;
                    k1 -= nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += 2;
                    k1 += nh;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 += nm;
                    k1 += nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 -= nh;
                    k1 -= 2;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                    j1 -= nm;
                    k1 -= nm;
                    idx1 = offa + j1;
                    idx2 = offa + k1;
                    xr = a[idx1];
                    xi = -a[idx1 + 1];
                    yr = a[idx2];
                    yi = -a[idx2 + 1];
                    a[idx1] = yr;
                    a[idx1 + 1] = yi;
                    a[idx2] = xr;
                    a[idx2 + 1] = xi;
                }
                k1 = idx0 + ip[m + k];
                j1 = k1 + 2;
                k1 += nh;
                idx1 = offa + j1;
                idx2 = offa + k1;
                a[idx1 - 1] = -a[idx1 - 1];
                xr = a[idx1];
                xi = -a[idx1 + 1];
                yr = a[idx2];
                yi = -a[idx2 + 1];
                a[idx1] = yr;
                a[idx1 + 1] = yi;
                a[idx2] = xr;
                a[idx2 + 1] = xi;
                a[idx2 + 3] = -a[idx2 + 3];
                j1 += nm;
                k1 += nm;
                idx1 = offa + j1;
                idx2 = offa + k1;
                a[idx1 - 1] = -a[idx1 - 1];
                xr = a[idx1];
                xi = -a[idx1 + 1];
                yr = a[idx2];
                yi = -a[idx2 + 1];
                a[idx1] = yr;
                a[idx1 + 1] = yi;
                a[idx2] = xr;
                a[idx2 + 1] = xi;
                a[idx2 + 3] = -a[idx2 + 3];
            }
        }
    }

    private static void bitrv216(float[] a, int offa) {

        var x1r = a[offa + 2];
        var x1i = a[offa + 3];
        var x2r = a[offa + 4];
        var x2i = a[offa + 5];
        var x3r = a[offa + 6];
        var x3i = a[offa + 7];
        var x4r = a[offa + 8];
        var x4i = a[offa + 9];
        var x5r = a[offa + 10];
        var x5i = a[offa + 11];
        var x7r = a[offa + 14];
        var x7i = a[offa + 15];
        var x8r = a[offa + 16];
        var x8i = a[offa + 17];
        var x10r = a[offa + 20];
        var x10i = a[offa + 21];
        var x11r = a[offa + 22];
        var x11i = a[offa + 23];
        var x12r = a[offa + 24];
        var x12i = a[offa + 25];
        var x13r = a[offa + 26];
        var x13i = a[offa + 27];
        var x14r = a[offa + 28];
        var x14i = a[offa + 29];
        a[offa + 2] = x8r;
        a[offa + 3] = x8i;
        a[offa + 4] = x4r;
        a[offa + 5] = x4i;
        a[offa + 6] = x12r;
        a[offa + 7] = x12i;
        a[offa + 8] = x2r;
        a[offa + 9] = x2i;
        a[offa + 10] = x10r;
        a[offa + 11] = x10i;
        a[offa + 14] = x14r;
        a[offa + 15] = x14i;
        a[offa + 16] = x1r;
        a[offa + 17] = x1i;
        a[offa + 20] = x5r;
        a[offa + 21] = x5i;
        a[offa + 22] = x13r;
        a[offa + 23] = x13i;
        a[offa + 24] = x3r;
        a[offa + 25] = x3i;
        a[offa + 26] = x11r;
        a[offa + 27] = x11i;
        a[offa + 28] = x7r;
        a[offa + 29] = x7i;
    }

    private static void bitrv216neg(float[] a, int offa) {

        var x1r = a[offa + 2];
        var x1i = a[offa + 3];
        var x2r = a[offa + 4];
        var x2i = a[offa + 5];
        var x3r = a[offa + 6];
        var x3i = a[offa + 7];
        var x4r = a[offa + 8];
        var x4i = a[offa + 9];
        var x5r = a[offa + 10];
        var x5i = a[offa + 11];
        var x6r = a[offa + 12];
        var x6i = a[offa + 13];
        var x7r = a[offa + 14];
        var x7i = a[offa + 15];
        var x8r = a[offa + 16];
        var x8i = a[offa + 17];
        var x9r = a[offa + 18];
        var x9i = a[offa + 19];
        var x10r = a[offa + 20];
        var x10i = a[offa + 21];
        var x11r = a[offa + 22];
        var x11i = a[offa + 23];
        var x12r = a[offa + 24];
        var x12i = a[offa + 25];
        var x13r = a[offa + 26];
        var x13i = a[offa + 27];
        var x14r = a[offa + 28];
        var x14i = a[offa + 29];
        var x15r = a[offa + 30];
        var x15i = a[offa + 31];
        a[offa + 2] = x15r;
        a[offa + 3] = x15i;
        a[offa + 4] = x7r;
        a[offa + 5] = x7i;
        a[offa + 6] = x11r;
        a[offa + 7] = x11i;
        a[offa + 8] = x3r;
        a[offa + 9] = x3i;
        a[offa + 10] = x13r;
        a[offa + 11] = x13i;
        a[offa + 12] = x5r;
        a[offa + 13] = x5i;
        a[offa + 14] = x9r;
        a[offa + 15] = x9i;
        a[offa + 16] = x1r;
        a[offa + 17] = x1i;
        a[offa + 18] = x14r;
        a[offa + 19] = x14i;
        a[offa + 20] = x6r;
        a[offa + 21] = x6i;
        a[offa + 22] = x10r;
        a[offa + 23] = x10i;
        a[offa + 24] = x2r;
        a[offa + 25] = x2i;
        a[offa + 26] = x12r;
        a[offa + 27] = x12i;
        a[offa + 28] = x4r;
        a[offa + 29] = x4i;
        a[offa + 30] = x8r;
        a[offa + 31] = x8i;
    }

    private static void bitrv208(float[] a, int offa) {

        var x1r = a[offa + 2];
        var x1i = a[offa + 3];
        var x3r = a[offa + 6];
        var x3i = a[offa + 7];
        var x4r = a[offa + 8];
        var x4i = a[offa + 9];
        var x6r = a[offa + 12];
        var x6i = a[offa + 13];
        a[offa + 2] = x4r;
        a[offa + 3] = x4i;
        a[offa + 6] = x6r;
        a[offa + 7] = x6i;
        a[offa + 8] = x1r;
        a[offa + 9] = x1i;
        a[offa + 12] = x3r;
        a[offa + 13] = x3i;
    }

    private static void bitrv208neg(float[] a, int offa) {

        var x1r = a[offa + 2];
        var x1i = a[offa + 3];
        var x2r = a[offa + 4];
        var x2i = a[offa + 5];
        var x3r = a[offa + 6];
        var x3i = a[offa + 7];
        var x4r = a[offa + 8];
        var x4i = a[offa + 9];
        var x5r = a[offa + 10];
        var x5i = a[offa + 11];
        var x6r = a[offa + 12];
        var x6i = a[offa + 13];
        var x7r = a[offa + 14];
        var x7i = a[offa + 15];
        a[offa + 2] = x7r;
        a[offa + 3] = x7i;
        a[offa + 4] = x3r;
        a[offa + 5] = x3i;
        a[offa + 6] = x5r;
        a[offa + 7] = x5i;
        a[offa + 8] = x1r;
        a[offa + 9] = x1i;
        a[offa + 10] = x6r;
        a[offa + 11] = x6i;
        a[offa + 12] = x2r;
        a[offa + 13] = x2i;
        a[offa + 14] = x4r;
        a[offa + 15] = x4i;
    }

    private static void cftf1st(int n, float[] a, int offa, float[] w, int startw) {
        var mh = n >> 3;
        var m = 2 * mh;
        var j1 = m;
        var j2 = j1 + m;
        var j3 = j2 + m;
        var idx1 = offa + j1;
        var idx2 = offa + j2;
        var idx3 = offa + j3;
        var x0r = a[offa] + a[idx2];
        var x0i = a[offa + 1] + a[idx2 + 1];
        var x1r = a[offa] - a[idx2];
        var x1i = a[offa + 1] - a[idx2 + 1];
        var x2r = a[idx1] + a[idx3];
        var x2i = a[idx1 + 1] + a[idx3 + 1];
        var x3r = a[idx1] - a[idx3];
        var x3i = a[idx1 + 1] - a[idx3 + 1];
        a[offa] = x0r + x2r;
        a[offa + 1] = x0i + x2i;
        a[idx1] = x0r - x2r;
        a[idx1 + 1] = x0i - x2i;
        a[idx2] = x1r - x3i;
        a[idx2 + 1] = x1i + x3r;
        a[idx3] = x1r + x3i;
        a[idx3 + 1] = x1i - x3r;
        var wn4r = w[startw + 1];
        var csc1 = w[startw + 2];
        var csc3 = w[startw + 3];
        float wd1r = 1;
        float wd1i = 0;
        float wd3r = 1;
        float wd3i = 0;
        var k = 0;
        int idx0;
        float wk3i;
        float wk3r;
        float wk1i;
        float wk1r;
        int j0;
        for (var j = 2; j < mh - 2; j += 4) {
            k += 4;
            var idx4 = startw + k;
            wk1r = csc1 * (wd1r + w[idx4]);
            wk1i = csc1 * (wd1i + w[idx4 + 1]);
            wk3r = csc3 * (wd3r + w[idx4 + 2]);
            wk3i = csc3 * (wd3i + w[idx4 + 3]);
            wd1r = w[idx4];
            wd1i = w[idx4 + 1];
            wd3r = w[idx4 + 2];
            wd3i = w[idx4 + 3];
            j1 = j + m;
            j2 = j1 + m;
            j3 = j2 + m;
            idx1 = offa + j1;
            idx2 = offa + j2;
            idx3 = offa + j3;
            var idx5 = offa + j;
            x0r = a[idx5] + a[idx2];
            x0i = a[idx5 + 1] + a[idx2 + 1];
            x1r = a[idx5] - a[idx2];
            x1i = a[idx5 + 1] - a[idx2 + 1];
            var y0r = a[idx5 + 2] + a[idx2 + 2];
            var y0i = a[idx5 + 3] + a[idx2 + 3];
            var y1r = a[idx5 + 2] - a[idx2 + 2];
            var y1i = a[idx5 + 3] - a[idx2 + 3];
            x2r = a[idx1] + a[idx3];
            x2i = a[idx1 + 1] + a[idx3 + 1];
            x3r = a[idx1] - a[idx3];
            x3i = a[idx1 + 1] - a[idx3 + 1];
            var y2r = a[idx1 + 2] + a[idx3 + 2];
            var y2i = a[idx1 + 3] + a[idx3 + 3];
            var y3r = a[idx1 + 2] - a[idx3 + 2];
            var y3i = a[idx1 + 3] - a[idx3 + 3];
            a[idx5] = x0r + x2r;
            a[idx5 + 1] = x0i + x2i;
            a[idx5 + 2] = y0r + y2r;
            a[idx5 + 3] = y0i + y2i;
            a[idx1] = x0r - x2r;
            a[idx1 + 1] = x0i - x2i;
            a[idx1 + 2] = y0r - y2r;
            a[idx1 + 3] = y0i - y2i;
            x0r = x1r - x3i;
            x0i = x1i + x3r;
            a[idx2] = wk1r * x0r - wk1i * x0i;
            a[idx2 + 1] = wk1r * x0i + wk1i * x0r;
            x0r = y1r - y3i;
            x0i = y1i + y3r;
            a[idx2 + 2] = wd1r * x0r - wd1i * x0i;
            a[idx2 + 3] = wd1r * x0i + wd1i * x0r;
            x0r = x1r + x3i;
            x0i = x1i - x3r;
            a[idx3] = wk3r * x0r + wk3i * x0i;
            a[idx3 + 1] = wk3r * x0i - wk3i * x0r;
            x0r = y1r + y3i;
            x0i = y1i - y3r;
            a[idx3 + 2] = wd3r * x0r + wd3i * x0i;
            a[idx3 + 3] = wd3r * x0i - wd3i * x0r;
            j0 = m - j;
            j1 = j0 + m;
            j2 = j1 + m;
            j3 = j2 + m;
            idx0 = offa + j0;
            idx1 = offa + j1;
            idx2 = offa + j2;
            idx3 = offa + j3;
            x0r = a[idx0] + a[idx2];
            x0i = a[idx0 + 1] + a[idx2 + 1];
            x1r = a[idx0] - a[idx2];
            x1i = a[idx0 + 1] - a[idx2 + 1];
            y0r = a[idx0 - 2] + a[idx2 - 2];
            y0i = a[idx0 - 1] + a[idx2 - 1];
            y1r = a[idx0 - 2] - a[idx2 - 2];
            y1i = a[idx0 - 1] - a[idx2 - 1];
            x2r = a[idx1] + a[idx3];
            x2i = a[idx1 + 1] + a[idx3 + 1];
            x3r = a[idx1] - a[idx3];
            x3i = a[idx1 + 1] - a[idx3 + 1];
            y2r = a[idx1 - 2] + a[idx3 - 2];
            y2i = a[idx1 - 1] + a[idx3 - 1];
            y3r = a[idx1 - 2] - a[idx3 - 2];
            y3i = a[idx1 - 1] - a[idx3 - 1];
            a[idx0] = x0r + x2r;
            a[idx0 + 1] = x0i + x2i;
            a[idx0 - 2] = y0r + y2r;
            a[idx0 - 1] = y0i + y2i;
            a[idx1] = x0r - x2r;
            a[idx1 + 1] = x0i - x2i;
            a[idx1 - 2] = y0r - y2r;
            a[idx1 - 1] = y0i - y2i;
            x0r = x1r - x3i;
            x0i = x1i + x3r;
            a[idx2] = wk1i * x0r - wk1r * x0i;
            a[idx2 + 1] = wk1i * x0i + wk1r * x0r;
            x0r = y1r - y3i;
            x0i = y1i + y3r;
            a[idx2 - 2] = wd1i * x0r - wd1r * x0i;
            a[idx2 - 1] = wd1i * x0i + wd1r * x0r;
            x0r = x1r + x3i;
            x0i = x1i - x3r;
            a[idx3] = wk3i * x0r + wk3r * x0i;
            a[idx3 + 1] = wk3i * x0i - wk3r * x0r;
            x0r = y1r + y3i;
            x0i = y1i - y3r;
            a[offa + j3 - 2] = wd3i * x0r + wd3r * x0i;
            a[offa + j3 - 1] = wd3i * x0i - wd3r * x0r;
        }
        wk1r = csc1 * (wd1r + wn4r);
        wk1i = csc1 * (wd1i + wn4r);
        wk3r = csc3 * (wd3r - wn4r);
        wk3i = csc3 * (wd3i - wn4r);
        j0 = mh;
        j1 = j0 + m;
        j2 = j1 + m;
        j3 = j2 + m;
        idx0 = offa + j0;
        idx1 = offa + j1;
        idx2 = offa + j2;
        idx3 = offa + j3;
        x0r = a[idx0 - 2] + a[idx2 - 2];
        x0i = a[idx0 - 1] + a[idx2 - 1];
        x1r = a[idx0 - 2] - a[idx2 - 2];
        x1i = a[idx0 - 1] - a[idx2 - 1];
        x2r = a[idx1 - 2] + a[idx3 - 2];
        x2i = a[idx1 - 1] + a[idx3 - 1];
        x3r = a[idx1 - 2] - a[idx3 - 2];
        x3i = a[idx1 - 1] - a[idx3 - 1];
        a[idx0 - 2] = x0r + x2r;
        a[idx0 - 1] = x0i + x2i;
        a[idx1 - 2] = x0r - x2r;
        a[idx1 - 1] = x0i - x2i;
        x0r = x1r - x3i;
        x0i = x1i + x3r;
        a[idx2 - 2] = wk1r * x0r - wk1i * x0i;
        a[idx2 - 1] = wk1r * x0i + wk1i * x0r;
        x0r = x1r + x3i;
        x0i = x1i - x3r;
        a[idx3 - 2] = wk3r * x0r + wk3i * x0i;
        a[idx3 - 1] = wk3r * x0i - wk3i * x0r;
        x0r = a[idx0] + a[idx2];
        x0i = a[idx0 + 1] + a[idx2 + 1];
        x1r = a[idx0] - a[idx2];
        x1i = a[idx0 + 1] - a[idx2 + 1];
        x2r = a[idx1] + a[idx3];
        x2i = a[idx1 + 1] + a[idx3 + 1];
        x3r = a[idx1] - a[idx3];
        x3i = a[idx1 + 1] - a[idx3 + 1];
        a[idx0] = x0r + x2r;
        a[idx0 + 1] = x0i + x2i;
        a[idx1] = x0r - x2r;
        a[idx1 + 1] = x0i - x2i;
        x0r = x1r - x3i;
        x0i = x1i + x3r;
        a[idx2] = wn4r * (x0r - x0i);
        a[idx2 + 1] = wn4r * (x0i + x0r);
        x0r = x1r + x3i;
        x0i = x1i - x3r;
        a[idx3] = -wn4r * (x0r + x0i);
        a[idx3 + 1] = -wn4r * (x0i - x0r);
        x0r = a[idx0 + 2] + a[idx2 + 2];
        x0i = a[idx0 + 3] + a[idx2 + 3];
        x1r = a[idx0 + 2] - a[idx2 + 2];
        x1i = a[idx0 + 3] - a[idx2 + 3];
        x2r = a[idx1 + 2] + a[idx3 + 2];
        x2i = a[idx1 + 3] + a[idx3 + 3];
        x3r = a[idx1 + 2] - a[idx3 + 2];
        x3i = a[idx1 + 3] - a[idx3 + 3];
        a[idx0 + 2] = x0r + x2r;
        a[idx0 + 3] = x0i + x2i;
        a[idx1 + 2] = x0r - x2r;
        a[idx1 + 3] = x0i - x2i;
        x0r = x1r - x3i;
        x0i = x1i + x3r;
        a[idx2 + 2] = wk1i * x0r - wk1r * x0i;
        a[idx2 + 3] = wk1i * x0i + wk1r * x0r;
        x0r = x1r + x3i;
        x0i = x1i - x3r;
        a[idx3 + 2] = wk3i * x0r + wk3r * x0i;
        a[idx3 + 3] = wk3i * x0i - wk3r * x0r;
    }

    private static void cftb1st(int n, float[] a, int offa, float[] w, int startw) {
        var mh = n >> 3;
        var m = 2 * mh;
        var j1 = m;
        var j2 = j1 + m;
        var j3 = j2 + m;
        var idx1 = offa + j1;
        var idx2 = offa + j2;
        var idx3 = offa + j3;

        var x0r = a[offa] + a[idx2];
        var x0i = -a[offa + 1] - a[idx2 + 1];
        var x1r = a[offa] - a[idx2];
        var x1i = -a[offa + 1] + a[idx2 + 1];
        var x2r = a[idx1] + a[idx3];
        var x2i = a[idx1 + 1] + a[idx3 + 1];
        var x3r = a[idx1] - a[idx3];
        var x3i = a[idx1 + 1] - a[idx3 + 1];
        a[offa] = x0r + x2r;
        a[offa + 1] = x0i - x2i;
        a[idx1] = x0r - x2r;
        a[idx1 + 1] = x0i + x2i;
        a[idx2] = x1r + x3i;
        a[idx2 + 1] = x1i + x3r;
        a[idx3] = x1r - x3i;
        a[idx3 + 1] = x1i - x3r;
        var wn4r = w[startw + 1];
        var csc1 = w[startw + 2];
        var csc3 = w[startw + 3];
        float wd1r = 1;
        float wd1i = 0;
        float wd3r = 1;
        float wd3i = 0;
        var k = 0;
        int idx0;
        float wk3i;
        float wk3r;
        float wk1i;
        float wk1r;
        int j0;
        for (var j = 2; j < mh - 2; j += 4) {
            k += 4;
            var idx4 = startw + k;
            wk1r = csc1 * (wd1r + w[idx4]);
            wk1i = csc1 * (wd1i + w[idx4 + 1]);
            wk3r = csc3 * (wd3r + w[idx4 + 2]);
            wk3i = csc3 * (wd3i + w[idx4 + 3]);
            wd1r = w[idx4];
            wd1i = w[idx4 + 1];
            wd3r = w[idx4 + 2];
            wd3i = w[idx4 + 3];
            j1 = j + m;
            j2 = j1 + m;
            j3 = j2 + m;
            idx1 = offa + j1;
            idx2 = offa + j2;
            idx3 = offa + j3;
            var idx5 = offa + j;
            x0r = a[idx5] + a[idx2];
            x0i = -a[idx5 + 1] - a[idx2 + 1];
            x1r = a[idx5] - a[offa + j2];
            x1i = -a[idx5 + 1] + a[idx2 + 1];
            var y0r = a[idx5 + 2] + a[idx2 + 2];
            var y0i = -a[idx5 + 3] - a[idx2 + 3];
            var y1r = a[idx5 + 2] - a[idx2 + 2];
            var y1i = -a[idx5 + 3] + a[idx2 + 3];
            x2r = a[idx1] + a[idx3];
            x2i = a[idx1 + 1] + a[idx3 + 1];
            x3r = a[idx1] - a[idx3];
            x3i = a[idx1 + 1] - a[idx3 + 1];
            var y2r = a[idx1 + 2] + a[idx3 + 2];
            var y2i = a[idx1 + 3] + a[idx3 + 3];
            var y3r = a[idx1 + 2] - a[idx3 + 2];
            var y3i = a[idx1 + 3] - a[idx3 + 3];
            a[idx5] = x0r + x2r;
            a[idx5 + 1] = x0i - x2i;
            a[idx5 + 2] = y0r + y2r;
            a[idx5 + 3] = y0i - y2i;
            a[idx1] = x0r - x2r;
            a[idx1 + 1] = x0i + x2i;
            a[idx1 + 2] = y0r - y2r;
            a[idx1 + 3] = y0i + y2i;
            x0r = x1r + x3i;
            x0i = x1i + x3r;
            a[idx2] = wk1r * x0r - wk1i * x0i;
            a[idx2 + 1] = wk1r * x0i + wk1i * x0r;
            x0r = y1r + y3i;
            x0i = y1i + y3r;
            a[idx2 + 2] = wd1r * x0r - wd1i * x0i;
            a[idx2 + 3] = wd1r * x0i + wd1i * x0r;
            x0r = x1r - x3i;
            x0i = x1i - x3r;
            a[idx3] = wk3r * x0r + wk3i * x0i;
            a[idx3 + 1] = wk3r * x0i - wk3i * x0r;
            x0r = y1r - y3i;
            x0i = y1i - y3r;
            a[idx3 + 2] = wd3r * x0r + wd3i * x0i;
            a[idx3 + 3] = wd3r * x0i - wd3i * x0r;
            j0 = m - j;
            j1 = j0 + m;
            j2 = j1 + m;
            j3 = j2 + m;
            idx0 = offa + j0;
            idx1 = offa + j1;
            idx2 = offa + j2;
            idx3 = offa + j3;
            x0r = a[idx0] + a[idx2];
            x0i = -a[idx0 + 1] - a[idx2 + 1];
            x1r = a[idx0] - a[idx2];
            x1i = -a[idx0 + 1] + a[idx2 + 1];
            y0r = a[idx0 - 2] + a[idx2 - 2];
            y0i = -a[idx0 - 1] - a[idx2 - 1];
            y1r = a[idx0 - 2] - a[idx2 - 2];
            y1i = -a[idx0 - 1] + a[idx2 - 1];
            x2r = a[idx1] + a[idx3];
            x2i = a[idx1 + 1] + a[idx3 + 1];
            x3r = a[idx1] - a[idx3];
            x3i = a[idx1 + 1] - a[idx3 + 1];
            y2r = a[idx1 - 2] + a[idx3 - 2];
            y2i = a[idx1 - 1] + a[idx3 - 1];
            y3r = a[idx1 - 2] - a[idx3 - 2];
            y3i = a[idx1 - 1] - a[idx3 - 1];
            a[idx0] = x0r + x2r;
            a[idx0 + 1] = x0i - x2i;
            a[idx0 - 2] = y0r + y2r;
            a[idx0 - 1] = y0i - y2i;
            a[idx1] = x0r - x2r;
            a[idx1 + 1] = x0i + x2i;
            a[idx1 - 2] = y0r - y2r;
            a[idx1 - 1] = y0i + y2i;
            x0r = x1r + x3i;
            x0i = x1i + x3r;
            a[idx2] = wk1i * x0r - wk1r * x0i;
            a[idx2 + 1] = wk1i * x0i + wk1r * x0r;
            x0r = y1r + y3i;
            x0i = y1i + y3r;
            a[idx2 - 2] = wd1i * x0r - wd1r * x0i;
            a[idx2 - 1] = wd1i * x0i + wd1r * x0r;
            x0r = x1r - x3i;
            x0i = x1i - x3r;
            a[idx3] = wk3i * x0r + wk3r * x0i;
            a[idx3 + 1] = wk3i * x0i - wk3r * x0r;
            x0r = y1r - y3i;
            x0i = y1i - y3r;
            a[idx3 - 2] = wd3i * x0r + wd3r * x0i;
            a[idx3 - 1] = wd3i * x0i - wd3r * x0r;
        }
        wk1r = csc1 * (wd1r + wn4r);
        wk1i = csc1 * (wd1i + wn4r);
        wk3r = csc3 * (wd3r - wn4r);
        wk3i = csc3 * (wd3i - wn4r);
        j0 = mh;
        j1 = j0 + m;
        j2 = j1 + m;
        j3 = j2 + m;
        idx0 = offa + j0;
        idx1 = offa + j1;
        idx2 = offa + j2;
        idx3 = offa + j3;
        x0r = a[idx0 - 2] + a[idx2 - 2];
        x0i = -a[idx0 - 1] - a[idx2 - 1];
        x1r = a[idx0 - 2] - a[idx2 - 2];
        x1i = -a[idx0 - 1] + a[idx2 - 1];
        x2r = a[idx1 - 2] + a[idx3 - 2];
        x2i = a[idx1 - 1] + a[idx3 - 1];
        x3r = a[idx1 - 2] - a[idx3 - 2];
        x3i = a[idx1 - 1] - a[idx3 - 1];
        a[idx0 - 2] = x0r + x2r;
        a[idx0 - 1] = x0i - x2i;
        a[idx1 - 2] = x0r - x2r;
        a[idx1 - 1] = x0i + x2i;
        x0r = x1r + x3i;
        x0i = x1i + x3r;
        a[idx2 - 2] = wk1r * x0r - wk1i * x0i;
        a[idx2 - 1] = wk1r * x0i + wk1i * x0r;
        x0r = x1r - x3i;
        x0i = x1i - x3r;
        a[idx3 - 2] = wk3r * x0r + wk3i * x0i;
        a[idx3 - 1] = wk3r * x0i - wk3i * x0r;
        x0r = a[idx0] + a[idx2];
        x0i = -a[idx0 + 1] - a[idx2 + 1];
        x1r = a[idx0] - a[idx2];
        x1i = -a[idx0 + 1] + a[idx2 + 1];
        x2r = a[idx1] + a[idx3];
        x2i = a[idx1 + 1] + a[idx3 + 1];
        x3r = a[idx1] - a[idx3];
        x3i = a[idx1 + 1] - a[idx3 + 1];
        a[idx0] = x0r + x2r;
        a[idx0 + 1] = x0i - x2i;
        a[idx1] = x0r - x2r;
        a[idx1 + 1] = x0i + x2i;
        x0r = x1r + x3i;
        x0i = x1i + x3r;
        a[idx2] = wn4r * (x0r - x0i);
        a[idx2 + 1] = wn4r * (x0i + x0r);
        x0r = x1r - x3i;
        x0i = x1i - x3r;
        a[idx3] = -wn4r * (x0r + x0i);
        a[idx3 + 1] = -wn4r * (x0i - x0r);
        x0r = a[idx0 + 2] + a[idx2 + 2];
        x0i = -a[idx0 + 3] - a[idx2 + 3];
        x1r = a[idx0 + 2] - a[idx2 + 2];
        x1i = -a[idx0 + 3] + a[idx2 + 3];
        x2r = a[idx1 + 2] + a[idx3 + 2];
        x2i = a[idx1 + 3] + a[idx3 + 3];
        x3r = a[idx1 + 2] - a[idx3 + 2];
        x3i = a[idx1 + 3] - a[idx3 + 3];
        a[idx0 + 2] = x0r + x2r;
        a[idx0 + 3] = x0i - x2i;
        a[idx1 + 2] = x0r - x2r;
        a[idx1 + 3] = x0i + x2i;
        x0r = x1r + x3i;
        x0i = x1i + x3r;
        a[idx2 + 2] = wk1i * x0r - wk1r * x0i;
        a[idx2 + 3] = wk1i * x0i + wk1r * x0r;
        x0r = x1r - x3i;
        x0i = x1i - x3r;
        a[idx3 + 2] = wk3i * x0r + wk3r * x0i;
        a[idx3 + 3] = wk3i * x0i - wk3r * x0r;
    }

    private static int cfttree(int n, int j, int k, float[] a, int offa, int nw, float[] w) {
        int isplt;
        if ((k & 3) != 0) {
            isplt = k & 1;
            var idx1 = offa - n;
            if (isplt != 0) {
                cftmdl1(n, a, idx1 + j, w, nw - (n >> 1));
            } else {
                cftmdl2(n, a, idx1 + j, w, nw - n);
            }
        } else {
            var m = n;
            int i;
            for (i = k; (i & 3) == 0; i >>= 2) {
                m <<= 2;
            }
            isplt = i & 1;
            var idx2 = offa + j;
            if (isplt != 0) {
                while (m > 128) {
                    cftmdl1(m, a, idx2 - m, w, nw - (m >> 1));
                    m >>= 2;
                }
            } else {
                while (m > 128) {
                    cftmdl2(m, a, idx2 - m, w, nw - m);
                    m >>= 2;
                }
            }
        }
        return isplt;
    }

    private static void cftleaf(int n, int isplt, float[] a, int offa, int nw, float[] w) {
        if (n == 512) {
            cftmdl1(128, a, offa, w, nw - 64);
            cftf161(a, offa, w, nw - 8);
            cftf162(a, offa + 32, w, nw - 32);
            cftf161(a, offa + 64, w, nw - 8);
            cftf161(a, offa + 96, w, nw - 8);
            cftmdl2(128, a, offa + 128, w, nw - 128);
            cftf161(a, offa + 128, w, nw - 8);
            cftf162(a, offa + 160, w, nw - 32);
            cftf161(a, offa + 192, w, nw - 8);
            cftf162(a, offa + 224, w, nw - 32);
            cftmdl1(128, a, offa + 256, w, nw - 64);
            cftf161(a, offa + 256, w, nw - 8);
            cftf162(a, offa + 288, w, nw - 32);
            cftf161(a, offa + 320, w, nw - 8);
            cftf161(a, offa + 352, w, nw - 8);
            if (isplt != 0) {
                cftmdl1(128, a, offa + 384, w, nw - 64);
                cftf161(a, offa + 480, w, nw - 8);
            } else {
                cftmdl2(128, a, offa + 384, w, nw - 128);
                cftf162(a, offa + 480, w, nw - 32);
            }
            cftf161(a, offa + 384, w, nw - 8);
            cftf162(a, offa + 416, w, nw - 32);
            cftf161(a, offa + 448, w, nw - 8);
        } else {
            cftmdl1(64, a, offa, w, nw - 32);
            cftf081(a, offa, w, nw - 8);
            cftf082(a, offa + 16, w, nw - 8);
            cftf081(a, offa + 32, w, nw - 8);
            cftf081(a, offa + 48, w, nw - 8);
            cftmdl2(64, a, offa + 64, w, nw - 64);
            cftf081(a, offa + 64, w, nw - 8);
            cftf082(a, offa + 80, w, nw - 8);
            cftf081(a, offa + 96, w, nw - 8);
            cftf082(a, offa + 112, w, nw - 8);
            cftmdl1(64, a, offa + 128, w, nw - 32);
            cftf081(a, offa + 128, w, nw - 8);
            cftf082(a, offa + 144, w, nw - 8);
            cftf081(a, offa + 160, w, nw - 8);
            cftf081(a, offa + 176, w, nw - 8);
            if (isplt != 0) {
                cftmdl1(64, a, offa + 192, w, nw - 32);
                cftf081(a, offa + 240, w, nw - 8);
            } else {
                cftmdl2(64, a, offa + 192, w, nw - 64);
                cftf082(a, offa + 240, w, nw - 8);
            }
            cftf081(a, offa + 192, w, nw - 8);
            cftf082(a, offa + 208, w, nw - 8);
            cftf081(a, offa + 224, w, nw - 8);
        }
    }

    private static void cftmdl1(int n, float[] a, int offa, float[] w, int startw) {

        var mh = n >> 3;
        var m = 2 * mh;
        var j1 = m;
        var j2 = j1 + m;
        var j3 = j2 + m;
        var idx1 = offa + j1;
        var idx2 = offa + j2;
        var idx3 = offa + j3;
        var x0r = a[offa] + a[idx2];
        var x0i = a[offa + 1] + a[idx2 + 1];
        var x1r = a[offa] - a[idx2];
        var x1i = a[offa + 1] - a[idx2 + 1];
        var x2r = a[idx1] + a[idx3];
        var x2i = a[idx1 + 1] + a[idx3 + 1];
        var x3r = a[idx1] - a[idx3];
        var x3i = a[idx1 + 1] - a[idx3 + 1];
        a[offa] = x0r + x2r;
        a[offa + 1] = x0i + x2i;
        a[idx1] = x0r - x2r;
        a[idx1 + 1] = x0i - x2i;
        a[idx2] = x1r - x3i;
        a[idx2 + 1] = x1i + x3r;
        a[idx3] = x1r + x3i;
        a[idx3 + 1] = x1i - x3r;
        var wn4r = w[startw + 1];
        var k = 0;
        int idx0;
        int j0;
        for (var j = 2; j < mh; j += 2) {
            k += 4;
            var idx4 = startw + k;
            var wk1r = w[idx4];
            var wk1i = w[idx4 + 1];
            var wk3r = w[idx4 + 2];
            var wk3i = w[idx4 + 3];
            j1 = j + m;
            j2 = j1 + m;
            j3 = j2 + m;
            idx1 = offa + j1;
            idx2 = offa + j2;
            idx3 = offa + j3;
            var idx5 = offa + j;
            x0r = a[idx5] + a[idx2];
            x0i = a[idx5 + 1] + a[idx2 + 1];
            x1r = a[idx5] - a[idx2];
            x1i = a[idx5 + 1] - a[idx2 + 1];
            x2r = a[idx1] + a[idx3];
            x2i = a[idx1 + 1] + a[idx3 + 1];
            x3r = a[idx1] - a[idx3];
            x3i = a[idx1 + 1] - a[idx3 + 1];
            a[idx5] = x0r + x2r;
            a[idx5 + 1] = x0i + x2i;
            a[idx1] = x0r - x2r;
            a[idx1 + 1] = x0i - x2i;
            x0r = x1r - x3i;
            x0i = x1i + x3r;
            a[idx2] = wk1r * x0r - wk1i * x0i;
            a[idx2 + 1] = wk1r * x0i + wk1i * x0r;
            x0r = x1r + x3i;
            x0i = x1i - x3r;
            a[idx3] = wk3r * x0r + wk3i * x0i;
            a[idx3 + 1] = wk3r * x0i - wk3i * x0r;
            j0 = m - j;
            j1 = j0 + m;
            j2 = j1 + m;
            j3 = j2 + m;
            idx0 = offa + j0;
            idx1 = offa + j1;
            idx2 = offa + j2;
            idx3 = offa + j3;
            x0r = a[idx0] + a[idx2];
            x0i = a[idx0 + 1] + a[idx2 + 1];
            x1r = a[idx0] - a[idx2];
            x1i = a[idx0 + 1] - a[idx2 + 1];
            x2r = a[idx1] + a[idx3];
            x2i = a[idx1 + 1] + a[idx3 + 1];
            x3r = a[idx1] - a[idx3];
            x3i = a[idx1 + 1] - a[idx3 + 1];
            a[idx0] = x0r + x2r;
            a[idx0 + 1] = x0i + x2i;
            a[idx1] = x0r - x2r;
            a[idx1 + 1] = x0i - x2i;
            x0r = x1r - x3i;
            x0i = x1i + x3r;
            a[idx2] = wk1i * x0r - wk1r * x0i;
            a[idx2 + 1] = wk1i * x0i + wk1r * x0r;
            x0r = x1r + x3i;
            x0i = x1i - x3r;
            a[idx3] = wk3i * x0r + wk3r * x0i;
            a[idx3 + 1] = wk3i * x0i - wk3r * x0r;
        }
        j0 = mh;
        j1 = j0 + m;
        j2 = j1 + m;
        j3 = j2 + m;
        idx0 = offa + j0;
        idx1 = offa + j1;
        idx2 = offa + j2;
        idx3 = offa + j3;
        x0r = a[idx0] + a[idx2];
        x0i = a[idx0 + 1] + a[idx2 + 1];
        x1r = a[idx0] - a[idx2];
        x1i = a[idx0 + 1] - a[idx2 + 1];
        x2r = a[idx1] + a[idx3];
        x2i = a[idx1 + 1] + a[idx3 + 1];
        x3r = a[idx1] - a[idx3];
        x3i = a[idx1 + 1] - a[idx3 + 1];
        a[idx0] = x0r + x2r;
        a[idx0 + 1] = x0i + x2i;
        a[idx1] = x0r - x2r;
        a[idx1 + 1] = x0i - x2i;
        x0r = x1r - x3i;
        x0i = x1i + x3r;
        a[idx2] = wn4r * (x0r - x0i);
        a[idx2 + 1] = wn4r * (x0i + x0r);
        x0r = x1r + x3i;
        x0i = x1i - x3r;
        a[idx3] = -wn4r * (x0r + x0i);
        a[idx3 + 1] = -wn4r * (x0i - x0r);
    }

    private static void cftmdl2(int n, float[] a, int offa, float[] w, int startw) {

        var mh = n >> 3;
        var m = 2 * mh;
        var wn4r = w[startw + 1];
        var j1 = m;
        var j2 = j1 + m;
        var j3 = j2 + m;
        var idx1 = offa + j1;
        var idx2 = offa + j2;
        var idx3 = offa + j3;
        var x0r = a[offa] - a[idx2 + 1];
        var x0i = a[offa + 1] + a[idx2];
        var x1r = a[offa] + a[idx2 + 1];
        var x1i = a[offa + 1] - a[idx2];
        var x2r = a[idx1] - a[idx3 + 1];
        var x2i = a[idx1 + 1] + a[idx3];
        var x3r = a[idx1] + a[idx3 + 1];
        var x3i = a[idx1 + 1] - a[idx3];
        var y0r = wn4r * (x2r - x2i);
        var y0i = wn4r * (x2i + x2r);
        a[offa] = x0r + y0r;
        a[offa + 1] = x0i + y0i;
        a[idx1] = x0r - y0r;
        a[idx1 + 1] = x0i - y0i;
        y0r = wn4r * (x3r - x3i);
        y0i = wn4r * (x3i + x3r);
        a[idx2] = x1r - y0i;
        a[idx2 + 1] = x1i + y0r;
        a[idx3] = x1r + y0i;
        a[idx3 + 1] = x1i - y0r;
        var k = 0;
        var kr = 2 * m;
        int idx0;
        float y2i;
        float y2r;
        float wk1i;
        float wk1r;
        int j0;
        for (var j = 2; j < mh; j += 2) {
            k += 4;
            var idx4 = startw + k;
            wk1r = w[idx4];
            wk1i = w[idx4 + 1];
            var wk3r = w[idx4 + 2];
            var wk3i = w[idx4 + 3];
            kr -= 4;
            var idx5 = startw + kr;
            var wd1i = w[idx5];
            var wd1r = w[idx5 + 1];
            var wd3i = w[idx5 + 2];
            var wd3r = w[idx5 + 3];
            j1 = j + m;
            j2 = j1 + m;
            j3 = j2 + m;
            idx1 = offa + j1;
            idx2 = offa + j2;
            idx3 = offa + j3;
            var idx6 = offa + j;
            x0r = a[idx6] - a[idx2 + 1];
            x0i = a[idx6 + 1] + a[idx2];
            x1r = a[idx6] + a[idx2 + 1];
            x1i = a[idx6 + 1] - a[idx2];
            x2r = a[idx1] - a[idx3 + 1];
            x2i = a[idx1 + 1] + a[idx3];
            x3r = a[idx1] + a[idx3 + 1];
            x3i = a[idx1 + 1] - a[idx3];
            y0r = wk1r * x0r - wk1i * x0i;
            y0i = wk1r * x0i + wk1i * x0r;
            y2r = wd1r * x2r - wd1i * x2i;
            y2i = wd1r * x2i + wd1i * x2r;
            a[idx6] = y0r + y2r;
            a[idx6 + 1] = y0i + y2i;
            a[idx1] = y0r - y2r;
            a[idx1 + 1] = y0i - y2i;
            y0r = wk3r * x1r + wk3i * x1i;
            y0i = wk3r * x1i - wk3i * x1r;
            y2r = wd3r * x3r + wd3i * x3i;
            y2i = wd3r * x3i - wd3i * x3r;
            a[idx2] = y0r + y2r;
            a[idx2 + 1] = y0i + y2i;
            a[idx3] = y0r - y2r;
            a[idx3 + 1] = y0i - y2i;
            j0 = m - j;
            j1 = j0 + m;
            j2 = j1 + m;
            j3 = j2 + m;
            idx0 = offa + j0;
            idx1 = offa + j1;
            idx2 = offa + j2;
            idx3 = offa + j3;
            x0r = a[idx0] - a[idx2 + 1];
            x0i = a[idx0 + 1] + a[idx2];
            x1r = a[idx0] + a[idx2 + 1];
            x1i = a[idx0 + 1] - a[idx2];
            x2r = a[idx1] - a[idx3 + 1];
            x2i = a[idx1 + 1] + a[idx3];
            x3r = a[idx1] + a[idx3 + 1];
            x3i = a[idx1 + 1] - a[idx3];
            y0r = wd1i * x0r - wd1r * x0i;
            y0i = wd1i * x0i + wd1r * x0r;
            y2r = wk1i * x2r - wk1r * x2i;
            y2i = wk1i * x2i + wk1r * x2r;
            a[idx0] = y0r + y2r;
            a[idx0 + 1] = y0i + y2i;
            a[idx1] = y0r - y2r;
            a[idx1 + 1] = y0i - y2i;
            y0r = wd3i * x1r + wd3r * x1i;
            y0i = wd3i * x1i - wd3r * x1r;
            y2r = wk3i * x3r + wk3r * x3i;
            y2i = wk3i * x3i - wk3r * x3r;
            a[idx2] = y0r + y2r;
            a[idx2 + 1] = y0i + y2i;
            a[idx3] = y0r - y2r;
            a[idx3 + 1] = y0i - y2i;
        }
        wk1r = w[startw + m];
        wk1i = w[startw + m + 1];
        j0 = mh;
        j1 = j0 + m;
        j2 = j1 + m;
        j3 = j2 + m;
        idx0 = offa + j0;
        idx1 = offa + j1;
        idx2 = offa + j2;
        idx3 = offa + j3;
        x0r = a[idx0] - a[idx2 + 1];
        x0i = a[idx0 + 1] + a[idx2];
        x1r = a[idx0] + a[idx2 + 1];
        x1i = a[idx0 + 1] - a[idx2];
        x2r = a[idx1] - a[idx3 + 1];
        x2i = a[idx1 + 1] + a[idx3];
        x3r = a[idx1] + a[idx3 + 1];
        x3i = a[idx1 + 1] - a[idx3];
        y0r = wk1r * x0r - wk1i * x0i;
        y0i = wk1r * x0i + wk1i * x0r;
        y2r = wk1i * x2r - wk1r * x2i;
        y2i = wk1i * x2i + wk1r * x2r;
        a[idx0] = y0r + y2r;
        a[idx0 + 1] = y0i + y2i;
        a[idx1] = y0r - y2r;
        a[idx1 + 1] = y0i - y2i;
        y0r = wk1i * x1r - wk1r * x1i;
        y0i = wk1i * x1i + wk1r * x1r;
        y2r = wk1r * x3r - wk1i * x3i;
        y2i = wk1r * x3i + wk1i * x3r;
        a[idx2] = y0r - y2r;
        a[idx2 + 1] = y0i - y2i;
        a[idx3] = y0r + y2r;
        a[idx3 + 1] = y0i + y2i;
    }

    private static void cftfx41(int n, float[] a, int offa, int nw, float[] w) {
        if (n == 128) {
            cftf161(a, offa, w, nw - 8);
            cftf162(a, offa + 32, w, nw - 32);
            cftf161(a, offa + 64, w, nw - 8);
            cftf161(a, offa + 96, w, nw - 8);
        } else {
            cftf081(a, offa, w, nw - 8);
            cftf082(a, offa + 16, w, nw - 8);
            cftf081(a, offa + 32, w, nw - 8);
            cftf081(a, offa + 48, w, nw - 8);
        }
    }

    private static void cftf161(float[] a, int offa, float[] w, int startw) {

        var wn4r = w[startw + 1];
        var wk1r = w[startw + 2];
        var wk1i = w[startw + 3];

        var x0r = a[offa] + a[offa + 16];
        var x0i = a[offa + 1] + a[offa + 17];
        var x1r = a[offa] - a[offa + 16];
        var x1i = a[offa + 1] - a[offa + 17];
        var x2r = a[offa + 8] + a[offa + 24];
        var x2i = a[offa + 9] + a[offa + 25];
        var x3r = a[offa + 8] - a[offa + 24];
        var x3i = a[offa + 9] - a[offa + 25];
        var y0r = x0r + x2r;
        var y0i = x0i + x2i;
        var y4r = x0r - x2r;
        var y4i = x0i - x2i;
        var y8r = x1r - x3i;
        var y8i = x1i + x3r;
        var y12r = x1r + x3i;
        var y12i = x1i - x3r;
        x0r = a[offa + 2] + a[offa + 18];
        x0i = a[offa + 3] + a[offa + 19];
        x1r = a[offa + 2] - a[offa + 18];
        x1i = a[offa + 3] - a[offa + 19];
        x2r = a[offa + 10] + a[offa + 26];
        x2i = a[offa + 11] + a[offa + 27];
        x3r = a[offa + 10] - a[offa + 26];
        x3i = a[offa + 11] - a[offa + 27];
        var y1r = x0r + x2r;
        var y1i = x0i + x2i;
        var y5r = x0r - x2r;
        var y5i = x0i - x2i;
        x0r = x1r - x3i;
        x0i = x1i + x3r;
        var y9r = wk1r * x0r - wk1i * x0i;
        var y9i = wk1r * x0i + wk1i * x0r;
        x0r = x1r + x3i;
        x0i = x1i - x3r;
        var y13r = wk1i * x0r - wk1r * x0i;
        var y13i = wk1i * x0i + wk1r * x0r;
        x0r = a[offa + 4] + a[offa + 20];
        x0i = a[offa + 5] + a[offa + 21];
        x1r = a[offa + 4] - a[offa + 20];
        x1i = a[offa + 5] - a[offa + 21];
        x2r = a[offa + 12] + a[offa + 28];
        x2i = a[offa + 13] + a[offa + 29];
        x3r = a[offa + 12] - a[offa + 28];
        x3i = a[offa + 13] - a[offa + 29];
        var y2r = x0r + x2r;
        var y2i = x0i + x2i;
        var y6r = x0r - x2r;
        var y6i = x0i - x2i;
        x0r = x1r - x3i;
        x0i = x1i + x3r;
        var y10r = wn4r * (x0r - x0i);
        var y10i = wn4r * (x0i + x0r);
        x0r = x1r + x3i;
        x0i = x1i - x3r;
        var y14r = wn4r * (x0r + x0i);
        var y14i = wn4r * (x0i - x0r);
        x0r = a[offa + 6] + a[offa + 22];
        x0i = a[offa + 7] + a[offa + 23];
        x1r = a[offa + 6] - a[offa + 22];
        x1i = a[offa + 7] - a[offa + 23];
        x2r = a[offa + 14] + a[offa + 30];
        x2i = a[offa + 15] + a[offa + 31];
        x3r = a[offa + 14] - a[offa + 30];
        x3i = a[offa + 15] - a[offa + 31];
        var y3r = x0r + x2r;
        var y3i = x0i + x2i;
        var y7r = x0r - x2r;
        var y7i = x0i - x2i;
        x0r = x1r - x3i;
        x0i = x1i + x3r;
        var y11r = wk1i * x0r - wk1r * x0i;
        var y11i = wk1i * x0i + wk1r * x0r;
        x0r = x1r + x3i;
        x0i = x1i - x3r;
        var y15r = wk1r * x0r - wk1i * x0i;
        var y15i = wk1r * x0i + wk1i * x0r;
        x0r = y12r - y14r;
        x0i = y12i - y14i;
        x1r = y12r + y14r;
        x1i = y12i + y14i;
        x2r = y13r - y15r;
        x2i = y13i - y15i;
        x3r = y13r + y15r;
        x3i = y13i + y15i;
        a[offa + 24] = x0r + x2r;
        a[offa + 25] = x0i + x2i;
        a[offa + 26] = x0r - x2r;
        a[offa + 27] = x0i - x2i;
        a[offa + 28] = x1r - x3i;
        a[offa + 29] = x1i + x3r;
        a[offa + 30] = x1r + x3i;
        a[offa + 31] = x1i - x3r;
        x0r = y8r + y10r;
        x0i = y8i + y10i;
        x1r = y8r - y10r;
        x1i = y8i - y10i;
        x2r = y9r + y11r;
        x2i = y9i + y11i;
        x3r = y9r - y11r;
        x3i = y9i - y11i;
        a[offa + 16] = x0r + x2r;
        a[offa + 17] = x0i + x2i;
        a[offa + 18] = x0r - x2r;
        a[offa + 19] = x0i - x2i;
        a[offa + 20] = x1r - x3i;
        a[offa + 21] = x1i + x3r;
        a[offa + 22] = x1r + x3i;
        a[offa + 23] = x1i - x3r;
        x0r = y5r - y7i;
        x0i = y5i + y7r;
        x2r = wn4r * (x0r - x0i);
        x2i = wn4r * (x0i + x0r);
        x0r = y5r + y7i;
        x0i = y5i - y7r;
        x3r = wn4r * (x0r - x0i);
        x3i = wn4r * (x0i + x0r);
        x0r = y4r - y6i;
        x0i = y4i + y6r;
        x1r = y4r + y6i;
        x1i = y4i - y6r;
        a[offa + 8] = x0r + x2r;
        a[offa + 9] = x0i + x2i;
        a[offa + 10] = x0r - x2r;
        a[offa + 11] = x0i - x2i;
        a[offa + 12] = x1r - x3i;
        a[offa + 13] = x1i + x3r;
        a[offa + 14] = x1r + x3i;
        a[offa + 15] = x1i - x3r;
        x0r = y0r + y2r;
        x0i = y0i + y2i;
        x1r = y0r - y2r;
        x1i = y0i - y2i;
        x2r = y1r + y3r;
        x2i = y1i + y3i;
        x3r = y1r - y3r;
        x3i = y1i - y3i;
        a[offa] = x0r + x2r;
        a[offa + 1] = x0i + x2i;
        a[offa + 2] = x0r - x2r;
        a[offa + 3] = x0i - x2i;
        a[offa + 4] = x1r - x3i;
        a[offa + 5] = x1i + x3r;
        a[offa + 6] = x1r + x3i;
        a[offa + 7] = x1i - x3r;
    }

    /* -------- initializing routines -------- */

    /*---------------------------------------------------------
       cffti: initialization of Complex FFT
      --------------------------------------------------------*/

    private static void cftf162(float[] a, int offa, float[] w, int startw) {

        var wn4r = w[startw + 1];
        var wk1r = w[startw + 4];
        var wk1i = w[startw + 5];
        var wk3r = w[startw + 6];
        var wk3i = -w[startw + 7];
        var wk2r = w[startw + 8];
        var wk2i = w[startw + 9];
        var x1r = a[offa] - a[offa + 17];
        var x1i = a[offa + 1] + a[offa + 16];
        var x0r = a[offa + 8] - a[offa + 25];
        var x0i = a[offa + 9] + a[offa + 24];
        var x2r = wn4r * (x0r - x0i);
        var x2i = wn4r * (x0i + x0r);
        var y0r = x1r + x2r;
        var y0i = x1i + x2i;
        var y4r = x1r - x2r;
        var y4i = x1i - x2i;
        x1r = a[offa] + a[offa + 17];
        x1i = a[offa + 1] - a[offa + 16];
        x0r = a[offa + 8] + a[offa + 25];
        x0i = a[offa + 9] - a[offa + 24];
        x2r = wn4r * (x0r - x0i);
        x2i = wn4r * (x0i + x0r);
        var y8r = x1r - x2i;
        var y8i = x1i + x2r;
        var y12r = x1r + x2i;
        var y12i = x1i - x2r;
        x0r = a[offa + 2] - a[offa + 19];
        x0i = a[offa + 3] + a[offa + 18];
        x1r = wk1r * x0r - wk1i * x0i;
        x1i = wk1r * x0i + wk1i * x0r;
        x0r = a[offa + 10] - a[offa + 27];
        x0i = a[offa + 11] + a[offa + 26];
        x2r = wk3i * x0r - wk3r * x0i;
        x2i = wk3i * x0i + wk3r * x0r;
        var y1r = x1r + x2r;
        var y1i = x1i + x2i;
        var y5r = x1r - x2r;
        var y5i = x1i - x2i;
        x0r = a[offa + 2] + a[offa + 19];
        x0i = a[offa + 3] - a[offa + 18];
        x1r = wk3r * x0r - wk3i * x0i;
        x1i = wk3r * x0i + wk3i * x0r;
        x0r = a[offa + 10] + a[offa + 27];
        x0i = a[offa + 11] - a[offa + 26];
        x2r = wk1r * x0r + wk1i * x0i;
        x2i = wk1r * x0i - wk1i * x0r;
        var y9r = x1r - x2r;
        var y9i = x1i - x2i;
        var y13r = x1r + x2r;
        var y13i = x1i + x2i;
        x0r = a[offa + 4] - a[offa + 21];
        x0i = a[offa + 5] + a[offa + 20];
        x1r = wk2r * x0r - wk2i * x0i;
        x1i = wk2r * x0i + wk2i * x0r;
        x0r = a[offa + 12] - a[offa + 29];
        x0i = a[offa + 13] + a[offa + 28];
        x2r = wk2i * x0r - wk2r * x0i;
        x2i = wk2i * x0i + wk2r * x0r;
        var y2r = x1r + x2r;
        var y2i = x1i + x2i;
        var y6r = x1r - x2r;
        var y6i = x1i - x2i;
        x0r = a[offa + 4] + a[offa + 21];
        x0i = a[offa + 5] - a[offa + 20];
        x1r = wk2i * x0r - wk2r * x0i;
        x1i = wk2i * x0i + wk2r * x0r;
        x0r = a[offa + 12] + a[offa + 29];
        x0i = a[offa + 13] - a[offa + 28];
        x2r = wk2r * x0r - wk2i * x0i;
        x2i = wk2r * x0i + wk2i * x0r;
        var y10r = x1r - x2r;
        var y10i = x1i - x2i;
        var y14r = x1r + x2r;
        var y14i = x1i + x2i;
        x0r = a[offa + 6] - a[offa + 23];
        x0i = a[offa + 7] + a[offa + 22];
        x1r = wk3r * x0r - wk3i * x0i;
        x1i = wk3r * x0i + wk3i * x0r;
        x0r = a[offa + 14] - a[offa + 31];
        x0i = a[offa + 15] + a[offa + 30];
        x2r = wk1i * x0r - wk1r * x0i;
        x2i = wk1i * x0i + wk1r * x0r;
        var y3r = x1r + x2r;
        var y3i = x1i + x2i;
        var y7r = x1r - x2r;
        var y7i = x1i - x2i;
        x0r = a[offa + 6] + a[offa + 23];
        x0i = a[offa + 7] - a[offa + 22];
        x1r = wk1i * x0r + wk1r * x0i;
        x1i = wk1i * x0i - wk1r * x0r;
        x0r = a[offa + 14] + a[offa + 31];
        x0i = a[offa + 15] - a[offa + 30];
        x2r = wk3i * x0r - wk3r * x0i;
        x2i = wk3i * x0i + wk3r * x0r;
        var y11r = x1r + x2r;
        var y11i = x1i + x2i;
        var y15r = x1r - x2r;
        var y15i = x1i - x2i;
        x1r = y0r + y2r;
        x1i = y0i + y2i;
        x2r = y1r + y3r;
        x2i = y1i + y3i;
        a[offa] = x1r + x2r;
        a[offa + 1] = x1i + x2i;
        a[offa + 2] = x1r - x2r;
        a[offa + 3] = x1i - x2i;
        x1r = y0r - y2r;
        x1i = y0i - y2i;
        x2r = y1r - y3r;
        x2i = y1i - y3i;
        a[offa + 4] = x1r - x2i;
        a[offa + 5] = x1i + x2r;
        a[offa + 6] = x1r + x2i;
        a[offa + 7] = x1i - x2r;
        x1r = y4r - y6i;
        x1i = y4i + y6r;
        x0r = y5r - y7i;
        x0i = y5i + y7r;
        x2r = wn4r * (x0r - x0i);
        x2i = wn4r * (x0i + x0r);
        a[offa + 8] = x1r + x2r;
        a[offa + 9] = x1i + x2i;
        a[offa + 10] = x1r - x2r;
        a[offa + 11] = x1i - x2i;
        x1r = y4r + y6i;
        x1i = y4i - y6r;
        x0r = y5r + y7i;
        x0i = y5i - y7r;
        x2r = wn4r * (x0r - x0i);
        x2i = wn4r * (x0i + x0r);
        a[offa + 12] = x1r - x2i;
        a[offa + 13] = x1i + x2r;
        a[offa + 14] = x1r + x2i;
        a[offa + 15] = x1i - x2r;
        x1r = y8r + y10r;
        x1i = y8i + y10i;
        x2r = y9r - y11r;
        x2i = y9i - y11i;
        a[offa + 16] = x1r + x2r;
        a[offa + 17] = x1i + x2i;
        a[offa + 18] = x1r - x2r;
        a[offa + 19] = x1i - x2i;
        x1r = y8r - y10r;
        x1i = y8i - y10i;
        x2r = y9r + y11r;
        x2i = y9i + y11i;
        a[offa + 20] = x1r - x2i;
        a[offa + 21] = x1i + x2r;
        a[offa + 22] = x1r + x2i;
        a[offa + 23] = x1i - x2r;
        x1r = y12r - y14i;
        x1i = y12i + y14r;
        x0r = y13r + y15i;
        x0i = y13i - y15r;
        x2r = wn4r * (x0r - x0i);
        x2i = wn4r * (x0i + x0r);
        a[offa + 24] = x1r + x2r;
        a[offa + 25] = x1i + x2i;
        a[offa + 26] = x1r - x2r;
        a[offa + 27] = x1i - x2i;
        x1r = y12r + y14i;
        x1i = y12i - y14r;
        x0r = y13r - y15i;
        x0i = y13i + y15r;
        x2r = wn4r * (x0r - x0i);
        x2i = wn4r * (x0i + x0r);
        a[offa + 28] = x1r - x2i;
        a[offa + 29] = x1i + x2r;
        a[offa + 30] = x1r + x2i;
        a[offa + 31] = x1i - x2r;
    }

    private static void cftf081(float[] a, int offa, float[] w, int startw) {

        var wn4r = w[startw + 1];
        var x0r = a[offa] + a[offa + 8];
        var x0i = a[offa + 1] + a[offa + 9];
        var x1r = a[offa] - a[offa + 8];
        var x1i = a[offa + 1] - a[offa + 9];
        var x2r = a[offa + 4] + a[offa + 12];
        var x2i = a[offa + 5] + a[offa + 13];
        var x3r = a[offa + 4] - a[offa + 12];
        var x3i = a[offa + 5] - a[offa + 13];
        var y0r = x0r + x2r;
        var y0i = x0i + x2i;
        var y2r = x0r - x2r;
        var y2i = x0i - x2i;
        var y1r = x1r - x3i;
        var y1i = x1i + x3r;
        var y3r = x1r + x3i;
        var y3i = x1i - x3r;
        x0r = a[offa + 2] + a[offa + 10];
        x0i = a[offa + 3] + a[offa + 11];
        x1r = a[offa + 2] - a[offa + 10];
        x1i = a[offa + 3] - a[offa + 11];
        x2r = a[offa + 6] + a[offa + 14];
        x2i = a[offa + 7] + a[offa + 15];
        x3r = a[offa + 6] - a[offa + 14];
        x3i = a[offa + 7] - a[offa + 15];
        var y4r = x0r + x2r;
        var y4i = x0i + x2i;
        var y6r = x0r - x2r;
        var y6i = x0i - x2i;
        x0r = x1r - x3i;
        x0i = x1i + x3r;
        x2r = x1r + x3i;
        x2i = x1i - x3r;
        var y5r = wn4r * (x0r - x0i);
        var y5i = wn4r * (x0r + x0i);
        var y7r = wn4r * (x2r - x2i);
        var y7i = wn4r * (x2r + x2i);
        a[offa + 8] = y1r + y5r;
        a[offa + 9] = y1i + y5i;
        a[offa + 10] = y1r - y5r;
        a[offa + 11] = y1i - y5i;
        a[offa + 12] = y3r - y7i;
        a[offa + 13] = y3i + y7r;
        a[offa + 14] = y3r + y7i;
        a[offa + 15] = y3i - y7r;
        a[offa] = y0r + y4r;
        a[offa + 1] = y0i + y4i;
        a[offa + 2] = y0r - y4r;
        a[offa + 3] = y0i - y4i;
        a[offa + 4] = y2r - y6i;
        a[offa + 5] = y2i + y6r;
        a[offa + 6] = y2r + y6i;
        a[offa + 7] = y2i - y6r;
    }

    private static void cftf082(float[] a, int offa, float[] w, int startw) {

        var wn4r = w[startw + 1];
        var wk1r = w[startw + 2];
        var wk1i = w[startw + 3];
        var y0r = a[offa] - a[offa + 9];
        var y0i = a[offa + 1] + a[offa + 8];
        var y1r = a[offa] + a[offa + 9];
        var y1i = a[offa + 1] - a[offa + 8];
        var x0r = a[offa + 4] - a[offa + 13];
        var x0i = a[offa + 5] + a[offa + 12];
        var y2r = wn4r * (x0r - x0i);
        var y2i = wn4r * (x0i + x0r);
        x0r = a[offa + 4] + a[offa + 13];
        x0i = a[offa + 5] - a[offa + 12];
        var y3r = wn4r * (x0r - x0i);
        var y3i = wn4r * (x0i + x0r);
        x0r = a[offa + 2] - a[offa + 11];
        x0i = a[offa + 3] + a[offa + 10];
        var y4r = wk1r * x0r - wk1i * x0i;
        var y4i = wk1r * x0i + wk1i * x0r;
        x0r = a[offa + 2] + a[offa + 11];
        x0i = a[offa + 3] - a[offa + 10];
        var y5r = wk1i * x0r - wk1r * x0i;
        var y5i = wk1i * x0i + wk1r * x0r;
        x0r = a[offa + 6] - a[offa + 15];
        x0i = a[offa + 7] + a[offa + 14];
        var y6r = wk1i * x0r - wk1r * x0i;
        var y6i = wk1i * x0i + wk1r * x0r;
        x0r = a[offa + 6] + a[offa + 15];
        x0i = a[offa + 7] - a[offa + 14];
        var y7r = wk1r * x0r - wk1i * x0i;
        var y7i = wk1r * x0i + wk1i * x0r;
        x0r = y0r + y2r;
        x0i = y0i + y2i;
        var x1r = y4r + y6r;
        a[offa] = x0r + x1r;
        var x1i = y4i + y6i;
        a[offa + 1] = x0i + x1i;
        a[offa + 2] = x0r - x1r;
        a[offa + 3] = x0i - x1i;
        x0r = y0r - y2r;
        x0i = y0i - y2i;
        x1r = y4r - y6r;
        x1i = y4i - y6i;
        a[offa + 4] = x0r - x1i;
        a[offa + 5] = x0i + x1r;
        a[offa + 6] = x0r + x1i;
        a[offa + 7] = x0i - x1r;
        x0r = y1r - y3i;
        x0i = y1i + y3r;
        x1r = y5r - y7r;
        x1i = y5i - y7i;
        a[offa + 8] = x0r + x1r;
        a[offa + 9] = x0i + x1i;
        a[offa + 10] = x0r - x1r;
        a[offa + 11] = x0i - x1i;
        x0r = y1r + y3i;
        x0i = y1i - y3r;
        x1r = y5r + y7r;
        x1i = y5i + y7i;
        a[offa + 12] = x0r - x1i;
        a[offa + 13] = x0i + x1r;
        a[offa + 14] = x0r + x1i;
        a[offa + 15] = x0i - x1r;
    }

    private static void cftf040(float[] a, int offa) {

        var x0r = a[offa] + a[offa + 4];
        var x0i = a[offa + 1] + a[offa + 5];
        var x1r = a[offa] - a[offa + 4];
        var x1i = a[offa + 1] - a[offa + 5];
        var x2r = a[offa + 2] + a[offa + 6];
        var x2i = a[offa + 3] + a[offa + 7];
        var x3r = a[offa + 2] - a[offa + 6];
        var x3i = a[offa + 3] - a[offa + 7];
        a[offa] = x0r + x2r;
        a[offa + 1] = x0i + x2i;
        a[offa + 2] = x1r - x3i;
        a[offa + 3] = x1i + x3r;
        a[offa + 4] = x0r - x2r;
        a[offa + 5] = x0i - x2i;
        a[offa + 6] = x1r + x3i;
        a[offa + 7] = x1i - x3r;
    }

    private static void cftb040(float[] a, int offa) {

        var x0r = a[offa] + a[offa + 4];
        var x0i = a[offa + 1] + a[offa + 5];
        var x1r = a[offa] - a[offa + 4];
        var x1i = a[offa + 1] - a[offa + 5];
        var x2r = a[offa + 2] + a[offa + 6];
        var x2i = a[offa + 3] + a[offa + 7];
        var x3r = a[offa + 2] - a[offa + 6];
        var x3i = a[offa + 3] - a[offa + 7];
        a[offa] = x0r + x2r;
        a[offa + 1] = x0i + x2i;
        a[offa + 2] = x1r + x3i;
        a[offa + 3] = x1i - x3r;
        a[offa + 4] = x0r - x2r;
        a[offa + 5] = x0i - x2i;
        a[offa + 6] = x1r - x3i;
        a[offa + 7] = x1i + x3r;
    }

    private static void cftx020(float[] a, int offa) {
        var x0r = a[offa] - a[offa + 2];
        var x0i = -a[offa + 1] + a[offa + 3];
        a[offa] += a[offa + 2];
        a[offa + 1] += a[offa + 3];
        a[offa + 2] = x0r;
        a[offa + 3] = x0i;
    }

    private static void cftxb020(float[] a, int offa) {

        var x0r = a[offa] - a[offa + 2];
        var x0i = a[offa + 1] - a[offa + 3];
        a[offa] += a[offa + 2];
        a[offa + 1] += a[offa + 3];
        a[offa + 2] = x0r;
        a[offa + 3] = x0i;
    }

    private static void cftxc020(float[] a, int offa) {
        var x0r = a[offa] - a[offa + 2];
        var x0i = a[offa + 1] + a[offa + 3];
        a[offa] += a[offa + 2];
        a[offa + 1] -= a[offa + 3];
        a[offa + 2] = x0r;
        a[offa + 3] = x0i;
    }

    private static void rftfsub(int n, float[] a, int offa, int nc, float[] c, int startc) {

        var m = n >> 1;
        var ks = 2 * nc / m;
        var kk = 0;
        for (var j = 2; j < m; j += 2) {
            var k = n - j;
            kk += ks;
            var wkr = (float) (0.5 - c[startc + nc - kk]);
            var wki = c[startc + kk];
            var idx1 = offa + j;
            var idx2 = offa + k;
            var xr = a[idx1] - a[idx2];
            var xi = a[idx1 + 1] + a[idx2 + 1];
            var yr = wkr * xr - wki * xi;
            a[idx1] -= yr;
            var yi = wkr * xi + wki * xr;
            a[idx1 + 1] = yi - a[idx1 + 1];
            a[idx2] += yr;
            a[idx2 + 1] = yi - a[idx2 + 1];
        }
        a[offa + m + 1] = -a[offa + m + 1];
    }

    private static void rftbsub(int n, float[] a, int offa, int nc, float[] c, int startc) {

        var m = n >> 1;
        var ks = 2 * nc / m;
        var kk = 0;
        for (var j = 2; j < m; j += 2) {
            var k = n - j;
            kk += ks;
            var wkr = (float) (0.5 - c[startc + nc - kk]);
            var wki = c[startc + kk];
            var idx1 = offa + j;
            var idx2 = offa + k;
            var xr = a[idx1] - a[idx2];
            var xi = a[idx1 + 1] + a[idx2 + 1];
            var yr = wkr * xr - wki * xi;
            a[idx1] -= yr;
            var yi = wkr * xi + wki * xr;
            a[idx1 + 1] -= yi;
            a[idx2] += yr;
            a[idx2 + 1] -= yi;
        }
    }

    /**
     * Computes 1D forward DFT of complex data leaving the result in
     * <code>a</code>. Complex number is stored as two float values in
     * sequence: the real and imaginary part, i.e. the size of the input array
     * must be greater or equal 2*n. The physical layout of the input data has
     * to be as follows:<br>
     *
     * <pre>
     * a[2*k] = Re[k],
     * a[2*k+1] = Im[k], 0&lt;=k&lt;n
     * </pre>
     *
     * @param a data to transform
     */
    public void complexForward(float[] a) {
        complexForward(a, 0);
    }

    /**
     * Computes 1D forward DFT of complex data leaving the result in
     * <code>a</code>. Complex number is stored as two float values in
     * sequence: the real and imaginary part, i.e. the size of the input array
     * must be greater or equal 2*n. The physical layout of the input data has
     * to be as follows:<br>
     *
     * <pre>
     * a[offa+2*k] = Re[k],
     * a[offa+2*k+1] = Im[k], 0&lt;=k&lt;n
     * </pre>
     *
     * @param a    data to transform
     * @param offa index of the first element in array <code>a</code>
     */
    private void complexForward(float[] a, int offa) {
        if (n == 1)
            return;
        switch (plan) {
            case SPLIT_RADIX:
                cftbsub(2 * n, a, offa, ip, nw, w);
                break;
            case MIXED_RADIX:
                cfftf(a, offa, -1);
                break;
            case BLUESTEIN:
                bluestein_complex(a, offa, -1);
                break;
        }
    }

    /**
     * Computes 1D inverse DFT of complex data leaving the result in
     * <code>a</code>. Complex number is stored as two float values in
     * sequence: the real and imaginary part, i.e. the size of the input array
     * must be greater or equal 2*n. The physical layout of the input data has
     * to be as follows:<br>
     *
     * <pre>
     * a[2*k] = Re[k],
     * a[2*k+1] = Im[k], 0&lt;=k&lt;n
     * </pre>
     *
     * @param a     data to transform
     * @param scale if true then scaling is performed
     */
    public void complexInverse(float[] a, boolean scale) {
        complexInverse(a, 0, scale);
    }

    /**
     * Computes 1D inverse DFT of complex data leaving the result in
     * <code>a</code>. Complex number is stored as two float values in
     * sequence: the real and imaginary part, i.e. the size of the input array
     * must be greater or equal 2*n. The physical layout of the input data has
     * to be as follows:<br>
     *
     * <pre>
     * a[offa+2*k] = Re[k],
     * a[offa+2*k+1] = Im[k], 0&lt;=k&lt;n
     * </pre>
     *
     * @param a     data to transform
     * @param offa  index of the first element in array <code>a</code>
     * @param scale if true then scaling is performed
     */
    private void complexInverse(float[] a, int offa, boolean scale) {
        if (n == 1)
            return;
        switch (plan) {
            case SPLIT_RADIX:
                cftfsub(2 * n, a, offa, ip, nw, w);
                break;
            case MIXED_RADIX:
                cfftf(a, offa, +1);
                break;
            case BLUESTEIN:
                bluestein_complex(a, offa, 1);
                break;
        }
        if (scale) {
            scale(n, a, offa, true);
        }
    }

    /**
     * Computes 1D forward DFT of real data leaving the result in <code>a</code>
     * . The physical layout of the output data is as follows:<br>
     * <p>
     * if n is even then
     *
     * <pre>
     * a[2*k] = Re[k], 0&lt;=k&lt;n/2
     * a[2*k+1] = Im[k], 0&lt;k&lt;n/2
     * a[1] = Re[n/2]
     * </pre>
     * <p>
     * if n is odd then
     *
     * <pre>
     * a[2*k] = Re[k], 0&lt;=k&lt;(n+1)/2
     * a[2*k+1] = Im[k], 0&lt;k&lt;(n-1)/2
     * a[1] = Im[(n-1)/2]
     * </pre>
     * <p>
     * This method computes only half of the elements of the real transform. The
     * other half satisfies the symmetry condition. If you want the full real
     * forward transform, use <code>realForwardFull</code>. To get back the
     * original data, use <code>realInverse</code> on the output of this method.
     *
     * @param a data to transform
     */
    public void realForward(float[] a) {
        realForward(a, 0);
    }

    /**
     * Computes 1D forward DFT of real data leaving the result in <code>a</code>
     * . The physical layout of the output data is as follows:<br>
     * <p>
     * if n is even then
     *
     * <pre>
     * a[offa+2*k] = Re[k], 0&lt;=k&lt;n/2
     * a[offa+2*k+1] = Im[k], 0&lt;k&lt;n/2
     * a[offa+1] = Re[n/2]
     * </pre>
     * <p>
     * if n is odd then
     *
     * <pre>
     * a[offa+2*k] = Re[k], 0&lt;=k&lt;(n+1)/2
     * a[offa+2*k+1] = Im[k], 0&lt;k&lt;(n-1)/2
     * a[offa+1] = Im[(n-1)/2]
     * </pre>
     * <p>
     * This method computes only half of the elements of the real transform. The
     * other half satisfies the symmetry condition. If you want the full real
     * forward transform, use <code>realForwardFull</code>. To get back the
     * original data, use <code>realInverse</code> on the output of this method.
     *
     * @param a    data to transform
     * @param offa index of the first element in array <code>a</code>
     */
    private void realForward(float[] a, int offa) {
        if (n == 1)
            return;

        switch (plan) {
            case SPLIT_RADIX:

                if (n > 4) {
                    cftfsub(n, a, offa, ip, nw, w);
                    rftfsub(n, a, offa, nc, w, nw);
                } else if (n == 4) {
                    cftx020(a, offa);
                }
                var xi = a[offa] - a[offa + 1];
                a[offa] += a[offa + 1];
                a[offa + 1] = xi;
                break;
            case MIXED_RADIX:
                rfftf(a, offa);
                for (var k = n - 1; k >= 2; k--) {
                    var idx = offa + k;
                    var tmp = a[idx];
                    a[idx] = a[idx - 1];
                    a[idx - 1] = tmp;
                }
                break;
            case BLUESTEIN:
                bluestein_real_forward(a, offa);
                break;
        }
    }

    /**
     * Computes 1D forward DFT of real data leaving the result in <code>a</code>
     * . This method computes the full real forward transform, i.e. you will get
     * the same result as from <code>complexForward</code> called with all
     * imaginary parts equal 0. Because the result is stored in <code>a</code>,
     * the size of the input array must greater or equal 2*n, with only the
     * first n elements filled with real data. To get back the original data,
     * use <code>complexInverse</code> on the output of this method.
     *
     * @param a data to transform
     */
    public void realForwardFull(float[] a) {
        realForwardFull(a, 0);
    }

    /**
     * Computes 1D forward DFT of real data leaving the result in <code>a</code>
     * . This method computes the full real forward transform, i.e. you will get
     * the same result as from <code>complexForward</code> called with all
     * imaginary part equal 0. Because the result is stored in <code>a</code>,
     * the size of the input array must greater or equal 2*n, with only the
     * first n elements filled with real data. To get back the original data,
     * use <code>complexInverse</code> on the output of this method.
     *
     * @param a    data to transform
     * @param offa index of the first element in array <code>a</code>
     */
    private void realForwardFull(float[] a, int offa) {

        var twon = 2 * n;
        switch (plan) {
            case SPLIT_RADIX:
                realForward(a, offa);
                var nthreads = ConcurrencyUtils.getNumberOfThreads();
                if ((nthreads > 1) && (n / 2 > ConcurrencyUtils.getThreadsBeginN_1D_FFT_2Threads())) {
                    Future<?>[] futures = new Future[nthreads];
                    var k = n / 2 / nthreads;
                    for (var i = 0; i < nthreads; i++) {
                        var firstIdx = i * k;
                        var lastIdx = (i == (nthreads - 1)) ? n / 2 : firstIdx + k;
                        futures[i] = ConcurrencyUtils.submit(() -> {
                            for (var k1 = firstIdx; k1 < lastIdx; k1++) {
                                var idx1 = 2 * k1;
                                var idx2 = offa + ((twon - idx1) % twon);
                                a[idx2] = a[offa + idx1];
                                a[idx2 + 1] = -a[offa + idx1 + 1];
                            }
                        });
                    }
                    ConcurrencyUtils.waitForCompletion(futures);
                } else {
                    for (var k = 0; k < n / 2; k++) {
                        var idx1 = 2 * k;
                        var idx2 = offa + ((twon - idx1) % twon);
                        a[idx2] = a[offa + idx1];
                        a[idx2 + 1] = -a[offa + idx1 + 1];
                    }
                }
                a[offa + n] = -a[offa + 1];
                a[offa + 1] = 0;
                break;
            case MIXED_RADIX:
                rfftf(a, offa);
                int m;
                if (n % 2 == 0) {
                    m = n / 2;
                } else {
                    m = (n + 1) / 2;
                }
                for (var k = 1; k < m; k++) {
                    var idx1 = offa + twon - 2 * k;
                    var idx2 = offa + 2 * k;
                    a[idx1 + 1] = -a[idx2];
                    a[idx1] = a[idx2 - 1];
                }
                for (var k = 1; k < n; k++) {
                    var idx = offa + n - k;
                    var tmp = a[idx + 1];
                    a[idx + 1] = a[idx];
                    a[idx] = tmp;
                }
                a[offa + 1] = 0;
                break;
            case BLUESTEIN:
                bluestein_real_full(a, offa, -1);
                break;
        }
    }

    /**
     * Computes 1D inverse DFT of real data leaving the result in <code>a</code>
     * . The physical layout of the input data has to be as follows:<br>
     * <p>
     * if n is even then
     *
     * <pre>
     * a[2*k] = Re[k], 0&lt;=k&lt;n/2
     * a[2*k+1] = Im[k], 0&lt;k&lt;n/2
     * a[1] = Re[n/2]
     * </pre>
     * <p>
     * if n is odd then
     *
     * <pre>
     * a[2*k] = Re[k], 0&lt;=k&lt;(n+1)/2
     * a[2*k+1] = Im[k], 0&lt;k&lt;(n-1)/2
     * a[1] = Im[(n-1)/2]
     * </pre>
     * <p>
     * This method computes only half of the elements of the real transform. The
     * other half satisfies the symmetry condition. If you want the full real
     * inverse transform, use <code>realInverseFull</code>.
     *
     * @param a     data to transform
     * @param scale if true then scaling is performed
     */
    public void realInverse(float[] a, boolean scale) {
        realInverse(a, 0, scale);
    }

    /**
     * Computes 1D inverse DFT of real data leaving the result in <code>a</code>
     * . The physical layout of the input data has to be as follows:<br>
     * <p>
     * if n is even then
     *
     * <pre>
     * a[offa+2*k] = Re[k], 0&lt;=k&lt;n/2
     * a[offa+2*k+1] = Im[k], 0&lt;k&lt;n/2
     * a[offa+1] = Re[n/2]
     * </pre>
     * <p>
     * if n is odd then
     *
     * <pre>
     * a[offa+2*k] = Re[k], 0&lt;=k&lt;(n+1)/2
     * a[offa+2*k+1] = Im[k], 0&lt;k&lt;(n-1)/2
     * a[offa+1] = Im[(n-1)/2]
     * </pre>
     * <p>
     * This method computes only half of the elements of the real transform. The
     * other half satisfies the symmetry condition. If you want the full real
     * inverse transform, use <code>realInverseFull</code>.
     *
     * @param a     data to transform
     * @param offa  index of the first element in array <code>a</code>
     * @param scale if true then scaling is performed
     */
    private void realInverse(float[] a, int offa, boolean scale) {
        if (n == 1)
            return;
        switch (plan) {
            case SPLIT_RADIX:
                a[offa + 1] = (float) (0.5 * (a[offa] - a[offa + 1]));
                a[offa] -= a[offa + 1];
                if (n > 4) {
                    rftfsub(n, a, offa, nc, w, nw);
                    cftbsub(n, a, offa, ip, nw, w);
                } else if (n == 4) {
                    cftxc020(a, offa);
                }
                if (scale) {
                    scale(n / 2, a, offa, false);
                }
                break;
            case MIXED_RADIX:
                for (var k = 2; k < n; k++) {
                    var idx = offa + k;
                    var tmp = a[idx - 1];
                    a[idx - 1] = a[idx];
                    a[idx] = tmp;
                }
                rfftb(a, offa);
                if (scale) {
                    scale(n, a, offa, false);
                }
                break;
            case BLUESTEIN:
                bluestein_real_inverse(a, offa);
                if (scale) {
                    scale(n, a, offa, false);
                }
                break;
        }

    }

    /**
     * Computes 1D inverse DFT of real data leaving the result in <code>a</code>
     * . This method computes the full real inverse transform, i.e. you will get
     * the same result as from <code>complexInverse</code> called with all
     * imaginary part equal 0. Because the result is stored in <code>a</code>,
     * the size of the input array must greater or equal 2*n, with only the
     * first n elements filled with real data.
     *
     * @param a     data to transform
     * @param scale if true then scaling is performed
     */
    public void realInverseFull(float[] a, boolean scale) {
        realInverseFull(a, 0, scale);
    }

    /**
     * Computes 1D inverse DFT of real data leaving the result in <code>a</code>
     * . This method computes the full real inverse transform, i.e. you will get
     * the same result as from <code>complexInverse</code> called with all
     * imaginary part equal 0. Because the result is stored in <code>a</code>,
     * the size of the input array must greater or equal 2*n, with only the
     * first n elements filled with real data.
     *
     * @param a     data to transform
     * @param offa  index of the first element in array <code>a</code>
     * @param scale if true then scaling is performed
     */
    private void realInverseFull(float[] a, int offa, boolean scale) {
        var twon = 2 * n;
        switch (plan) {
            case SPLIT_RADIX:
                realInverse2(a, offa, scale);
                var nthreads = ConcurrencyUtils.getNumberOfThreads();
                if ((nthreads > 1) && (n / 2 > ConcurrencyUtils.getThreadsBeginN_1D_FFT_2Threads())) {
                    Future<?>[] futures = new Future[nthreads];
                    var k = n / 2 / nthreads;
                    for (var i = 0; i < nthreads; i++) {
                        var firstIdx = i * k;
                        var lastIdx = (i == (nthreads - 1)) ? n / 2 : firstIdx + k;
                        futures[i] = ConcurrencyUtils.submit(() -> {
                            for (var k1 = firstIdx; k1 < lastIdx; k1++) {
                                var idx1 = 2 * k1;
                                var idx2 = offa + ((twon - idx1) % twon);
                                a[idx2] = a[offa + idx1];
                                a[idx2 + 1] = -a[offa + idx1 + 1];
                            }
                        });
                    }
                    ConcurrencyUtils.waitForCompletion(futures);
                } else {
                    for (var k = 0; k < n / 2; k++) {
                        var idx1 = 2 * k;
                        var idx2 = offa + ((twon - idx1) % twon);
                        a[idx2] = a[offa + idx1];
                        a[idx2 + 1] = -a[offa + idx1 + 1];
                    }
                }
                a[offa + n] = -a[offa + 1];
                a[offa + 1] = 0;
                break;
            case MIXED_RADIX:
                rfftf(a, offa);
                if (scale) {
                    scale(n, a, offa, false);
                }
                int m;
                if (n % 2 == 0) {
                    m = n / 2;
                } else {
                    m = (n + 1) / 2;
                }
                for (var k = 1; k < m; k++) {
                    var idx1 = offa + 2 * k;
                    var idx2 = offa + twon - 2 * k;
                    a[idx1] = -a[idx1];
                    a[idx2 + 1] = -a[idx1];
                    a[idx2] = a[idx1 - 1];
                }
                for (var k = 1; k < n; k++) {
                    var idx = offa + n - k;
                    var tmp = a[idx + 1];
                    a[idx + 1] = a[idx];
                    a[idx] = tmp;
                }
                a[offa + 1] = 0;
                break;
            case BLUESTEIN:
                bluestein_real_full(a, offa, 1);
                if (scale) {
                    scale(n, a, offa, true);
                }
                break;
        }
    }

    private void realInverse2(float[] a, int offa, boolean scale) {
        if (n == 1)
            return;
        switch (plan) {
            case SPLIT_RADIX:

                if (n > 4) {
                    cftfsub(n, a, offa, ip, nw, w);
                    rftbsub(n, a, offa, nc, w, nw);
                } else if (n == 4) {
                    cftbsub(n, a, offa, ip, nw, w);
                }
                var xi = a[offa] - a[offa + 1];
                a[offa] += a[offa + 1];
                a[offa + 1] = xi;
                if (scale) {
                    scale(n, a, offa, false);
                }
                break;
            case MIXED_RADIX:
                rfftf(a, offa);
                for (var k = n - 1; k >= 2; k--) {
                    var idx = offa + k;
                    var tmp = a[idx];
                    a[idx] = a[idx - 1];
                    a[idx - 1] = tmp;
                }
                if (scale) {
                    scale(n, a, offa, false);
                }
                int m;
                if (n % 2 == 0) {
                    m = n / 2;
                    for (var i = 1; i < m; i++) {
                        var idx = offa + 2 * i + 1;
                        a[idx] = -a[idx];
                    }
                } else {
                    m = (n - 1) / 2;
                    for (var i = 0; i < m; i++) {
                        var idx = offa + 2 * i + 1;
                        a[idx] = -a[idx];
                    }
                }
                break;
            case BLUESTEIN:
                bluestein_real_inverse2(a, offa);
                if (scale) {
                    scale(n, a, offa, false);
                }
                break;
        }
    }

    void cffti(int n, int offw) {
        if (n == 1)
            return;

        var fourn = 4 * n;
        int ntry = 0, i;

        var nl = n;
        var nf = 0;
        var j = 0;

        factorize_loop:
        while (true) {
            j++;
            if (j <= 4)
                ntry = factors[j - 1];
            else
                ntry += 2;
            do {
                var nq = nl / ntry;
                var nr = nl - ntry * nq;
                if (nr != 0)
                    continue factorize_loop;
                nf++;
                wtable[offw + nf + 1 + fourn] = ntry;
                nl = nq;
                if (ntry == 2 && nf != 1) {
                    for (i = 2; i <= nf; i++) {
                        var ib = nf - i + 2;
                        var idx = ib + fourn;
                        wtable[offw + idx + 1] = wtable[offw + idx];
                    }
                    wtable[offw + 2 + fourn] = 2;
                }
            } while (nl != 1);
            break factorize_loop;
        }
        wtable[offw + fourn] = n;
        wtable[offw + 1 + fourn] = nf;
        var argh = TWO_PI / (float) n;
        i = 1;
        var l1 = 1;
        var twon = 2 * n;
        for (var k1 = 1; k1 <= nf; k1++) {
            var ip = (int) wtable[offw + k1 + 1 + fourn];
            var ld = 0;
            var l2 = l1 * ip;
            var ido = n / l2;
            var idot = ido + ido + 2;
            var ipm = ip - 1;
            for (j = 1; j <= ipm; j++) {
                var i1 = i;
                wtable[offw + i - 1 + twon] = 1;
                wtable[offw + i + twon] = 0;
                ld += l1;
                float fi = 0;
                var argld = ld * argh;
                for (var ii = 4; ii <= idot; ii += 2) {
                    i += 2;
                    fi += 1;
                    var arg = fi * argld;
                    var idx = i + twon;
                    wtable[offw + idx - 1] = (float) Math.cos(arg);
                    wtable[offw + idx] = (float) Math.sin(arg);
                }
                if (ip > 5) {
                    var idx1 = i1 + twon;
                    var idx2 = i + twon;
                    wtable[offw + idx1 - 1] = wtable[offw + idx2 - 1];
                    wtable[offw + idx1] = wtable[offw + idx2];
                }
            }
            l1 = l2;
        }

    }

    private void cffti() {
        if (n == 1)
            return;

        var fourn = 4 * n;
        int ntry = 0, i;

        var nl = n;
        var nf = 0;
        var j = 0;

        factorize_loop:
        while (true) {
            j++;
            if (j <= 4)
                ntry = factors[j - 1];
            else
                ntry += 2;
            do {
                var nq = nl / ntry;
                var nr = nl - ntry * nq;
                if (nr != 0)
                    continue factorize_loop;
                nf++;
                wtable[nf + 1 + fourn] = ntry;
                nl = nq;
                if (ntry == 2 && nf != 1) {
                    for (i = 2; i <= nf; i++) {
                        var ib = nf - i + 2;
                        var idx = ib + fourn;
                        wtable[idx + 1] = wtable[idx];
                    }
                    wtable[2 + fourn] = 2;
                }
            } while (nl != 1);
            break factorize_loop;
        }
        wtable[fourn] = n;
        wtable[1 + fourn] = nf;
        var argh = TWO_PI / (float) n;
        i = 1;
        var l1 = 1;
        var twon = 2 * n;
        for (var k1 = 1; k1 <= nf; k1++) {
            var ip = (int) wtable[k1 + 1 + fourn];
            var ld = 0;
            var l2 = l1 * ip;
            var ido = n / l2;
            var idot = ido + ido + 2;
            var ipm = ip - 1;
            for (j = 1; j <= ipm; j++) {
                var i1 = i;
                wtable[i - 1 + twon] = 1;
                wtable[i + twon] = 0;
                ld += l1;
                float fi = 0;
                var argld = ld * argh;
                for (var ii = 4; ii <= idot; ii += 2) {
                    i += 2;
                    fi += 1;
                    var arg = fi * argld;
                    var idx = i + twon;
                    wtable[idx - 1] = (float) Math.cos(arg);
                    wtable[idx] = (float) Math.sin(arg);
                }
                if (ip > 5) {
                    var idx1 = i1 + twon;
                    var idx2 = i + twon;
                    wtable[idx1 - 1] = wtable[idx2 - 1];
                    wtable[idx1] = wtable[idx2];
                }
            }
            l1 = l2;
        }

    }

    /*----------------------------------------------------------------------
       passf2: Complex FFT's forward/backward processing of factor 2;
       isign is +1 for backward and -1 for forward transforms
      ----------------------------------------------------------------------*/

    private void rffti() {

        if (n == 1)
            return;
        var twon = 2 * n;
        int ntry = 0, i;

        var nl = n;
        var nf = 0;
        var j = 0;

        factorize_loop:
        while (true) {
            ++j;
            if (j <= 4)
                ntry = factors[j - 1];
            else
                ntry += 2;
            do {
                var nq = nl / ntry;
                var nr = nl - ntry * nq;
                if (nr != 0)
                    continue factorize_loop;
                ++nf;
                wtable_r[nf + 1 + twon] = ntry;

                nl = nq;
                if (ntry == 2 && nf != 1) {
                    for (i = 2; i <= nf; i++) {
                        var ib = nf - i + 2;
                        var idx = ib + twon;
                        wtable_r[idx + 1] = wtable_r[idx];
                    }
                    wtable_r[2 + twon] = 2;
                }
            } while (nl != 1);
            break factorize_loop;
        }
        wtable_r[twon] = n;
        wtable_r[1 + twon] = nf;
        var argh = TWO_PI / (float) (n);
        var nfm1 = nf - 1;
        if (nfm1 == 0)
            return;
        var l1 = 1;
        var is = 0;
        for (var k1 = 1; k1 <= nfm1; k1++) {
            var ip = (int) wtable_r[k1 + 1 + twon];
            var ld = 0;
            var l2 = l1 * ip;
            var ido = n / l2;
            var ipm = ip - 1;
            for (j = 1; j <= ipm; ++j) {
                ld += l1;
                i = is;
                var argld = (float) ld * argh;

                float fi = 0;
                for (var ii = 3; ii <= ido; ii += 2) {
                    i += 2;
                    fi += 1;
                    var arg = fi * argld;
                    var idx = i + n;
                    wtable_r[idx - 2] = (float) Math.cos(arg);
                    wtable_r[idx - 1] = (float) Math.sin(arg);
                }
                is += ido;
            }
            l1 = l2;
        }
    }

    private void bluesteini() {
        bk1[0] = 1;
        bk1[1] = 0;
        var pi_n = PI / n;
        var k = 0;
        for (var i = 1; i < n; i++) {
            k += 2 * i - 1;
            if (k >= 2 * n)
                k -= 2 * n;
            var arg = pi_n * k;
            bk1[2 * i] = (float) Math.cos(arg);
            bk1[2 * i + 1] = (float) Math.sin(arg);
        }
        var scale = (float) (1.0 / nBluestein);
        bk2[0] = bk1[0] * scale;
        bk2[1] = bk1[1] * scale;
        for (var i = 2; i < 2 * n; i += 2) {
            bk2[i] = bk1[i] * scale;
            bk2[i + 1] = bk1[i + 1] * scale;
            bk2[2 * nBluestein - i] = bk2[i];
            bk2[2 * nBluestein - i + 1] = bk2[i + 1];
        }
        cftbsub(2 * nBluestein, bk2, 0, ip, nw, w);
    }

    private void makewt(int nw) {

        ip[0] = nw;
        ip[1] = 1;
        if (nw > 2) {
            var nwh = nw >> 1;
            var delta = (float) (0.785398163397448278999490867136046290 / nwh);
            var wn4r = (float) Math.cos(delta * nwh);
            w[0] = 1;
            w[1] = wn4r;
            int j;
            var delta2 = delta * 2;
            if (nwh == 4) {
                w[2] = (float) Math.cos(delta2);
                w[3] = (float) Math.sin(delta2);
            } else if (nwh > 4) {
                makeipt(nw);
                w[2] = (float) (0.5 / Math.cos(delta2));
                w[3] = (float) (0.5 / Math.cos(delta * 6));
                for (j = 4; j < nwh; j += 4) {
                    var deltaj = delta * j;
                    w[j] = (float) Math.cos(deltaj);
                    w[j + 1] = (float) Math.sin(deltaj);
                    var deltaj3 = 3 * deltaj;
                    w[j + 2] = (float) Math.cos(deltaj3);
                    w[j + 3] = (float) -Math.sin(deltaj3);
                }
            }
            var nw0 = 0;
            while (nwh > 2) {
                var nw1 = nw0 + nwh;
                nwh >>= 1;
                w[nw1] = 1;
                w[nw1 + 1] = wn4r;
                float wk1i;
                float wk1r;
                if (nwh == 4) {
                    wk1r = w[nw0 + 4];
                    wk1i = w[nw0 + 5];
                    w[nw1 + 2] = wk1r;
                    w[nw1 + 3] = wk1i;
                } else if (nwh > 4) {
                    wk1r = w[nw0 + 4];
                    var wk3r = w[nw0 + 6];
                    w[nw1 + 2] = (float) (0.5 / wk1r);
                    w[nw1 + 3] = (float) (0.5 / wk3r);
                    for (j = 4; j < nwh; j += 4) {
                        var idx1 = nw0 + 2 * j;
                        var idx2 = nw1 + j;
                        wk1r = w[idx1];
                        wk1i = w[idx1 + 1];
                        wk3r = w[idx1 + 2];
                        var wk3i = w[idx1 + 3];
                        w[idx2] = wk1r;
                        w[idx2 + 1] = wk1i;
                        w[idx2 + 2] = wk3r;
                        w[idx2 + 3] = wk3i;
                    }
                }
                nw0 = nw1;
            }
        }
    }

    private void makeipt(int nw) {

        ip[2] = 0;
        ip[3] = 16;
        var m = 2;
        for (var l = nw; l > 32; l >>= 2) {
            var m2 = m << 1;
            var q = m2 << 3;
            for (var j = m; j < m2; j++) {
                var p = ip[j] << 2;
                ip[m + j] = p;
                ip[m2 + j] = p + q;
            }
            m = m2;
        }
    }

    private void makect(int nc, float[] c, int startc) {

        ip[1] = nc;
        if (nc > 1) {
            var nch = nc >> 1;
            var delta = (float) (0.785398163397448278999490867136046290 / nch);
            c[startc] = (float) Math.cos(delta * nch);
            c[startc + nch] = (float) (0.5 * c[startc]);
            for (var j = 1; j < nch; j++) {
                var deltaj = delta * j;
                c[startc + j] = (float) (0.5 * Math.cos(deltaj));
                c[startc + nc - j] = (float) (0.5 * Math.sin(deltaj));
            }
        }
    }

    private void bluestein_complex(float[] a, int offa, int isign) {
        var ak = new float[2 * nBluestein];
        var nthreads = ConcurrencyUtils.getNumberOfThreads();
        if ((nthreads > 1) && (n > ConcurrencyUtils.getThreadsBeginN_1D_FFT_2Threads())) {
            nthreads = 2;
            if ((nthreads >= 4) && (n > ConcurrencyUtils.getThreadsBeginN_1D_FFT_4Threads())) {
                nthreads = 4;
            }
            Future<?>[] futures = new Future[nthreads];
            var k = n / nthreads;
            for (var i = 0; i < nthreads; i++) {
                var firstIdx = i * k;
                var lastIdx = (i == (nthreads - 1)) ? n : firstIdx + k;
                futures[i] = ConcurrencyUtils.submit(() -> {
                    if (isign > 0) {
                        for (var i13 = firstIdx; i13 < lastIdx; i13++) {
                            var idx1 = 2 * i13;
                            var idx2 = idx1 + 1;
                            var idx3 = offa + idx1;
                            var idx4 = offa + idx2;
                            ak[idx1] = a[idx3] * bk1[idx1] - a[idx4] * bk1[idx2];
                            ak[idx2] = a[idx3] * bk1[idx2] + a[idx4] * bk1[idx1];
                        }
                    } else {
                        for (var i13 = firstIdx; i13 < lastIdx; i13++) {
                            var idx1 = 2 * i13;
                            var idx2 = idx1 + 1;
                            var idx3 = offa + idx1;
                            var idx4 = offa + idx2;
                            ak[idx1] = a[idx3] * bk1[idx1] + a[idx4] * bk1[idx2];
                            ak[idx2] = -a[idx3] * bk1[idx2] + a[idx4] * bk1[idx1];
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            cftbsub(2 * nBluestein, ak, 0, ip, nw, w);

            k = nBluestein / nthreads;
            for (var i = 0; i < nthreads; i++) {
                var firstIdx = i * k;
                var lastIdx = (i == (nthreads - 1)) ? nBluestein : firstIdx + k;
                futures[i] = ConcurrencyUtils.submit(() -> {
                    if (isign > 0) {
                        for (var i12 = firstIdx; i12 < lastIdx; i12++) {
                            var idx1 = 2 * i12;
                            var idx2 = idx1 + 1;
                            var im = -ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1];
                            ak[idx1] = ak[idx1] * bk2[idx1] + ak[idx2] * bk2[idx2];
                            ak[idx2] = im;
                        }
                    } else {
                        for (var i12 = firstIdx; i12 < lastIdx; i12++) {
                            var idx1 = 2 * i12;
                            var idx2 = idx1 + 1;
                            var im = ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1];
                            ak[idx1] = ak[idx1] * bk2[idx1] - ak[idx2] * bk2[idx2];
                            ak[idx2] = im;
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            cftfsub(2 * nBluestein, ak, 0, ip, nw, w);

            k = n / nthreads;
            for (var i = 0; i < nthreads; i++) {
                var firstIdx = i * k;
                var lastIdx = (i == (nthreads - 1)) ? n : firstIdx + k;
                futures[i] = ConcurrencyUtils.submit(() -> {
                    if (isign > 0) {
                        for (var i1 = firstIdx; i1 < lastIdx; i1++) {
                            var idx1 = 2 * i1;
                            var idx2 = idx1 + 1;
                            var idx3 = offa + idx1;
                            a[idx3] = bk1[idx1] * ak[idx1] - bk1[idx2] * ak[idx2];
                            var idx4 = offa + idx2;
                            a[idx4] = bk1[idx2] * ak[idx1] + bk1[idx1] * ak[idx2];
                        }
                    } else {
                        for (var i1 = firstIdx; i1 < lastIdx; i1++) {
                            var idx1 = 2 * i1;
                            var idx2 = idx1 + 1;
                            var idx3 = offa + idx1;
                            a[idx3] = bk1[idx1] * ak[idx1] + bk1[idx2] * ak[idx2];
                            var idx4 = offa + idx2;
                            a[idx4] = -bk1[idx2] * ak[idx1] + bk1[idx1] * ak[idx2];
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);
        } else {
            if (isign > 0) {
                for (var i = 0; i < n; i++) {
                    var idx1 = 2 * i;
                    var idx2 = idx1 + 1;
                    var idx3 = offa + idx1;
                    var idx4 = offa + idx2;
                    ak[idx1] = a[idx3] * bk1[idx1] - a[idx4] * bk1[idx2];
                    ak[idx2] = a[idx3] * bk1[idx2] + a[idx4] * bk1[idx1];
                }
            } else {
                for (var i = 0; i < n; i++) {
                    var idx1 = 2 * i;
                    var idx2 = idx1 + 1;
                    var idx3 = offa + idx1;
                    var idx4 = offa + idx2;
                    ak[idx1] = a[idx3] * bk1[idx1] + a[idx4] * bk1[idx2];
                    ak[idx2] = -a[idx3] * bk1[idx2] + a[idx4] * bk1[idx1];
                }
            }

            cftbsub(2 * nBluestein, ak, 0, ip, nw, w);

            if (isign > 0) {
                for (var i = 0; i < nBluestein; i++) {
                    var idx1 = 2 * i;
                    var idx2 = idx1 + 1;
                    var im = -ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1];
                    ak[idx1] = ak[idx1] * bk2[idx1] + ak[idx2] * bk2[idx2];
                    ak[idx2] = im;
                }
            } else {
                for (var i = 0; i < nBluestein; i++) {
                    var idx1 = 2 * i;
                    var idx2 = idx1 + 1;
                    var im = ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1];
                    ak[idx1] = ak[idx1] * bk2[idx1] - ak[idx2] * bk2[idx2];
                    ak[idx2] = im;
                }
            }

            cftfsub(2 * nBluestein, ak, 0, ip, nw, w);
            if (isign > 0) {
                for (var i = 0; i < n; i++) {
                    var idx1 = 2 * i;
                    var idx2 = idx1 + 1;
                    var idx3 = offa + idx1;
                    a[idx3] = bk1[idx1] * ak[idx1] - bk1[idx2] * ak[idx2];
                    var idx4 = offa + idx2;
                    a[idx4] = bk1[idx2] * ak[idx1] + bk1[idx1] * ak[idx2];
                }
            } else {
                for (var i = 0; i < n; i++) {
                    var idx1 = 2 * i;
                    var idx2 = idx1 + 1;
                    var idx3 = offa + idx1;
                    a[idx3] = bk1[idx1] * ak[idx1] + bk1[idx2] * ak[idx2];
                    var idx4 = offa + idx2;
                    a[idx4] = -bk1[idx2] * ak[idx1] + bk1[idx1] * ak[idx2];
                }
            }
        }
    }

    private void bluestein_real_full(float[] a, int offa, int isign) {
        var ak = new float[2 * nBluestein];
        var nthreads = ConcurrencyUtils.getNumberOfThreads();
        if ((nthreads > 1) && (n > ConcurrencyUtils.getThreadsBeginN_1D_FFT_2Threads())) {
            nthreads = 2;
            if ((nthreads >= 4) && (n > ConcurrencyUtils.getThreadsBeginN_1D_FFT_4Threads())) {
                nthreads = 4;
            }
            Future<?>[] futures = new Future[nthreads];
            var k = n / nthreads;
            for (var i = 0; i < nthreads; i++) {
                var firstIdx = i * k;
                var lastIdx = (i == (nthreads - 1)) ? n : firstIdx + k;
                futures[i] = ConcurrencyUtils.submit(() -> {
                    if (isign > 0) {
                        for (var i13 = firstIdx; i13 < lastIdx; i13++) {
                            var idx1 = 2 * i13;
                            var idx3 = offa + i13;
                            ak[idx1] = a[idx3] * bk1[idx1];
                            var idx2 = idx1 + 1;
                            ak[idx2] = a[idx3] * bk1[idx2];
                        }
                    } else {
                        for (var i13 = firstIdx; i13 < lastIdx; i13++) {
                            var idx1 = 2 * i13;
                            var idx3 = offa + i13;
                            ak[idx1] = a[idx3] * bk1[idx1];
                            var idx2 = idx1 + 1;
                            ak[idx2] = -a[idx3] * bk1[idx2];
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            cftbsub(2 * nBluestein, ak, 0, ip, nw, w);

            k = nBluestein / nthreads;
            for (var i = 0; i < nthreads; i++) {
                var firstIdx = i * k;
                var lastIdx = (i == (nthreads - 1)) ? nBluestein : firstIdx + k;
                futures[i] = ConcurrencyUtils.submit(() -> {
                    if (isign > 0) {
                        for (var i12 = firstIdx; i12 < lastIdx; i12++) {
                            var idx1 = 2 * i12;
                            var idx2 = idx1 + 1;
                            var im = -ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1];
                            ak[idx1] = ak[idx1] * bk2[idx1] + ak[idx2] * bk2[idx2];
                            ak[idx2] = im;
                        }
                    } else {
                        for (var i12 = firstIdx; i12 < lastIdx; i12++) {
                            var idx1 = 2 * i12;
                            var idx2 = idx1 + 1;
                            var im = ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1];
                            ak[idx1] = ak[idx1] * bk2[idx1] - ak[idx2] * bk2[idx2];
                            ak[idx2] = im;
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            cftfsub(2 * nBluestein, ak, 0, ip, nw, w);

            k = n / nthreads;
            for (var i = 0; i < nthreads; i++) {
                var firstIdx = i * k;
                var lastIdx = (i == (nthreads - 1)) ? n : firstIdx + k;
                futures[i] = ConcurrencyUtils.submit(() -> {
                    if (isign > 0) {
                        for (var i1 = firstIdx; i1 < lastIdx; i1++) {
                            var idx1 = 2 * i1;
                            var idx2 = idx1 + 1;
                            a[offa + idx1] = bk1[idx1] * ak[idx1] - bk1[idx2] * ak[idx2];
                            a[offa + idx2] = bk1[idx2] * ak[idx1] + bk1[idx1] * ak[idx2];
                        }
                    } else {
                        for (var i1 = firstIdx; i1 < lastIdx; i1++) {
                            var idx1 = 2 * i1;
                            var idx2 = idx1 + 1;
                            a[offa + idx1] = bk1[idx1] * ak[idx1] + bk1[idx2] * ak[idx2];
                            a[offa + idx2] = -bk1[idx2] * ak[idx1] + bk1[idx1] * ak[idx2];
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);
        } else {
            if (isign > 0) {
                for (var i = 0; i < n; i++) {
                    var idx1 = 2 * i;
                    var idx3 = offa + i;
                    ak[idx1] = a[idx3] * bk1[idx1];
                    var idx2 = idx1 + 1;
                    ak[idx2] = a[idx3] * bk1[idx2];
                }
            } else {
                for (var i = 0; i < n; i++) {
                    var idx1 = 2 * i;
                    var idx3 = offa + i;
                    ak[idx1] = a[idx3] * bk1[idx1];
                    var idx2 = idx1 + 1;
                    ak[idx2] = -a[idx3] * bk1[idx2];
                }
            }

            cftbsub(2 * nBluestein, ak, 0, ip, nw, w);

            if (isign > 0) {
                for (var i = 0; i < nBluestein; i++) {
                    var idx1 = 2 * i;
                    var idx2 = idx1 + 1;
                    var im = -ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1];
                    ak[idx1] = ak[idx1] * bk2[idx1] + ak[idx2] * bk2[idx2];
                    ak[idx2] = im;
                }
            } else {
                for (var i = 0; i < nBluestein; i++) {
                    var idx1 = 2 * i;
                    var idx2 = idx1 + 1;
                    var im = ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1];
                    ak[idx1] = ak[idx1] * bk2[idx1] - ak[idx2] * bk2[idx2];
                    ak[idx2] = im;
                }
            }

            cftfsub(2 * nBluestein, ak, 0, ip, nw, w);

            if (isign > 0) {
                for (var i = 0; i < n; i++) {
                    var idx1 = 2 * i;
                    var idx2 = idx1 + 1;
                    a[offa + idx1] = bk1[idx1] * ak[idx1] - bk1[idx2] * ak[idx2];
                    a[offa + idx2] = bk1[idx2] * ak[idx1] + bk1[idx1] * ak[idx2];
                }
            } else {
                for (var i = 0; i < n; i++) {
                    var idx1 = 2 * i;
                    var idx2 = idx1 + 1;
                    a[offa + idx1] = bk1[idx1] * ak[idx1] + bk1[idx2] * ak[idx2];
                    a[offa + idx2] = -bk1[idx2] * ak[idx1] + bk1[idx1] * ak[idx2];
                }
            }
        }
    }

    private void bluestein_real_forward(float[] a, int offa) {
        var ak = new float[2 * nBluestein];
        var nthreads = ConcurrencyUtils.getNumberOfThreads();
        if ((nthreads > 1) && (n > ConcurrencyUtils.getThreadsBeginN_1D_FFT_2Threads())) {
            nthreads = 2;
            if ((nthreads >= 4) && (n > ConcurrencyUtils.getThreadsBeginN_1D_FFT_4Threads())) {
                nthreads = 4;
            }
            Future<?>[] futures = new Future[nthreads];
            var k = n / nthreads;
            for (var i = 0; i < nthreads; i++) {
                var firstIdx = i * k;
                var lastIdx = (i == (nthreads - 1)) ? n : firstIdx + k;
                futures[i] = ConcurrencyUtils.submit(() -> {
                    for (var i12 = firstIdx; i12 < lastIdx; i12++) {
                        var idx1 = 2 * i12;
                        var idx3 = offa + i12;
                        ak[idx1] = a[idx3] * bk1[idx1];
                        var idx2 = idx1 + 1;
                        ak[idx2] = -a[idx3] * bk1[idx2];
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            cftbsub(2 * nBluestein, ak, 0, ip, nw, w);

            k = nBluestein / nthreads;
            for (var i = 0; i < nthreads; i++) {
                var firstIdx = i * k;
                var lastIdx = (i == (nthreads - 1)) ? nBluestein : firstIdx + k;
                futures[i] = ConcurrencyUtils.submit(() -> {
                    for (var i1 = firstIdx; i1 < lastIdx; i1++) {
                        var idx1 = 2 * i1;
                        var idx2 = idx1 + 1;
                        var im = ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1];
                        ak[idx1] = ak[idx1] * bk2[idx1] - ak[idx2] * bk2[idx2];
                        ak[idx2] = im;
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

        } else {

            for (var i = 0; i < n; i++) {
                var idx1 = 2 * i;
                var idx3 = offa + i;
                ak[idx1] = a[idx3] * bk1[idx1];
                var idx2 = idx1 + 1;
                ak[idx2] = -a[idx3] * bk1[idx2];
            }

            cftbsub(2 * nBluestein, ak, 0, ip, nw, w);

            for (var i = 0; i < nBluestein; i++) {
                var idx1 = 2 * i;
                var idx2 = idx1 + 1;
                var im = ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1];
                ak[idx1] = ak[idx1] * bk2[idx1] - ak[idx2] * bk2[idx2];
                ak[idx2] = im;
            }
        }

        cftfsub(2 * nBluestein, ak, 0, ip, nw, w);

        if (n % 2 == 0) {
            a[offa] = bk1[0] * ak[0] + bk1[1] * ak[1];
            a[offa + 1] = bk1[n] * ak[n] + bk1[n + 1] * ak[n + 1];
            for (var i = 1; i < n / 2; i++) {
                var idx1 = 2 * i;
                var idx2 = idx1 + 1;
                a[offa + idx1] = bk1[idx1] * ak[idx1] + bk1[idx2] * ak[idx2];
                a[offa + idx2] = -bk1[idx2] * ak[idx1] + bk1[idx1] * ak[idx2];
            }
        } else {
            a[offa] = bk1[0] * ak[0] + bk1[1] * ak[1];
            a[offa + 1] = -bk1[n] * ak[n - 1] + bk1[n - 1] * ak[n];
            for (var i = 1; i < (n - 1) / 2; i++) {
                var idx1 = 2 * i;
                var idx2 = idx1 + 1;
                a[offa + idx1] = bk1[idx1] * ak[idx1] + bk1[idx2] * ak[idx2];
                a[offa + idx2] = -bk1[idx2] * ak[idx1] + bk1[idx1] * ak[idx2];
            }
            a[offa + n - 1] = bk1[n - 1] * ak[n - 1] + bk1[n] * ak[n];
        }

    }

    private void bluestein_real_inverse(float[] a, int offa) {
        var ak = new float[2 * nBluestein];
        if (n % 2 == 0) {
            ak[0] = a[offa] * bk1[0];
            ak[1] = a[offa] * bk1[1];

            for (var i = 1; i < n / 2; i++) {
                var idx1 = 2 * i;
                var idx2 = idx1 + 1;
                var idx3 = offa + idx1;
                var idx4 = offa + idx2;
                ak[idx1] = a[idx3] * bk1[idx1] - a[idx4] * bk1[idx2];
                ak[idx2] = a[idx3] * bk1[idx2] + a[idx4] * bk1[idx1];
            }

            ak[n] = a[offa + 1] * bk1[n];
            ak[n + 1] = a[offa + 1] * bk1[n + 1];

            for (var i = n / 2 + 1; i < n; i++) {
                var idx1 = 2 * i;
                var idx2 = idx1 + 1;
                var idx3 = offa + 2 * n - idx1;
                var idx4 = idx3 + 1;
                ak[idx1] = a[idx3] * bk1[idx1] + a[idx4] * bk1[idx2];
                ak[idx2] = a[idx3] * bk1[idx2] - a[idx4] * bk1[idx1];
            }

        } else {
            ak[0] = a[offa] * bk1[0];
            ak[1] = a[offa] * bk1[1];

            for (var i = 1; i < (n - 1) / 2; i++) {
                var idx1 = 2 * i;
                var idx2 = idx1 + 1;
                var idx3 = offa + idx1;
                var idx4 = offa + idx2;
                ak[idx1] = a[idx3] * bk1[idx1] - a[idx4] * bk1[idx2];
                ak[idx2] = a[idx3] * bk1[idx2] + a[idx4] * bk1[idx1];
            }

            ak[n - 1] = a[offa + n - 1] * bk1[n - 1] - a[offa + 1] * bk1[n];
            ak[n] = a[offa + n - 1] * bk1[n] + a[offa + 1] * bk1[n - 1];

            ak[n + 1] = a[offa + n - 1] * bk1[n + 1] + a[offa + 1] * bk1[n + 2];
            ak[n + 2] = a[offa + n - 1] * bk1[n + 2] - a[offa + 1] * bk1[n + 1];

            for (var i = (n - 1) / 2 + 2; i < n; i++) {
                var idx1 = 2 * i;
                var idx2 = idx1 + 1;
                var idx3 = offa + 2 * n - idx1;
                var idx4 = idx3 + 1;
                ak[idx1] = a[idx3] * bk1[idx1] + a[idx4] * bk1[idx2];
                ak[idx2] = a[idx3] * bk1[idx2] - a[idx4] * bk1[idx1];
            }
        }

        cftbsub(2 * nBluestein, ak, 0, ip, nw, w);

        var nthreads = ConcurrencyUtils.getNumberOfThreads();
        if ((nthreads > 1) && (n > ConcurrencyUtils.getThreadsBeginN_1D_FFT_2Threads())) {
            nthreads = 2;
            if ((nthreads >= 4) && (n > ConcurrencyUtils.getThreadsBeginN_1D_FFT_4Threads())) {
                nthreads = 4;
            }
            Future<?>[] futures = new Future[nthreads];
            var k = nBluestein / nthreads;
            for (var i = 0; i < nthreads; i++) {
                var firstIdx = i * k;
                var lastIdx = (i == (nthreads - 1)) ? nBluestein : firstIdx + k;
                futures[i] = ConcurrencyUtils.submit(() -> {
                    for (var i12 = firstIdx; i12 < lastIdx; i12++) {
                        var idx1 = 2 * i12;
                        var idx2 = idx1 + 1;
                        var im = -ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1];
                        ak[idx1] = ak[idx1] * bk2[idx1] + ak[idx2] * bk2[idx2];
                        ak[idx2] = im;
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            cftfsub(2 * nBluestein, ak, 0, ip, nw, w);

            k = n / nthreads;
            for (var i = 0; i < nthreads; i++) {
                var firstIdx = i * k;
                var lastIdx = (i == (nthreads - 1)) ? n : firstIdx + k;
                futures[i] = ConcurrencyUtils.submit(() -> {
                    for (var i1 = firstIdx; i1 < lastIdx; i1++) {
                        var idx1 = 2 * i1;
                        var idx2 = idx1 + 1;
                        a[offa + i1] = bk1[idx1] * ak[idx1] - bk1[idx2] * ak[idx2];
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

        } else {

            for (var i = 0; i < nBluestein; i++) {
                var idx1 = 2 * i;
                var idx2 = idx1 + 1;
                var im = -ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1];
                ak[idx1] = ak[idx1] * bk2[idx1] + ak[idx2] * bk2[idx2];
                ak[idx2] = im;
            }

            cftfsub(2 * nBluestein, ak, 0, ip, nw, w);

            for (var i = 0; i < n; i++) {
                var idx1 = 2 * i;
                var idx2 = idx1 + 1;
                a[offa + i] = bk1[idx1] * ak[idx1] - bk1[idx2] * ak[idx2];
            }
        }
    }

    private void bluestein_real_inverse2(float[] a, int offa) {
        var ak = new float[2 * nBluestein];
        var nthreads = ConcurrencyUtils.getNumberOfThreads();
        if ((nthreads > 1) && (n > ConcurrencyUtils.getThreadsBeginN_1D_FFT_2Threads())) {
            nthreads = 2;
            if ((nthreads >= 4) && (n > ConcurrencyUtils.getThreadsBeginN_1D_FFT_4Threads())) {
                nthreads = 4;
            }
            Future<?>[] futures = new Future[nthreads];
            var k = n / nthreads;
            for (var i = 0; i < nthreads; i++) {
                var firstIdx = i * k;
                var lastIdx = (i == (nthreads - 1)) ? n : firstIdx + k;
                futures[i] = ConcurrencyUtils.submit(() -> {
                    for (var i12 = firstIdx; i12 < lastIdx; i12++) {
                        var idx1 = 2 * i12;
                        var idx3 = offa + i12;
                        ak[idx1] = a[idx3] * bk1[idx1];
                        var idx2 = idx1 + 1;
                        ak[idx2] = a[idx3] * bk1[idx2];
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            cftbsub(2 * nBluestein, ak, 0, ip, nw, w);

            k = nBluestein / nthreads;
            for (var i = 0; i < nthreads; i++) {
                var firstIdx = i * k;
                var lastIdx = (i == (nthreads - 1)) ? nBluestein : firstIdx + k;
                futures[i] = ConcurrencyUtils.submit(() -> {
                    for (var i1 = firstIdx; i1 < lastIdx; i1++) {
                        var idx1 = 2 * i1;
                        var idx2 = idx1 + 1;
                        var im = -ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1];
                        ak[idx1] = ak[idx1] * bk2[idx1] + ak[idx2] * bk2[idx2];
                        ak[idx2] = im;
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

        } else {

            for (var i = 0; i < n; i++) {
                var idx1 = 2 * i;
                var idx3 = offa + i;
                ak[idx1] = a[idx3] * bk1[idx1];
                var idx2 = idx1 + 1;
                ak[idx2] = a[idx3] * bk1[idx2];
            }

            cftbsub(2 * nBluestein, ak, 0, ip, nw, w);

            for (var i = 0; i < nBluestein; i++) {
                var idx1 = 2 * i;
                var idx2 = idx1 + 1;
                var im = -ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1];
                ak[idx1] = ak[idx1] * bk2[idx1] + ak[idx2] * bk2[idx2];
                ak[idx2] = im;
            }
        }

        cftfsub(2 * nBluestein, ak, 0, ip, nw, w);

        if (n % 2 == 0) {
            a[offa] = bk1[0] * ak[0] - bk1[1] * ak[1];
            a[offa + 1] = bk1[n] * ak[n] - bk1[n + 1] * ak[n + 1];
            for (var i = 1; i < n / 2; i++) {
                var idx1 = 2 * i;
                var idx2 = idx1 + 1;
                a[offa + idx1] = bk1[idx1] * ak[idx1] - bk1[idx2] * ak[idx2];
                a[offa + idx2] = bk1[idx2] * ak[idx1] + bk1[idx1] * ak[idx2];
            }
        } else {
            a[offa] = bk1[0] * ak[0] - bk1[1] * ak[1];
            a[offa + 1] = bk1[n] * ak[n - 1] + bk1[n - 1] * ak[n];
            for (var i = 1; i < (n - 1) / 2; i++) {
                var idx1 = 2 * i;
                var idx2 = idx1 + 1;
                a[offa + idx1] = bk1[idx1] * ak[idx1] - bk1[idx2] * ak[idx2];
                a[offa + idx2] = bk1[idx2] * ak[idx1] + bk1[idx1] * ak[idx2];
            }
            a[offa + n - 1] = bk1[n - 1] * ak[n - 1] - bk1[n] * ak[n];
        }
    }

    /*---------------------------------------------------------
       rfftf1: further processing of Real forward FFT
      --------------------------------------------------------*/
    private void rfftf(float[] a, int offa) {
        if (n == 1)
            return;

        var ch = new float[n];
        var twon = 2 * n;
        var nf = (int) wtable_r[1 + twon];
        var na = 1;
        var l2 = n;
        var iw = twon - 1;
        for (var k1 = 1; k1 <= nf; ++k1) {
            var kh = nf - k1;
            var ip = (int) wtable_r[kh + 2 + twon];
            var l1 = l2 / ip;
            var ido = n / l2;
            iw -= (ip - 1) * ido;
            na = 1 - na;
            var idl1 = ido * l1;
            switch (ip) {
                case 2:
                    if (na == 0) {
                        radf2(ido, l1, a, offa, ch, 0, iw);
                    } else {
                        radf2(ido, l1, ch, 0, a, offa, iw);
                    }
                    break;
                case 3:
                    if (na == 0) {
                        radf3(ido, l1, a, offa, ch, 0, iw);
                    } else {
                        radf3(ido, l1, ch, 0, a, offa, iw);
                    }
                    break;
                case 4:
                    if (na == 0) {
                        radf4(ido, l1, a, offa, ch, 0, iw);
                    } else {
                        radf4(ido, l1, ch, 0, a, offa, iw);
                    }
                    break;
                case 5:
                    if (na == 0) {
                        radf5(ido, l1, a, offa, ch, 0, iw);
                    } else {
                        radf5(ido, l1, ch, 0, a, offa, iw);
                    }
                    break;
                default:
                    if (ido == 1)
                        na = 1 - na;
                    if (na == 0) {
                        radfg(ido, ip, l1, idl1, a, offa, ch, 0, iw);
                        na = 1;
                    } else {
                        radfg(ido, ip, l1, idl1, ch, 0, a, offa, iw);
                        na = 0;
                    }
                    break;
            }
            l2 = l1;
        }
        if (na == 1)
            return;
        System.arraycopy(ch, 0, a, offa, n);
    }

    /*---------------------------------------------------------
       rfftb1: further processing of Real backward FFT
      --------------------------------------------------------*/
    private void rfftb(float[] a, int offa) {
        if (n == 1)
            return;

        var ch = new float[n];
        var twon = 2 * n;
        var nf = (int) wtable_r[1 + twon];
        var na = 0;
        var l1 = 1;
        var iw = n;
        for (var k1 = 1; k1 <= nf; k1++) {
            var ip = (int) wtable_r[k1 + 1 + twon];
            var l2 = ip * l1;
            var ido = n / l2;
            var idl1 = ido * l1;
            switch (ip) {
                case 2:
                    if (na == 0) {
                        radb2(ido, l1, a, offa, ch, 0, iw);
                    } else {
                        radb2(ido, l1, ch, 0, a, offa, iw);
                    }
                    na = 1 - na;
                    break;
                case 3:
                    if (na == 0) {
                        radb3(ido, l1, a, offa, ch, 0, iw);
                    } else {
                        radb3(ido, l1, ch, 0, a, offa, iw);
                    }
                    na = 1 - na;
                    break;
                case 4:
                    if (na == 0) {
                        radb4(ido, l1, a, offa, ch, 0, iw);
                    } else {
                        radb4(ido, l1, ch, 0, a, offa, iw);
                    }
                    na = 1 - na;
                    break;
                case 5:
                    if (na == 0) {
                        radb5(ido, l1, a, offa, ch, 0, iw);
                    } else {
                        radb5(ido, l1, ch, 0, a, offa, iw);
                    }
                    na = 1 - na;
                    break;
                default:
                    if (na == 0) {
                        radbg(ido, ip, l1, idl1, a, offa, ch, 0, iw);
                    } else {
                        radbg(ido, ip, l1, idl1, ch, 0, a, offa, iw);
                    }
                    if (ido == 1)
                        na = 1 - na;
                    break;
            }
            l1 = l2;
            iw += (ip - 1) * ido;
        }
        if (na == 0)
            return;
        System.arraycopy(ch, 0, a, offa, n);
    }

    /*-------------------------------------------------
       radf2: Real FFT's forward processing of factor 2
      -------------------------------------------------*/
    private void radf2(int ido, int l1, float[] in, int in_off, float[] out, int out_off, int offset) {
        var idx0 = l1 * ido;
        var idx1 = 2 * ido;
        for (var k = 0; k < l1; k++) {
            var oidx1 = out_off + k * idx1;
            var oidx2 = oidx1 + idx1 - 1;
            var iidx1 = in_off + k * ido;
            var iidx2 = iidx1 + idx0;

            var i1r = in[iidx1];
            var i2r = in[iidx2];

            out[oidx1] = i1r + i2r;
            out[oidx2] = i1r - i2r;
        }
        if (ido < 2)
            return;
        int idx2;
        if (ido != 2) {
            var iw1 = offset;
            for (var k = 0; k < l1; k++) {
                idx1 = k * ido;
                idx2 = 2 * idx1;
                var idx3 = idx2 + ido;
                var idx4 = idx1 + idx0;
                for (var i = 2; i < ido; i += 2) {
                    var ic = ido - i;
                    var widx1 = i - 1 + iw1;
                    var oidx1 = out_off + i + idx2;
                    var iidx1 = in_off + i + idx1;
                    var iidx2 = in_off + i + idx4;

                    var a1i = in[iidx1 - 1];
                    var a1r = in[iidx1];
                    var a2i = in[iidx2 - 1];
                    var a2r = in[iidx2];

                    var w1r = wtable_r[widx1 - 1];
                    var w1i = wtable_r[widx1];

                    var t1i = w1r * a2r - w1i * a2i;

                    out[oidx1] = a1r + t1i;
                    var t1r = w1r * a2i + w1i * a2r;
                    out[oidx1 - 1] = a1i + t1r;

                    var oidx2 = out_off + ic + idx3;
                    out[oidx2] = t1i - a1r;
                    out[oidx2 - 1] = a1i - t1r;
                }
            }
            if (ido % 2 == 1)
                return;
        }
        idx2 = 2 * idx1;
        for (var k = 0; k < l1; k++) {
            idx1 = k * ido;
            var oidx1 = out_off + idx2 + ido;
            var iidx1 = in_off + ido - 1 + idx1;

            out[oidx1] = -in[iidx1 + idx0];
            out[oidx1 - 1] = in[iidx1];
        }
    }

    /*-------------------------------------------------
       radb2: Real FFT's backward processing of factor 2
      -------------------------------------------------*/
    private void radb2(int ido, int l1, float[] in, int in_off, float[] out, int out_off, int offset) {

        var idx0 = l1 * ido;
        for (var k = 0; k < l1; k++) {
            var idx1 = k * ido;
            var idx2 = 2 * idx1;
            var idx3 = idx2 + ido;
            var oidx1 = out_off + idx1;
            var iidx1 = in_off + idx2;
            var iidx2 = in_off + ido - 1 + idx3;
            var i1r = in[iidx1];
            var i2r = in[iidx2];
            out[oidx1] = i1r + i2r;
            out[oidx1 + idx0] = i1r - i2r;
        }
        if (ido < 2)
            return;
        if (ido != 2) {
            var iw1 = offset;
            for (var k = 0; k < l1; ++k) {
                var idx1 = k * ido;
                var idx2 = 2 * idx1;
                var idx3 = idx2 + ido;
                var idx4 = idx1 + idx0;
                for (var i = 2; i < ido; i += 2) {
                    var ic = ido - i;
                    var idx5 = i - 1 + iw1;
                    var idx6 = out_off + i;
                    var idx7 = in_off + i;
                    var idx8 = in_off + ic;
                    var w1r = wtable_r[idx5 - 1];
                    var w1i = wtable_r[idx5];
                    var iidx1 = idx7 + idx2;
                    var iidx2 = idx8 + idx3;
                    var t1r = in[iidx1 - 1] - in[iidx2 - 1];
                    var t1i = in[iidx1] + in[iidx2];
                    var i1i = in[iidx1];
                    var i1r = in[iidx1 - 1];
                    var i2i = in[iidx2];
                    var i2r = in[iidx2 - 1];

                    var oidx1 = idx6 + idx1;
                    out[oidx1 - 1] = i1r + i2r;
                    out[oidx1] = i1i - i2i;
                    var oidx2 = idx6 + idx4;
                    out[oidx2 - 1] = w1r * t1r - w1i * t1i;
                    out[oidx2] = w1r * t1i + w1i * t1r;
                }
            }
            if (ido % 2 == 1)
                return;
        }
        for (var k = 0; k < l1; k++) {
            var idx1 = k * ido;
            var idx2 = 2 * idx1;
            var oidx1 = out_off + ido - 1 + idx1;
            var iidx1 = in_off + idx2 + ido;
            out[oidx1] = 2 * in[iidx1 - 1];
            out[oidx1 + idx0] = -2 * in[iidx1];
        }
    }

    /*-------------------------------------------------
       radf3: Real FFT's forward processing of factor 3
      -------------------------------------------------*/
    private void radf3(int ido, int l1, float[] in, int in_off, float[] out, int out_off, int offset) {
        final var taur = -0.5f;
        final var taui = 0.866025403784438707610604524234076962f;
        float cr2;

        var idx0 = l1 * ido;
        for (var k = 0; k < l1; k++) {
            var idx1 = k * ido;
            var idx3 = 2 * idx0;
            var idx4 = (3 * k + 1) * ido;
            var iidx1 = in_off + idx1;
            var iidx2 = iidx1 + idx0;
            var iidx3 = iidx1 + idx3;
            var i1r = in[iidx1];
            var i2r = in[iidx2];
            var i3r = in[iidx3];
            cr2 = i2r + i3r;
            out[out_off + 3 * idx1] = i1r + cr2;
            out[out_off + idx4 + ido] = taui * (i3r - i2r);
            out[out_off + ido - 1 + idx4] = i1r + taur * cr2;
        }
        if (ido == 1)
            return;
        var iw1 = offset;
        var iw2 = iw1 + ido;
        for (var k = 0; k < l1; k++) {
            var idx3 = k * ido;
            var idx4 = 3 * idx3;
            var idx5 = idx3 + idx0;
            var idx6 = idx5 + idx0;
            var idx7 = idx4 + ido;
            var idx8 = idx7 + ido;
            for (var i = 2; i < ido; i += 2) {
                var ic = ido - i;
                var widx1 = i - 1 + iw1;
                var widx2 = i - 1 + iw2;

                var w1r = wtable_r[widx1 - 1];
                var w1i = wtable_r[widx1];
                var w2r = wtable_r[widx2 - 1];
                var w2i = wtable_r[widx2];

                var idx9 = in_off + i;
                var idx10 = out_off + i;
                var iidx1 = idx9 + idx3;
                var iidx2 = idx9 + idx5;
                var iidx3 = idx9 + idx6;

                var i1i = in[iidx1 - 1];
                var i1r = in[iidx1];
                var i2i = in[iidx2 - 1];
                var i2r = in[iidx2];
                var i3i = in[iidx3 - 1];
                var i3r = in[iidx3];

                var dr2 = w1r * i2i + w1i * i2r;
                var dr3 = w2r * i3i + w2i * i3r;
                cr2 = dr2 + dr3;
                var di3 = w2r * i3r - w2i * i3i;
                var di2 = w1r * i2r - w1i * i2i;
                var ci2 = di2 + di3;
                var tr2 = i1i + taur * cr2;

                var oidx1 = idx10 + idx4;

                out[oidx1 - 1] = i1i + cr2;
                out[oidx1] = i1r + ci2;
                var idx11 = out_off + ic;
                var oidx2 = idx11 + idx7;
                var tr3 = taui * (di2 - di3);
                out[oidx2 - 1] = tr2 - tr3;
                var ti3 = taui * (dr3 - dr2);
                var ti2 = i1r + taur * ci2;
                out[oidx2] = ti3 - ti2;
                var oidx3 = idx10 + idx8;
                out[oidx3 - 1] = tr2 + tr3;
                out[oidx3] = ti2 + ti3;
            }
        }
    }

    /*-------------------------------------------------
       radb3: Real FFT's backward processing of factor 3
      -------------------------------------------------*/
    private void radb3(int ido, int l1, float[] in, int in_off, float[] out, int out_off, int offset) {
        final var taur = -0.5f;
        final var taui = 0.866025403784438707610604524234076962f;
        float ci3, cr2, tr2;

        for (var k = 0; k < l1; k++) {
            var idx1 = k * ido;
            var iidx1 = in_off + 3 * idx1;
            var iidx2 = iidx1 + 2 * ido;
            var i1i = in[iidx1];

            tr2 = 2 * in[iidx2 - 1];
            cr2 = i1i + taur * tr2;
            ci3 = 2 * taui * in[iidx2];

            out[out_off + idx1] = i1i + tr2;
            out[out_off + (k + l1) * ido] = cr2 - ci3;
            out[out_off + (k + 2 * l1) * ido] = cr2 + ci3;
        }
        if (ido == 1)
            return;
        var idx0 = l1 * ido;
        var iw1 = offset;
        var iw2 = iw1 + ido;
        for (var k = 0; k < l1; k++) {
            var idx1 = k * ido;
            var idx2 = 3 * idx1;
            var idx3 = idx2 + ido;
            var idx4 = idx3 + ido;
            var idx5 = idx1 + idx0;
            var idx6 = idx5 + idx0;
            for (var i = 2; i < ido; i += 2) {
                var ic = ido - i;
                var idx7 = in_off + i;
                var idx8 = in_off + ic;
                var idx9 = out_off + i;
                var iidx1 = idx7 + idx2;
                var iidx2 = idx7 + idx4;
                var iidx3 = idx8 + idx3;

                var i1i = in[iidx1 - 1];
                var i1r = in[iidx1];
                var i2i = in[iidx2 - 1];
                var i2r = in[iidx2];
                var i3i = in[iidx3 - 1];
                var i3r = in[iidx3];

                tr2 = i2i + i3i;
                cr2 = i1i + taur * tr2;
                var ti2 = i2r - i3r;
                ci3 = taui * (i2r + i3r);
                var dr2 = cr2 - ci3;
                var dr3 = cr2 + ci3;

                var widx1 = i - 1 + iw1;
                var widx2 = i - 1 + iw2;

                var w1r = wtable_r[widx1 - 1];
                var w1i = wtable_r[widx1];
                var w2r = wtable_r[widx2 - 1];
                var w2i = wtable_r[widx2];

                var oidx1 = idx9 + idx1;

                out[oidx1 - 1] = i1i + tr2;
                out[oidx1] = i1r + ti2;
                var oidx2 = idx9 + idx5;
                var cr3 = taui * (i2i - i3i);
                var ci2 = i1r + taur * ti2;
                var di2 = ci2 + cr3;
                out[oidx2 - 1] = w1r * dr2 - w1i * di2;
                out[oidx2] = w1r * di2 + w1i * dr2;
                var oidx3 = idx9 + idx6;
                var di3 = ci2 - cr3;
                out[oidx3 - 1] = w2r * dr3 - w2i * di3;
                out[oidx3] = w2r * di3 + w2i * dr3;
            }
        }
    }

    /*-------------------------------------------------
       radf4: Real FFT's forward processing of factor 4
      -------------------------------------------------*/
    private void radf4(int ido, int l1, float[] in, int in_off, float[] out, int out_off, int offset) {
        float tr1, tr2;
        var idx0 = l1 * ido;
        for (var k = 0; k < l1; k++) {
            var idx1 = k * ido;
            var idx3 = idx1 + idx0;
            var idx4 = idx3 + idx0;
            var idx5 = idx4 + idx0;
            var i1r = in[in_off + idx1];
            var i2r = in[in_off + idx3];
            var i3r = in[in_off + idx4];
            var i4r = in[in_off + idx5];

            tr1 = i2r + i4r;
            tr2 = i1r + i3r;

            var idx2 = 4 * idx1;
            var oidx1 = out_off + idx2;

            out[oidx1] = tr1 + tr2;
            var idx6 = idx2 + ido;
            var oidx2 = out_off + idx6 + ido;
            out[oidx2 - 1 + ido + ido] = tr2 - tr1;
            out[oidx2 - 1] = i1r - i3r;
            out[oidx2] = i4r - i2r;
        }
        if (ido < 2)
            return;
        float ti1;
        if (ido != 2) {
            var iw2 = offset + ido;
            var iw3 = iw2 + ido;
            var iw1 = offset;
            for (var k = 0; k < l1; k++) {
                var idx1 = k * ido;
                var idx2 = idx1 + idx0;
                var idx3 = idx2 + idx0;
                var idx4 = idx3 + idx0;
                var idx5 = 4 * idx1;
                var idx6 = idx5 + ido;
                var idx7 = idx6 + ido;
                var idx8 = idx7 + ido;
                for (var i = 2; i < ido; i += 2) {
                    var ic = ido - i;
                    var widx1 = i - 1 + iw1;
                    var widx2 = i - 1 + iw2;
                    var widx3 = i - 1 + iw3;
                    var w1r = wtable_r[widx1 - 1];
                    var w1i = wtable_r[widx1];
                    var w2r = wtable_r[widx2 - 1];
                    var w2i = wtable_r[widx2];
                    var w3r = wtable_r[widx3 - 1];
                    var w3i = wtable_r[widx3];

                    var idx9 = in_off + i;
                    var idx10 = out_off + i;
                    var iidx1 = idx9 + idx1;
                    var iidx2 = idx9 + idx2;
                    var iidx3 = idx9 + idx3;
                    var iidx4 = idx9 + idx4;

                    var i1i = in[iidx1 - 1];
                    var i1r = in[iidx1];
                    var i2i = in[iidx2 - 1];
                    var i2r = in[iidx2];
                    var i3i = in[iidx3 - 1];
                    var i3r = in[iidx3];
                    var i4i = in[iidx4 - 1];
                    var i4r = in[iidx4];

                    var cr2 = w1r * i2i + w1i * i2r;
                    var cr4 = w3r * i4i + w3i * i4r;
                    tr1 = cr2 + cr4;
                    var ci4 = w3r * i4r - w3i * i4i;
                    var ci2 = w1r * i2r - w1i * i2i;
                    ti1 = ci2 + ci4;
                    var cr3 = w2r * i3i + w2i * i3r;
                    tr2 = i1i + cr3;

                    var oidx1 = idx10 + idx5;

                    out[oidx1 - 1] = tr1 + tr2;
                    var idx11 = out_off + ic;
                    var oidx4 = idx11 + idx8;
                    out[oidx4 - 1] = tr2 - tr1;
                    var ci3 = w2r * i3r - w2i * i3i;
                    var ti2 = i1r + ci3;
                    out[oidx1] = ti1 + ti2;
                    out[oidx4] = ti1 - ti2;
                    var oidx3 = idx10 + idx7;
                    var tr3 = i1i - cr3;
                    var ti4 = ci2 - ci4;
                    out[oidx3 - 1] = ti4 + tr3;
                    var oidx2 = idx11 + idx6;
                    out[oidx2 - 1] = tr3 - ti4;
                    var ti3 = i1r - ci3;
                    var tr4 = cr4 - cr2;
                    out[oidx3] = tr4 + ti3;
                    out[oidx2] = tr4 - ti3;
                }
            }
            if (ido % 2 == 1)
                return;
        }
        final var hsqt2 = 0.707106781186547572737310929369414225f;
        for (var k = 0; k < l1; k++) {
            var idx1 = k * ido;
            var idx2 = 4 * idx1;
            var idx3 = idx1 + idx0;
            var idx4 = idx3 + idx0;
            var idx5 = idx4 + idx0;
            var idx6 = idx2 + ido;
            var idx9 = in_off + ido;

            var i1i = in[idx9 - 1 + idx1];
            var i2i = in[idx9 - 1 + idx3];
            var i3i = in[idx9 - 1 + idx4];
            var i4i = in[idx9 - 1 + idx5];

            ti1 = -hsqt2 * (i2i + i4i);
            tr1 = hsqt2 * (i2i - i4i);

            var idx10 = out_off + ido;
            out[idx10 - 1 + idx2] = tr1 + i1i;
            var idx7 = idx6 + ido;
            out[idx10 - 1 + idx7] = i1i - tr1;
            out[out_off + idx6] = ti1 - i3i;
            var idx8 = idx7 + ido;
            out[out_off + idx8] = ti1 + i3i;
        }
    }

    /*-------------------------------------------------
       radb4: Real FFT's backward processing of factor 4
      -------------------------------------------------*/
    private void radb4(int ido, int l1, float[] in, int in_off, float[] out, int out_off, int offset) {
        float tr1, tr2, tr3, tr4;
        var iw1 = offset;

        var idx0 = l1 * ido;
        for (var k = 0; k < l1; k++) {
            var idx1 = k * ido;
            var idx2 = 4 * idx1;
            var idx3 = idx1 + idx0;
            var idx6 = idx2 + ido;
            var idx7 = idx6 + ido;
            var idx8 = idx7 + ido;

            var i1r = in[in_off + idx2];
            var i2r = in[in_off + idx7];
            var i3r = in[in_off + ido - 1 + idx8];
            var i4r = in[in_off + ido - 1 + idx6];

            tr1 = i1r - i3r;
            tr2 = i1r + i3r;
            tr3 = i4r + i4r;
            tr4 = i2r + i2r;

            out[out_off + idx1] = tr2 + tr3;
            out[out_off + idx3] = tr1 - tr4;
            var idx4 = idx3 + idx0;
            out[out_off + idx4] = tr2 - tr3;
            var idx5 = idx4 + idx0;
            out[out_off + idx5] = tr1 + tr4;
        }
        if (ido < 2)
            return;
        float ti2;
        float ti1;
        if (ido != 2) {
            var iw2 = iw1 + ido;
            var iw3 = iw2 + ido;
            for (var k = 0; k < l1; ++k) {
                var idx1 = k * ido;
                var idx2 = idx1 + idx0;
                var idx3 = idx2 + idx0;
                var idx4 = idx3 + idx0;
                var idx5 = 4 * idx1;
                var idx6 = idx5 + ido;
                var idx7 = idx6 + ido;
                var idx8 = idx7 + ido;
                for (var i = 2; i < ido; i += 2) {
                    var ic = ido - i;
                    var widx1 = i - 1 + iw1;
                    var widx2 = i - 1 + iw2;
                    var widx3 = i - 1 + iw3;
                    var w1r = wtable_r[widx1 - 1];
                    var w1i = wtable_r[widx1];
                    var w2r = wtable_r[widx2 - 1];
                    var w2i = wtable_r[widx2];
                    var w3r = wtable_r[widx3 - 1];
                    var w3i = wtable_r[widx3];

                    var idx12 = in_off + i;
                    var idx13 = in_off + ic;
                    var idx14 = out_off + i;

                    var iidx1 = idx12 + idx5;
                    var iidx2 = idx13 + idx6;
                    var iidx3 = idx12 + idx7;
                    var iidx4 = idx13 + idx8;

                    var i1i = in[iidx1 - 1];
                    var i1r = in[iidx1];
                    var i2i = in[iidx2 - 1];
                    var i2r = in[iidx2];
                    var i3i = in[iidx3 - 1];
                    var i3r = in[iidx3];
                    var i4i = in[iidx4 - 1];
                    var i4r = in[iidx4];

                    ti1 = i1r + i4r;
                    ti2 = i1r - i4r;
                    tr4 = i3r + i2r;
                    tr1 = i1i - i4i;
                    tr2 = i1i + i4i;
                    tr3 = i3i + i2i;
                    var cr3 = tr2 - tr3;
                    var ti3 = i3r - i2r;
                    var ci3 = ti2 - ti3;
                    var cr2 = tr1 - tr4;
                    var cr4 = tr1 + tr4;
                    var ti4 = i3i - i2i;
                    var ci2 = ti1 + ti4;
                    var ci4 = ti1 - ti4;

                    var oidx1 = idx14 + idx1;

                    out[oidx1 - 1] = tr2 + tr3;
                    out[oidx1] = ti2 + ti3;
                    var oidx2 = idx14 + idx2;
                    out[oidx2 - 1] = w1r * cr2 - w1i * ci2;
                    out[oidx2] = w1r * ci2 + w1i * cr2;
                    var oidx3 = idx14 + idx3;
                    out[oidx3 - 1] = w2r * cr3 - w2i * ci3;
                    out[oidx3] = w2r * ci3 + w2i * cr3;
                    var oidx4 = idx14 + idx4;
                    out[oidx4 - 1] = w3r * cr4 - w3i * ci4;
                    out[oidx4] = w3r * ci4 + w3i * cr4;
                }
            }
            if (ido % 2 == 1)
                return;
        }
        final var sqrt2 = 1.41421356237309514547462185873882845f;
        for (var k = 0; k < l1; k++) {
            var idx1 = k * ido;
            var idx2 = 4 * idx1;
            var idx3 = idx1 + idx0;
            var idx6 = idx2 + ido;
            var idx7 = idx6 + ido;
            var idx8 = idx7 + ido;
            var idx9 = in_off + ido;

            var i1r = in[idx9 - 1 + idx2];
            var i2r = in[idx9 - 1 + idx7];
            var i3r = in[in_off + idx6];
            var i4r = in[in_off + idx8];

            ti1 = i3r + i4r;
            ti2 = i4r - i3r;
            tr1 = i1r - i2r;
            tr2 = i1r + i2r;

            var idx10 = out_off + ido;
            out[idx10 - 1 + idx1] = tr2 + tr2;
            out[idx10 - 1 + idx3] = sqrt2 * (tr1 - ti1);
            var idx4 = idx3 + idx0;
            out[idx10 - 1 + idx4] = ti2 + ti2;
            var idx5 = idx4 + idx0;
            out[idx10 - 1 + idx5] = -sqrt2 * (tr1 + ti1);
        }
    }

    /*-------------------------------------------------
       radf5: Real FFT's forward processing of factor 5
      -------------------------------------------------*/
    private void radf5(int ido, int l1, float[] in, int in_off, float[] out, int out_off, int offset) {
        final var tr11 = 0.309016994374947451262869435595348477f;
        final var ti11 = 0.951056516295153531181938433292089030f;
        final var tr12 = -0.809016994374947340240566973079694435f;
        final var ti12 = 0.587785252292473248125759255344746634f;
        float ci4, ci5, cr2, cr3;
        var iw1 = offset;
        var iw2 = iw1 + ido;

        var idx0 = l1 * ido;
        for (var k = 0; k < l1; k++) {
            var idx1 = k * ido;
            var idx2 = 5 * idx1;
            var idx3 = idx2 + ido;
            var idx4 = idx3 + ido;
            var idx7 = idx1 + idx0;
            var idx8 = idx7 + idx0;
            var idx9 = idx8 + idx0;
            var idx10 = idx9 + idx0;

            var i1r = in[in_off + idx1];
            var i2r = in[in_off + idx7];
            var i3r = in[in_off + idx8];
            var i4r = in[in_off + idx9];
            var i5r = in[in_off + idx10];

            cr2 = i5r + i2r;
            ci5 = i5r - i2r;
            cr3 = i4r + i3r;
            ci4 = i4r - i3r;

            out[out_off + idx2] = i1r + cr2 + cr3;
            var idx11 = out_off + ido - 1;
            out[idx11 + idx3] = i1r + tr11 * cr2 + tr12 * cr3;
            out[out_off + idx4] = ti11 * ci5 + ti12 * ci4;
            var idx5 = idx4 + ido;
            out[idx11 + idx5] = i1r + tr12 * cr2 + tr11 * cr3;
            var idx6 = idx5 + ido;
            out[out_off + idx6] = ti12 * ci5 - ti11 * ci4;
        }
        if (ido == 1)
            return;
        var iw3 = iw2 + ido;
        var iw4 = iw3 + ido;
        for (var k = 0; k < l1; ++k) {
            var idx1 = k * ido;
            var idx2 = 5 * idx1;
            var idx3 = idx2 + ido;
            var idx4 = idx3 + ido;
            var idx5 = idx4 + ido;
            var idx6 = idx5 + ido;
            var idx7 = idx1 + idx0;
            var idx8 = idx7 + idx0;
            var idx9 = idx8 + idx0;
            var idx10 = idx9 + idx0;
            for (var i = 2; i < ido; i += 2) {
                var widx1 = i - 1 + iw1;
                var widx2 = i - 1 + iw2;
                var widx3 = i - 1 + iw3;
                var widx4 = i - 1 + iw4;
                var w1r = wtable_r[widx1 - 1];
                var w1i = wtable_r[widx1];
                var w2r = wtable_r[widx2 - 1];
                var w2i = wtable_r[widx2];
                var w3r = wtable_r[widx3 - 1];
                var w3i = wtable_r[widx3];
                var w4r = wtable_r[widx4 - 1];
                var w4i = wtable_r[widx4];

                var ic = ido - i;
                var idx15 = in_off + i;
                var idx16 = out_off + i;

                var iidx1 = idx15 + idx1;
                var iidx2 = idx15 + idx7;
                var iidx3 = idx15 + idx8;
                var iidx4 = idx15 + idx9;
                var iidx5 = idx15 + idx10;

                var i1i = in[iidx1 - 1];
                var i1r = in[iidx1];
                var i2i = in[iidx2 - 1];
                var i2r = in[iidx2];
                var i3i = in[iidx3 - 1];
                var i3r = in[iidx3];
                var i4i = in[iidx4 - 1];
                var i4r = in[iidx4];
                var i5i = in[iidx5 - 1];
                var i5r = in[iidx5];

                var dr2 = w1r * i2i + w1i * i2r;
                var dr5 = w4r * i5i + w4i * i5r;

                cr2 = dr2 + dr5;
                ci5 = dr5 - dr2;
                var dr4 = w3r * i4i + w3i * i4r;
                var dr3 = w2r * i3i + w2i * i3r;
                cr3 = dr3 + dr4;
                ci4 = dr4 - dr3;
                var di4 = w3r * i4r - w3i * i4i;
                var di3 = w2r * i3r - w2i * i3i;
                var cr4 = di3 - di4;
                var ci3 = di3 + di4;

                var tr2 = i1i + tr11 * cr2 + tr12 * cr3;
                var tr3 = i1i + tr12 * cr2 + tr11 * cr3;
                var ti5 = ti11 * ci5 + ti12 * ci4;
                var ti4 = ti12 * ci5 - ti11 * ci4;

                var oidx1 = idx16 + idx2;

                out[oidx1 - 1] = i1i + cr2 + cr3;
                var di5 = w4r * i5r - w4i * i5i;
                var di2 = w1r * i2r - w1i * i2i;
                var ci2 = di2 + di5;
                out[oidx1] = i1r + ci2 + ci3;
                var oidx3 = idx16 + idx4;
                var cr5 = di2 - di5;
                var tr5 = ti11 * cr5 + ti12 * cr4;
                out[oidx3 - 1] = tr2 + tr5;
                var idx17 = out_off + ic;
                var oidx2 = idx17 + idx3;
                out[oidx2 - 1] = tr2 - tr5;
                var ti2 = i1r + tr11 * ci2 + tr12 * ci3;
                out[oidx3] = ti2 + ti5;
                out[oidx2] = ti5 - ti2;
                var oidx5 = idx16 + idx6;
                var tr4 = ti12 * cr5 - ti11 * cr4;
                out[oidx5 - 1] = tr3 + tr4;
                var oidx4 = idx17 + idx5;
                out[oidx4 - 1] = tr3 - tr4;
                var ti3 = i1r + tr12 * ci2 + tr11 * ci3;
                out[oidx5] = ti3 + ti4;
                out[oidx4] = ti4 - ti3;
            }
        }
    }

    /*-------------------------------------------------
       radb5: Real FFT's backward processing of factor 5
      -------------------------------------------------*/
    private void radb5(int ido, int l1, float[] in, int in_off, float[] out, int out_off, int offset) {
        final var tr11 = 0.309016994374947451262869435595348477f;
        final var ti11 = 0.951056516295153531181938433292089030f;
        final var tr12 = -0.809016994374947340240566973079694435f;
        final var ti12 = 0.587785252292473248125759255344746634f;
        float ci4, ci5, cr2, cr3, ti4, ti5, tr2, tr3;
        var iw1 = offset;
        var iw2 = iw1 + ido;

        var idx0 = l1 * ido;
        for (var k = 0; k < l1; k++) {
            var idx1 = k * ido;
            var idx2 = 5 * idx1;
            var idx3 = idx2 + ido;
            var idx4 = idx3 + ido;
            var idx7 = idx1 + idx0;
            var idx8 = idx7 + idx0;

            var i1r = in[in_off + idx2];

            ti5 = 2 * in[in_off + idx4];
            var idx5 = idx4 + ido;
            var idx6 = idx5 + ido;
            ti4 = 2 * in[in_off + idx6];
            var idx11 = in_off + ido - 1;
            tr2 = 2 * in[idx11 + idx3];
            tr3 = 2 * in[idx11 + idx5];
            cr2 = i1r + tr11 * tr2 + tr12 * tr3;
            cr3 = i1r + tr12 * tr2 + tr11 * tr3;
            ci5 = ti11 * ti5 + ti12 * ti4;
            ci4 = ti12 * ti5 - ti11 * ti4;

            out[out_off + idx1] = i1r + tr2 + tr3;
            out[out_off + idx7] = cr2 - ci5;
            out[out_off + idx8] = cr3 - ci4;
            var idx9 = idx8 + idx0;
            out[out_off + idx9] = cr3 + ci4;
            var idx10 = idx9 + idx0;
            out[out_off + idx10] = cr2 + ci5;
        }
        if (ido == 1)
            return;
        var iw3 = iw2 + ido;
        var iw4 = iw3 + ido;
        for (var k = 0; k < l1; ++k) {
            var idx1 = k * ido;
            var idx2 = 5 * idx1;
            var idx3 = idx2 + ido;
            var idx4 = idx3 + ido;
            var idx5 = idx4 + ido;
            var idx6 = idx5 + ido;
            var idx7 = idx1 + idx0;
            var idx8 = idx7 + idx0;
            var idx9 = idx8 + idx0;
            var idx10 = idx9 + idx0;
            for (var i = 2; i < ido; i += 2) {
                var ic = ido - i;
                var widx1 = i - 1 + iw1;
                var widx2 = i - 1 + iw2;
                var widx3 = i - 1 + iw3;
                var widx4 = i - 1 + iw4;
                var w1r = wtable_r[widx1 - 1];
                var w1i = wtable_r[widx1];
                var w2r = wtable_r[widx2 - 1];
                var w2i = wtable_r[widx2];
                var w3r = wtable_r[widx3 - 1];
                var w3i = wtable_r[widx3];
                var w4r = wtable_r[widx4 - 1];
                var w4i = wtable_r[widx4];

                var idx15 = in_off + i;
                var idx16 = in_off + ic;
                var idx17 = out_off + i;

                var iidx1 = idx15 + idx2;
                var iidx2 = idx16 + idx3;
                var iidx3 = idx15 + idx4;
                var iidx4 = idx16 + idx5;
                var iidx5 = idx15 + idx6;

                var i1i = in[iidx1 - 1];
                var i1r = in[iidx1];
                var i2i = in[iidx2 - 1];
                var i2r = in[iidx2];
                var i3i = in[iidx3 - 1];
                var i3r = in[iidx3];
                var i4i = in[iidx4 - 1];
                var i4r = in[iidx4];
                var i5i = in[iidx5 - 1];
                var i5r = in[iidx5];

                ti5 = i3r + i2r;
                ti4 = i5r + i4r;
                tr2 = i3i + i2i;
                tr3 = i5i + i4i;

                cr2 = i1i + tr11 * tr2 + tr12 * tr3;
                cr3 = i1i + tr12 * tr2 + tr11 * tr3;
                ci5 = ti11 * ti5 + ti12 * ti4;
                ci4 = ti12 * ti5 - ti11 * ti4;
                var dr3 = cr3 - ci4;
                var dr4 = cr3 + ci4;
                var dr5 = cr2 + ci5;
                var dr2 = cr2 - ci5;

                var oidx1 = idx17 + idx1;

                out[oidx1 - 1] = i1i + tr2 + tr3;
                var ti3 = i5r - i4r;
                var ti2 = i3r - i2r;
                out[oidx1] = i1r + ti2 + ti3;
                var oidx2 = idx17 + idx7;
                var tr4 = i5i - i4i;
                var tr5 = i3i - i2i;
                var cr5 = ti11 * tr5 + ti12 * tr4;
                var ci2 = i1r + tr11 * ti2 + tr12 * ti3;
                var di2 = ci2 + cr5;
                out[oidx2 - 1] = w1r * dr2 - w1i * di2;
                out[oidx2] = w1r * di2 + w1i * dr2;
                var oidx3 = idx17 + idx8;
                var cr4 = ti12 * tr5 - ti11 * tr4;
                var ci3 = i1r + tr12 * ti2 + tr11 * ti3;
                var di3 = ci3 + cr4;
                out[oidx3 - 1] = w2r * dr3 - w2i * di3;
                out[oidx3] = w2r * di3 + w2i * dr3;
                var oidx4 = idx17 + idx9;
                var di4 = ci3 - cr4;
                out[oidx4 - 1] = w3r * dr4 - w3i * di4;
                out[oidx4] = w3r * di4 + w3i * dr4;
                var oidx5 = idx17 + idx10;
                var di5 = ci2 - cr5;
                out[oidx5 - 1] = w4r * dr5 - w4i * di5;
                out[oidx5] = w4r * di5 + w4i * dr5;
            }
        }
    }

    /*---------------------------------------------------------
       radfg: Real FFT's forward processing of general factor
      --------------------------------------------------------*/
    private void radfg(int ido, int ip, int l1, int idl1, float[] in, int in_off, float[] out, int out_off, int offset) {
        int jc;

        var arg = TWO_PI / (float) ip;
        var dcp = (float) Math.cos(arg);
        var dsp = (float) Math.sin(arg);
        var ipph = (ip + 1) / 2;
        var nbd = (ido - 1) / 2;
        if (ido != 1) {
            if (idl1 >= 0) System.arraycopy(in, in_off, out, out_off, idl1);
            for (var j = 1; j < ip; j++) {
                var idx1 = j * l1 * ido;
                for (var k = 0; k < l1; k++) {
                    var idx2 = k * ido + idx1;
                    out[out_off + idx2] = in[in_off + idx2];
                }
            }
            var iw1 = offset;
            float w1i;
            float w1r;
            int is;
            int idij;
            if (nbd <= l1) {
                is = -ido;
                for (var j = 1; j < ip; j++) {
                    is += ido;
                    idij = is - 1;
                    var idx1 = j * l1 * ido;
                    for (var i = 2; i < ido; i += 2) {
                        idij += 2;
                        var idx2 = idij + iw1;
                        var idx4 = in_off + i;
                        var idx5 = out_off + i;
                        w1r = wtable_r[idx2 - 1];
                        w1i = wtable_r[idx2];
                        for (var k = 0; k < l1; k++) {
                            var idx3 = k * ido + idx1;
                            var oidx1 = idx5 + idx3;
                            var iidx1 = idx4 + idx3;
                            var i1i = in[iidx1 - 1];
                            var i1r = in[iidx1];

                            out[oidx1 - 1] = w1r * i1i + w1i * i1r;
                            out[oidx1] = w1r * i1r - w1i * i1i;
                        }
                    }
                }
            } else {
                is = -ido;
                for (var j = 1; j < ip; j++) {
                    is += ido;
                    var idx1 = j * l1 * ido;
                    for (var k = 0; k < l1; k++) {
                        idij = is - 1;
                        var idx3 = k * ido + idx1;
                        for (var i = 2; i < ido; i += 2) {
                            idij += 2;
                            var idx2 = idij + iw1;
                            w1r = wtable_r[idx2 - 1];
                            w1i = wtable_r[idx2];
                            var oidx1 = out_off + i + idx3;
                            var iidx1 = in_off + i + idx3;
                            var i1i = in[iidx1 - 1];
                            var i1r = in[iidx1];

                            out[oidx1 - 1] = w1r * i1i + w1i * i1r;
                            out[oidx1] = w1r * i1r - w1i * i1i;
                        }
                    }
                }
            }
            if (nbd >= l1) {
                for (var j = 1; j < ipph; j++) {
                    jc = ip - j;
                    var idx1 = j * l1 * ido;
                    var idx2 = jc * l1 * ido;
                    for (var k = 0; k < l1; k++) {
                        var idx3 = k * ido + idx1;
                        var idx4 = k * ido + idx2;
                        for (var i = 2; i < ido; i += 2) {
                            var idx5 = in_off + i;
                            var idx6 = out_off + i;
                            var iidx1 = idx5 + idx3;
                            var oidx1 = idx6 + idx3;
                            var oidx2 = idx6 + idx4;
                            var o1i = out[oidx1 - 1];
                            var o1r = out[oidx1];
                            var o2i = out[oidx2 - 1];
                            var o2r = out[oidx2];

                            in[iidx1 - 1] = o1i + o2i;
                            in[iidx1] = o1r + o2r;

                            var iidx2 = idx5 + idx4;
                            in[iidx2 - 1] = o1r - o2r;
                            in[iidx2] = o2i - o1i;
                        }
                    }
                }
            } else {
                for (var j = 1; j < ipph; j++) {
                    jc = ip - j;
                    var idx1 = j * l1 * ido;
                    var idx2 = jc * l1 * ido;
                    for (var i = 2; i < ido; i += 2) {
                        var idx5 = in_off + i;
                        var idx6 = out_off + i;
                        for (var k = 0; k < l1; k++) {
                            var idx3 = k * ido + idx1;
                            var idx4 = k * ido + idx2;
                            var iidx1 = idx5 + idx3;
                            var oidx1 = idx6 + idx3;
                            var oidx2 = idx6 + idx4;
                            var o1i = out[oidx1 - 1];
                            var o1r = out[oidx1];
                            var o2i = out[oidx2 - 1];
                            var o2r = out[oidx2];

                            in[iidx1 - 1] = o1i + o2i;
                            in[iidx1] = o1r + o2r;
                            var iidx2 = idx5 + idx4;
                            in[iidx2 - 1] = o1r - o2r;
                            in[iidx2] = o2i - o1i;
                        }
                    }
                }
            }
        } else {
            System.arraycopy(out, out_off, in, in_off, idl1);
        }
        for (var j = 1; j < ipph; j++) {
            jc = ip - j;
            var idx1 = j * l1 * ido;
            var idx2 = jc * l1 * ido;
            for (var k = 0; k < l1; k++) {
                var idx3 = k * ido + idx1;
                var idx4 = k * ido + idx2;
                var oidx1 = out_off + idx3;
                var oidx2 = out_off + idx4;
                var o1r = out[oidx1];
                var o2r = out[oidx2];

                in[in_off + idx3] = o1r + o2r;
                in[in_off + idx4] = o2r - o1r;
            }
        }

        float ar1 = 1;
        float ai1 = 0;
        var idx0 = (ip - 1) * idl1;
        for (var l = 1; l < ipph; l++) {
            var lc = ip - l;
            var ar1h = dcp * ar1 - dsp * ai1;
            ai1 = dcp * ai1 + dsp * ar1;
            ar1 = ar1h;
            var idx1 = l * idl1;
            var idx2 = lc * idl1;
            for (var ik = 0; ik < idl1; ik++) {
                var idx3 = out_off + ik;
                var idx4 = in_off + ik;
                out[idx3 + idx1] = in[idx4] + ar1 * in[idx4 + idl1];
                out[idx3 + idx2] = ai1 * in[idx4 + idx0];
            }
            var dc2 = ar1;
            var ds2 = ai1;
            var ar2 = ar1;
            var ai2 = ai1;
            for (var j = 2; j < ipph; j++) {
                jc = ip - j;
                var ar2h = dc2 * ar2 - ds2 * ai2;
                ai2 = dc2 * ai2 + ds2 * ar2;
                ar2 = ar2h;
                var idx3 = j * idl1;
                var idx4 = jc * idl1;
                for (var ik = 0; ik < idl1; ik++) {
                    var idx5 = out_off + ik;
                    var idx6 = in_off + ik;
                    out[idx5 + idx1] += ar2 * in[idx6 + idx3];
                    out[idx5 + idx2] += ai2 * in[idx6 + idx4];
                }
            }
        }
        for (var j = 1; j < ipph; j++) {
            var idx1 = j * idl1;
            for (var ik = 0; ik < idl1; ik++) {
                out[out_off + ik] += in[in_off + ik + idx1];
            }
        }

        if (ido >= l1) {
            for (var k = 0; k < l1; k++) {
                var idx1 = k * ido;
                var idx2 = idx1 * ip;
                for (var i = 0; i < ido; i++) {
                    in[in_off + i + idx2] = out[out_off + i + idx1];
                }
            }
        } else {
            for (var i = 0; i < ido; i++) {
                for (var k = 0; k < l1; k++) {
                    var idx1 = k * ido;
                    in[in_off + i + idx1 * ip] = out[out_off + i + idx1];
                }
            }
        }
        var idx01 = ip * ido;
        int j2;
        for (var j = 1; j < ipph; j++) {
            jc = ip - j;
            j2 = 2 * j;
            var idx1 = j * l1 * ido;
            var idx2 = jc * l1 * ido;
            var idx3 = j2 * ido;
            for (var k = 0; k < l1; k++) {
                var idx4 = k * ido;
                var idx5 = idx4 + idx1;
                var idx7 = k * idx01;
                in[in_off + ido - 1 + idx3 - ido + idx7] = out[out_off + idx5];
                var idx6 = idx4 + idx2;
                in[in_off + idx3 + idx7] = out[out_off + idx6];
            }
        }
        if (ido == 1)
            return;
        int ic;
        if (nbd >= l1) {
            for (var j = 1; j < ipph; j++) {
                jc = ip - j;
                j2 = 2 * j;
                var idx1 = j * l1 * ido;
                var idx2 = jc * l1 * ido;
                var idx3 = j2 * ido;
                for (var k = 0; k < l1; k++) {
                    var idx4 = k * idx01;
                    var idx5 = k * ido;
                    for (var i = 2; i < ido; i += 2) {
                        ic = ido - i;
                        var idx6 = in_off + i;
                        var idx7 = in_off + ic;
                        var idx8 = out_off + i;
                        var iidx1 = idx6 + idx3 + idx4;
                        var oidx1 = idx8 + idx5 + idx1;
                        var oidx2 = idx8 + idx5 + idx2;
                        var o1i = out[oidx1 - 1];
                        var o1r = out[oidx1];
                        var o2i = out[oidx2 - 1];
                        var o2r = out[oidx2];

                        in[iidx1 - 1] = o1i + o2i;
                        var iidx2 = idx7 + idx3 - ido + idx4;
                        in[iidx2 - 1] = o1i - o2i;
                        in[iidx1] = o1r + o2r;
                        in[iidx2] = o2r - o1r;
                    }
                }
            }
        } else {
            for (var j = 1; j < ipph; j++) {
                jc = ip - j;
                j2 = 2 * j;
                var idx1 = j * l1 * ido;
                var idx2 = jc * l1 * ido;
                var idx3 = j2 * ido;
                for (var i = 2; i < ido; i += 2) {
                    ic = ido - i;
                    var idx6 = in_off + i;
                    var idx7 = in_off + ic;
                    var idx8 = out_off + i;
                    for (var k = 0; k < l1; k++) {
                        var idx4 = k * idx01;
                        var idx5 = k * ido;
                        var iidx1 = idx6 + idx3 + idx4;
                        var oidx1 = idx8 + idx5 + idx1;
                        var oidx2 = idx8 + idx5 + idx2;
                        var o1i = out[oidx1 - 1];
                        var o1r = out[oidx1];
                        var o2i = out[oidx2 - 1];
                        var o2r = out[oidx2];

                        in[iidx1 - 1] = o1i + o2i;
                        var iidx2 = idx7 + idx3 - ido + idx4;
                        in[iidx2 - 1] = o1i - o2i;
                        in[iidx1] = o1r + o2r;
                        in[iidx2] = o2r - o1r;
                    }
                }
            }
        }
    }

    /*---------------------------------------------------------
       radbg: Real FFT's backward processing of general factor
      --------------------------------------------------------*/
    private void radbg(int ido, int ip, int l1, int idl1, float[] in, int in_off, float[] out, int out_off, int offset) {

        var arg = TWO_PI / (float) ip;
        var dcp = (float) Math.cos(arg);
        var dsp = (float) Math.sin(arg);
        var idx0 = ip * ido;
        if (ido >= l1) {
            for (var k = 0; k < l1; k++) {
                var idx1 = k * ido;
                var idx2 = k * idx0;
                for (var i = 0; i < ido; i++) {
                    out[out_off + i + idx1] = in[in_off + i + idx2];
                }
            }
        } else {
            for (var i = 0; i < ido; i++) {
                var idx1 = out_off + i;
                var idx2 = in_off + i;
                for (var k = 0; k < l1; k++) {
                    out[idx1 + k * ido] = in[idx2 + k * idx0];
                }
            }
        }
        var iidx0 = in_off + ido - 1;
        var ipph = (ip + 1) / 2;
        int jc;
        for (var j = 1; j < ipph; j++) {
            jc = ip - j;
            var j2 = 2 * j;
            var idx1 = j * l1 * ido;
            var idx2 = jc * l1 * ido;
            var idx3 = j2 * ido;
            for (var k = 0; k < l1; k++) {
                var idx4 = k * ido;
                var idx5 = idx4 * ip;
                var iidx1 = iidx0 + idx3 + idx5 - ido;
                var iidx2 = in_off + idx3 + idx5;
                var i1r = in[iidx1];
                var i2r = in[iidx2];

                out[out_off + idx4 + idx1] = i1r + i1r;
                out[out_off + idx4 + idx2] = i2r + i2r;
            }
        }

        var nbd = (ido - 1) / 2;
        if (ido != 1) {
            int ic;
            if (nbd >= l1) {
                for (var j = 1; j < ipph; j++) {
                    jc = ip - j;
                    var idx1 = j * l1 * ido;
                    var idx2 = jc * l1 * ido;
                    var idx3 = 2 * j * ido;
                    for (var k = 0; k < l1; k++) {
                        var idx4 = k * ido + idx1;
                        var idx5 = k * ido + idx2;
                        var idx6 = k * ip * ido + idx3;
                        for (var i = 2; i < ido; i += 2) {
                            ic = ido - i;
                            var idx7 = out_off + i;
                            var idx8 = in_off + ic;
                            var idx9 = in_off + i;
                            var oidx1 = idx7 + idx4;
                            var iidx1 = idx9 + idx6;
                            var iidx2 = idx8 + idx6 - ido;
                            var a1i = in[iidx1 - 1];
                            var a1r = in[iidx1];
                            var a2i = in[iidx2 - 1];
                            var a2r = in[iidx2];

                            out[oidx1 - 1] = a1i + a2i;
                            var oidx2 = idx7 + idx5;
                            out[oidx2 - 1] = a1i - a2i;
                            out[oidx1] = a1r - a2r;
                            out[oidx2] = a1r + a2r;
                        }
                    }
                }
            } else {
                for (var j = 1; j < ipph; j++) {
                    jc = ip - j;
                    var idx1 = j * l1 * ido;
                    var idx2 = jc * l1 * ido;
                    var idx3 = 2 * j * ido;
                    for (var i = 2; i < ido; i += 2) {
                        ic = ido - i;
                        var idx7 = out_off + i;
                        var idx8 = in_off + ic;
                        var idx9 = in_off + i;
                        for (var k = 0; k < l1; k++) {
                            var idx4 = k * ido + idx1;
                            var idx5 = k * ido + idx2;
                            var idx6 = k * ip * ido + idx3;
                            var oidx1 = idx7 + idx4;
                            var iidx1 = idx9 + idx6;
                            var iidx2 = idx8 + idx6 - ido;
                            var a1i = in[iidx1 - 1];
                            var a1r = in[iidx1];
                            var a2i = in[iidx2 - 1];
                            var a2r = in[iidx2];

                            out[oidx1 - 1] = a1i + a2i;
                            var oidx2 = idx7 + idx5;
                            out[oidx2 - 1] = a1i - a2i;
                            out[oidx1] = a1r - a2r;
                            out[oidx2] = a1r + a2r;
                        }
                    }
                }
            }
        }

        float ar1 = 1;
        float ai1 = 0;
        var idx01 = (ip - 1) * idl1;
        for (var l = 1; l < ipph; l++) {
            var lc = ip - l;
            var ar1h = dcp * ar1 - dsp * ai1;
            ai1 = dcp * ai1 + dsp * ar1;
            ar1 = ar1h;
            var idx1 = l * idl1;
            var idx2 = lc * idl1;
            for (var ik = 0; ik < idl1; ik++) {
                var idx3 = in_off + ik;
                var idx4 = out_off + ik;
                in[idx3 + idx1] = out[idx4] + ar1 * out[idx4 + idl1];
                in[idx3 + idx2] = ai1 * out[idx4 + idx01];
            }
            var dc2 = ar1;
            var ds2 = ai1;
            var ar2 = ar1;
            var ai2 = ai1;
            for (var j = 2; j < ipph; j++) {
                jc = ip - j;
                var ar2h = dc2 * ar2 - ds2 * ai2;
                ai2 = dc2 * ai2 + ds2 * ar2;
                ar2 = ar2h;
                var idx5 = j * idl1;
                var idx6 = jc * idl1;
                for (var ik = 0; ik < idl1; ik++) {
                    var idx7 = in_off + ik;
                    var idx8 = out_off + ik;
                    in[idx7 + idx1] += ar2 * out[idx8 + idx5];
                    in[idx7 + idx2] += ai2 * out[idx8 + idx6];
                }
            }
        }
        for (var j = 1; j < ipph; j++) {
            var idx1 = j * idl1;
            for (var ik = 0; ik < idl1; ik++) {
                var idx2 = out_off + ik;
                out[idx2] += out[idx2 + idx1];
            }
        }
        for (var j = 1; j < ipph; j++) {
            jc = ip - j;
            var idx1 = j * l1 * ido;
            var idx2 = jc * l1 * ido;
            for (var k = 0; k < l1; k++) {
                var idx3 = k * ido;
                var oidx1 = out_off + idx3;
                var iidx1 = in_off + idx3 + idx1;
                var iidx2 = in_off + idx3 + idx2;
                var i1r = in[iidx1];
                var i2r = in[iidx2];

                out[oidx1 + idx1] = i1r - i2r;
                out[oidx1 + idx2] = i1r + i2r;
            }
        }

        if (ido == 1)
            return;
        if (nbd >= l1) {
            for (var j = 1; j < ipph; j++) {
                jc = ip - j;
                var idx1 = j * l1 * ido;
                var idx2 = jc * l1 * ido;
                for (var k = 0; k < l1; k++) {
                    var idx3 = k * ido;
                    for (var i = 2; i < ido; i += 2) {
                        var idx4 = out_off + i;
                        var idx5 = in_off + i;
                        var oidx1 = idx4 + idx3 + idx1;
                        var iidx1 = idx5 + idx3 + idx1;
                        var iidx2 = idx5 + idx3 + idx2;
                        var i1i = in[iidx1 - 1];
                        var i1r = in[iidx1];
                        var i2i = in[iidx2 - 1];
                        var i2r = in[iidx2];

                        out[oidx1 - 1] = i1i - i2r;
                        var oidx2 = idx4 + idx3 + idx2;
                        out[oidx2 - 1] = i1i + i2r;
                        out[oidx1] = i1r + i2i;
                        out[oidx2] = i1r - i2i;
                    }
                }
            }
        } else {
            for (var j = 1; j < ipph; j++) {
                jc = ip - j;
                var idx1 = j * l1 * ido;
                var idx2 = jc * l1 * ido;
                for (var i = 2; i < ido; i += 2) {
                    var idx4 = out_off + i;
                    var idx5 = in_off + i;
                    for (var k = 0; k < l1; k++) {
                        var idx3 = k * ido;
                        var oidx1 = idx4 + idx3 + idx1;
                        var iidx1 = idx5 + idx3 + idx1;
                        var iidx2 = idx5 + idx3 + idx2;
                        var i1i = in[iidx1 - 1];
                        var i1r = in[iidx1];
                        var i2i = in[iidx2 - 1];
                        var i2r = in[iidx2];

                        out[oidx1 - 1] = i1i - i2r;
                        var oidx2 = idx4 + idx3 + idx2;
                        out[oidx2 - 1] = i1i + i2r;
                        out[oidx1] = i1r + i2i;
                        out[oidx2] = i1r - i2i;
                    }
                }
            }
        }
        System.arraycopy(out, out_off, in, in_off, idl1);
        for (var j = 1; j < ip; j++) {
            var idx1 = j * l1 * ido;
            for (var k = 0; k < l1; k++) {
                var idx2 = k * ido + idx1;
                in[in_off + idx2] = out[out_off + idx2];
            }
        }
        var iw1 = offset;
        float w1i;
        float w1r;
        int is;
        int idij;
        if (nbd <= l1) {
            is = -ido;
            for (var j = 1; j < ip; j++) {
                is += ido;
                idij = is - 1;
                var idx1 = j * l1 * ido;
                for (var i = 2; i < ido; i += 2) {
                    idij += 2;
                    var idx2 = idij + iw1;
                    w1r = wtable_r[idx2 - 1];
                    w1i = wtable_r[idx2];
                    var idx4 = in_off + i;
                    var idx5 = out_off + i;
                    for (var k = 0; k < l1; k++) {
                        var idx3 = k * ido + idx1;
                        var iidx1 = idx4 + idx3;
                        var oidx1 = idx5 + idx3;
                        var o1i = out[oidx1 - 1];
                        var o1r = out[oidx1];

                        in[iidx1 - 1] = w1r * o1i - w1i * o1r;
                        in[iidx1] = w1r * o1r + w1i * o1i;
                    }
                }
            }
        } else {
            is = -ido;
            for (var j = 1; j < ip; j++) {
                is += ido;
                var idx1 = j * l1 * ido;
                for (var k = 0; k < l1; k++) {
                    idij = is - 1;
                    var idx3 = k * ido + idx1;
                    for (var i = 2; i < ido; i += 2) {
                        idij += 2;
                        var idx2 = idij + iw1;
                        w1r = wtable_r[idx2 - 1];
                        w1i = wtable_r[idx2];
                        var idx4 = in_off + i;
                        var idx5 = out_off + i;
                        var iidx1 = idx4 + idx3;
                        var oidx1 = idx5 + idx3;
                        var o1i = out[oidx1 - 1];
                        var o1r = out[oidx1];

                        in[iidx1 - 1] = w1r * o1i - w1i * o1r;
                        in[iidx1] = w1r * o1r + w1i * o1i;

                    }
                }
            }
        }
    }

    /*---------------------------------------------------------
       cfftf1: further processing of Complex forward FFT
      --------------------------------------------------------*/
    private void cfftf(float[] a, int offa, int isign) {
        var nac = new int[1];

        nac[0] = 0;
        var iw2 = 4 * n;
        var nf = (int) wtable[1 + iw2];
        var na = 0;
        var l1 = 1;
        var twon = 2 * n;
        var iw1 = twon;
        var iw = iw1;
        var ch = new float[twon];
        for (var k1 = 2; k1 <= nf + 1; k1++) {
            var ip = (int) wtable[k1 + iw2];
            var l2 = ip * l1;
            var ido = n / l2;
            var idot = ido + ido;
            var idl1 = idot * l1;
            switch (ip) {
                case 4:
                    if (na == 0) {
                        passf4(idot, l1, a, offa, ch, 0, iw, isign);
                    } else {
                        passf4(idot, l1, ch, 0, a, offa, iw, isign);
                    }
                    na = 1 - na;
                    break;
                case 2:
                    if (na == 0) {
                        passf2(idot, l1, a, offa, ch, 0, iw, isign);
                    } else {
                        passf2(idot, l1, ch, 0, a, offa, iw, isign);
                    }
                    na = 1 - na;
                    break;
                case 3:
                    if (na == 0) {
                        passf3(idot, l1, a, offa, ch, 0, iw, isign);
                    } else {
                        passf3(idot, l1, ch, 0, a, offa, iw, isign);
                    }
                    na = 1 - na;
                    break;
                case 5:
                    if (na == 0) {
                        passf5(idot, l1, a, offa, ch, 0, iw, isign);
                    } else {
                        passf5(idot, l1, ch, 0, a, offa, iw, isign);
                    }
                    na = 1 - na;
                    break;
                default:
                    if (na == 0) {
                        passfg(nac, idot, ip, l1, idl1, a, offa, ch, 0, iw, isign);
                    } else {
                        passfg(nac, idot, ip, l1, idl1, ch, 0, a, offa, iw, isign);
                    }
                    if (nac[0] != 0)
                        na = 1 - na;
                    break;
            }
            l1 = l2;
            iw += (ip - 1) * idot;
        }
        if (na == 0)
            return;
        System.arraycopy(ch, 0, a, offa, twon);

    }

    private void passf2(int ido, int l1, float[] in, int in_off, float[] out, int out_off, int offset, int isign) {
        var idx = ido * l1;
        if (ido <= 2) {
            for (var k = 0; k < l1; k++) {
                var idx0 = k * ido;
                var iidx1 = in_off + 2 * idx0;
                var iidx2 = iidx1 + ido;
                var a1r = in[iidx1];
                var a1i = in[iidx1 + 1];
                var a2r = in[iidx2];
                var a2i = in[iidx2 + 1];

                var oidx1 = out_off + idx0;
                out[oidx1] = a1r + a2r;
                out[oidx1 + 1] = a1i + a2i;
                var oidx2 = oidx1 + idx;
                out[oidx2] = a1r - a2r;
                out[oidx2 + 1] = a1i - a2i;
            }
        } else {
            var iw1 = offset;
            for (var k = 0; k < l1; k++) {
                for (var i = 0; i < ido - 1; i += 2) {
                    var idx0 = k * ido;
                    var iidx1 = in_off + i + 2 * idx0;
                    var iidx2 = iidx1 + ido;
                    var i1r = in[iidx1];
                    var i1i = in[iidx1 + 1];
                    var i2r = in[iidx2];
                    var i2i = in[iidx2 + 1];

                    var widx1 = i + iw1;
                    var w1r = wtable[widx1];
                    var w1i = isign * wtable[widx1 + 1];

                    var oidx1 = out_off + i + idx0;
                    out[oidx1] = i1r + i2r;
                    out[oidx1 + 1] = i1i + i2i;
                    var oidx2 = oidx1 + idx;
                    var t1i = i1i - i2i;
                    var t1r = i1r - i2r;
                    out[oidx2] = w1r * t1r - w1i * t1i;
                    out[oidx2 + 1] = w1r * t1i + w1i * t1r;
                }
            }
        }
    }

    /*----------------------------------------------------------------------
       passf3: Complex FFT's forward/backward processing of factor 3;
       isign is +1 for backward and -1 for forward transforms
      ----------------------------------------------------------------------*/
    private void passf3(int ido, int l1, float[] in, int in_off, float[] out, int out_off, int offset, int isign) {
        final var taur = -0.5f;
        final var taui = 0.866025403784438707610604524234076962f;
        float ci2, ci3, cr2, cr3, ti2, tr2;

        var idxt = l1 * ido;

        if (ido == 2) {
            for (var k = 1; k <= l1; k++) {
                var iidx1 = in_off + (3 * k - 2) * 2;
                var iidx2 = iidx1 + ido;
                var iidx3 = iidx1 - ido;
                var i1r = in[iidx1];
                var i1i = in[iidx1 + 1];
                var i2r = in[iidx2];
                var i2i = in[iidx2 + 1];
                var i3r = in[iidx3];
                var i3i = in[iidx3 + 1];

                tr2 = i1r + i2r;
                cr2 = i3r + taur * tr2;
                ti2 = i1i + i2i;
                ci2 = i3i + taur * ti2;
                cr3 = isign * taui * (i1r - i2r);
                ci3 = isign * taui * (i1i - i2i);

                var oidx1 = out_off + (k - 1) * ido;
                out[oidx1] = in[iidx3] + tr2;
                out[oidx1 + 1] = i3i + ti2;
                var oidx2 = oidx1 + idxt;
                out[oidx2] = cr2 - ci3;
                out[oidx2 + 1] = ci2 + cr3;
                var oidx3 = oidx2 + idxt;
                out[oidx3] = cr2 + ci3;
                out[oidx3 + 1] = ci2 - cr3;
            }
        } else {
            var iw1 = offset;
            var iw2 = iw1 + ido;
            for (var k = 1; k <= l1; k++) {
                var idx1 = in_off + (3 * k - 2) * ido;
                var idx2 = out_off + (k - 1) * ido;
                for (var i = 0; i < ido - 1; i += 2) {
                    var iidx1 = i + idx1;
                    var iidx2 = iidx1 + ido;
                    var iidx3 = iidx1 - ido;
                    var a1r = in[iidx1];
                    var a1i = in[iidx1 + 1];
                    var a2r = in[iidx2];
                    var a2i = in[iidx2 + 1];
                    var a3r = in[iidx3];
                    var a3i = in[iidx3 + 1];

                    tr2 = a1r + a2r;
                    cr2 = a3r + taur * tr2;
                    ti2 = a1i + a2i;
                    ci2 = a3i + taur * ti2;
                    cr3 = isign * taui * (a1r - a2r);
                    ci3 = isign * taui * (a1i - a2i);
                    var dr2 = cr2 - ci3;
                    var dr3 = cr2 + ci3;
                    var di2 = ci2 + cr3;
                    var di3 = ci2 - cr3;

                    var widx1 = i + iw1;
                    var widx2 = i + iw2;
                    var w1r = wtable[widx1];
                    var w1i = isign * wtable[widx1 + 1];
                    var w2r = wtable[widx2];
                    var w2i = isign * wtable[widx2 + 1];

                    var oidx1 = i + idx2;
                    out[oidx1] = a3r + tr2;
                    out[oidx1 + 1] = a3i + ti2;
                    var oidx2 = oidx1 + idxt;
                    out[oidx2] = w1r * dr2 - w1i * di2;
                    out[oidx2 + 1] = w1r * di2 + w1i * dr2;
                    var oidx3 = oidx2 + idxt;
                    out[oidx3] = w2r * dr3 - w2i * di3;
                    out[oidx3 + 1] = w2r * di3 + w2i * dr3;
                }
            }
        }
    }

    /*----------------------------------------------------------------------
       passf4: Complex FFT's forward/backward processing of factor 4;
       isign is +1 for backward and -1 for forward transforms
      ----------------------------------------------------------------------*/
    private void passf4(int ido, int l1, float[] in, int in_off, float[] out, int out_off, int offset, int isign) {
        float ti1, ti2, ti3, ti4, tr1, tr2, tr3, tr4;
        var iw1 = offset;

        var idx0 = l1 * ido;
        if (ido == 2) {
            for (var k = 0; k < l1; k++) {
                var idxt1 = k * 2;
                var iidx1 = in_off + 4 * idxt1 + 1;
                var iidx2 = iidx1 + ido;
                var iidx3 = iidx2 + ido;
                var iidx4 = iidx3 + ido;

                var i1i = in[iidx1 - 1];
                var i1r = in[iidx1];
                var i2i = in[iidx2 - 1];
                var i2r = in[iidx2];
                var i3i = in[iidx3 - 1];
                var i3r = in[iidx3];
                var i4i = in[iidx4 - 1];
                var i4r = in[iidx4];

                ti1 = i1r - i3r;
                ti2 = i1r + i3r;
                tr4 = i4r - i2r;
                ti3 = i2r + i4r;
                tr1 = i1i - i3i;
                tr2 = i1i + i3i;
                ti4 = i2i - i4i;
                tr3 = i2i + i4i;

                var oidx1 = out_off + idxt1;
                var oidx2 = oidx1 + idx0;
                out[oidx1] = tr2 + tr3;
                out[oidx1 + 1] = ti2 + ti3;
                out[oidx2] = tr1 + isign * tr4;
                out[oidx2 + 1] = ti1 + isign * ti4;
                var oidx3 = oidx2 + idx0;
                out[oidx3] = tr2 - tr3;
                out[oidx3 + 1] = ti2 - ti3;
                var oidx4 = oidx3 + idx0;
                out[oidx4] = tr1 - isign * tr4;
                out[oidx4 + 1] = ti1 - isign * ti4;
            }
        } else {
            var iw2 = iw1 + ido;
            var iw3 = iw2 + ido;
            for (var k = 0; k < l1; k++) {
                var idx1 = k * ido;
                var idx2 = in_off + 1 + 4 * idx1;
                for (var i = 0; i < ido - 1; i += 2) {
                    var iidx1 = i + idx2;
                    var iidx2 = iidx1 + ido;
                    var iidx3 = iidx2 + ido;
                    var iidx4 = iidx3 + ido;
                    var i1i = in[iidx1 - 1];
                    var i1r = in[iidx1];
                    var i2i = in[iidx2 - 1];
                    var i2r = in[iidx2];
                    var i3i = in[iidx3 - 1];
                    var i3r = in[iidx3];
                    var i4i = in[iidx4 - 1];
                    var i4r = in[iidx4];

                    ti1 = i1r - i3r;
                    ti2 = i1r + i3r;
                    ti3 = i2r + i4r;
                    tr4 = i4r - i2r;
                    tr1 = i1i - i3i;
                    tr2 = i1i + i3i;
                    ti4 = i2i - i4i;
                    tr3 = i2i + i4i;
                    var cr3 = tr2 - tr3;
                    var ci3 = ti2 - ti3;
                    var cr2 = tr1 + isign * tr4;
                    var cr4 = tr1 - isign * tr4;
                    var ci2 = ti1 + isign * ti4;
                    var ci4 = ti1 - isign * ti4;

                    var widx1 = i + iw1;
                    var widx2 = i + iw2;
                    var widx3 = i + iw3;
                    var w1r = wtable[widx1];
                    var w1i = isign * wtable[widx1 + 1];
                    var w2r = wtable[widx2];
                    var w2i = isign * wtable[widx2 + 1];
                    var w3r = wtable[widx3];
                    var w3i = isign * wtable[widx3 + 1];

                    var oidx1 = out_off + i + idx1;
                    var oidx2 = oidx1 + idx0;
                    out[oidx1] = tr2 + tr3;
                    out[oidx1 + 1] = ti2 + ti3;
                    out[oidx2] = w1r * cr2 - w1i * ci2;
                    out[oidx2 + 1] = w1r * ci2 + w1i * cr2;
                    var oidx3 = oidx2 + idx0;
                    out[oidx3] = w2r * cr3 - w2i * ci3;
                    out[oidx3 + 1] = w2r * ci3 + w2i * cr3;
                    var oidx4 = oidx3 + idx0;
                    out[oidx4] = w3r * cr4 - w3i * ci4;
                    out[oidx4 + 1] = w3r * ci4 + w3i * cr4;
                }
            }
        }
    }

    /*----------------------------------------------------------------------
       passf5: Complex FFT's forward/backward processing of factor 5;
       isign is +1 for backward and -1 for forward transforms
      ----------------------------------------------------------------------*/
    private void passf5(int ido, int l1, float[] in, int in_off, float[] out, int out_off, int offset, int isign)
    /* isign==-1 for forward transform and+1 for backward transform */ {
        final var tr11 = 0.309016994374947451262869435595348477f;
        final var ti11 = 0.951056516295153531181938433292089030f;
        final var tr12 = -0.809016994374947340240566973079694435f;
        final var ti12 = 0.587785252292473248125759255344746634f;
        float ci2, ci3, ci4, ci5, cr2, cr3, cr5, cr4, ti2, ti3, ti4, ti5, tr2, tr3, tr4, tr5;

        var iw1 = offset;
        var iw2 = iw1 + ido;

        var idx0 = l1 * ido;

        if (ido == 2) {
            for (var k = 1; k <= l1; ++k) {
                var iidx1 = in_off + (5 * k - 4) * 2 + 1;
                var iidx2 = iidx1 + ido;
                var iidx3 = iidx1 - ido;
                var iidx4 = iidx2 + ido;
                var iidx5 = iidx4 + ido;

                var i1i = in[iidx1 - 1];
                var i1r = in[iidx1];
                var i2i = in[iidx2 - 1];
                var i2r = in[iidx2];
                var i3i = in[iidx3 - 1];
                var i3r = in[iidx3];
                var i4i = in[iidx4 - 1];
                var i4r = in[iidx4];
                var i5i = in[iidx5 - 1];
                var i5r = in[iidx5];

                ti5 = i1r - i5r;
                ti2 = i1r + i5r;
                ti4 = i2r - i4r;
                ti3 = i2r + i4r;
                tr5 = i1i - i5i;
                tr2 = i1i + i5i;
                tr4 = i2i - i4i;
                tr3 = i2i + i4i;
                cr2 = i3i + tr11 * tr2 + tr12 * tr3;
                ci2 = i3r + tr11 * ti2 + tr12 * ti3;
                cr3 = i3i + tr12 * tr2 + tr11 * tr3;
                ci3 = i3r + tr12 * ti2 + tr11 * ti3;
                cr5 = isign * (ti11 * tr5 + ti12 * tr4);
                ci5 = isign * (ti11 * ti5 + ti12 * ti4);
                cr4 = isign * (ti12 * tr5 - ti11 * tr4);
                ci4 = isign * (ti12 * ti5 - ti11 * ti4);

                var oidx1 = out_off + (k - 1) * ido;
                var oidx2 = oidx1 + idx0;
                var oidx3 = oidx2 + idx0;
                out[oidx1] = i3i + tr2 + tr3;
                out[oidx1 + 1] = i3r + ti2 + ti3;
                out[oidx2] = cr2 - ci5;
                out[oidx2 + 1] = ci2 + cr5;
                out[oidx3] = cr3 - ci4;
                out[oidx3 + 1] = ci3 + cr4;
                var oidx4 = oidx3 + idx0;
                out[oidx4] = cr3 + ci4;
                out[oidx4 + 1] = ci3 - cr4;
                var oidx5 = oidx4 + idx0;
                out[oidx5] = cr2 + ci5;
                out[oidx5 + 1] = ci2 - cr5;
            }
        } else {
            var iw3 = iw2 + ido;
            var iw4 = iw3 + ido;
            for (var k = 1; k <= l1; k++) {
                var idx1 = in_off + 1 + (k * 5 - 4) * ido;
                var idx2 = out_off + (k - 1) * ido;
                for (var i = 0; i < ido - 1; i += 2) {
                    var iidx1 = i + idx1;
                    var iidx2 = iidx1 + ido;
                    var iidx3 = iidx1 - ido;
                    var iidx4 = iidx2 + ido;
                    var iidx5 = iidx4 + ido;
                    var i1i = in[iidx1 - 1];
                    var i1r = in[iidx1];
                    var i2i = in[iidx2 - 1];
                    var i2r = in[iidx2];
                    var i3i = in[iidx3 - 1];
                    var i3r = in[iidx3];
                    var i4i = in[iidx4 - 1];
                    var i4r = in[iidx4];
                    var i5i = in[iidx5 - 1];
                    var i5r = in[iidx5];

                    ti5 = i1r - i5r;
                    ti2 = i1r + i5r;
                    ti4 = i2r - i4r;
                    ti3 = i2r + i4r;
                    tr5 = i1i - i5i;
                    tr2 = i1i + i5i;
                    tr4 = i2i - i4i;
                    tr3 = i2i + i4i;
                    cr2 = i3i + tr11 * tr2 + tr12 * tr3;
                    ci2 = i3r + tr11 * ti2 + tr12 * ti3;
                    cr3 = i3i + tr12 * tr2 + tr11 * tr3;
                    ci3 = i3r + tr12 * ti2 + tr11 * ti3;
                    cr5 = isign * (ti11 * tr5 + ti12 * tr4);
                    ci5 = isign * (ti11 * ti5 + ti12 * ti4);
                    cr4 = isign * (ti12 * tr5 - ti11 * tr4);
                    ci4 = isign * (ti12 * ti5 - ti11 * ti4);
                    var dr3 = cr3 - ci4;
                    var dr4 = cr3 + ci4;
                    var di3 = ci3 + cr4;
                    var di4 = ci3 - cr4;
                    var dr5 = cr2 + ci5;
                    var dr2 = cr2 - ci5;
                    var di5 = ci2 - cr5;
                    var di2 = ci2 + cr5;

                    var widx1 = i + iw1;
                    var widx2 = i + iw2;
                    var widx3 = i + iw3;
                    var widx4 = i + iw4;
                    var w1r = wtable[widx1];
                    var w1i = isign * wtable[widx1 + 1];
                    var w2r = wtable[widx2];
                    var w2i = isign * wtable[widx2 + 1];
                    var w3r = wtable[widx3];
                    var w3i = isign * wtable[widx3 + 1];
                    var w4r = wtable[widx4];
                    var w4i = isign * wtable[widx4 + 1];

                    var oidx1 = i + idx2;
                    var oidx2 = oidx1 + idx0;
                    var oidx3 = oidx2 + idx0;
                    out[oidx1] = i3i + tr2 + tr3;
                    out[oidx1 + 1] = i3r + ti2 + ti3;
                    out[oidx2] = w1r * dr2 - w1i * di2;
                    out[oidx2 + 1] = w1r * di2 + w1i * dr2;
                    out[oidx3] = w2r * dr3 - w2i * di3;
                    out[oidx3 + 1] = w2r * di3 + w2i * dr3;
                    var oidx4 = oidx3 + idx0;
                    out[oidx4] = w3r * dr4 - w3i * di4;
                    out[oidx4 + 1] = w3r * di4 + w3i * dr4;
                    var oidx5 = oidx4 + idx0;
                    out[oidx5] = w4r * dr5 - w4i * di5;
                    out[oidx5 + 1] = w4r * di5 + w4i * dr5;
                }
            }
        }
    }

    /*----------------------------------------------------------------------
       passfg: Complex FFT's forward/backward processing of general factor;
       isign is +1 for backward and -1 for forward transforms
      ----------------------------------------------------------------------*/
    private void passfg(int[] nac, int ido, int ip, int l1, int idl1, float[] in, int in_off, float[] out, int out_off, int offset, int isign) {
        int jc;

        var ipph = (ip + 1) / 2;
        if (ido >= l1) {
            for (var j = 1; j < ipph; j++) {
                jc = ip - j;
                var idx1 = j * ido;
                var idx2 = jc * ido;
                for (var k = 0; k < l1; k++) {
                    var idx3 = k * ido;
                    var idx4 = idx3 + idx1 * l1;
                    var idx5 = idx3 + idx2 * l1;
                    var idx6 = idx3 * ip;
                    for (var i = 0; i < ido; i++) {
                        var oidx1 = out_off + i;
                        var i1r = in[in_off + i + idx1 + idx6];
                        var i2r = in[in_off + i + idx2 + idx6];
                        out[oidx1 + idx4] = i1r + i2r;
                        out[oidx1 + idx5] = i1r - i2r;
                    }
                }
            }
            for (var k = 0; k < l1; k++) {
                var idxt1 = k * ido;
                var idxt2 = idxt1 * ip;
                for (var i = 0; i < ido; i++) {
                    out[out_off + i + idxt1] = in[in_off + i + idxt2];
                }
            }
        } else {
            for (var j = 1; j < ipph; j++) {
                jc = ip - j;
                var idxt1 = j * l1 * ido;
                var idxt2 = jc * l1 * ido;
                var idxt3 = j * ido;
                var idxt4 = jc * ido;
                for (var i = 0; i < ido; i++) {
                    for (var k = 0; k < l1; k++) {
                        var idx1 = k * ido;
                        var idx2 = idx1 * ip;
                        var idx3 = out_off + i;
                        var idx4 = in_off + i;
                        var i1r = in[idx4 + idxt3 + idx2];
                        var i2r = in[idx4 + idxt4 + idx2];
                        out[idx3 + idx1 + idxt1] = i1r + i2r;
                        out[idx3 + idx1 + idxt2] = i1r - i2r;
                    }
                }
            }
            for (var i = 0; i < ido; i++) {
                for (var k = 0; k < l1; k++) {
                    var idx1 = k * ido;
                    out[out_off + i + idx1] = in[in_off + i + idx1 * ip];
                }
            }
        }

        var idl = 2 - ido;
        var inc = 0;
        var idxt0 = (ip - 1) * idl1;
        var idp = ip * ido;
        var iw1 = offset;
        float w1i;
        float w1r;
        for (var l = 1; l < ipph; l++) {
            var lc = ip - l;
            idl += ido;
            var idxt1 = l * idl1;
            var idxt3 = idl + iw1;
            w1r = wtable[idxt3 - 2];
            w1i = isign * wtable[idxt3 - 1];
            var idxt2 = lc * idl1;
            for (var ik = 0; ik < idl1; ik++) {
                var idx1 = in_off + ik;
                var idx2 = out_off + ik;
                in[idx1 + idxt1] = out[idx2] + w1r * out[idx2 + idl1];
                in[idx1 + idxt2] = w1i * out[idx2 + idxt0];
            }
            var idlj = idl;
            inc += ido;
            for (var j = 2; j < ipph; j++) {
                jc = ip - j;
                idlj += inc;
                if (idlj > idp)
                    idlj -= idp;
                var idxt4 = idlj + iw1;
                var w2r = wtable[idxt4 - 2];
                var w2i = isign * wtable[idxt4 - 1];
                var idxt5 = j * idl1;
                var idxt6 = jc * idl1;
                for (var ik = 0; ik < idl1; ik++) {
                    var idx1 = in_off + ik;
                    var idx2 = out_off + ik;
                    in[idx1 + idxt1] += w2r * out[idx2 + idxt5];
                    in[idx1 + idxt2] += w2i * out[idx2 + idxt6];
                }
            }
        }
        for (var j = 1; j < ipph; j++) {
            var idxt1 = j * idl1;
            for (var ik = 0; ik < idl1; ik++) {
                var idx1 = out_off + ik;
                out[idx1] += out[idx1 + idxt1];
            }
        }
        for (var j = 1; j < ipph; j++) {
            jc = ip - j;
            var idx1 = j * idl1;
            var idx2 = jc * idl1;
            for (var ik = 1; ik < idl1; ik += 2) {
                var idx3 = out_off + ik;
                var idx4 = in_off + ik;
                var iidx1 = idx4 + idx1;
                var iidx2 = idx4 + idx2;
                var i1i = in[iidx1 - 1];
                var i1r = in[iidx1];
                var i2i = in[iidx2 - 1];
                var i2r = in[iidx2];

                var oidx1 = idx3 + idx1;
                out[oidx1 - 1] = i1i - i2r;
                var oidx2 = idx3 + idx2;
                out[oidx2 - 1] = i1i + i2r;
                out[oidx1] = i1r + i2i;
                out[oidx2] = i1r - i2i;
            }
        }
        nac[0] = 1;
        if (ido == 2)
            return;
        nac[0] = 0;
        System.arraycopy(out, out_off, in, in_off, idl1);
        var idx0 = l1 * ido;
        for (var j = 1; j < ip; j++) {
            var idx1 = j * idx0;
            for (var k = 0; k < l1; k++) {
                var idx2 = k * ido;
                var oidx1 = out_off + idx2 + idx1;
                var iidx1 = in_off + idx2 + idx1;
                in[iidx1] = out[oidx1];
                in[iidx1 + 1] = out[oidx1 + 1];
            }
        }
        var idot = ido / 2;
        int idij;
        if (idot <= l1) {
            idij = 0;
            for (var j = 1; j < ip; j++) {
                idij += 2;
                var idx1 = j * l1 * ido;
                for (var i = 3; i < ido; i += 2) {
                    idij += 2;
                    var idx2 = idij + iw1 - 1;
                    w1r = wtable[idx2 - 1];
                    w1i = isign * wtable[idx2];
                    var idx3 = in_off + i;
                    var idx4 = out_off + i;
                    for (var k = 0; k < l1; k++) {
                        var idx5 = k * ido + idx1;
                        var iidx1 = idx3 + idx5;
                        var oidx1 = idx4 + idx5;
                        var o1i = out[oidx1 - 1];
                        var o1r = out[oidx1];
                        in[iidx1 - 1] = w1r * o1i - w1i * o1r;
                        in[iidx1] = w1r * o1r + w1i * o1i;
                    }
                }
            }
        } else {
            var idj = 2 - ido;
            for (var j = 1; j < ip; j++) {
                idj += ido;
                var idx1 = j * l1 * ido;
                for (var k = 0; k < l1; k++) {
                    idij = idj;
                    var idx3 = k * ido + idx1;
                    for (var i = 3; i < ido; i += 2) {
                        idij += 2;
                        var idx2 = idij - 1 + iw1;
                        w1r = wtable[idx2 - 1];
                        w1i = isign * wtable[idx2];
                        var iidx1 = in_off + i + idx3;
                        var oidx1 = out_off + i + idx3;
                        var o1i = out[oidx1 - 1];
                        var o1r = out[oidx1];
                        in[iidx1 - 1] = w1r * o1i - w1i * o1r;
                        in[iidx1] = w1r * o1r + w1i * o1i;
                    }
                }
            }
        }
    }

    private static void cftfsub(int n, float[] a, int offa, int[] ip, int nw, float[] w) {
        if (n > 8) {
            if (n > 32) {
                cftf1st(n, a, offa, w, nw - (n >> 2));
                if ((ConcurrencyUtils.getNumberOfThreads() > 1) && (n > ConcurrencyUtils.getThreadsBeginN_1D_FFT_2Threads())) {
                    cftrec4_th(n, a, offa, nw, w);
                } else if (n > 512) {
                    cftrec4(n, a, offa, nw, w);
                } else if (n > 128) {
                    cftleaf(n, 1, a, offa, nw, w);
                } else {
                    cftfx41(n, a, offa, nw, w);
                }
                bitrv2(n, ip, a, offa);
            } else if (n == 32) {
                cftf161(a, offa, w, nw - 8);
                bitrv216(a, offa);
            } else {
                cftf081(a, offa, w, 0);
                bitrv208(a, offa);
            }
        } else if (n == 8) {
            cftf040(a, offa);
        } else if (n == 4) {
            cftxb020(a, offa);
        }
    }

    private static void cftbsub(int n, float[] a, int offa, int[] ip, int nw, float[] w) {
        if (n > 8) {
            if (n > 32) {
                cftb1st(n, a, offa, w, nw - (n >> 2));
                if ((ConcurrencyUtils.getNumberOfThreads() > 1) && (n > ConcurrencyUtils.getThreadsBeginN_1D_FFT_2Threads())) {
                    cftrec4_th(n, a, offa, nw, w);
                } else if (n > 512) {
                    cftrec4(n, a, offa, nw, w);
                } else if (n > 128) {
                    cftleaf(n, 1, a, offa, nw, w);
                } else {
                    cftfx41(n, a, offa, nw, w);
                }
                bitrv2conj(n, ip, a, offa);
            } else if (n == 32) {
                cftf161(a, offa, w, nw - 8);
                bitrv216neg(a, offa);
            } else {
                cftf081(a, offa, w, 0);
                bitrv208neg(a, offa);
            }
        } else if (n == 8) {
            cftb040(a, offa);
        } else if (n == 4) {
            cftxb020(a, offa);
        }
    }

    private static void cftrec4_th(int n, float[] a, int offa, int nw, float[] w) {
        var nthreads = 2;
        var idiv4 = 0;
        var m = n >> 1;
        if (n > ConcurrencyUtils.getThreadsBeginN_1D_FFT_4Threads()) {
            nthreads = 4;
            idiv4 = 1;
            m >>= 1;
        }
        Future<?>[] futures = new Future[nthreads];
        var mf = m;
        var idx = 0;
        for (var i = 0; i < nthreads; i++) {
            var firstIdx = offa + i * m;
            if (i != idiv4) {
                futures[idx++] = ConcurrencyUtils.submit(() -> {
                    var idx1 = firstIdx + mf;
                    var m12 = n;
                    while (m12 > 512) {
                        m12 >>= 2;
                        cftmdl1(m12, a, idx1 - m12, w, nw - (m12 >> 1));
                    }
                    cftleaf(m12, 1, a, idx1 - m12, nw, w);
                    var k = 0;
                    var idx2 = firstIdx - m12;
                    for (var j = mf - m12; j > 0; j -= m12) {
                        k++;
                        var isplt = cfttree(m12, j, k, a, firstIdx, nw, w);
                        cftleaf(m12, isplt, a, idx2 + j, nw, w);
                    }
                });
            } else {
                futures[idx++] = ConcurrencyUtils.submit(() -> {
                    var idx1 = firstIdx + mf;
                    var k = 1;
                    var m1 = n;
                    while (m1 > 512) {
                        m1 >>= 2;
                        k <<= 2;
                        cftmdl2(m1, a, idx1 - m1, w, nw - m1);
                    }
                    cftleaf(m1, 0, a, idx1 - m1, nw, w);
                    k >>= 1;
                    var idx2 = firstIdx - m1;
                    for (var j = mf - m1; j > 0; j -= m1) {
                        k++;
                        var isplt = cfttree(m1, j, k, a, firstIdx, nw, w);
                        cftleaf(m1, isplt, a, idx2 + j, nw, w);
                    }
                });
            }
        }
        ConcurrencyUtils.waitForCompletion(futures);
    }

    private static void cftrec4(int n, float[] a, int offa, int nw, float[] w) {

        var m = n;
        var idx1 = offa + n;
        while (m > 512) {
            m >>= 2;
            cftmdl1(m, a, idx1 - m, w, nw - (m >> 1));
        }
        cftleaf(m, 1, a, idx1 - m, nw, w);
        var k = 0;
        var idx2 = offa - m;
        for (var j = n - m; j > 0; j -= m) {
            k++;
            var isplt = cfttree(m, j, k, a, offa, nw, w);
            cftleaf(m, isplt, a, idx2 + j, nw, w);
        }
    }

    private void scale(float m, float[] a, int offa, boolean complex) {
        var norm = (float) (1.0 / m);
        int n2;
        if (complex) {
            n2 = 2 * n;
        } else {
            n2 = n;
        }
        var nthreads = ConcurrencyUtils.getNumberOfThreads();
        if ((nthreads > 1) && (n2 >= ConcurrencyUtils.getThreadsBeginN_1D_FFT_2Threads())) {
            var k = n2 / nthreads;
            Future<?>[] futures = new Future[nthreads];
            for (var i = 0; i < nthreads; i++) {
                var firstIdx = offa + i * k;
                var lastIdx = (i == (nthreads - 1)) ? offa + n2 : firstIdx + k;
                futures[i] = ConcurrencyUtils.submit(() -> {
                    for (var i1 = firstIdx; i1 < lastIdx; i1++) {
                        a[i1] *= norm;
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);
        } else {
            for (var i = offa; i < offa + n2; i++) {
                a[i] *= norm;
            }

        }
    }

    private enum Plans {
        SPLIT_RADIX, MIXED_RADIX, BLUESTEIN
    }
}
