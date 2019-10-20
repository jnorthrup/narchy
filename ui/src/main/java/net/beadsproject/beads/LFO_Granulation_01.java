package net.beadsproject.beads;

import jcog.Util;
import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.data.Sample;
import net.beadsproject.beads.data.WaveFactory;
import net.beadsproject.beads.ugens.*;

public class LFO_Granulation_01 {
    public static void main(String[] args) {

        var ac = new AudioContext();

        
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


        var gsp = new GranularSamplePlayer(ac, sourceSample);

        
        gsp.setLoopType(SamplePlayer.LoopType.LOOP_FORWARDS);


        var wpGrainDurationLFO = new WavePlayer(ac, 0.03f, WaveFactory.SINE);
        var grainDurationLFO = new FuncGen(wpGrainDurationLFO) {
            @Override
            public float floatValueOf(float[] x) {
                return 1.0f + ((x[0] + 1.0f) * 50.0f);
            }

        };
        
        gsp.setGrainSize(grainDurationLFO);


        var wpGrainIntervalLFO = new WavePlayer(ac, 0.02f, WaveFactory.SINE);
        var grainIntervalLFO = new FuncGen(wpGrainIntervalLFO) {
            @Override
            public float floatValueOf(float[] x) {
                return 1.0f + ((x[0] + 1.0f) * 50.0f);
            }
        };
        
        gsp.setGrainInterval(grainIntervalLFO);

        
        gsp.setRandomness(new Static(ac, 10.0f));


        var gain = new Gain(ac, 1, 0.5f);
        gain.in(gsp);

        
        ac.out.in(gain);

        
        ac.start();
        Util.sleepMS((100 * 1000));
    }
}