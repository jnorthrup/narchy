package spacegraph.audio.midi;

import javax.sound.midi.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/** from https://github.com/Xyene/VirtualSynth */
public class Synth {
    private static MidiChannel channel;
    private static Synthesizer synthesizer;
    private static Instrument[] soundbank;
    private static JComboBox instrumentControl;
    private static JSlider volumeControl;
    private static JSlider forceControl;

    public static void main(String[] args) throws MidiUnavailableException {
        //UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        JFrame frame = new JFrame("Virtual MIDI Synthesizer");
        frame.setLayout(new BorderLayout());


        (instrumentControl = new JComboBox()).setFocusable(false);

        (synthesizer = MidiSystem.getSynthesizer()).open();
//        MidiSystem.getTransmitter();
        var v = MidiSystem.getMidiDevice(MidiSystem.getMidiDeviceInfo()[1]).getReceiver();
        System.out.println(v);

//        Sequencer seq = MidiSystem.getSequencer();
//        seq.open();

//        seq.setSequence(new Sequence(Sequence.PPQ, 96));

//        seq.addControllerEventListener(metaMessage -> System.out.println(metaMessage), new int[] { 0 });

//        for (var x : MidiSystem.getMidiDeviceInfo())
//            System.out.println(x.getName() + " " + x.getDescription() + " " + x.getVendor());

        Soundbank sb = synthesizer.getDefaultSoundbank();
        soundbank = sb.getInstruments();
        for (Instrument is : soundbank) instrumentControl.addItem(is.getName());

        //System.out.println("Loaded " + soundbank.length + " instruments.");

        synthesizer.loadInstrument(soundbank[0]);

        channel = synthesizer.getChannels()[0];

        instrumentControl.addActionListener(new ChangeInstrumentAction());

        KeyboardControl keyboardControl;
        frame.add(keyboardControl = new KeyboardControl(new MIDIController()), BorderLayout.NORTH);

        JPanel controls = new JPanel(new BorderLayout());
        {
            controls.add(group(new JLabel("Instruments: "), instrumentControl), BorderLayout.NORTH);
            controls.add(group(new JLabel("Volume:      "), volumeControl = new JSlider()), BorderLayout.CENTER);
            controls.add(group(new JLabel("Velocity:     "), forceControl = new JSlider()), BorderLayout.SOUTH);
            volumeControl.setMaximum(127);
            volumeControl.setValue(127);
            forceControl.setMaximum(127);
            forceControl.setValue(127);
            volumeControl.addChangeListener(e -> channel.controlChange(7, volumeControl.getValue()));
            volumeControl.setFocusable(false);
            forceControl.setFocusable(false);
        }
        frame.add(controls, BorderLayout.SOUTH);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.setResizable(false);
    }

    private static JPanel group(JComponent one, JComponent two) {
        JPanel holder = new JPanel(new BorderLayout());
        holder.add(one, BorderLayout.WEST);
        holder.add(two, BorderLayout.CENTER);
        return holder;
    }

    public static class ChangeInstrumentAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            synthesizer.loadInstrument(soundbank[instrumentControl.getSelectedIndex()]);
            channel.programChange(instrumentControl.getSelectedIndex());
        }
    }

    static class MIDIController implements KeyboardControl.Controller {
        @Override
        public void press(int key) {
            channel.noteOn(key, forceControl.getValue());
        }

        @Override
        public void release(int key) {
            channel.noteOff(key, forceControl.getValue());
        }
    }
}
