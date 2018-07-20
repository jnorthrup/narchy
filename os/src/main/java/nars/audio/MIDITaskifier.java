package nars.audio;

import jcog.exe.Loop;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.concept.Concept;
import nars.concept.action.GoalActionConcept;
import nars.gui.NARui;
import nars.task.DerivedTask;
import nars.task.NALTask;
import nars.term.Term;
import nars.time.Tense;
import nars.util.TimeAware;
import org.jetbrains.annotations.NotNull;
import spacegraph.SpaceGraph;
import spacegraph.audio.synth.SineWave;

import javax.sound.midi.*;
import javax.sound.sampled.LineUnavailableException;
import java.util.Arrays;
import java.util.List;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;
import static nars.time.Tense.ETERNAL;


/**
 * generic MIDI input interface
 */
public class MIDITaskifier {

    float volume[] = new float[128];

    public MIDITaskifier() {
        NAR nar = NARS.threadSafe();
        nar.termVolumeMax.set(16);


        nar.onTask(t -> {
            if (t instanceof DerivedTask && t.isGoal()) {
                
                System.err.println(t.proof());
            }
        });
        
        


        MidiInReceiver midi = MIDI(nar);

        Arrays.fill(volume, Float.NaN);

        SoNAR s = new SoNAR(nar);
        








        final List<Concept> keys = $.newArrayList();
        for (int i = 36; i <= 51; i++) {
            Term key =
                    channelKey(9, i);

            Term keyTerm = $.p(key);

            int finalI = i;






            GoalActionConcept c = new GoalActionConcept(keyTerm, nar, (b, d) -> {



                if (d == null)
                    return null;
                float v = d.freq();
                if (v > 0.55f)
                    return $.t(v, nar.confDefault(BELIEF));
                else if (b != null && b.freq() > 0.5f)
                    return $.t(0, nar.confDefault(BELIEF));
                else
                    return null;
            });
            nar.on(c);

            
            nar.input(new NALTask(c.term(), BELIEF, $.t(0f, 0.35f), 0, ETERNAL, ETERNAL, nar.evidence()));
            nar.input(new NALTask(c.term(), GOAL, $.t(0f, 0.1f), 0, ETERNAL, ETERNAL, nar.evidence()));
            nar.onCycle(n -> {

                float v = volume[finalI];

                if (v == 0) {
                    volume[finalI] = Float.NaN;
                }

                
                

                int dur = n.dur();
                n.input(c.update(n.time(), n.time()+ dur, null /* TODO */));
            });


            keys.add(c);




            s.listen(c, (k) -> {
                return new SineWave((float) (100 + Math.random() * 1000));
            });

        }


        
        new Loop(2f) {

            final Term now = $.p("now");

            @Override
            public boolean next() {
                nar.believe(now, Tense.Present);
                return true;
            }
        };





        SpaceGraph.window(NARui.beliefCharts(64, keys, nar), 900, 900);


        nar.startFPS(60f);


    }

    public static void main(String[] arg) throws LineUnavailableException {
        new MIDITaskifier();
    }

    public MidiInReceiver MIDI(TimeAware timeAware) {
        
        MidiDevice device;
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();


        for (int i = 0; i < infos.length; i++) {
            try {
                MidiDevice.Info ii = infos[i];

                device = MidiSystem.getMidiDevice(ii);

                System.out.println(device + "\t" + device.getClass());
                System.out.println("\t" + device.getDeviceInfo());
                System.out.println("\ttx: " + device.getTransmitters());
                System.out.println("\trx: " + device.getReceivers());

                if (receive(device)) {
                    return new MidiInReceiver(device, timeAware);
                }

                /*if (device instanceof Synthesizer) {
                    synthInfos.add((Synthesizer) ii);
                } else if (device instanceof MidiDevice) {
                    midis.add((MidiDevice) ii);
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

        

        private final MidiDevice device;
        private final TimeAware timeAware;

        public MidiInReceiver(MidiDevice device, TimeAware timeAware) throws MidiUnavailableException {
            this.device = device;
            this.timeAware = timeAware;

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
                        if ((volume[s.getData1()] == volume[s.getData1()]) && (volume[s.getData1()] > 0))
                            volume[s.getData1()] = 0;




                        
                        break;
                    case ShortMessage.NOTE_ON:
                        volume[s.getData1()] = 0.6f + 0.4f * s.getData2() / 128f;



                        
                        
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
