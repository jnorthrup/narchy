/*
 * Copyright (c) 2018 Charles Grassin
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package jcog.learn.pid;


import static java.lang.Double.isFinite;

/**
 * A simple PID closed control loop.
 * <br><br>
 * License : MIT
 * @author Charles Grassin
 * https://github.com/CGrassin/SimplyPID
 *
 * untested and broken during initial experiments
 */
public class SimplyPID {

	/* PID coefficients */
	/** proportional gain coefficient */
	private double kP;

	/** integral gain coefficient */
	private double kI;

	/** derivative gain coefficient */
	private double kD;

	/** PID SetPoint */
    private double set;

    /** Limit bound of the output. */
    private double outMin = Double.NaN , outMax = Double.NaN;
    
    private double timePrev = Double.NaN;
    private double pErr = 0;

    private double iErr = 0;
	private double dErr;
	private double outPrev = Double.NaN;

	/**
     * @param set The initial target value.
     */
    public SimplyPID(final double set, final double kP, final double kI, final double kD) {
    	this.set(set);
        this.kP = kP;
        this.kI = kI;
        this.kD = kD;
    }

    /**
     * Updates the controller with the current time and value
     * and outputs the PID controller output.
     *
     * @param timeNext The current time (in arbitrary time unit, such as seconds).
     * If the PID is assumed to run at a constant frequency, you can simply put '1'.
     * @param nextValue The current, measured value.
     *
     * @return The PID controller output.  NaN if impossible to determine (due to starting condition, or other exception)
     */
    public synchronized double out(final double timeNext, final double nextValue) {
    	final double dt;

    	double timePrev = this.timePrev;
		this.timePrev = timeNext;

		if (timePrev != timePrev) { //proper NaN check
			//start
			return 0;
		} else {
			dt = (timeNext - timePrev); //allow negative dt
		}

		if (Math.abs(dt) > Double.MIN_NORMAL)
			return integrate(dt, nextValue);
		else
			return outPrev = 0;
	}

	/** // Compute Integral & Derivative error*/
	protected double integrate(double dt, double nextValue) {
		final double error = nextValue - set;

		dErr = ((error - pErr) / dt);
		iErr += error * dt;
		pErr = error; // Save history

		return this.outPrev = outFilter((kP * error) + (kI * iErr) + (kD * dErr));
	}

	/**
     * Resets the integral and derivative errors.
	 * @return
	 */
    public SimplyPID clear() {
        timePrev = 0;
        pErr = 0;
        iErr = 0;
        return this;
    }

    /**
     * Bounds the PID output between the lower limit
     * and the upper limit.
     * 
     * @param x The target output value.
     * @return The output value, bounded to the limits.
     */
    private double outFilter(final double x){
    	double min = this.outMin, max = this.outMax;
    	if ((min==min) && x < min)
    		return min;
    	else if ((max==max) && x > max)
    		return max;
    	else
    		return x;
    }
    


    /**
     * Sets the output limits of the PID controller. 
     * If the minLimit is superior to the maxLimit,
     * it will use the smallest as the minLimit.
     *
     *	@param minLimit The lower limit of the PID output.
     *	@param maxLimit The upper limit of the PID output.
     */
    public void outLimit(final double minLimit, final double maxLimit) {
    	assert(isFinite(maxLimit) && isFinite(minLimit) && (maxLimit - minLimit > Double.MIN_NORMAL));
		this.outMin = minLimit;
		this.outMax = maxLimit;
    }
    
    /**
     * Removes the output limits of the PID controller
     *
     * @param setPoint The new target point.
     */
    public void outLimitClear() {
        this.outMin = Double.NaN;
        this.outMax = Double.NaN;
    }
    
	/**
	 * @return the kP parameter
	 */
	public double kP() {
		return kP;
	}

	/**
	 * @param kP the kP parameter to set
	 * @return
	 */
	public SimplyPID kP(double kP) {
		this.kP = kP;
        return clear();
	}

	/**
	 * @return the kI parameter
	 */
	public double kI() {
		return kI;
	}

	/**
	 * @param kI the kI parameter to set
	 * @return
	 */
	public SimplyPID kI(double kI) {
		this.kI = kI;
        return clear();
	}

	/**
	 * @return the kD parameter
	 */
	public double kD() {
		return kD;
	}

	/**
	 * @param kD the kD parameter to set
	 * @return
	 */
	public SimplyPID kD(double kD) {
		this.kD = kD;
        return clear();
	}

	/**
	 * @return the setPoint
	 */
	public double set() {
		return set;
	}
	
    /**
     * Establishes a new set point for the PID controller.
     *
	 * @param setPoint The new target point.
	 * @return
	 */
    public SimplyPID set(final double setPoint) {
        this.set = setPoint;
        return clear();
    }
}
