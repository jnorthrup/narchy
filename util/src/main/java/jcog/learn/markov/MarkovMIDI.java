package jcog.learn.markov;

import jcog.data.list.FasterList;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

public class MarkovMIDI extends MarkovSampler<MarkovMIDI.MidiMessageWrapper> {

    private final MarkovChain<Long> mLengthChain;

    public MarkovMIDI(int n) {
        super(new MarkovChain<>(n), new Random());
        mLengthChain = new MarkovChain<>(n);
    }

//    public void importMIDI(File file) throws InvalidMidiDataException, IOException {
//        Sequence s = MidiSystem.getSequence(file);
//        learnSequence(s, MidiSystem.getMidiFileFormat(file));
//    }

    public void learnSequence(Sequence s) {
        for (Track track : s.getTracks())
            learnTrack(track);
    }

    public void learnTrack(Track t) {
        int trackSize = t.size();
        if (trackSize == 0) return;
        List<MidiMessageWrapper> msgs = new FasterList<>();
        List<Long> times = new FasterList<>();

        MidiEvent event = t.get(0);
        long lastTick = event.getTick();
        MidiMessage msg = event.getMessage();
        MidiMessageWrapper wrap = new MidiMessageWrapper(msg);
        times.add(lastTick);

        
//        int resolution = fmt.getResolution();
//        long beats = event.getTick() / resolution;
//        int adds = 0;

        for (int i = 1; i < trackSize; i++) {
            event = t.get(i);
            msg = event.getMessage();
            wrap = new MidiMessageWrapper(msg);
            long eTick = event.getTick();
            times.add(eTick - lastTick);
            lastTick = eTick;
            msgs.add(wrap);
        }

        if (msgs.size() > 0)
            model.learn(msgs);

        mLengthChain.learn(times);
    }

    public void exportTrack(String filename, float divisionType, int resolution, int fileType)
            throws InvalidMidiDataException, IOException {
        exportTrack(new File(filename), divisionType, resolution, fileType, 0);
    }

    public void exportTrack(String filename, float divisionType, int resolution, int fileType, int maxLength)
            throws InvalidMidiDataException, IOException {
        exportTrack(new File(filename), divisionType, resolution, fileType, maxLength);
    }

    public void exportTrack(File file, float divisionType, int resolution, int fileType, int maxLength)
            throws InvalidMidiDataException, IOException {

        Sequence s = new Sequence(divisionType, resolution, 1);
        Track t = s.createTrack();
        reset();

        MarkovSampler<Long> mLengthSampler = mLengthChain.sample();

        int ticks = 0;
        MidiMessageWrapper wrpmsg;
        System.out.println("Max length: " + maxLength);
        while ((wrpmsg = next(maxLength)) != null) {
            MidiMessage msg = wrpmsg.getMessage();

            long dt = mLengthSampler.nextLoop();
            ticks += dt;
            MidiEvent event = new MidiEvent(msg, ticks);

            if (t.add(event) == false) {
            }
        }

        MidiSystem.write(s, fileType, file);
    }

    public void exportTrack(String s, File inputFile, int maxLen) throws InvalidMidiDataException, IOException {
        MidiFileFormat fmt = MidiSystem.getMidiFileFormat(inputFile);
        exportTrack(s, fmt.getDivisionType(), fmt.getResolution(), fmt.getType(), maxLen);
    }

    public static class MidiMessageWrapper implements Comparable<MidiMessageWrapper> {
        private final MidiMessage mMessage;

        public MidiMessageWrapper(MidiMessage msg) {
            mMessage = msg;
        }

        public MidiMessage getMessage() {
            return mMessage;
        }

        public int hashCode() {
            if (mMessage == null || mMessage.getLength() == 0) return 0;
            String str = toString();
            return str.hashCode();
        }

        @Override
        public int compareTo(MidiMessageWrapper other) {
            byte[] mymsg = mMessage.getMessage();
            byte[] theirmsg = mMessage.getMessage();


            for (int i = 0; i < mymsg.length && i < theirmsg.length; i++) {
                if (mymsg[i] > theirmsg[i]) {
                    return 1;
                } else if (theirmsg[i] > mymsg[i]) return -1;
            }

            return Integer.compare(mymsg.length, theirmsg.length);
        }

        public boolean equals(Object o) {
            try {
                MidiMessageWrapper other = (MidiMessageWrapper) o;
                byte[] mine = mMessage.getMessage();
                byte[] theirs = other.getMessage().getMessage();

                if (mine.length != theirs.length) return false;

                for (int i = 0; i < mine.length; i++) {
                    if (mine[i] != theirs[i]) {
                        return false;
                    }
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        public String toString() {
            String out = "";
            for (int i = 0; i < mMessage.getLength(); i++) {
                out += String.format("%x", mMessage.getMessage()[i]);
                if (i < mMessage.getLength() - 1) out += " ";
            }
            return out;
        }

    }
}
