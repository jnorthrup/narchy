/*
 * This file is part of Beads. See http:
 */
package net.beadsproject.beads.ugens;

/**
 * BiquadCustomCoeffCalculator provides a mechanism to define custom filter
 * coefficients for a {@link BiquadFilter} based on frequency and Q. Users can
 * create their own coefficient calculator classes by extending this class and
 * passing it to a BiquadFilter instance with {@link BiquadFilter#setType(int)}.
 * <p>
 * <p>
 * An instance of such a custom class should override
 * {@link #calcCoeffs(float, float, float)} to define the coefficient
 * calculation algorithm. The floats a0, a1, a2, b0, b1, and b2 should be setAt
 * according to the input parameters freq, q, and gain, as well as the useful
 * class variables {@link #sampFreq} and {@link #two_pi_over_sf}.
 * </p>
 *
 * @author Benito Crawford
 * @version .9.1
 * @beads.category filter
 */
class BiquadCustomCoeffCalculator {
    public float a0 = 1.0F;
    public float a1;
    public float a2;
    public float b0;
    public float b1;
    public float b2;
    /**
     * The sampling frequency.
     */
    private float sampFreq;
    /**
     * Two * pi / sampling frequency.
     */
    private float two_pi_over_sf;

    /**
     * Constructor for a given sampling frequency.
     *
     * @param sf The sampling frequency, in Hertz.
     */
    BiquadCustomCoeffCalculator(float sf) {
        setSamplingFrequency(sf);
    }

    /**
     * Constructor with default sampling frequency of 44100.
     */
    BiquadCustomCoeffCalculator() {
        setSamplingFrequency(44100.0F);
    }

    /**
     * Sets the sampling frequency.
     *
     * @param sf The sampling frequency in Hertz.
     */
    private void setSamplingFrequency(float sf) {
        sampFreq = sf;
        two_pi_over_sf = (float) (Math.PI * 2.0 / (double) sf);
    }

    /**
     * Override this function with code that sets a0, a1, etc.&nbsp;in terms of
     * frequency, Q, and sampling frequency.
     *
     * @param freq The frequency of the filter in Hertz.
     * @param q    The Q-value of the filter.
     * @param gain The gain of the filter.
     */
    public void calcCoeffs(float freq, float q, float gain) {
        
    }
}
