package nars.audio;

import jcog.signal.buffer.CircularFloatBuffer;
import jcog.signal.wave1d.SignalInput;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.game.Game;
import nars.game.sensor.FreqVectorSensor;
import nars.gui.sensor.VectorSensorChart;
import nars.term.Term;
import spacegraph.SpaceGraph;
import spacegraph.audio.AudioSource;
import spacegraph.space2d.container.time.SignalView;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space2d.widget.meter.WaveBitmap;

import javax.sound.sampled.LineUnavailableException;

import static spacegraph.space2d.container.grid.Gridding.grid;

/**
 * global audio input (mixed by the sound system)
 */
public class NARAudio extends WaveIn {

    public NARAudio(Term id, NAR nar, SignalInput src, float fps) {
        super($.quote(src.toString()/*HACK*/), src, fps);

        Game h = new Game("hear");

        /**
         * buffer time in seconds
         */
//    public SignalSampling(AudioSource src, float bufferTime) {
//            this(src::writeTo, src.sampleRate(),Math.round(src.sampleRate() * bufferTime));
//        }
        CircularFloatBuffer hearBuf = new CircularFloatBuffer(in.data);
        /* hack */
        FreqVectorSensor hear = new FreqVectorSensor(nar, hearBuf /* hack */,
                512, 16, f -> $.inh(id, $.the(f)));
        h.addSensor(hear);

        //addSensor(hear);
        WaveBitmap hearView = new WaveBitmap(hearBuf, 300, 64);
        h.onFrame(hearView::update);

        SpaceGraph.window(grid(new VectorSensorChart(hear, nar).withControls(),
                //spectrogram(hear.buf, 0.1f,512, 16),
                new ObjectSurface(hear), hearView), 400, 400);


        nar.add(this);

    }

    public static void main(String[] args) throws LineUnavailableException {

        NAR n = NARS.tmp();//NARS.shell();
        n.log();

        AudioSource audio = AudioSource.all().get(0); //HACK
        SignalInput i = new SignalInput();
        i.set(audio, 1.0F /30f);

        audio.start();

        NARAudio na = new NARAudio($.quote(audio.toString()), n, i, 30f);

        SpaceGraph.window(new SignalView(i), 800, 800);

        n.startFPS(15f);

    }


}
