package net.beadsproject.beads;

import jcog.Util;
import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.data.WaveFactory;
import net.beadsproject.beads.ugens.BiquadFilter;
import net.beadsproject.beads.ugens.Envelope;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.ugens.WavePlayer;

public class drum_machine_01 {
    WavePlayer kick, snareNoise, snareTone;
    Envelope kickGainEnvelope, snareGainEnvelope;
    Gain kickGain, snareGain;
    BiquadFilter kickFilter, snareFilter;

    public static void main(String[] args) {
        drum_machine_01 synth = new drum_machine_01();
        synth.setup();
    }

    // construct the synthesizer
    public void setup() {
        AudioContext ac = new AudioContext();

        // set up the envelope for kick gain
        kickGainEnvelope = new Envelope(ac, 0.0f);
        // construct the kick WavePlayer
        kick = new WavePlayer(ac, 100.0f, WaveFactory.SINE);
        // set up the filters
        kickFilter = new BiquadFilter(ac, BiquadFilter.BESSEL_LP, 500.0f, 1.0f);
        kickFilter.in(kick);
        // set up the Gain
        kickGain = new Gain(ac, 1, kickGainEnvelope);
        kickGain.in(kickFilter);

        // connect the gain to the main out
        ac.out.in(kickGain);


        // set up the snare envelope
        snareGainEnvelope = new Envelope(ac, 0.0f);
        // set up the snare WavePlayers
        snareNoise = new WavePlayer(ac, 1.0f, WaveFactory.NOISE);
        snareTone = new WavePlayer(ac, 200.0f, WaveFactory.SINE);
        // set up the filters
        snareFilter = new BiquadFilter(ac, BiquadFilter.BP_SKIRT, 2500.0f, 1.0f);
        snareFilter.in(snareNoise);
        snareFilter.in(snareTone);
        // set up the Gain
        snareGain = new Gain(ac, 1, snareGainEnvelope);
        snareGain.in(snareFilter);

        // connect the gain to the main out
        ac.out.in(snareGain);

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

        ac.start();
        Util.sleep(100000L);
    }

    public void keyDown(int midiPitch) {
        // kick should trigger on C
        if (midiPitch % 12 == 0) {
            // attack segment
            kickGainEnvelope.add(0.5f, 2.0f);
            // decay segment
            kickGainEnvelope.add(0.2f, 5.0f);
            // release segment
            kickGainEnvelope.add(0.0f, 50.0f);
        }

        // snare should trigger on E
        if (midiPitch % 12 == 4) {
            // attack segment
            snareGainEnvelope.add(0.5f, 2.00f);
            // decay segment
            snareGainEnvelope.add(0.2f, 8.0f);
            // release segment
            snareGainEnvelope.add(0.0f, 80.0f);
        }
    }

    public void keyUp(int midiPitch) {
        // do nothing
    }
}