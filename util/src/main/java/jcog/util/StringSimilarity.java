/*
Copyright 2015 Thibault Debatty.

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit
persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package jcog.util;

import java.util.Arrays;

public class StringSimilarity {

    /**
     * This class is copied from: https://github.com/tdebatty/java-string-similarity/blob/master/src/main/java/info/debatty/java/stringsimilarity/JaroWinkler.java
     * and slightly modified. *
     * <p>
     * The Jaro–Winkler distance metric is designed and best suited for short
     * strings such as person names, and to detect typos; it is (roughly) a
     * variation of Damerau-Levenshtein, where the substitution of 2 close
     * characters is considered less important then the substitution of 2 characters
     * that a far from each other.
     * Jaro-Winkler was developed in the area of record linkage (duplicate
     * detection) (Winkler, 1990). It returns a value in the interval [0.0, 1.0].
     * The distance is computed as 1 - Jaro-Winkler similarity.
     *
     * @author Thibault Debatty
     *
     * From graphhopper
     */
    static class JaroWinkler {

        private static final double DEFAULT_THRESHOLD = 0.7;
        private static final int THREE = 3;
        private static final double JW_COEF = 0.1;
        private final double threshold;

        /**
         * Instantiate with default threshold (0.7).
         */
        public JaroWinkler() {
            this.threshold = DEFAULT_THRESHOLD;
        }

        /**
         * Instantiate with given threshold to determine when Winkler bonus should
         * be used.
         * Set threshold to a negative value to get the Jaro distance.
         */
        public JaroWinkler(double threshold) {
            this.threshold = threshold;
        }

        /**
         * Returns the current value of the threshold used for adding the Winkler
         * bonus. The default value is 0.7.
         *
         * @return the current value of the threshold
         */
        public final double getThreshold() {
            return threshold;
        }

        /**
         * Compute JW similarity.
         */
        public final double similarity(String s1, String s2) {
            int[] mtp = matches(s1, s2);
            float m = (float) mtp[0];
            if (m == (float) 0) {
                return 0;
            }
            double j = (double) (((m / (float) s1.length() + m / (float) s2.length() + (m - (float) mtp[1]) / m))
                    / (float) THREE);
            double jw = j;

            if (j > getThreshold()) {
                jw = j + Math.min(JW_COEF, 1.0 / (double) mtp[THREE]) * (double) mtp[2] * (1.0 - j);
            }
            return jw;
        }


        /**
         * Return 1 - similarity.
         */
        public final double distance(String s1, String s2) {
            return 1.0 - similarity(s1, s2);
        }

        private static int[] matches(String s1, String s2) {
            String max, min;
            if (s1.length() > s2.length()) {
                max = s1;
                min = s2;
            } else {
                max = s2;
                min = s1;
            }
            int range = Math.max(max.length() / 2 - 1, 0);
            int[] matchIndexes = new int[min.length()];
            Arrays.fill(matchIndexes, -1);
            boolean[] matchFlags = new boolean[max.length()];
            int matches = 0;
            for (int mi = 0; mi < min.length(); mi++) {
                char c1 = min.charAt(mi);
                for (int xi = Math.max(mi - range, 0),
                     xn = Math.min(mi + range + 1, max.length()); xi < xn; xi++) {
                    if (!matchFlags[xi] && (int) c1 == (int) max.charAt(xi)) {
                        matchIndexes[mi] = xi;
                        matchFlags[xi] = true;
                        matches++;
                        break;
                    }
                }
            }
            char[] ms1 = new char[matches];
            char[] ms2 = new char[matches];
            for (int i = 0, si = 0; i < min.length(); i++) {
                if (matchIndexes[i] != -1) {
                    ms1[si] = min.charAt(i);
                    si++;
                }
            }
            for (int i = 0, si = 0; i < max.length(); i++) {
                if (matchFlags[i]) {
                    ms2[si] = max.charAt(i);
                    si++;
                }
            }
            long count = 0L;
            for (int i = 0; i < ms1.length; i++) {
                if ((int) ms1[i] != (int) ms2[i]) {
                    count++;
                }
            }
            int transpositions = (int) count;
            int prefix = 0;
            for (int mi = 0; mi < min.length(); mi++) {
                if ((int) s1.charAt(mi) == (int) s2.charAt(mi)) {
                    prefix++;
                } else {
                    break;
                }
            }
            return new int[]{matches, transpositions / 2, prefix, max.length()};
        }

    }
}