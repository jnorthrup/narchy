/*******************************************************************************
 * Copyright (c) 2010 Haifeng Li
 *   
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *     http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package jcog.math;

import java.util.Arrays;

/**
 * Rolling Quantiles
 *
 * This class provide a robust and extremely fast algorithm to estimate arbitary
 * quantile values from a continuing stream of data values. Basically, the data
 * values fly by in a stream. We look at each value only once and do a
 * constant-time process on it. From time to time, we can use this class to
 * report any arbitary p-quantile value of the data that we have seen thus far.
 *
 * https:
 * @author Haifeng Li
 */
public class Quantiler {

    private float[] qileNext;
    private final int nbuf;
    private final int nq;
    int nt;
    int nd;
    private final float[] pval;
    private final float[] dbuf;
    private float[] qile;
    private float q0;
    private float qm;


    /**
     * Constructor.
     * @param nbuf batch size. You may use 10000 if you expected
     * &gt; 10<sup>6</sup> data values. Otherwise, 1000 should be fine.
     */
    public Quantiler(int nbuf) {
        this.nbuf = nbuf;

        nq = 251;
        nt = 0;
        nd = 0;
        q0 = Float.POSITIVE_INFINITY;
        qm = Float.NEGATIVE_INFINITY;

        pval = new float[nq];
        dbuf = new float[nbuf];
        qile = new float[nq];
        qileNext = new float[nq];

        for (int j = 85; j <= 165; j++) {
            pval[j] = (j - 75f) / 100f;
        }

        for (int j = 84; j >= 0; j--) {
            pval[j] = 0.87191909f * pval[j + 1];
            pval[250 - j] = 1f - pval[j];
        }
    }

    /**
     * Assimilate a new value from the stream.
     */
    public void add(float datum) {

        dbuf[nd++] = datum;
        if (datum < q0) {
            q0 = datum;
        }
        if (datum > qm) {
            qm = datum;
        }
        if (nd == nbuf) {
            update();
        }
    }

    /**
     * Batch update. This method is called by addAt() or quantile().
     */
    private void update() {
        float[] newqile = qileNext;
        Arrays.sort(dbuf, 0, nd);
        float qnew;
        float qold = qnew = qile[0] = newqile[0] = q0;
        qile[nq - 1] = newqile[nq - 1] = qm;
        pval[0] = Math.min(0.5f / (nt + nd), 0.5f * pval[1]);
        pval[nq - 1] = Math.max(1f- 0.5f / (nt + nd), 0.5f * (1f + pval[nq - 2]));
        float tnew = 0f;
        float told = 0f;
        int jq = 1;
        int jd = 0;
        for (int iq = 1; iq < nq - 1; iq++) {
            float target = (nt + nd) * pval[iq];
            if (tnew < target) {
                for (;;) {
                    if (jq < nq && (jd >= nd || qile[jq] < dbuf[jd])) {
                        qnew = qile[jq];
                        tnew = jd + nt * pval[jq++];
					} else {
                        qnew = dbuf[jd];
                        tnew = told;
                        if (qile[jq] > qile[jq - 1]) {
                            tnew += nt * (pval[jq] - pval[jq - 1]) * (qnew - qold) / (qile[jq] - qile[jq - 1]);
                        }
                        jd++;
                        if (tnew >= target) {
                            break;
                        }
                        told = tnew++;
                        qold = qnew;
					}
					if (tnew >= target) {
						break;
					}
					told = tnew;
                    qold = qnew;
                }
            }
            if (tnew == told) {
                newqile[iq] = 0.5f * (qold + qnew);
            } else {
                newqile[iq] = qold + (qnew - qold) * (target - told) / (tnew - told);
            }
            told = tnew;
            qold = qnew;
        }

        Arrays.fill(this.qileNext = qile, 0f);
        this.qile = newqile;

        nt += nd;
        nd = 0;
    }

    /**
     * Returns the estimated p-quantile for the data seen so far. For example,
     * p = 0.5f for median.
     */
    public float quantile(float p) {
        if (nd > 0) {
            update();
        }
        int jl = 0, jh = nq - 1;
        while (jh - jl > 1) {
            int j = (jh + jl) >> 1;
            if (p > pval[j]) {
                jl = j;
            } else {
                jh = j;
            }
        }

        float qjl = qile[jl];
        float pjl = pval[jl];
        float q = qjl + (((qile[jl + 1] - qjl) * (p - pjl)) / (pval[jl + 1] - pjl));
        return Math.max(qile[0], Math.min(qile[nq - 1], q));
    }


}