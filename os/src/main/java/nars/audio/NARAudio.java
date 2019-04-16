package nars.audio;

import jcog.signal.buffer.CircularFloatBuffer;
import jcog.signal.wave1d.SignalInput;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.agent.Game;
import nars.concept.sensor.FreqVectorSensor;
import nars.gui.sensor.VectorSensorView;
import spacegraph.SpaceGraph;
import spacegraph.audio.AudioSource;
import spacegraph.space2d.container.time.SignalView;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space2d.widget.meter.WaveBitmap;

import static spacegraph.space2d.container.grid.Gridding.grid;

/**
 * global audio input (mixed by the sound system)
 */
public class NARAudio extends WaveIn {

    private final FreqVectorSensor hear;

    public NARAudio(NAR nar, SignalInput src, float fps) {
        super($.quote(src.toString()/*HACK*/), src, fps);

        Game h = new Game("hear", nar);

        /**
         * buffer time in seconds
         */
//    public SignalSampling(AudioSource src, float bufferTime) {
//            this(src::writeTo, src.sampleRate(),Math.round(src.sampleRate() * bufferTime));
//        }
        CircularFloatBuffer hearBuf = new CircularFloatBuffer(in.data);
        hear = new FreqVectorSensor(hearBuf /* hack */,
                f->$.inh($.the(f), /*src.id*/ "hear"), 512,16, nar);
        h.addSensor(hear);

        //addSensor(hear);
        WaveBitmap hearView = new WaveBitmap(hearBuf, 300, 64);
        h.onFrame(hearView::update);

        SpaceGraph.window(grid(new VectorSensorView(hear, nar).withControls(),
                //spectrogram(hear.buf, 0.1f,512, 16),
                new ObjectSurface(hear), hearView), 400, 400);


        nar.start(this);

    }

    public static void main(String[] args) {

        NAR n = NARS.shell();

        AudioSource audio = new AudioSource();
        SignalInput i = new SignalInput();
        i.set(audio, 1/30f);

        audio.start();

        NARAudio na = new NARAudio(n, i, 30f);

        SpaceGraph.window(new SignalView(i).withControls(), 800, 800);

        n.startFPS(15f);

    }


}
