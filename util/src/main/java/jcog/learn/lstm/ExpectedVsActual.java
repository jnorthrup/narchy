package jcog.learn.lstm;

import jcog.Texts;

import java.util.Arrays;

/**
 * Created by me on 5/23/16.
 */
@Deprecated public final class ExpectedVsActual {

    public double[] expected;
    public double[] actual;
    public double[] predicted; //???

    /** forget rate, between 0 and 1 */
    public float forget;


    public static ExpectedVsActual the(int numActual, int numExpected) {
        return the(new double[numActual], new double[numExpected]);
    }

    public static ExpectedVsActual the(double[] actual, double[] expected) {
        return the(actual, expected, false);
    }

    public static ExpectedVsActual the(double[] actual, double[] expected, boolean reset) {
        ExpectedVsActual i = new ExpectedVsActual();
        i.actual = actual;
        i.expected = expected;
        i.forget = reset ? 1f : 0f;
        return i;
    }

    @Override
    public String toString() {
        return Texts.n4(actual) + "    ||    " +
                Texts.n4(expected) + "   ||   " +
                Texts.n4(predicted)
                
                ;
    }

    public void zero() {
        Arrays.fill(actual, (double) 0);
        if (expected!=null)
            Arrays.fill(expected, (double) 0);
    }
}
