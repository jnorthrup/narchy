package jcog.learn.lstm.test;

import jcog.Texts;
import jcog.learn.lstm.ExpectedVsActual;

import java.util.Arrays;

/**
 * Tests an LSTM in continuous active mode
 */
public class LiveSTMTest {

    public static void main(String[] args) {

        int inputs = 4;
        int outputs = 8;

        ExpectedVsActual i = ExpectedVsActual.the(inputs, outputs);
        double[] expect = i.expected;
        i.zero();


        int cells = 8;
        final int seqPeriod = 12;
        var l = new LiveSTM(inputs, outputs, cells) {

            int t;


            @Override
            protected ExpectedVsActual observe() {


                i.expected = expect;
                int tt = t % seqPeriod;


                i.forget =
                        
                        0.1f;

                Arrays.fill(i.actual, (double) 0);
                i.actual[(tt/3)%4] = 1.0;
                Arrays.fill(expect, (double) 0);
                expect[(int)Math.round(  ((Math.sin((double) tt)+ 1)/ 2)* 7) ] = 1;

                
                    
                

                
                
                

                if ((t/20)%2 == 0) {
                    
                    validation_mode = false;
                } /*else {
                    validation_mode = true;
                    
                    
                }*/

                t++;

                return i;
            }
        };

        for (int c = 0; c < 400000; c++) {
            System.out.println(c + "\t\t" + Texts.n4( l.next()) + "\t\t" + i);
        }
    }

}
