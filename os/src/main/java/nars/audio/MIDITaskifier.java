package nars.audio;

import jcog.exe.Loop;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.concept.Concept;
import nars.game.action.GoalActionConcept;
import nars.gui.NARui;
import nars.task.DerivedTask;
import nars.task.NALTask;
import nars.term.Term;
import nars.time.Tense;
import nars.util.Timed;
import org.jetbrains.annotations.NotNull;
import spacegraph.SpaceGraph;
import spacegraph.audio.synth.SineWave;

import javax.sound.midi.*;
import java.util.Arrays;
import java.util.List;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;
import static nars.time.Tense.ETERNAL;


/**
 * generic MIDI input interface
 */
public class MIDITaskifier {

    float[] volume = new float[128];

    public MIDITaskifier() {
        NAR nar = NARS.threadSafe();
        nar.termVolMax.set(16);


        nar.onTask(t -> {
            if (t instanceof DerivedTask && t.isGoal()) {
                
                System.err.println(t.proof());
            }
        });


        MidiInReceiver midi = MIDI(nar);

        Arrays.fill(volume, Float.NaN);

        SoNAR s = new SoNAR(nar);
        








        List<Concept> keys = $.newArrayList();
        for (int i = 36; i <= 51; i++) {
            Term key =
                    channelKey(9, i);

            Term keyTerm = $.p(key);

            int finalI = i;


            GoalActionConcept c = new GoalActionConcept(keyTerm, (b, d) -> {



                if (d == null)
                    return null;
                float v = d.freq();
                if (v > 0.55f)
                    return $.t(v, nar.confDefault(BELIEF));
                else if (b != null && b.freq() > 0.5f)
                    return $.t((float) 0, nar.confDefault(BELIEF));
                else
                    return null;
            }, nar);
            nar.add(c);


            nar.input(NALTask.the(c.term(), BELIEF, $.t(0f, 0.35f), (long) 0, ETERNAL, ETERNAL, nar.evidence()));
            nar.input(NALTask.the(c.term(), GOAL, $.t(0f, 0.1f), (long) 0, ETERNAL, ETERNAL, nar.evidence()));
            nar.onCycle(n -> {

                float v = volume[finalI];

                if (v == (float) 0) {
                    volume[finalI] = Float.NaN;
                }

                
                

//                int dur = n.dur();
                //c.update(n.time()-dur, n.time(), null);
                c.accept(null);
            });


            keys.add(c);




            s.listen(c, (k) -> new SineWave((float) (100.0 + Math.random() * 1000.0)));

        }


        
        new Loop(2f) {

            final Term now = $.p("now");

            @Override
            public boolean next() {
                nar.believe(now, Tense.Present);
                return true;
            }
        };





        SpaceGraph.window(NARui.beliefCharts(nar, keys), 900, 900);


        nar.startFPS(60f);


    }

    public static void main(String[] arg) {
        new MIDITaskifier();
    }

    public MidiInReceiver MIDI(Timed timed) {

        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();


        for (int i = 0; i < infos.length; i++) {
            try {
                MidiDevice.Info ii = infos[i];

                MidiDevice device = MidiSystem.getMidiDevice(ii);

                System.out.println(device + "\t" + device.getClass());
                System.out.println("\t" + device.getDeviceInfo());
                System.out.println("\ttx: " + device.getTransmitters());
                System.out.println("\trx: " + device.getReceivers());

                if (receive(device)) {
                    return new MidiInReceiver(device, timed);
                }

                /*if (device instanceof Synthesizer) {
                    synthInfos.addAt((Synthesizer) ii);
                } else if (device instanceof MidiDevice) {
                    midis.addAt((MidiDevice) ii);
                }*/
            } catch (MidiUnavailableException e) {
                
            }
        }

        return null;
    }

    public static boolean receive(MidiDevice device) {
        return device.getDeviceInfo().getName().startsWith("MPD218");
    }

    public class MidiInReceiver implements Receiver {


        public MidiInReceiver(MidiDevice device, Timed timed) throws MidiUnavailableException {

            if (!device.isOpen()) {
                device.open();
            }

            device.getTransmitter().setReceiver(this);
        }

        @Override
        public void send(MidiMessage m, long timeStamp) {


            if (m instanceof ShortMessage) {
                ShortMessage s = (ShortMessage) m;
                int cmd = s.getCommand();
                switch (cmd) {
                    case ShortMessage.NOTE_OFF:
                        if ((volume[s.getData1()] == volume[s.getData1()]) && (volume[s.getData1()] > (float) 0))
                            volume[s.getData1()] = (float) 0;




                        
                        break;
                    case ShortMessage.NOTE_ON:
                        volume[s.getData1()] = 0.6f + 0.4f * (float) s.getData2() / 128f;



                        
                        
                        break;
                    default:
                        
                        break;
                    
                }
            }

        }











        @Override
        public void close() {

        }
    }





    public static @NotNull Term channelKey(int channel, int key) {
        return $.the(key);
        
    }


}
