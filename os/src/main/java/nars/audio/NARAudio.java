package nars.audio;

import jcog.event.Off;
import jcog.signal.buffer.CircularFloatBuffer;
import jcog.signal.tensor.ArrayTensor;
import jcog.signal.tensor.RingBufferTensor;
import jcog.signal.wave1d.SlidingDFTTensor;
import nars.$;
import nars.NAR;
import nars.NARS;
import spacegraph.audio.AudioSource;
import spacegraph.audio.WaveCapture;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.meta.MetaFrame;
import spacegraph.space2d.widget.meter.BitmapMatrixView;
import spacegraph.space2d.widget.meter.WaveView;
import spacegraph.video.Draw;

import static spacegraph.SpaceGraph.window;

/**
 * global audio input (mixed by the sound system)
 */
public class NARAudio extends WaveIn {

    public static class Spectrogram {
        final RingBufferTensor freq;
        final SlidingDFTTensor now;
        private final CircularFloatBuffer in;
        private final ArrayTensor inWave;

        public Spectrogram(CircularFloatBuffer in, float sampleTime, int sampleRate, int fftSize, int history) {

            this.in = in;

            this.inWave = new ArrayTensor(new float[(int) Math.ceil(sampleTime * sampleRate)]);
            now = new SlidingDFTTensor(inWave, fftSize);
            freq = (RingBufferTensor) RingBufferTensor.get(now, history);


        }

        public static Surface live(WaveCapture capture, float sampleTime, int fftSize, int history) {
            Spectrogram s = new Spectrogram(capture.buffer, sampleTime, capture.source.samplesPerSecond(), fftSize, history);
            BitmapMatrixView bmp = BitmapMatrixView.get(s.freq, Draw::colorBipolar);

            return new Gridding(bmp) {
                protected void update() {
                    s.update();
                    bmp.update();
                }

                private final Off off;
                {
                    this.off = capture.frame.on(this::update);
                }
                @Override
                public boolean stop() {
                    if (super.stop()) {
                        off.off();
                        return true;
                    }
                    return false;
                }

            };
        }

        public void update() {
            in.peekLast(inWave.data);
            now.update();
            freq.snapshot();
        }

    }

    public NARAudio(NAR nar, float bufferTime) {
        super(nar, $.the("audio"), new WaveCapture(new AudioSource(20), bufferTime));
    }

    public static void main(String[] args) {
        NAR n = NARS.shell();
        NARAudio a = new NARAudio(n, 4f);
        a.start(n);
        Gridding c = new Gridding(

                a.capture.view(),

                Spectrogram.live(a.capture, 0.1f, 128, 64),

                new Gridding(
                    new WaveView(a.capture.buffer, 1024, 256) {
                        private final Off off;
                        {
                            this.off = a.capture.frame.on(this::update);
                        }
                        @Override
                        public boolean stop() {
                            if (super.stop()) {
                                off.off();
                                return true;
                            }
                            return false;
                        }

                        @Override
                        public void update() {
                            long width= vis.last-vis.first;
                            vis.last = a.capture.buffer._viewPtr; //getPeekPosition();
                            vis.first = Math.max(0, vis.last - (width));
                            super.update();
                        }


                    },
                    new PushButton("Record Clip", () -> {
                        window(new MetaFrame(new WaveView(a.capture, 1f, 1024, 256)), 400, 400);
                    })
                )
        );
        window(c, 800, 800);

    }

}
