package jcog.learn.ntm.control;

public class Sigmoid
{
    /** alpha=1.0 */
    public static double getValue(double x) {
        return 1.0 / (1 + Math.exp(-x));
    }

    public static double getValue(double x, double alpha) {
        return 1.0 / (1 + Math.exp(-x * alpha));
    }

    public static double expFast(double val) {
        var tmp = (long) (1512775 * val + (1072693248 - 60801));
        return Double.longBitsToDouble(tmp << 32);
    }

}
