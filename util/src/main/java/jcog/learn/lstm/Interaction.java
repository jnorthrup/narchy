package jcog.learn.lstm;

import jcog.Texts;

import java.util.Arrays;

/**
 * Created by me on 5/23/16.
 */
public final class Interaction {

    public double[] actual;
    public double[] expected;
    public double[] predicted;

    /** forget rate, between 0 and 1 */
    public float forget;


    public static Interaction the(int numActual, int numExpected) {
        return the(new double[numActual], new double[numExpected]);
    }

    public static Interaction the(double[] actual, double[] expected) {
        return the(actual, expected, false);
    }

    public static Interaction the(double[] actual, double[] expected, boolean reset) {
        Interaction i = new Interaction();
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
        Arrays.fill(actual, 0);
        if (expected!=null)
            Arrays.fill(expected, 0);
    }
}
