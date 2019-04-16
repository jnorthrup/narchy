package jcog.learn.cpg;

import jcog.Util;
import jcog.data.atomic.AtomicFloat;
import spacegraph.SpaceGraph;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.meter.Plot2D;

/**
 * central pattern generator - network
 * <p>
 * <p>
 * https://github.com/ecila/NOSC/
 * https://github.com/DanBennettDev/CPGLib/
 */
public class CPG {

    /**
     * // Alice August 2016.
     * //
     * // Implements Neural Oscillators - pair of continuous time neurons arranged in mutual inhibition -- using simple Euler method
     * //
     * // -- As specified in Williamson 99 (ftp://www.ai.mit.edu/people/matt/iros99.pdf)
     * // -- After K. Matsuoka. Sustained oscillations generated by mutually  inhibiting  neurons  with  adaption. Biological Cybernetics, 52:367{376, 1985
     * // -- Written in processing 2.2.1
     * <p>
     */
    public static class MatsuokaNeuron {

        float input;

        float y, yp, yn;
        float gain = 0.05f;//input weight
        float v1, v2, xp, xn; // state variables

        float bi =
                //2.0f;
                3.52f;
        float A =
                2.0f;
                //2.5f;

        // time constants
//        float tau1 = 0.1f; // natural freq is proportional to 1/tau1
//        float tau2 = 0.6f; // tau1:tau2 controls shape of osc (stable in range 0.1 - 0.5)
        float tau_R =
                //0.05f,
                1f,
            tau_A = 2f;

        float Si = 0.1f; // controls amplitude of oscillation

        //        float dt = 0.005f; // euler step size
        float dx1, dx2, dv1, dv2; // tmp vars for euler approximation


        // to model periodic input
        float t = 0.0f;
//        float period = 20.0f;
//        float inc = TWO_PI * period;
//        int val = 0;

        public MatsuokaNeuron() {

            input = 0.0f;
            dx2 = dx1 = 0.0f;

            // init state vars
            v1 = 0.0f;
            xp = 0.0f;
//            v2 = 1.0f;
//            xn = 1.0f;
            v2 = 0.1f;
            xn = 0.0f;
        }

        /*             // check for updated input
                    inc = TWO_PI/ period; */
        public void next(float nextInput, float dt) {

            //input = (float) Math.sin(t);
            input = nextInput;

            //  calc all changes in state vars using current values
            dx1 = (-xp - (A * xn) - (bi * v1) + (gain * input) + Si) / tau_R;
            dx2 = (-xn - (A * xp) - (bi * v2) + (gain * input) + Si) / tau_R;

            dv1 = (yp - v1) / tau_A; //(max(xp, 0.0f) - v1) / tau2;
            dv2 = (yn - v2) / tau_A; //(max(xn, 0.0f) - v2) / tau2;

            // update state vars (Euler)
            xp = xp + dx1 * dt;
            xn = xn + dx2 * dt;
            v1 = v1 + dv1 * dt;
            v2 = v2 + dv2 * dt;

            //  Calc outputs each neuron using new vals
            yp = Util.clamp(xp, -1, +1); //max(xp, 0.0f);
            yn = Util.clamp(xn, -1, +1); //max(xn, 0.0f);

            //  and the final output
            y = yp - yn;

            t = t + dt;
        }

        /*

        // enable parameter updates
        void keyPressed() {

          // update period of input oscillation
          if (key == 'F'){
            period = period+1;
            println("Freq = ", 1.0/period);
          }
          else if (key == 'f'){
            period = period-1;
            println("Freq = ", 1.0/ period);
          }
          else if (key == '1'){
            period = 10;
            println("Freq = ", 1.0/period);
          }
           else if (key == '2'){
            period = 100;
            println("Freq = ", 1.0/period);
          }
           else if (key == '3'){
            period = 1000;
            println("Freq = ", 1.0/period);
          }

          // turn input weights on/off
          else if (key == 'h'){
            h = 0.0;
            println("h = ", h );
          }
          else if (key == 'H'){
            h = 1.0;
            println("h = ", h );
          }

            // update tau1
          else if (key == 't'){
            tau1 = tau1-0.01;
            print("t1 = ", tau1 );
            println("t1:t2 = ", tau1/tau2 );
          }
          else if (key == 'T'){
            tau1 = tau1+0.01;
            print("t1 = ", tau1 );
            println(" t1:t2 = ", tau1/tau2 );
          }
         */
    }

    public static void main(String[] args) {
        final AtomicFloat nextInput = new AtomicFloat(), nextOutput = new AtomicFloat();
        Plot2D p = new Plot2D(4200, Plot2D.Line)
                .add("input", () -> nextInput.floatValue())
                .add("output", () -> nextOutput.floatValue())
                ;

        MatsuokaNeuron n = new MatsuokaNeuron();
        for (int i = 0; i < 4200; i++) {
            nextInput.set(
                    //i < 600 ? (float) Math.sin(Math.cos(i/40f) / 4f) : -1
                    (float) Math.sin(i / (i < 2200 ?  4f : 10f))*0.5f
                    //0.01f * (Math.random() - 0.5f)
            );

            n.next(nextInput.floatValue(), 0.02f);

            nextOutput.set( n.y );

            p.commit();
        }




        SpaceGraph.window(
                new Gridding(p),
                800 , 800);
    }

}
