package spacegraph.test;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.Auvent;
import net.beadsproject.beads.data.WaveFactory;
import net.beadsproject.beads.ugens.*;
import spacegraph.SpaceGraph;
import spacegraph.input.Finger;
import spacegraph.widget.meter.BitmapMatrixView;
import spacegraph.widget.windo.Widget;

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


        // custom function to arpeggiate the pitch
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
        // add arpeggiator as a dependent to the AudioContext
        ac.out(arpeggiator);

        // the square generator
        WavePlayer square = new WavePlayer(ac, arpeggiator, WaveFactory.TRIANGLE);

        // set up a clock to keep time
        beatClock = new Clock(ac, 800.0f);
        beatClock.setTicksPerBeat(4);
        beatClock.on(arpeggiator);
        ac.out.dependsOn(beatClock);

        // set up the Gain and connect it to the main output
        gain = new Gain(ac, 1, gainEnvelope);
        gain.in(square);


        ac.out.in(gain);

        // set up the keyboard input
//    MidiKeyboard keys = new MidiKeyboard();
//    keys.addActionListener(new ActionListener(){
//      @Override
//      public void actionPerformed(ActionEvent e)
//      {
//        // if the event is not null
//        if( e != null )
//        {
//          // if the event is a MIDI event
//          if( e.getSource() instanceof ShortMessage)
//          {
//            // get the MIDI event
//            ShortMessage sm = (ShortMessage)e.getSource();
//
//            // if the event is a key down
//            if( sm.getCommand() == MidiKeyboard.NOTE_ON && sm.getData2() > 1 )
//              keyDown(sm.getData1());
//            // if the event is a key up
//            else if( sm.getCommand() == MidiKeyboard.NOTE_OFF )
//              keyUp(sm.getData1());
//          }
//        }
//      }
//    });
        content(new BitmapMatrixView(4,4, (x,y)->0) {

            @Override
            public void updateTouch(Finger finger) {
                super.updateTouch(finger);
                if (finger.pressed(0))
                    key( Math.round((touchPos.y * 4)+touchPos.x));
            }
        });


        beatClock.start();
    }

    protected void key(int key) {
        frequency = midiPitchToFrequency(30+key);

        beatClock.reset();

        // interrupt the envelope
        gainEnvelope.clear();
        // attack segment
        gainEnvelope.add(0.5f, 10.0f);
    }

    public static void main(String[] args) {
        SpaceGraph.window(new Synthiano(), 500, 500);
    }
}
