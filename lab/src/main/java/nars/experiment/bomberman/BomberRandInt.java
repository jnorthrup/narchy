package nars.experiment.bomberman; /**
 * File:         BomberRandInt.java
 * Copyright:    Copyright (c) 2001
 *
 * @author Sammy Leong
 * @version 1.0
 */

/**
 * This class generates random integers within a given range by creating a
 * stack of 101 random doubles then generate random integers from the stack
 * randomly.  First, a stack of size 101 is created and filled with random
 * doubles.  Everytime an integer is drawn, an element is selected from the
 * stack randomly.  The value at the selected element is used to generate
 * a random integer then that element in the stack is filled with a
 * newly generated double.
 */
public class BomberRandInt {
    /** lowest integer in the range */
    private final int low;
    /** highest integer in the range */
    private final int high;

    /** stack size */
    private static final int BUFFER_SIZE = 101;
    /** stack to hold random doubles */
    private static final double[] buffer = new double[BUFFER_SIZE];

    /**
     * Fill the stack with 101 random doubles using the built-in random double
     * generator.
     */
    static {
        for (var i = 0; i < BUFFER_SIZE; i++)
            buffer[i] = Math.random();
    }

    /**
     * Constructs an object that generates random integers in a given range.
     * @param low the lowest integer in the range
     * @param high the highest integer in the range
     */
    public BomberRandInt(int low, int high) {
        this.low = low;
        this.high = high;
    }

    /**
     * Get the next random double from the stack then generate an integer from
     * it.
     * @return a random integer
     */
    public int draw() {
        var result = low + (int) ((high - low + 1) * nextRandom());
        return result;
    }

    /**
     * Pick a random element in the stack and store the value of it into a
     * variable to be returned then fill that element with a new random double.
     * @return a random double
     */
    private static double nextRandom() {
        /** pick a random element in the stack */
        var position = (int) (Math.random() * BUFFER_SIZE);
        if (position == BUFFER_SIZE)
            position = BUFFER_SIZE - 1;
        /** store the value of that element */
        var result = buffer[position];
        /** fill that element with a new random double */
        buffer[position] = Math.random();
        /** return the value */
        return result;
    }
}