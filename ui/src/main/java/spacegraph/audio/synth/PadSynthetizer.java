package spacegraph.audio.synth;


//import science.unlicense.common.api.character.Chars;
//import science.unlicense.common.api.exception.InvalidArgumentException;
//import science.unlicense.common.api.number.Endianness;
//import science.unlicense.concurrent.api.Paths;
//import science.unlicense.encoding.api.io.DataOutputStream;
//import science.unlicense.encoding.api.io.IOException;
//import science.unlicense.encoding.api.path.Path;
//import science.unlicense.math.api.Maths;
//import science.unlicense.math.impl.transform.FFT;


import jcog.util.ArrayUtil;
import org.apache.commons.math3.util.ArithmeticUtils;

/**
 *
 * Origin :
 * http://zynaddsubfx.sourceforge.net/doc/PADsynth/PADsynth.htm
 * science.unlicenes
 *
 * @author Nasca Octavian Paul (Original code in C/C++)
 * @author Johann Sorel (Ported to Java and Unlicense-lib)
 */
public class PadSynthetizer {

    private static class FFTFREQS{
        public final double[] c;
        public final double[] s;

        public FFTFREQS(int size) {
            c = new double[size];
            s = new double[size];
        }
    }

    private static final int RAND_MAX = 1;
    private static final int N = 262144;
    private static final int number_harmonics = 64;

    /* Random number generator */
    private static double RND(){
        return Math.random() / ((double) RAND_MAX +1.0);
    }

    /* This is the profile of one harmonic
       In this case is a Gaussian distribution (e^(-x^2))
       The amplitude is divided by the bandwidth to ensure that the harmonic
       keeps the same amplitude regardless of the bandwidth */
    private static double profile(double fi,double bwi){
        double x=fi/bwi;
        x*=x;
        //this avoids computing the e^(-x^2) where it's results are very close to zero
        if (x>14.71280603) return 0.0;
        return Math.exp(-x)/bwi;
    }

    /*
        Inverse Fast Fourier Transform
        You may replace it with any IFFT routine
    */
    private static void IFFT(int N,double[] freq_amp,double[] freq_phase,double[] smp){
        int n2 = N/2;
        //FFTwrapper fft(N);
        FFTFREQS fftfreqs = new FFTFREQS(n2);

        for (int i = 0; i<n2; i++){
            double fai = freq_amp[i];
            double fpi = freq_phase[i];
            fftfreqs.c[i]= fai * Math.cos(fpi);
            fftfreqs.s[i]= fai * Math.sin(fpi);
        }
        FFT.ifft(fftfreqs.c, fftfreqs.s);
        if (n2 >= 0) System.arraycopy(fftfreqs.c, 0, smp, 0, n2);
        //fft.freqs2smps(fftfreqs,smp);
    }

    enum FFT { ;

        /**
         * Fast Fourrier Transform (Danielson-Lanczos algorithm). O(n.log(n) but only works for pow of 2 data length)
         *
         * @param dataReal data real part array
         * @param dataImag data imaginary part array
         */
        public static void fft( double[] dataReal, double[] dataImag ) {

            if ( dataReal.length != dataImag.length ) {
                throw new UnsupportedOperationException("dataReal.length and dataImag.length must be equals.");
            }

            if ( !ArithmeticUtils.isPowerOfTwo((long) dataReal.length) ) {
                throw new UnsupportedOperationException("The length of data must be a pow of 2.");
            }

            bitReversalReordering ( dataReal, dataImag );

            DanielsonLanczos ( dataReal, dataImag );
        }

        /**
         * Inverse Fast Fourrier Transform. (O(n.log(n) but only works for pow of 2 data length)
         *
         * @param dataReal data real part array
         * @param dataImag data imaginary part array
         */
        public static void ifft( double[] dataReal, double[] dataImag ) {

            // Conjugate input (inverse sign of dataImag values).
            for (int i = 0; i<dataImag.length; i++ ) {
                dataImag[i] = -dataImag[i];
            }

            // forward fft
            FFT.fft( dataReal, dataImag );

            // Conjugate again and divide by data length.
            for (int i = 0; i<dataImag.length; i++ ) {
                dataReal[i] = dataReal[i] / (double) dataReal.length;
                dataImag[i] = -dataImag[i]/ (double) dataImag.length;
            }
        }

        /**
         * Reorder an array elements by bit reversal index order.
         * (Length of dR and dI must be equals and a power of 2)
         * E.g.: for a 8 length array:
         * 000 -----> 000
         * 001 -----> 100
         * 010 -----> 010
         * 011 -----> 110
         * 100 -----> 001
         * 101 -----> 101
         * 110 -----> 011
         * 111 -----> 111
         * @param dataReal
         * @param dataImag
         */
        private static void bitReversalReordering( double[] dataReal, double[] dataImag ) {
            int nBits = Integer.numberOfTrailingZeros(dataReal.length);
            for (int i = 1; i<dataReal.length-1; i++) {
                int iRev = reverseBits(i, nBits);
                if ( iRev > i) {
					ArrayUtil.swapDouble(dataReal, i, iRev);
					ArrayUtil.swapDouble(dataImag, i, iRev);
				}
            }
        }
        /**
         * Reverses the bits order of x in a nBits length integer.
         * E.g:
         *      x=4 nBits=3
         *      x=4=100b then returns 001b=1
         *
         *      x=7 nBits=4
         *      x=7=0111b then returns 1110b=13
         *
         * @param i value
         * @param nBits number of bits of x
         * @return x in reverse bits order
         */
        public static int reverseBits ( int i, int nBits ) {
            int iRev = 0;
            while ( (i!=0) && (nBits-- >0) ) {
                iRev <<= 1;
                iRev |= (i & 1);
                i >>= 1;
            }
            while (nBits-- > 0) {
                iRev <<= 1;
            }
            return iRev;
        }
        /**
         * The Danielson-Lanczos algorithm.
         *
         * @param dataReal data real part array
         * @param dataImag data imaginary part array
         */
        private static void DanielsonLanczos(double[] dataReal, double[] dataImag) {

            int mmax = 1;

            while (dataReal.length > mmax) {

                int istep = 2 * mmax;
                double theta = -Math.PI / (double) mmax;
                double sinHalfTheta = Math.sin(0.5 * theta);

                // wp = -2.0 * SIN(0.5_8*theta)**2 + i* SIN(theta)
                double wpR = -2.0 * sinHalfTheta * sinHalfTheta;
                double wpI = Math.sin(theta);

                // w = 1. + i*0.
                double wR = 1.0;
                double wI = 0.0;

                for (int m = 1; m<=mmax; m++) {

                    double tempI;
                    double tempR;
                    for (int i = m-1; i<dataReal.length; i+=istep) {

                        int j = i + mmax;

                        // temp = ws * data[j]
                        tempR = wR * dataReal[j] - wI * dataImag[j];
                        tempI = wR * dataImag[j] + wI * dataReal[j];

                        // data[j] = data[i] - ws*data[j]
                        dataReal[j] = dataReal[i] - tempR;
                        dataImag[j] = dataImag[i] - tempI;

                        // data[i] = data[i] + ws*data[j]
                        dataReal[i] += tempR;
                        dataImag[i] += tempI;
                    }

                    //w = w*wp + w
                    tempR = wR;
                    tempI = wI;
                    wR = tempR * wpR - tempI * wpI + tempR;
                    wI = tempR * wpI + tempI * wpR + tempI;
                }

                mmax = istep;
            }

        }
    }

    /*
        Simple normalization function. It normalizes the sound to 1/sqrt(2)
    */
    private static void normalize(int N, double[] smp){
        int i;
        double max=0.0;
        for (i=0;i<N;i++) {
            if (Math.abs(smp[i])>max) max= Math.abs(smp[i]);
        }

        if (max == (double) 0) {
            throw new UnsupportedOperationException("Provided parameters resulted in a zero factor.");
        }
        if (max<1e-5) max=1e-5;
        for (i=0;i<N;i++) {
            smp[i]/=max*1.4142;
        }
    }


    /**
     * This is the implementation of PADsynth algorithm.
     *
     * @param N
     * @param samplerate
     * @param f
     * @param bw
     * @param number_harmonics
     * @param A input data
     * @param smp output data
     */
    private static void padsynth_basic_algorithm(int N, int samplerate, double f,
            double bw,int number_harmonics, double[] A, double[] smp) {

        int i;
        double[] freq_amp=new double[N/2];

        //default, all the frequency amplitudes are zero
        for (i=0;i<N/2;i++) freq_amp[i]=0.0;

        for (int nh = 1; nh<number_harmonics; nh++){//for each harmonic
            //bandwidth of the current harmonic measured in Hz
            double bw_Hz = (Math.pow(2.0, bw / 1200.0) - 1.0) * f * (double) nh;

            double bwi = bw_Hz / (2.0 * (double) samplerate);
            double fi = f * (double) nh / (double) samplerate;
            for (i=0;i<N/2;i++){
                double hprofile = profile(((double) i / (double) N) - fi, bwi);
                freq_amp[i]+=hprofile*A[nh];
            }
        }

        //Add random phases
        double[] freq_phase = new double[N / 2];
        for (i=0; i<N/2; i++){
            freq_phase[i]=RND()*2.0*Math.PI;
        }

        IFFT(N,freq_amp,freq_phase,smp);
        normalize(N,smp);

    }


    public static void main(String[] args) {

        double[] A = new double[number_harmonics];
        //A[0] is not used
        A[0]=0.0;

        double[] sample = new double[N];
        for (int note = 0; note<=24; note+=4){
            double f1=130.81* Math.pow(2.0, (double) note /12.0);
            System.out.print("Generating frequency: "+(int) f1+" Hz\n");
            for (int i = 1; i<number_harmonics; i++) {
                A[i]=1.0/ (double) i;
                double formants=
                    Math.exp(-Math.pow(((double) i *f1-600.0)/150.0,2.0))+
                    Math.exp(-Math.pow(((double) i *f1-900.0)/250.0,2.0))+
                    Math.exp(-Math.pow(((double) i *f1-2200.0)/200.0,2.0))+
                    Math.exp(-Math.pow(((double) i *f1-2600.0)/250.0,2.0))+
                    Math.exp(-Math.pow(((double) i *f1)/3000.0,2.0))*0.1;
                A[i]*=formants;
            }
            padsynth_basic_algorithm(N,44100,f1,60.0,number_harmonics,A,sample);

            /* Output the data to the 16 bit, mono raw file */
            short[] isample = new short[N];
            for (int i = 0; i<N; i++) {
                isample[i]= (short) (sample[i]*32768.0);
            }

//            final Path path = Paths.resolve(new Chars("file:./padsynth.wav"));
//            try{
//                final DataOutputStream ds = new DataOutputStream(path.createOutputStream());
//                ds.setEndianness(Endianness.LITTLE_ENDIAN);
//
//                int bitdepth = 16;
//                int byteDepth = 2;
//                int channels = 1;
//                int sampleRate = 44100;
//                int nbSample = isample.length;
//                ds.write("RIFF".getBytes());
//                ds.writeInt(36+(nbSample*byteDepth));
//                ds.write("WAVEfmt ".getBytes());
//                ds.writeInt(16);
//                ds.writeShort((short) 1);
//                ds.writeUShort(channels);
//                ds.writeInt(sampleRate);
//                ds.writeInt(sampleRate*channels*byteDepth);
//                ds.writeShort((short) (channels*byteDepth));
//                ds.writeShort((short) (byteDepth*8));
//                ds.write("data".getBytes());
//                ds.writeInt(nbSample*byteDepth);
//                ds.writeShort(isample);
//                ds.close();
//
//            }catch(IOException ex){
//                ex.printStackTrace();
//            }

        }
    }


}
