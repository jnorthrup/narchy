package nars.audio;

import jcog.event.On;
import nars.$;
import nars.NAR;
import nars.NARS;
import spacegraph.audio.AudioSource;
import spacegraph.audio.WaveCapture;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.meter.Plot2D;

import static spacegraph.SpaceGraph.window;

/**
 * global audio input (mixed by the sound system)
 */
public class NARAudio extends WaveIn {

    public NARAudio(NAR nar) {
        super(nar, $.the("audio"), new WaveCapture(new AudioSource(20)));
    }

    static class ClipRecordingView extends Gridding {

        private final long startMS;
        private final int totalSamples;
        private final float[] wave;
        private final On capturing;
        private int dataPos = 0;

        public ClipRecordingView(WaveCapture capture, float seconds) {
            super();
            this.startMS = System.currentTimeMillis();
            this.totalSamples = (int) Math.ceil(seconds * capture.source.samplesPerSecond());
            this.wave = new float[totalSamples];

            capturing = capture.frame.on(this::capture);
            System.out.println("capturing " + totalSamples);
        }

        private void capture(WaveCapture f) {
            int samplesRecv = Math.min(wave.length - dataPos, f.samples.length); //HACK
            System.arraycopy(f.samples, 0, wave, dataPos, samplesRecv); //HACK
            dataPos += samplesRecv;
            if (dataPos >= totalSamples) {
                capturing.off();
                System.out.println("done");
                showWave();
            } else {
                System.out.println("got: " + dataPos);
            }
        }

        private void showWave() {
            Plot2D p = new Plot2D(totalSamples, new Plot2D.BitmapWave(1024,128)).add("Amp", wave);
            set(p);
        }

    }

    public static void main(String[] args) {
        NAR n = NARS.shell();
        NARAudio a = new NARAudio(n);
        a.start(n);
        Surface v = a.capture.view();
        Gridding c = new Gridding(
                v,
                new PushButton("Record Clip", () -> {
                    window(new ClipRecordingView(a.capture, 1f), 400, 400);
                })
        );
        window(c, 800, 800);

    }

}
