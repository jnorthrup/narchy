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
import spacegraph.space2d.widget.meta.MetaFrame;
import spacegraph.space2d.widget.meter.WaveView;

import static spacegraph.SpaceGraph.window;

/**
 * global audio input (mixed by the sound system)
 */
public class NARAudio extends WaveIn {

    public NARAudio(NAR nar, float bufferTime) {
        super(nar, $.the("audio"), new WaveCapture(new AudioSource(20), bufferTime));
    }

    public static void main(String[] args) {
        NAR n = NARS.shell();
        NARAudio a = new NARAudio(n, 10f);
        a.start(n);
        Surface v = a.capture.view();
        Gridding c = new Gridding(
                v,
                new Gridding(
                    new WaveView(a.capture.buffer, 1024, 256) {
                        private final On off;

                        {
                            this.off = a.capture.frame.on(this::update);
                        }

                        @Override
                        public void update() {
                            long width= vis.last-vis.first;
                            vis.last = a.capture.buffer._viewPtr; //getPeekPosition();
                            vis.first = Math.max(0, vis.last - (width));
                            super.update();
                        }

                        @Override
                        public boolean stop() {
                            if (super.stop()) {
                                off.off();
                                return true;
                            }
                            return false;
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
