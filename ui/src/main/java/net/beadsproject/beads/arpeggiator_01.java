package net.beadsproject.beads;

import jcog.Util;
import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.Bead;
import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.ugens.*;

public class arpeggiator_01
{
  float frequency = 100.0f;
  int tick = 0;
  Function arpeggiator;
  WavePlayer square;
  
  Envelope gainEnvelope;
  Gain gain;
  
  int lastKeyPressed = -1;
  
  Clock beatClock;
  
  public static void main(String[] args)
  {
    arpeggiator_01 synth = new arpeggiator_01();
    synth.setup();
  }
  
  // construct the synthesizer
  public void setup()
  {
    AudioContext ac = new AudioContext();
  
    // the gain envelope
    gainEnvelope = new Envelope(ac, 0.0f);
    
    // set up a custom function to arpeggiate the pitch
    arpeggiator = new Function(gainEnvelope)
    {
      @Override
      public float calculate()
      {
        return frequency * (1 + tick);
      }
      
      @Override
      public void messageReceived(Bead msg)
      {
        tick++;
        if( tick >= 4 ) tick = 0;
      }
    };
    // add arpeggiator as a dependent to the AudioContext
    ac.out.dependsOn(arpeggiator);
    
    // the square generator
    square = new WavePlayer(ac, arpeggiator, Buffer.SQUARE);

    // set up a clock to keep time
    beatClock = new Clock(ac, 500.0f);
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
        keyDown(79);

    beatClock.start();
    ac.start();
      Util.sleep((long) 100000);
  }
  
  static private float pitchToFrequency(int midiPitch)
  {
    /*
     *  MIDI pitch number to frequency conversion equation from
     *  http://newt.phys.unsw.edu.au/jw/notes.html
     */
    double exponent = (midiPitch - 69.0) / 12.0;
    return (float)(Math.pow(2, exponent) * 440.0f);
  }
  
  public void keyDown(int midiPitch)
  {
    if( square != null && gainEnvelope != null )
    {
      lastKeyPressed = midiPitch;
      
      // restart the arpeggiator
      frequency = pitchToFrequency(midiPitch);
      tick = -1;
      beatClock.reset();
      
      // interrupt the envelope
      gainEnvelope.clear();
      // attack segment
      gainEnvelope.add(0.5f, 10.0f);
    }
  }
  
  public void keyUp(int midiPitch)
  {
    // release segment
    if( midiPitch == lastKeyPressed && gainEnvelope != null )
      gainEnvelope.add(0.0f, 50.0f);
  }
}