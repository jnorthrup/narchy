package nars.audio;

import jcog.exe.Exe;
import nars.$;
import nars.NAR;
import nars.NARS;
import spacegraph.audio.AudioSource;
import spacegraph.audio.WaveCapture;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.meter.Plot2D;

import static spacegraph.SpaceGraph.window;

/**
 * global audio input (mixed by the sound system)
 */
public class NARAudio extends WaveIn {

    public NARAudio(NAR nar, float bufferTime) {
        super(nar, $.the("audio"), new WaveCapture(new AudioSource(20), bufferTime));
    }

    static class ClipRecordingView extends Gridding {

        private final long startMS;
        private final int totalSamples;
        private final float[] wave;
        private final WaveCapture capture;

        public ClipRecordingView(WaveCapture capture, float seconds) {
            super();
            this.startMS = System.currentTimeMillis();
            this.totalSamples = (int) Math.ceil(seconds * capture.source.samplesPerSecond());
            this.wave = new float[totalSamples];
            this.capture = capture;

        }

        @Override
        public boolean start(SurfaceBase parent) {
            if (super.start(parent)) {
                Exe.invoke(()->{
                    capture.buffer.peekLast(wave, wave.length);

                    showWave();
                });
                return true;
            }
            return false;
        }
        //        private void capture(WaveCapture f) {
//            int end = f.buffer.getPeekPosition();
//            int dataPos = f.buffer.read(wave, Math.max(0, end - wave.length), end, false);
//
//            //if (dataPos >= totalSamples) {
//                capturing.off();
//                showWave();
////            } else {
////                System.out.println("got: " + dataPos);
////            }
//        }

        private void showWave() {
            Plot2D p = new Plot2D(totalSamples, new Plot2D.BitmapWave(1024,128)).add("Amp", wave);
            set(p);
        }

    }

    public static void main(String[] args) {
        NAR n = NARS.shell();
        NARAudio a = new NARAudio(n, 10f);
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
