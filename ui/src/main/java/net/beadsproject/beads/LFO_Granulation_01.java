package net.beadsproject.beads;

import jcog.Util;
import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.data.WaveFactory;
import net.beadsproject.beads.data.Sample;
import net.beadsproject.beads.ugens.*;

public class LFO_Granulation_01 {
    public static void main(String[] args) {
        // instantiate the AudioContext
        AudioContext ac = new AudioContext();

        // load the source sample from a file
        Sample sourceSample = null;
        try {
            sourceSample = new Sample("/tmp/Vocal/wav/Laugh1.wav");
        } catch (Exception e) {
            /*
             * If the program exits with an error message,
             * then it most likely can't find the file
             * or can't open it. Make sure it is in the
             * root folder of your project in Eclipse.
             * Also make sure that it is a 16-bit,
             * 44.1kHz audio file. These can be created
             * using Audacity.
             */
            System.out.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        // instantiate a GranularSamplePlayer
        GranularSamplePlayer gsp = new GranularSamplePlayer(ac, sourceSample);

        // tell gsp to loop the file
        gsp.setLoopType(SamplePlayer.LoopType.LOOP_FORWARDS);

        // set up a custom function to convert a WavePlayer LFO to grain duration values
        WavePlayer wpGrainDurationLFO = new WavePlayer(ac, 0.03f, WaveFactory.SINE);
        FuncGen grainDurationLFO = new FuncGen(wpGrainDurationLFO) {
            @Override
            public float floatValueOf(float[] x) {
                return 1.0f + ((x[0] + 1.0f) * 50.0f);
            }

        };
        // set the grain size to the LFO
        gsp.setGrainSize(grainDurationLFO);

        // set up a custom function to convert a WavePlayer LFO to grain interval values
        WavePlayer wpGrainIntervalLFO = new WavePlayer(ac, 0.02f, WaveFactory.SINE);
        FuncGen grainIntervalLFO = new FuncGen(wpGrainIntervalLFO) {
            @Override
            public float floatValueOf(float[] x) {
                return 1.0f + ((x[0] + 1.0f) * 50.0f);
            }
        };
        // set the grain size to the LFO
        gsp.setGrainInterval(grainIntervalLFO);

        // tell gsp to behave somewhat randomly
        gsp.setRandomness(new Static(ac, 10.0f));

        // set up a gain
        Gain gain = new Gain(ac, 1, 0.5f);
        gain.in(gsp);

        // connect the Gain to ac
        ac.out.in(gain);

        // begin audio processing
        ac.start();
        Util.sleep((100 * 1000));
    }
}