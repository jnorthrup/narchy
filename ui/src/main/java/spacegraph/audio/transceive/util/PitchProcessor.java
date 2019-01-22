package spacegraph.audio.transceive.util;

public class PitchProcessor {
    private final PitchDetector detector;
    private final PitchDetectionHandler handler;

    public PitchProcessor(PitchProcessor.PitchEstimationAlgorithm var1, float var2, int var3, PitchDetectionHandler var4) {
        this.detector = PitchEstimationAlgorithm.getDetector(var2, var3);
        this.handler = var4;
    }

    public boolean process(AudioEvent var1) {
        float[] var2 = var1.getFloatBuffer();
        PitchDetectionResult var3 = this.detector.getPitch(var2);
        this.handler.handlePitch(var3, var1);
        return true;
    }

    public void processingFinished() {
    }

    public enum PitchEstimationAlgorithm {
        YIN,
        MPM,
        FFT_YIN,
        DYNAMIC_WAVELET,
        FFT_PITCH,
        AMDF;

        PitchEstimationAlgorithm() {
        }

        static PitchDetector getDetector(float var1, int var2) {
            PitchDetector var3;
//            if (this == MPM) {
//                var3 = new McLeodPitchMethod(var1, var2);
//            } else if (this == DYNAMIC_WAVELET) {
//                var3 = new DynamicWavelet(var1, var2);
//            } else if (this == FFT_YIN) {
            var3 = new FastYin(var1, var2);
//            } else if (this == AMDF) {
//                var3 = new AMDF(var1, var2);
//            } else if (this == FFT_PITCH) {
//                var3 = new FFTPitch(Math.round(var1), var2);
//            } else {
//                var3 = new Yin(var1, var2);
//            }

            return var3;
        }
    }

    public interface PitchDetector {
        PitchDetectionResult getPitch(float[] var1);
    }

    public interface PitchDetectionHandler {
        void handlePitch(PitchDetectionResult var1, AudioEvent var2);
    }

    //
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

    static final class FastYin implements PitchDetector {
        public static final int DEFAULT_BUFFER_SIZE = 2048;
        public static final int DEFAULT_OVERLAP = 1536;
        private static final double DEFAULT_THRESHOLD = 0.2D;
        private final double threshold;
        private final float sampleRate;
        private final float[] yinBuffer;
        private final PitchDetectionResult result;
        private final float[] audioBufferFFT;
        private final float[] kernel;
        private final float[] yinStyleACF;
        private final FloatFFT fft;

        FastYin(float var1, int var2) {
            this(var1, var2, 0.2D);
        }

        FastYin(float var1, int var2, double var3) {
            this.sampleRate = var1;
            this.threshold = var3;
            this.yinBuffer = new float[var2 / 2];
            this.audioBufferFFT = new float[2 * var2];
            this.kernel = new float[2 * var2];
            this.yinStyleACF = new float[2 * var2];
            this.fft = new FloatFFT(var2);
            this.result = new PitchDetectionResult();
        }

        public PitchDetectionResult getPitch(float[] var1) {
            this.difference(var1);
            this.cumulativeMeanNormalizedDifference();
            int var2 = this.absoluteThreshold();
            float var3;
            if (var2 != -1) {
                float var4 = this.parabolicInterpolation(var2);
                var3 = this.sampleRate / var4;
            } else {
                var3 = -1.0F;
            }

            this.result.setPitch(var3);
            return this.result;
        }

        private void difference(float[] var1) {
            float[] var2 = new float[this.yinBuffer.length];

            int var3;
            for (var3 = 0; var3 < this.yinBuffer.length; ++var3) {
                var2[0] += var1[var3] * var1[var3];
            }

            for (var3 = 1; var3 < this.yinBuffer.length; ++var3) {
                var2[var3] = var2[var3 - 1] - var1[var3 - 1] * var1[var3 - 1] + var1[var3 + this.yinBuffer.length] * var1[var3 + this.yinBuffer.length];
            }

            for (var3 = 0; var3 < var1.length; ++var3) {
                this.audioBufferFFT[2 * var3] = var1[var3];
                this.audioBufferFFT[2 * var3 + 1] = 0.0F;
            }

            this.fft.complexForward(this.audioBufferFFT);

            for (var3 = 0; var3 < this.yinBuffer.length; ++var3) {
                this.kernel[2 * var3] = var1[this.yinBuffer.length - 1 - var3];
                this.kernel[2 * var3 + 1] = 0.0F;
                this.kernel[2 * var3 + var1.length] = 0.0F;
                this.kernel[2 * var3 + var1.length + 1] = 0.0F;
            }

            this.fft.complexForward(this.kernel);

            for (var3 = 0; var3 < var1.length; ++var3) {
                this.yinStyleACF[2 * var3] = this.audioBufferFFT[2 * var3] * this.kernel[2 * var3] - this.audioBufferFFT[2 * var3 + 1] * this.kernel[2 * var3 + 1];
                this.yinStyleACF[2 * var3 + 1] = this.audioBufferFFT[2 * var3 + 1] * this.kernel[2 * var3] + this.audioBufferFFT[2 * var3] * this.kernel[2 * var3 + 1];
            }

            this.fft.complexInverse(this.yinStyleACF, true);

            for (var3 = 0; var3 < this.yinBuffer.length; ++var3) {
                this.yinBuffer[var3] = var2[0] + var2[var3] - 2.0F * this.yinStyleACF[2 * (this.yinBuffer.length - 1 + var3)];
            }

        }

        private void cumulativeMeanNormalizedDifference() {
            this.yinBuffer[0] = 1.0F;
            float var2 = 0.0F;

            for (int var1 = 1; var1 < this.yinBuffer.length; ++var1) {
                var2 += this.yinBuffer[var1];
                float[] var10000 = this.yinBuffer;
                var10000[var1] *= (float) var1 / var2;
            }

        }

        private int absoluteThreshold() {
            int var1;
            for (var1 = 2; var1 < this.yinBuffer.length; ++var1) {
                if ((double) this.yinBuffer[var1] < this.threshold) {
                    while (var1 + 1 < this.yinBuffer.length && this.yinBuffer[var1 + 1] < this.yinBuffer[var1]) {
                        ++var1;
                    }

                    this.result.setProbability(1.0F - this.yinBuffer[var1]);
                    break;
                }
            }

            if (var1 != this.yinBuffer.length && (double) this.yinBuffer[var1] < this.threshold && (double) this.result.getProbability() <= 1.0D) {
                this.result.setPitched(true);
            } else {
                var1 = -1;
                this.result.setProbability(0.0F);
                this.result.setPitched(false);
            }

            return var1;
        }

        private float parabolicInterpolation(int var1) {
            int var3;
            if (var1 < 1) {
                var3 = var1;
            } else {
                var3 = var1 - 1;
            }

            int var4;
            if (var1 + 1 < this.yinBuffer.length) {
                var4 = var1 + 1;
            } else {
                var4 = var1;
            }

            float var2;
            if (var3 == var1) {
                if (this.yinBuffer[var1] <= this.yinBuffer[var4]) {
                    var2 = (float) var1;
                } else {
                    var2 = (float) var4;
                }
            } else if (var4 == var1) {
                if (this.yinBuffer[var1] <= this.yinBuffer[var3]) {
                    var2 = (float) var1;
                } else {
                    var2 = (float) var3;
                }
            } else {
                float var5 = this.yinBuffer[var3];
                float var6 = this.yinBuffer[var1];
                float var7 = this.yinBuffer[var4];
                var2 = (float) var1 + (var7 - var5) / (2.0F * (2.0F * var6 - var7 - var5));
            }

            return var2;
        }
    }

}
