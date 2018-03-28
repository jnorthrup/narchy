package spacegraph.audio.midi;

import javax.sound.midi.*;
import java.util.Arrays;



/**
 * generic MIDI input interface
 */
public class MIDI {

    private final MidiInReceiver receiver;
    float volume[] = new float[128];

    public MIDI() {


        // Obtain information about all the installed synthesizers.
        MidiDevice device;
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();

        MidiInReceiver receiver = null;
        for (int i = 0; i < infos.length; i++) {
            try {
                MidiDevice.Info ii = infos[i];

                device = MidiSystem.getMidiDevice(ii);

                System.out.println(device + "\t" + device.getClass());
                System.out.println("\t" + device.getDeviceInfo());
                System.out.println("\ttx: " + device.getTransmitters());
                System.out.println("\trx: " + device.getReceivers());

                if (receive(device)) {
                    receiver = new MidiInReceiver(device);
                    break;
                }

                /*if (device instanceof Synthesizer) {
                    synthInfos.add((Synthesizer) ii);
                } else if (device instanceof MidiDevice) {
                    midis.add((MidiDevice) ii);
                }*/
            } catch (MidiUnavailableException e) {
                // Handle or throw exception...
            }
        }

        this.receiver = receiver;

        Arrays.fill(volume, Float.NaN);

    }


    public static boolean receive(MidiDevice device) {
        return device.getDeviceInfo().getName().startsWith("MPD218");
    }

    public class MidiInReceiver implements Receiver {

        //public final Map<Term,FloatParam> key = new ConcurrentHashMap<>();

        private final MidiDevice device;

        public MidiInReceiver(MidiDevice device) throws MidiUnavailableException {
            this.device = device;

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

//                        Compound t = $.inh(channelKey(s), Atomic.the("on"));
//
//                        nar.believe($.neg(t), Tense.Present);
                        //System.out.println(key(t));
                        break;
                    case ShortMessage.NOTE_ON:
                        volume[s.getData1()] = 0.6f + 0.4f * s.getData2() / 128f;

//                        Compound u = $.inh(channelKey(s), Atomic.the("on"));
//                        nar.believe(u, Tense.Present);
                        //key(u, 0.5f + 0.5f * s.getData2()/64f);
                        //System.out.println(key(t));
                        break;
                    default:
                        //System.out.println("unknown command: " + s);
                        break;
                    //case ShortMessage.CONTROL_CHANGE:
                }
            }

        }

//        public FloatParam key(Compound t) {
//            return key.computeIfAbsent(t, tt -> new FloatParam(Float.NaN));
//        }
//
//        public void key(Compound t, float v) {
//            v = Util.unitize(v);
//            MutableFloat m = key(t);
//            m.setValue(v);
//        }

        @Override
        public void close() {

        }
    }

//    public static @NotNull Compound channelKey(ShortMessage s) {
//        return channelKey(s.getChannel(), s.getData1() /* key */);
//    }

}
