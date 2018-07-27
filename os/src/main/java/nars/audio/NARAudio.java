package nars.audio;

import nars.$;
import nars.NAR;
import nars.NARS;
import spacegraph.audio.AudioSource;
import spacegraph.audio.WaveCapture;

import static spacegraph.SpaceGraph.window;

/**
 * global audio input (mixed by the sound system)
 */
public class NARAudio extends WaveIn {

    public NARAudio(NAR nar) {
        super(nar, $.the("audio"), new WaveCapture(new AudioSource(20)));
    }

    public static void main(String[] args) {
        NAR n = NARS.shell();
        NARAudio a = new NARAudio(n);
        a.start(n);
        window(a.capture.view(), 800, 800);
    }

}
