package net.beadsproject.beads;

import jcog.Util;
import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.Auvent;
import net.beadsproject.beads.data.WaveFactory;
import net.beadsproject.beads.ugens.*;

public class arpeggiator_01 {
    float frequency = 100.0f;
    int tick = 0;
    FuncGen arpeggiator;
    WavePlayer square;

    Envelope gainEnvelope;
    Gain gain;

    int lastKeyPressed = -1;

    Clock beatClock;

    public static void main(String[] args) {
        arpeggiator_01 synth = new arpeggiator_01();
        synth.setup();
    }

    
    public void setup() {
        AudioContext ac = new AudioContext();

        
        gainEnvelope = new Envelope(ac, 0.0f);

        
        arpeggiator = new FuncGen(gainEnvelope) {

            @Override
            public float floatValueOf(float[] anObject) {
                return frequency * (1 + tick);
            }

            @Override
            public void on(Auvent msg) {
                tick++;
                if (tick >= 4) tick = 0;
            }
        };
        
        ac.out(arpeggiator);

        
        square = new WavePlayer(ac, arpeggiator, WaveFactory.SQUARE);

        
        beatClock = new Clock(ac, 500.0f);
        beatClock.setTicksPerBeat(4);
        beatClock.on(arpeggiator);
        ac.out.dependsOn(beatClock);

        
        gain = new Gain(ac, 1, gainEnvelope);
        gain.in(square);


        ac.out.in(gain);

        
























        keyDown(79);

        beatClock.start();
        Util.sleep(100000L);
    }

    public static float midiPitchToFrequency(int midiPitch) {
        /*
         *  MIDI pitch number to frequency conversion equation from
         *  http:
         */
        double exponent = (midiPitch - 69.0) / 12.0;
        return (float) (Math.pow(2, exponent) * 440.0f);
    }

    public void keyDown(int midiPitch) {
        if (square != null && gainEnvelope != null) {
            lastKeyPressed = midiPitch;

            
            frequency = midiPitchToFrequency(midiPitch);
            tick = -1;
            beatClock.reset();

            
            gainEnvelope.clear();
            
            gainEnvelope.add(0.5f, 10.0f);
        }
    }

    public void keyUp(int midiPitch) {
        
        if (midiPitch == lastKeyPressed && gainEnvelope != null)
            gainEnvelope.add(0.0f, 50.0f);
    }
}