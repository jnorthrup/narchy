package spacegraph.audio.synth.string;


import jcog.Util;
import jcog.signal.buffer.CircularFloatBuffer;

import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;

/**
 * A client that uses the synthesizer package to replicate a plucked guitar string sound
 */
public class StringSynth {
    private static final double CONCERT_A = 440.0;
    private static final double base = indexToET(0, 33, 12); // 65.4064 = C2, 87.3071 = F2, 97.9989 = G2, 110 = A2
    //    private static final String keyboard = " `1qaz2wsx3edc4rfv5tgb6yhn7ujm8ik,9ol.0p;/-['=]\\~!QAZ@WSX#EDC$RFV%TGB^YHN&UJM*IK<(OL>)P:?_{\"+}|";
    private static final List<Integer> keycodes = Arrays.asList(192, 9, 20, 16, 49, 81, 65, 90, 50, 87, 83, 88, 51, 69, 68, 67, 52, 82, 70, 86, 53, 84, 71, 66, 54,
            89, 72, 78, 55, 85, 74, 77, 56, 73, 75, 44, 57, 79, 76, 46, 48, 80, 59, 47, 45, 91, 222, 16, 61, 93, 10, 8, 92);
    private final int keyboardSize = keycodes.size()/2;
    private final KarplusStrongString[] strings = new KarplusStrongString[keyboardSize];
    private int alt = 0;
//    private boolean hold = false;
    private float amp = 1.0F;

    public StringSynth() {
//        this.setPreferredSize(new Dimension(500, 500));
//        addKeyListener(this);
//
//        JFrame f = new JFrame();
//        f.getContentPane().addAt(this);
//        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
//        f.pack();
//        f.setVisible(true);
//        f.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, Collections.<AWTKeyStroke>emptySet());

        double[] asymmetric_5limit = {1.0, (double) ((float) 16 / 15.0F), (double) ((float) 9 / 8.0F), (double) ((float) 6 / 5.0F), (double) ((float) 5 / 4.0F), (double) ((float) 4 / 3.0F), (double) ((float) 45 / 32.0F), (double) ((float) 3 / 2.0F), (double) ((float) 8 / 5.0F), (double) ((float) 5 / 3.0F), (double) ((float) 9 / 5.0F), (double) ((float) 15 / 8.0F)};
        double[] symmetric_5limit = {1.0, (double) ((float) 16 / 15.0F), (double) ((float) 9 / 8.0F), (double) ((float) 6 / 5.0F), (double) ((float) 5 / 4.0F), (double) ((float) 4 / 3.0F), (double) ((float) 45 / 32.0F), (double) ((float) 3 / 2.0F), (double) ((float) 8 / 5.0F), (double) ((float) 5 / 3.0F), (double) ((float) 16 / 9.0F), (double) ((float) 15 / 8.0F)};
        double[] just_2minor = {1.0, (double) ((float) 16 / 15.0F), (double) ((float) 9 / 8.0F), (double) ((float) 6 / 5.0F), (double) ((float) 5 / 4.0F), (double) ((float) 27 / 20.0F), (double) ((float) 45 / 32.0F), (double) ((float) 3 / 2.0F), (double) ((float) 8 / 5.0F), (double) ((float) 27 / 16.0F), (double) ((float) 9 / 5.0F), (double) ((float) 15 / 8.0F)};
        double[] pythagorean_6 = {1.0, (double) ((float) 16 / 15.0F), (double) ((float) 9 / 8.0F), (double) ((float) 6 / 5.0F), (double) ((float) 5 / 4.0F), (double) ((float) 4 / 3.0F), (double) ((float) 45 / 32.0F), (double) ((float) 3 / 2.0F), (double) ((float) 8 / 5.0F), (double) ((float) 27 / 16.0F), (double) ((float) 9 / 5.0F), (double) ((float) 15 / 8.0F)};
        double[] just_major = {1.0, (double) ((float) 25 / 24.0F), (double) ((float) 9 / 8.0F), (double) ((float) 5 / 4.0F), (double) ((float) 4 / 3.0F), (double) ((float) 27 / 20.0F), (double) ((float) 45 / 32.0F), (double) ((float) 3 / 2.0F), (double) ((float) 25 / 16.0F), (double) ((float) 5 / 3.0F), (double) ((float) 27 / 16.0F), (double) ((float) 15 / 8.0F)};
        double[] harmonic12 = {1.0, (double) ((float) 17 / 16.0F), (double) ((float) 9 / 8.0F), (double) ((float) 19 / 16.0F), (double) ((float) 5 / 4.0F), (double) ((float) 21 / 16.0F), (double) ((float) 11 / 8.0F), (double) ((float) 23 / 16.0F), (double) ((float) 3 / 2.0F), (double) ((float) 13 / 8.0F), (double) ((float) 7 / 4.0F), (double) ((float) 15 / 8.0F)};
        double[] harmonic16 = {1.0, (double) ((float) 17 / 16.0F), (double) ((float) 9 / 8.0F), (double) ((float) 19 / 16.0F), (double) ((float) 5 / 4.0F), (double) ((float) 21 / 16.0F), (double) ((float) 11 / 8.0F), (double) ((float) 23 / 16.0F), (double) ((float) 3 / 2.0F), (double) ((float) 25 / 16.0F), (double) ((float) 13 / 8.0F), (double) ((float) 27 / 16.0F), (double) ((float) 7 / 4.0F), (double) ((float) 29 / 16.0F), (double) ((float) 15 / 8.0F), (double) ((float) 31 / 16.0F)};
        double[] harmonic8 = {1.0, (double) ((float) 9 / 8.0F), (double) ((float) 5 / 4.0F), (double) ((float) 11 / 8.0F), (double) ((float) 3 / 2.0F), (double) ((float) 13 / 8.0F), (double) ((float) 7 / 4.0F), (double) ((float) 15 / 8.0F)};
        double[] seventh = {1.0, 135.0 / 128.0, 9.0 / 8.0, 5.0 / 4.0, 21.0 / 16.0, 45.0 / 32.0, 189.0 / 128.0, 3.0 / 2.0, 27.0 / 16.0, 7.0 / 4.0, 15.0 / 8.0, 63.0 / 32.0};
        double[] partch = {1.0, 81.0 / 80.0, 33.0 / 32.0, 21.0 / 20.0, 16.0 / 15.0, 12.0 / 11.0, 11.0 / 10.0, 10.0 / 9.0, 9.0 / 8.0, 8.0 / 7.0, 7.0 / 6.0, 32.0 / 27.0, 6.0 / 5.0, 11.0 / 9.0, 5.0 / 4.0, 14.0 / 11.0,
                9.0 / 7.0, 21.0 / 16.0, 4.0 / 3.0, 27.0 / 20.0, 11.0 / 8.0, 7.0 / 5.0, 10.0 / 7.0, 16.0 / 11.0, 40.0 / 27.0, 3.0 / 2.0, 32.0 / 21.0, 14.0 / 9.0, 11.0 / 7.0, 8.0 / 5.0, 18.0 / 11.0, 5.0 / 3.0,
                27.0 / 16.0, 12.0 / 7.0, 7.0 / 4.0, 16.0 / 9.0, 9.0 / 5.0, 20.0 / 11.0, 11.0 / 6.0, 15.0 / 8.0, 40.0 / 21.0, 64.0 / 33.0, 160.0 / 81.0};

        for (int i = 0; i < keyboardSize; i++) {
            double F = indexToET(i, 35, 12);
            strings[i] =
                    //new SquareVolumeWave(F);
                    new GuitarString(F);
            //new ViolinString(F);
            //new ViolinDiffDecayString(F);
            //strings[i + keyboardSize] = new SquareVolumeWave(indexToCustom(i, 2, asymmetric_5limit));
        }
    }

    public void keyPressed(KeyEvent e) {
        int index = keycodes.indexOf(e.getKeyCode());
        if (index == 3 && e.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT) {
            index = 47;
        }
        if (index != -1) {
            keyPress(index, false);
        } else if (e.getKeyCode() == KeyEvent.VK_UP) {
            for (KarplusStrongString x : strings) {
                x.increaseVolume();
            }
        } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            for (KarplusStrongString x : strings) {
                x.decreaseVolume();
            }
        } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            // decrease pitch
        } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            // increase pitch
        } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            alt = keyboardSize - alt;
        } else if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
            releaseHeld();
            //hold = true;
        }
    }

    public void keyPress(int stringIndex, boolean hold) {
        KarplusStrongString string = strings[stringIndex + alt];
//        int status = string.status();
//        if (status == 3 || (status == 0 && hold)) {
//            string.setStatus(2);
//        } else if (status == 0) {
            string.setStatus(1);
//        }
//
//        if (hold) {
//            string.release();
//        }
    }

    public void keyReleased(KeyEvent e) {
        int index = keycodes.indexOf(e.getKeyCode());
        if (index == 3 && e.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT) {
            index = 47;
        }
        keyRelease(index, e.getKeyCode() == KeyEvent.VK_CONTROL);
    }

    public void keyRelease(int index, boolean cut) {
        if (index != -1) {
            if (strings[index].status() == -1) {
                strings[index].release();
                strings[index].setStatus(0);
            } else if (strings[index].status() == -2) {
                strings[index].setStatus(3);
            }
            if (strings[index + keyboardSize].status() == -1) {
                strings[index + keyboardSize].release();
                strings[index + keyboardSize].setStatus(0);
            } else if (strings[index + keyboardSize].status() == -2) {
                strings[index + keyboardSize].setStatus(3);
            }
        } else if (cut) {
            //hold = false;
        }
    }

    public void holdKeys() {
        for (KarplusStrongString x : strings) {
            if (x.status() == -1) {
                x.setStatus(-2);
            }
        }
    }

    public void releaseHeld() {
        for (KarplusStrongString x : strings) {
            if (x.status() == -2 || x.status() == 3) {
                x.setStatus(0);
                x.release();
            }
        }
    }


    // returns frequency in the octave above root
    private static double normalize(double root, double frequency) {
        if (frequency < root) {
            while (frequency < root) {
                frequency *= 2.0;
            }
        } else {
            while (frequency > root * 2.0) {
                frequency /= 2.0;
            }
        }
        return frequency;
    }

    private static double indexToET(int index, int indexOfA, int divisions) {
        return CONCERT_A * Math.pow(2.0, (double) ((float) (index - indexOfA) / (float) divisions));
    }

    // assumes base is in lowest octave
    private static double indexToGenerator(int index, int indexOfBase, double generator) {
        int note = (index + 12 - indexOfBase) % 12;
        int fifths = 0;
        while (note != 0) {
            note = (note - 7) % 12;
            fifths++;
        }
        if (fifths >= 7) {
            fifths -= 12;
        }
        double frequency = base * Math.pow(generator, (double) fifths);
        frequency = normalize(base, frequency);
        int octaves = Math.floorDiv(index - indexOfBase, 12);
        return frequency * Math.pow(2.0, (double) octaves);
    }

    private static double indexToCustom(int index, int indexOfBase, double[] ratios) {
        int octaveSize = ratios.length;
        int note = (index + octaveSize - indexOfBase) % octaveSize;
        int octaves = Math.floorDiv(index - indexOfBase, octaveSize);
        return base * ratios[note] * Math.pow(2.0, (double) octaves);
    }


    public static void main(String[] args) {


        //EchoFilter echo = new spacegraph.audio.filter.EchoFilter(0.75, 10, 44100);

        StringSynth h = new StringSynth();

        while (true) {

            double sample = h.next();

            /* play the sample on standard audio */


//            sample = Distortion.distortion(sample, 32, 1);
//            sample = echo.apply(sample);


            StdAudio.play(sample);
        }
    }

    public void next(float[] target) {
        for (int i = 0; i < target.length; ) {
            target[i++] = (float) next();
        }
    }

    float[] tmp = new float[0];

    public void next(CircularFloatBuffer target, int num) {
        if (tmp.length != num)
            tmp = new float[num];
        next(tmp);
        target.write(tmp);
    }

    public double next() {

        double sample = 0.0;

        for (KarplusStrongString x : strings) {
            int xs = x.status();
            if (xs == 1 || xs == 2) {
                x.pluck();
                x.setStatus(-1 * xs);
            }
            sample += x.sample() * (double) amp;
        }

        return Util.clampSafe(sample, -1.0, (double) +1);
    }

    public void amp(float a) {
        this.amp = a;
    }

    public float amp() {
        return amp;
    }
}