package nars.audio;

import nars.$;
import nars.NAR;
import nars.NARS;
import nars.agent.NAgent;
import nars.concept.sensor.FreqVectorSensor;
import nars.gui.sensor.VectorSensorView;
import spacegraph.audio.AudioBuffer;
import spacegraph.audio.AudioBufferView;
import spacegraph.audio.AudioSource;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space2d.widget.meter.WaveView;

import static spacegraph.SpaceGraph.window;
import static spacegraph.space2d.container.grid.Gridding.grid;

/**
 * global audio input (mixed by the sound system)
 */
public class NARAudio extends WaveIn {

    private final FreqVectorSensor hear;

    public NARAudio(NAR nar, AudioSource src, float bufferTime) {
        super($.the("audio"), new AudioBuffer(src, bufferTime));

        NAgent h = new NAgent("hear", nar);


        hear = new FreqVectorSensor(in.buffer,
                f->$.inh($.the(f), /*src.id*/ "hear"), 512,16, nar);
        h.addSensor(hear);

        //addSensor(hear);
        WaveView hearView = new WaveView(hear.buf, 300, 64);
        h.onFrame(()->{
            hearView.updateLive();
        });

        window(grid(new VectorSensorView(hear, nar).withControls(),
                //spectrogram(hear.buf, 0.1f,512, 16),
                new ObjectSurface(hear), hearView), 400, 400);


        nar.on(this);

    }

    public static void main(String[] args) {

        NAR n = NARS.shell();

        NARAudio a = new NARAudio(n, new AudioSource(), 1f);

        window(new AudioBufferView(a.in), 800, 800);

        n.startFPS(10f);

    }

}
