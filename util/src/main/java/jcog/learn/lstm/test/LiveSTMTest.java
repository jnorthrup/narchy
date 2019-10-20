package jcog.learn.lstm.test;

import jcog.Texts;
import jcog.learn.lstm.ExpectedVsActual;

import java.util.Arrays;

/**
 * Tests an LSTM in continuous active mode
 */
public class LiveSTMTest {

    public static void main(String[] args) {

        var inputs = 4;
        var outputs = 8;

        var i = ExpectedVsActual.the(inputs, outputs);
        var expect = i.expected;
        i.zero();


        var cells = 8;
        final var seqPeriod = 12;
        var l = new LiveSTM(inputs, outputs, cells) {

            int t;


            @Override
            protected ExpectedVsActual observe() {


                i.expected = expect;
                var tt = t % seqPeriod;


                i.forget =
                        
                        0.1f;

                Arrays.fill(i.actual, 0);
                i.actual[(tt/3)%4] = 1;
                Arrays.fill(expect, 0);
                expect[(int)Math.round(  ((Math.sin(tt)+1f)/2f)*7f ) ] = 1f;

                
                    
                

                
                
                

                if ((t/20)%2 == 0) {
                    
                    validation_mode = false;
                } /*else {
                    validation_mode = true;
                    
                    
                }*/

                t++;

                return i;
            }
        };

        for (var c = 0; c < 400000; c++) {
            System.out.println(c + "\t\t" + Texts.n4( l.next()) + "\t\t" + i);
        }
    }

}
