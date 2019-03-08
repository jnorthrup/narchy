package jcog.learn.pid;

import static jcog.Texts.n4;

/**
 * A simple PID closed control loop example.
 * <br><br>
 * License : MIT
 *
 * @author Charles Grassin
 */
class SimplyPIDTest {


    public static void main(String[] args) {

        // 'output' represent the PID output (for instance, torque on a motor)
        double output = 0;

        // 'currentValue' represents the value measured
        double x = 1;

        SimplyPID pid = new SimplyPID(0, 0.5, 0.1, 0.1);


//        System.out.printf("Time\tSet Point\tCurrent value\tOutput\tError\n");

        double noise = 0.05;
        double overshoot = 0; //0.01;

        double dt = 0.1;
        for (double i = 0; i < 30; i+= dt) {
            // Print the current status
            //System.out.printf("%f\t%3.2f\t%3.2f\t%3.2f\t%3.2f\n", i, pid.set(), x, output, (pid.set() - x));

            System.out.println(n4(i) + "\t" + n4(x) + " " + n4(output));

            // At 50%, change the setPoint
            if (i == 15)
                pid.set(0);

            // Compute the output (assuming 1 unit of time passed between each measurement)
            output = pid.out(i, x);

            // Add it to our current value (which would be the measurement in a true system),
            // with some random error and an arbitrary overshoot factor
            x += output + overshoot + (noise * ((Math.random() - .5) * 2));


        }
    }

}