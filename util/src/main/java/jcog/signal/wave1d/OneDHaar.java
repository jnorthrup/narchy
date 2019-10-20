package jcog.signal.wave1d;

import jcog.Util;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.System.arraycopy;
import static java.lang.System.out;

/**
 * ============================================================================
 * @author Vladimir Kulyukin
 * 
 * An implementation of 1D Ordered Fast Haar Wavelet Transform, Ordered Fast Inverse 
 * Haar Wavelet Transform, Inplace Fast Haar Wavelet Transform, and Inplace Fast Inverse 
 * Haar Wavelet Transform as specified in 
 * 
 * Ch. 01 of "Wavelets Made Easy" by Yves Nievergelt & "Ripples in Mathematics" by
 * A. Jensen, A. La Cour-Harbo.
 * 
 * Bugs to vladimir dot kulyukin at gmail dot com
 * ============================================================================
    https:
 */
public enum OneDHaar {
    ;

    private static final double FSNORM = Math.sqrt(2);
    private static final double FDNORM = 1/FSNORM;

    private static final double ISNORM = FDNORM;
    private static final double IDNORM = FSNORM;
    
    private static void displaySample(double[] sample) {
        out.print("Sample: ");
        for (var aSample : sample) {
            out.print(aSample + " ");
        }
        out.println();
    }

    public static double[] largestSubsignalOfPowerOf2(double[] signal) {
        if ( Util.isPowerOf2(signal.length) )
            return signal;
        else {
            var i = Util.largestPowerOf2NoGreaterThan(signal.length);
            if ( i == 0 ) return null;
            var subsignal = new double[i];
            arraycopy(signal, 0, subsignal, 0, i);
            return subsignal;
        }
    }

    
    public static void inPlaceFastHaarWaveletTransform(double[] sample) {
        if (sample.length < 2) {
            return;
        }
        if (!Util.isPowerOf2(sample.length)) {
            return;
        }
        var num_sweeps = (int) (Math.log(sample.length) / Util.log2);
        inPlaceFastHaarWaveletTransform(sample, num_sweeps);
    }
    
    public static void inPlaceFastHaarWaveletTransform(float[] sample) {


        var num_sweeps = (int) (Math.log(sample.length) / Util.log2);
        inPlaceFastHaarWaveletTransform(sample, num_sweeps);
    }

    
    public static void inPlaceFastHaarWaveletTransform(float[] sample, int num_iters) {
        if (sample.length < 2) {
            throw new RuntimeException(sample.length + " is not enough samples");
        }
        if (!Util.isPowerOf2(sample.length)) {
            throw new RuntimeException(sample.length + " is not power of 2");
        }
        var NUM_SAMPLE_VALS = sample.length;
        var n = (int) (Math.log(NUM_SAMPLE_VALS) / Math.log(2));
        if (num_iters < 1 || num_iters > n) {
            throw new RuntimeException(sample.length + " invalid number sweeps");
        }
        var GAP_SIZE = 2;
        var I = 1;
        for (var ITER_NUM = 1; ITER_NUM <= num_iters; ITER_NUM++) {
            NUM_SAMPLE_VALS /= 2;
            for (var K = 0; K < NUM_SAMPLE_VALS; K++) {
                var KGAPSIZE = GAP_SIZE * K;
                var sampleAtKGAPSIZE = sample[KGAPSIZE];
                var sampleAtKGAPSIZEPlusI = sample[KGAPSIZE + I];
                var a = (sampleAtKGAPSIZE + sampleAtKGAPSIZEPlusI) / 2;
                sample[KGAPSIZE] = a;
                var c = (sampleAtKGAPSIZE - sampleAtKGAPSIZEPlusI) / 2;
                sample[KGAPSIZE + I] = c;
            }
            I = GAP_SIZE;
            GAP_SIZE *= 2;
        }
    }
    
    private static void inPlaceFastHaarWaveletTransform(double[] sample, int num_iters) {
        if (sample.length < 2) {
            return;
        }
        if (!Util.isPowerOf2(sample.length)) {
            return;
        }
        var NUM_SAMPLE_VALS = sample.length;
        var n = (int) (Math.log(NUM_SAMPLE_VALS) / Math.log(2));
        if (num_iters < 1 || num_iters > n) {
            return;
        }
        var GAP_SIZE = 2;
        var I = 1;
        for (var ITER_NUM = 1; ITER_NUM <= num_iters; ITER_NUM++) {
            NUM_SAMPLE_VALS /= 2;
            for (var K = 0; K < NUM_SAMPLE_VALS; K++) {
                var KGAPSIZE = GAP_SIZE * K;
                var sampleAtKGAPSIZE = sample[KGAPSIZE];
                var sampleAtKGAPSIZEPlusI = sample[KGAPSIZE + I];
                var a = (sampleAtKGAPSIZE + sampleAtKGAPSIZEPlusI) / 2;
                sample[KGAPSIZE] = a;
                var c = (sampleAtKGAPSIZE - sampleAtKGAPSIZEPlusI) / 2;
                sample[KGAPSIZE + I] = c;
            }
            I = GAP_SIZE;
            GAP_SIZE *= 2;
        }
    }

    
    public static void doNthSweepOfInPlaceFastHaarWaveletTransform(double[] sample, int sweep_number) {
        if (sample.length % 2 != 0 || sample.length == 0) {
            return;
        }
        var I = (int) (Math.pow(2.0, sweep_number - 1));
        var GAP_SIZE = (int) (Math.pow(2.0, sweep_number));
        var NUM_SAMPLE_VALS = sample.length;
        var n = (int) (Math.log(NUM_SAMPLE_VALS) / Math.log(2));
        if (sweep_number < 1 || sweep_number > n) {
            return;
        }
        NUM_SAMPLE_VALS /= (int) (Math.pow(2.0, sweep_number));
        for (var K = 0; K < NUM_SAMPLE_VALS; K++) {
            var a = (sample[GAP_SIZE * K] + sample[GAP_SIZE * K + I]) / 2;
            var c = (sample[GAP_SIZE * K] - sample[GAP_SIZE * K + I]) / 2;
            sample[GAP_SIZE * K] = a;
            sample[GAP_SIZE * K + I] = c;
        }
    }
    
    public static void primitiveDoublesToFile(double[] signal, String filePath) throws IOException {
        var file = new FileWriter(filePath);
        var buffer = new BufferedWriter(file);
        
        for(var d: signal) {
            buffer.write(Double.toString(d));
            buffer.newLine();
        }
        buffer.flush();
        
        buffer.close();
        file.close();
    }
    
    private static double[] fileToPrimitiveDoubles(String filePath) throws IOException {
        var fstream = new FileInputStream(filePath);
        var br = new BufferedReader(new InputStreamReader(fstream));
        List<Double> aryOfDoubles = new ArrayList<>();

        String strLine;
        
        while (( strLine = br.readLine()) != null )   {
            
            out.println (strLine);
            aryOfDoubles.add(Double.valueOf(strLine));
        }

        
        br.close();

        var prims = new double[aryOfDoubles.size()];
        var i = 0;
        for(double d: aryOfDoubles) {
            prims[i++] = d;
        }

        return prims;
    }

    private static void orderedFastHaarWaveletTransform(double[] signal) {
        var n = signal.length;
        
        if (!Util.isPowerOf2(n)) {
            return;
        }

        var NUM_SWEEPS = (int) (Math.log(n) / Math.log(2.0));
        double acoeff, ccoeff;
        if (NUM_SWEEPS == 1) {
            acoeff = (signal[0] + signal[1]) / 2.0;
            ccoeff = (signal[0] - signal[1]) / 2.0;
            signal[0] = acoeff;
            signal[1] = ccoeff;
            return;
        }
        for (var SWEEP_NUM = 1; SWEEP_NUM < NUM_SWEEPS; SWEEP_NUM++) {


            var size = (int) Math.pow(2.0, (NUM_SWEEPS - SWEEP_NUM));
            var acoeffs = new double[size];
            var ccoeffs = new double[size];
            var ai = 0;
            var ci = 0;


            var end = ((int) Math.pow(2.0, (NUM_SWEEPS - SWEEP_NUM + 1))) - 1;
            for (var i = 0; i <= end; i += 2) {
                acoeffs[ai++] = (signal[i] + signal[i + 1]) / 2.0;
                ccoeffs[ci++] = (signal[i] - signal[i + 1]) / 2.0;
            }

            
            
            
            
            
            
            
            
            
            
            
            
            
            
            for (var i = 0; i < size; i++) {
                signal[i] = acoeffs[i];
                signal[i + size] = ccoeffs[i];
            }
            
            
        }
        
        
        acoeff = (signal[0] + signal[1]) / 2.0;
        ccoeff = (signal[0] - signal[1]) / 2.0;
        signal[0] = acoeff;
        signal[1] = ccoeff;
        
        
    }
    
    
    
    public static double[] orderedFastHaarWaveletTransform(String filePath) throws IOException {
        var signal = fileToPrimitiveDoubles(filePath);
        orderedFastHaarWaveletTransform(signal);
        return signal;
    }
    
    
    private static void orderedNormalizedFastHaarWaveletTransform(double[] sample) {
        var n = sample.length;
        
        if (!Util.isPowerOf2(n)) {
            return;
        }

        var NUM_SWEEPS = (int) (Math.log(n) / Math.log(2.0));
        double acoeff, ccoeff;
        if (NUM_SWEEPS == 1) {
            acoeff = FSNORM * (sample[0] + sample[1])/2.0;
            ccoeff = FDNORM * (sample[0] - sample[1]);
            sample[0] = acoeff;
            sample[1] = ccoeff;
            return;
        }
        for (var SWEEP_NUM = 1; SWEEP_NUM < NUM_SWEEPS; SWEEP_NUM++) {


            var size = (int) Math.pow(2.0, (NUM_SWEEPS - SWEEP_NUM));
            var acoeffs = new double[size];
            var ccoeffs = new double[size];
            var ai = 0;
            var ci = 0;


            var end = ((int) Math.pow(2.0, (NUM_SWEEPS - SWEEP_NUM + 1))) - 1;
            for (var i = 0; i <= end; i += 2) {
                acoeffs[ai++] = FSNORM * (sample[i] + sample[i + 1])/2.0;
                ccoeffs[ci++] = FDNORM * (sample[i] - sample[i + 1]);
            }

            
            
            
            
            
            
            
            
            
            
            
            
            
            
            for (var i = 0; i < size; i++) {
                sample[i] = acoeffs[i];
                sample[i + size] = ccoeffs[i];
            }
            
            
        }
        
        
        acoeff = FSNORM * (sample[0] + sample[1])/2.0;
        ccoeff = FDNORM * (sample[0] - sample[1]);
        sample[0] = acoeff;
        sample[1] = ccoeff;
        
        
    }
    
    
    
    public static double[] orderedNormalizedFastHaarWaveletTransform(String filePath) throws IOException {
        var signal = fileToPrimitiveDoubles(filePath);
        orderedNormalizedFastHaarWaveletTransform(signal);
        return signal;
    }
    
    
    private static void orderedFastHaarWaveletTransformForNumIters(double[] signal, int num_iters) {
        var n = signal.length;
        
        if ( !Util.isPowerOf2(n) ) return;

        var NUM_SWEEPS = (int) (Math.log(n) / Math.log(2.0));
        if ( num_iters > NUM_SWEEPS ) return;
        if ( NUM_SWEEPS == 1 ) {
            var acoeff = (signal[0] + signal[1]) / 2.0;
            var ccoeff = (signal[0] - signal[1]) / 2.0;
            signal[0] = acoeff;
            signal[1] = ccoeff;
            return;
        }
        for (var SWEEP_NUM = 1; SWEEP_NUM <= num_iters; SWEEP_NUM++) {


            var size = (int) Math.pow(2.0, (NUM_SWEEPS - SWEEP_NUM));
            var acoeffs = new double[size];
            var ccoeffs = new double[size];
            var ai = 0;
            var ci = 0;


            var end = ((int) Math.pow(2.0, (NUM_SWEEPS - SWEEP_NUM + 1))) - 1;
            for (var i = 0; i <= end; i += 2) {
                acoeffs[ai++] = (signal[i] + signal[i + 1]) / 2.0;
                ccoeffs[ci++] = (signal[i] - signal[i + 1]) / 2.0;
            }
            
            for (var i = 0; i < size; i++) {
                signal[i] = acoeffs[i];
                signal[i + size] = ccoeffs[i];
            }
            
            
        }
    }
    
    public static double[] orderedFastHaarWaveletTransformForNumIters(String filePath, int num_iters) throws IOException {
        var signal = fileToPrimitiveDoubles(filePath);
        orderedFastHaarWaveletTransformForNumIters(signal, num_iters);
        return signal;
    }
    
    
    
    private static void orderedNormalizedFastHaarWaveletTransformForNumIters(double[] sample, int num_iters) {
        var n = sample.length;
        
        if ( !Util.isPowerOf2(n) ) return;

        var NUM_SWEEPS = (int) (Math.log(n) / Math.log(2.0));
        if ( num_iters > NUM_SWEEPS ) return;
        if ( NUM_SWEEPS == 1 ) {
            var acoeff = FSNORM * (sample[0] + sample[1]) / 2.0;
            var ccoeff = FDNORM * (sample[0] - sample[1]);
            sample[0] = acoeff;
            sample[1] = ccoeff;
            return;
        }
        for (var SWEEP_NUM = 1; SWEEP_NUM <= num_iters; SWEEP_NUM++) {


            var size = (int) Math.pow(2.0, (NUM_SWEEPS - SWEEP_NUM));
            var acoeffs = new double[size];
            var ccoeffs = new double[size];
            var ai = 0;
            var ci = 0;


            var end = ((int) Math.pow(2.0, (NUM_SWEEPS - SWEEP_NUM + 1))) - 1;
            for (var i = 0; i <= end; i += 2) {
                acoeffs[ai++] = FSNORM * (sample[i] + sample[i + 1])/2.0;
                ccoeffs[ci++] = FDNORM * (sample[i] - sample[i + 1]);
            }
            
            for (var i = 0; i < size; i++) {
                sample[i] = acoeffs[i];
                sample[i + size] = ccoeffs[i];
            }
            
            
        }
    }
    
    public static double[] orderedNormalizedFastHaarWaveletTransformForNumIters(String filePath, int num_iters) throws IOException {
        var signal = fileToPrimitiveDoubles(filePath);
        orderedNormalizedFastHaarWaveletTransformForNumIters(signal, num_iters);
        return signal;
    }

    private static void orderedFastInverseHaarWaveletTransform(double[] sample) {
        var n = sample.length;
        if (n < 2 || !Util.isPowerOf2(n)) {
            return;
        }
        n = (int) (Math.log(n) / Math.log(2.0));
        for (var L = 1; L <= n; L++) {
            var GAP = (int) (Math.pow(2.0, L - 1));

            var restored_vals = new double[2 * GAP];
            for (var i = 0; i < GAP; i++) {
                var a0 = sample[i] + sample[GAP + i];
                var a1 = sample[i] - sample[GAP + i];
                
                
                restored_vals[2 * i] = a0;
                restored_vals[2 * i + 1] = a1;
                
                
            }
            
            
            arraycopy(restored_vals, 0, sample, 0, 2 * GAP);
            
            
        }
    }
    
    public static double[] orderedFastInverseHaarWaveletTransform(String filePath) throws IOException {
        var signal = fileToPrimitiveDoubles(filePath);
        orderedFastInverseHaarWaveletTransform(signal);
        return signal;
    }
    
    
    private static void orderedFastInverseHaarWaveletTransformForNumIters(double[] signal, int numIters) {
        var n = signal.length;
        if (n < 2 || !Util.isPowerOf2(n)) {
            return;
        }
        n = (int) (Math.log(n) / Math.log(2.0));
        if (numIters > n) {
            return;
        }
        
        for (var L = 1; L <= numIters; L++) {
            var GAP = (int) (Math.pow(2.0, L - 1));

            var restoredVals = new double[2 * GAP];
            for (var i = 0; i < GAP; i++) {
                var a0 = signal[i] + signal[GAP + i];
                var a1 = signal[i] - signal[GAP + i];
                
                
                restoredVals[2 * i] = a0;
                restoredVals[2 * i + 1] = a1;
                
                
            }
            
            
            arraycopy(restoredVals, 0, signal, 0, 2 * GAP);
            
            
        }
    }
    
    private static void thresholdSignal(double[] signal, double thresh) {
        var n = signal.length;
        var thresholdedSignal = new double[10];
        var count = 0;
        for (var t = 0; t < n; t++) {
            var v = Math.abs(signal[t]) > thresh ? signal[t] : 0;
            if (thresholdedSignal.length == count) thresholdedSignal = Arrays.copyOf(thresholdedSignal, count * 2);
            thresholdedSignal[count++] = v;
        }
        thresholdedSignal = Arrays.copyOfRange(thresholdedSignal, 0, count);

        arraycopy(thresholdedSignal, 0, signal, 0, n);
        double[] o = null;
    }
    
    public static void orderedFastInverseHaarWaveletTransformForNumIters(double[] signal, int numIters, double thresh) {
        var n = signal.length;
        if (n < 2 || !Util.isPowerOf2(n)) {
            return;
        }
        n = (int) (Math.log(n) / Math.log(2.0));
        if (numIters > n) {
            return;
        }
        
        thresholdSignal(signal, thresh);
        orderedFastInverseHaarWaveletTransformForNumIters(signal, numIters);
    }
    
    
    
    
    
    public static double[] orderedFastInverseHaarWaveletTransformForNumIters(String filePath, int num_iters) throws IOException {
        var signal = fileToPrimitiveDoubles(filePath);
        orderedFastInverseHaarWaveletTransformForNumIters(signal, num_iters);
        return signal;
    }
    
    
    private static void orderedNormalizedFastInverseHaarWaveletTransform(double[] sample) {
        var n = sample.length;
        if (n < 2 || !Util.isPowerOf2(n)) {
            return;
        }
        n = (int) (Math.log(n) / Math.log(2.0));
        for (var L = 1; L <= n; L++) {
            var GAP = (int) (Math.pow(2.0, L - 1));
            var restored_vals = new double[2 * GAP];
            for (var i = 0; i < GAP; i++) {
                var d = IDNORM * sample[GAP + i];
                var s = ISNORM * sample[i];


                var a0 = s + d / 2;
                restored_vals[2 * i] = a0;
                var a1 = s - d / 2;
                restored_vals[2 * i + 1] = a1;
            }
            
            
            arraycopy(restored_vals, 0, sample, 0, 2 * GAP);
            
            
        }
    }
    
    public static double[] orderedNormalizedFastInverseHaarWaveletTransform(String filePath) throws IOException {
        var signal = fileToPrimitiveDoubles(filePath);
        orderedNormalizedFastInverseHaarWaveletTransform(signal);
        return signal;
    }
    
    
    private static void orderedNormalizedFastInverseHaarWaveletTransformForNumIters(double[] sample, int num_iters) {
        var n = sample.length;
        if (n < 2 || !Util.isPowerOf2(n)) {
            return;
        }
        n = (int) (Math.log(n) / Math.log(2.0));
        if (num_iters > n) {
            return;
        }
        var GAP = (int)(Math.pow(2.0, n-num_iters));
        for (var L = 1; L <= num_iters; L++) {
            var restored_vals = new double[2 * GAP];
            for (var i = 0; i < GAP; i++) {
                var d = IDNORM * sample[GAP + i];
                var s = ISNORM * sample[i];


                var a0 = s + d / 2;
                restored_vals[2 * i] = a0;
                var a1 = s - d / 2;
                restored_vals[2 * i + 1] = a1;
            }
            
            
            arraycopy(restored_vals, 0, sample, 0, 2 * GAP);
            
            
            GAP *= 2;
        }
    }
    
    public static double[] orderedNormalizedFastInverseHaarWaveletTransformForNumIters(String filePath, int num_iters) throws IOException {
        var signal = fileToPrimitiveDoubles(filePath);
        orderedNormalizedFastInverseHaarWaveletTransformForNumIters(signal, num_iters);
        return signal;
    }

    public static void inPlaceFastInverseHaarWaveletTransform(double[] sample) {
        var n = sample.length;
        n = (int) (Math.log(n) / Math.log(2.0));
        var GAP_SIZE = (int) (Math.pow(2.0, n - 1));
        var JUMP = 2 * GAP_SIZE;
        var NUM_FREQS = 1;
        for (var SWEEP_NUM = n; SWEEP_NUM >= 1; SWEEP_NUM--) {
            for (var K = 0; K < NUM_FREQS; K++) {
                var aPlus = sample[JUMP * K] + sample[JUMP * K + GAP_SIZE];
                var aMinus = sample[JUMP * K] - sample[JUMP * K + GAP_SIZE];
                sample[JUMP * K] = aPlus;
                sample[JUMP * K + GAP_SIZE] = aMinus;
            }
            JUMP = GAP_SIZE;
            GAP_SIZE /= 2;
            NUM_FREQS *= 2;
        }
    }

    
    
    public static void inPlaceFastInverseHaarWaveletTransformForNumIters(double[] sample, int num_iters) {
        var n = sample.length;
        if (n < 2 || !Util.isPowerOf2(n)) {
            return;
        }
        n = (int) (Math.log(n) / Math.log(2.0));
        if (num_iters < 1 || num_iters > n) {
            return;
        }


        var lower_bound = n - num_iters + 1;
        for (var ITER_NUM = lower_bound; ITER_NUM <= n; ITER_NUM++) {
            var GAP_SIZE = (int) (Math.pow(2.0, n - ITER_NUM));
            var JUMP = 2 * GAP_SIZE;
            var NUM_FREQS = (int) (Math.pow(2.0, ITER_NUM - 1));
            for (var K = 0; K < NUM_FREQS; K++) {
                var aPlus = sample[JUMP * K] + sample[JUMP * K + GAP_SIZE];
                var aMinus = sample[JUMP * K] - sample[JUMP * K + GAP_SIZE];
                sample[JUMP * K] = aPlus;
                sample[JUMP * K + GAP_SIZE] = aMinus;
            }
        }
    }

    public static void doNthIterOfInPlaceFastInverseHaarWaveletTransform(double[] sample, int iter_number) {
        var n = sample.length;
        if (n < 2 || !Util.isPowerOf2(n)) {
            return;
        }
        n = (int) (Math.log(n) / Math.log(2.0));
        if (iter_number < 1 || iter_number > n) {
            return;
        }
        var GAP_SIZE = (int) (Math.pow(2.0, n - iter_number));
        var JUMP = 2 * GAP_SIZE;
        var NUM_FREQS = (int) (Math.pow(2.0, iter_number - 1));
        for (var K = 0; K < NUM_FREQS; K++) {
            var aPlus = sample[JUMP * K] + sample[JUMP * K + GAP_SIZE];
            var aMinus = sample[JUMP * K] - sample[JUMP * K + GAP_SIZE];
            sample[JUMP * K] = aPlus;
            sample[JUMP * K + GAP_SIZE] = aMinus;
        }
    }

    
    
    
    public static void reconstructSampleTransformedInPlaceForNumIters(double[] haar_transformed_sample, int num_iters) {
        var n = haar_transformed_sample.length;
        if (n < 2 || !Util.isPowerOf2(n)) {
            return;
        }
        n = (int) (Math.log(n) / Math.log(2.0));
        if (num_iters < 1 || num_iters > n) {
            return;
        }
        var GAP_SIZE = (int) (Math.pow(2.0, num_iters - 1));
        var JUMP = 2 * GAP_SIZE;
        var NUM_FREQS = (int) (Math.pow(2.0, n - num_iters));
        for (var ITER_NUM = 1; ITER_NUM <= num_iters; ITER_NUM++) {
            for (var K = 0; K < NUM_FREQS; K++) {
                var aPlus = haar_transformed_sample[JUMP * K] + haar_transformed_sample[JUMP * K + GAP_SIZE];
                var aMinus = haar_transformed_sample[JUMP * K] - haar_transformed_sample[JUMP * K + GAP_SIZE];
                haar_transformed_sample[JUMP * K] = aPlus;
                haar_transformed_sample[JUMP * K + GAP_SIZE] = aMinus;
            }
            JUMP = GAP_SIZE;
            GAP_SIZE /= 2;
            NUM_FREQS *= 2;
        }
    }

    
    
    
    public static void reconstructSampleTransformedInPlaceForNumItersWithOutput(double[] haar_transformed_sample, int num_iters) {
        var n = haar_transformed_sample.length;
        if (n < 2 || !Util.isPowerOf2(n)) {
            return;
        }
        n = (int) (Math.log(n) / Math.log(2.0));
        if (num_iters < 1 || num_iters > n) {
            return;
        }
        var GAP_SIZE = (int) (Math.pow(2.0, num_iters - 1));
        var JUMP = 2 * GAP_SIZE;
        var NUM_FREQS = (int) (Math.pow(2.0, n - num_iters));
        out.print("Reconstruction Sweep 0: ");
        displaySample(haar_transformed_sample);
        for (var ITER_NUM = 1; ITER_NUM <= num_iters; ITER_NUM++) {
            for (var K = 0; K < NUM_FREQS; K++) {
                var aPlus = haar_transformed_sample[JUMP * K] + haar_transformed_sample[JUMP * K + GAP_SIZE];
                var aMinus = haar_transformed_sample[JUMP * K] - haar_transformed_sample[JUMP * K + GAP_SIZE];
                haar_transformed_sample[JUMP * K] = aPlus;
                haar_transformed_sample[JUMP * K + GAP_SIZE] = aMinus;
            }
            out.print("Reconstruction Sweep " + ITER_NUM + ": ");
            displaySample(haar_transformed_sample);
            JUMP = GAP_SIZE;
            GAP_SIZE /= 2;
            NUM_FREQS *= 2;
        }
    }

    
    
    
    public static void displayOrderedFreqsFromOrderedHaar(double[] ordered_sample, PrintStream out) {
        var n = ordered_sample.length;
        if ((n < 2) || !Util.isPowerOf2(n)) {
            return;
        }
        n = (int) (Math.log(n) / Math.log(2));
        out.println(ordered_sample[0]);
        var start = 1;
        for (var sweep_num = 1; sweep_num <= n; sweep_num++) {
            var NUM_FREQS = (int) (Math.pow(2.0, sweep_num - 1));
            for (var i = start; i < (start + NUM_FREQS); i++) {
                out.print(ordered_sample[i] + "\t");
            }
            start += NUM_FREQS;
            out.println();
        }
    }

    
    
    
    public static void displayOrderedFreqsFromInPlaceHaar(double[] in_place_sample, PrintStream out) {
        var n = in_place_sample.length;
        
        if (n < 2 || !Util.isPowerOf2(n)) {
            return;
        }
        if (n == 2) {
            out.println(in_place_sample[0]);
            out.println(in_place_sample[1]);
            return;
        }
        out.println(in_place_sample[0]);
        out.println(in_place_sample[n / 2]);
        var START_INDEX = n / 4;
        var NUM_FREQS = 2;
        while (START_INDEX > 1) {
            var ODD = 1;
            for (var K = 0; K < NUM_FREQS; K++) {
                out.print(in_place_sample[START_INDEX * ODD] + "\t");
                ODD += 2;
            }
            out.println();
            START_INDEX /= 2;
            NUM_FREQS *= 2;
        }
        
        assert (START_INDEX == 1);
        for (var i = 1; i < n; i += 2) {
            out.print(in_place_sample[i] + "\t");
        }
        out.println();
    }

    
    
    
    public static void displayOrderedFreqsFromInPlaceHaar(float[] in_place_sample, PrintStream out) {
        var n = in_place_sample.length;
        
        if (n < 2 || !Util.isPowerOf2(n)) {
            return;
        }
        if (n == 2) {
            out.println(in_place_sample[0]);
            out.println(in_place_sample[1]);
            return;
        }
        out.println(in_place_sample[0]);
        out.println(in_place_sample[n / 2]);
        var START_INDEX = n / 4;
        var NUM_FREQS = 2;
        while (START_INDEX > 1) {
            var ODD = 1;
            for (var K = 0; K < NUM_FREQS; K++) {
                out.print(in_place_sample[START_INDEX * ODD] + "\t");
                ODD += 2;
            }
            out.println();
            START_INDEX /= 2;
            NUM_FREQS *= 2;
        }
        
        assert (START_INDEX == 1);
        for (var i = 1; i < n; i += 2) {
            out.print(in_place_sample[i] + "\t");
        }
        out.println();
    }

    public static double reconstructSingleValueFromOrderedHaarWaveletTransform(double[] sample, int n, int k) {
        var binstr = Integer.toBinaryString(k);

        if (binstr.length() < n) {
            var diff = n - binstr.length();
            for (var i = 0; i < diff; i++) {
                binstr = '0' + binstr;
            }
        }

        binstr = '0' + binstr;

        var binary = binstr.toCharArray();

        var s_k = sample[0];
        var I = (int) Math.pow(2.0, n - 2);
        var J = (int) Math.pow(2.0, n - 1);

        for (var L = 1; L <= n; L++) {
            switch (binary[L]) {
                case '0':
                    s_k += sample[J];
                    J -= I;
                    break;
                case '1':
                    s_k -= sample[J];
                    J += I;
                    break;
            }
            if (L < n) {
                I /= 2;
            }
        }
        return s_k;
    }
    
    
    public static double[][] computeForwardHaarTransformMatrix(int n) {
        var size = (int) Math.pow(2, n);
        var base_vector = new double[size];
        var fhw = new double[size][size];
        for(var col_num = 0; col_num < size; col_num++) {
            for(var i = 0; i < size; i++) {
                base_vector[i] = i == col_num ? 1 : 0;
            }
            orderedFastHaarWaveletTransform(base_vector);
            for(var row_num = 0; row_num < size; row_num++) {
                fhw[row_num][col_num] = base_vector[row_num];
            }
        }
        
        return fhw;
    }
    
    
    public static double[][] computeInverseHaarTransformMatrix(int n) {
        var size = (int) Math.pow(2, n);
        var base_vector = new double[size];
        var ihw = new double[size][size];
        for(var col_num = 0; col_num < size; col_num++) {
            for(var i = 0; i < size; i++) {
                base_vector[i] = i == col_num ? 1 : 0;
            }
            orderedFastInverseHaarWaveletTransform(base_vector);
            for(var row_num = 0; row_num < size; row_num++) {
                ihw[row_num][col_num] = base_vector[row_num];
            }
        }
        
        return ihw;
    }
    
    
    public static double[] applyHaarTransformMatrix(double[][] htm, double[] v) {
        var num_rows = htm.length;
        if ( num_rows < 1 ) return null;
        var num_cols = htm[0].length;
        if ( num_cols < 1 ) return null;
        if ( num_rows != num_cols ) return null;
        if ( num_rows != v.length ) return null;
        var inversed_v = new double[num_cols];
        for(var row = 0; row < num_rows; row++) {
            double dot_product = 0;
            for(var col = 0; col < num_cols; col++) {
                dot_product += htm[row][col]*v[col];
            }
            inversed_v[row] = dot_product;
        }
        return inversed_v;
    }
}