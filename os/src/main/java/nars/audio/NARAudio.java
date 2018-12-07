package nars.audio;

import nars.$;
import nars.NAR;
import nars.NARS;
import spacegraph.audio.AudioBuffer;
import spacegraph.audio.AudioBufferView;
import spacegraph.audio.AudioSource;

import static spacegraph.SpaceGraph.window;

/**
 * global audio input (mixed by the sound system)
 */
public class NARAudio extends WaveIn {

    public NARAudio(NAR nar, float bufferTime) {
        super($.the("audio"), new AudioBuffer(new AudioSource(), bufferTime));
        nar.on(this);
    }

    public static void main(String[] args) {

        NAR n = NARS.shell();

        NARAudio a = new NARAudio(n, 1f);

        window(new AudioBufferView(a.in), 800, 800);

    }

}
