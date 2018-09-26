package spacegraph.space3d.test;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.Auvent;
import net.beadsproject.beads.data.WaveFactory;
import net.beadsproject.beads.ugens.*;
import spacegraph.SpaceGraph;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.widget.Widget;
import spacegraph.space2d.widget.meter.BitmapMatrixView;

import static net.beadsproject.beads.arpeggiator_01.midiPitchToFrequency;

public class Synthiano extends Widget {

    private final AudioContext ac;

    private final FuncGen arpeggiator;
    private final Clock beatClock;
    private final Gain gain;
    private final Envelope gainEnvelope;
    float frequency = 0;

    public Synthiano() {
        ac = new AudioContext();

        gainEnvelope = new Envelope(ac, 0.0f);


        
        arpeggiator = new FuncGen(gainEnvelope) {

            int tick = 0;

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

        
        WavePlayer square = new WavePlayer(ac, arpeggiator, WaveFactory.TRIANGLE);

        
        beatClock = new Clock(ac, 800.0f);
        beatClock.setTicksPerBeat(4);
        beatClock.on(arpeggiator);
        ac.out.dependsOn(beatClock);

        
        gain = new Gain(ac, 1, gainEnvelope);
        gain.in(square);


        ac.out.in(gain);

        
























        set(new BitmapMatrixView(4,4, (x, y)->0) {

            @Override
            public void updateTouch(Finger finger) {
                super.updateTouch(finger);
                if (finger.pressing(0))
                    key( Math.round((touchPos.y * 4)+touchPos.x));
            }

            protected void key(int key) {
                frequency = midiPitchToFrequency(30+key);

                beatClock.reset();


                gainEnvelope.clear();

                gainEnvelope.add(0.5f, 10.0f);
            }
        });


        beatClock.start();
    }



    public static void main(String[] args) {
        SpaceGraph.window(new Synthiano(), 500, 500);
    }
}
