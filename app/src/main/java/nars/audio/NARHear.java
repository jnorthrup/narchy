package nars.audio;

import jcog.Util;
import jcog.learn.Autoencoder;
import jcog.math.random.XorShift128PlusRandom;
import nars.*;
import nars.concept.SensorConcept;
import nars.control.DurService;
import spacegraph.SpaceGraph;
import spacegraph.audio.AudioSource;
import spacegraph.audio.WaveCapture;
import spacegraph.render.Draw;
import spacegraph.widget.meter.MatrixView;
import spacegraph.widget.slider.FloatSlider;

import java.util.List;

import static spacegraph.layout.Grid.grid;
import static spacegraph.layout.Grid.row;

/**
 * Created by me on 11/29/16.
 */
public class NARHear extends NAgent {

    public static void main(String[] args) {

        //init();

        NAR n = NARS.tmp();
        n.log();
        NARHear a = new NARHear(n);
        a.runFPS(1f);
        NARLoop loop = a.nar.startFPS(10);



//        this.loop = nar.exe.loop(fps, () -> {
//            if (enabled.get()) {
//                this.now = nar.time();
//                senseAndMotor();
//                predict();
//            }
//        });

    }

    public NARHear(NAR nar) {
        super(nar);
        AudioSource audio = new AudioSource(7, 20);
        WaveCapture au = new WaveCapture(
                audio,
                //new SineSource(128),
                20);

        List<SensorConcept> freqInputs = null; //absolute value unipolar
        try {
            freqInputs = senseNumber(0, au.freqSamplesPerFrame,
                    i -> $.func("au", $.the(i)).toString(),

            //        i -> () -> (Util.clamp(au.history[i], -1f, 1f)+1f)/2f); //raw bipolar
                    i -> () -> (Util.sqr(Util.clamp(au.data[i], 0f, 1f))));
        } catch (Narsese.NarseseException e) {
            e.printStackTrace();
        }


        Autoencoder ae = new Autoencoder(au.data.length, 8, new XorShift128PlusRandom(1));
        //DurService.on(nar, ())
        onFrame(()-> {
            ae.put(au.data, 0.15f, 0.01f, 0.1f, true, true, true);
        });

        SpaceGraph.window(
                grid(
                    row(
                            au.newMonitorPane(),
                            new FloatSlider(audio.gain)
                    ),
                    //new MatrixView(ae.W.length, ae.W[0].length, MatrixView.arrayRenderer(ae.W)),
                    new MatrixView(ae.xx, (v, gl) -> { Draw.colorBipolar(gl, v); return 0; }),
                    new MatrixView(ae.y, (v, gl) -> { Draw.colorBipolar(gl, v); return 0; })
                    //Vis.conceptLinePlot(nar, freqInputs, 64)
                ),
                1200, 1200);

        //Vis.conceptsWindow2D(nar, 64, 4).show(800, 800);

//            b.setScene(new Scene(au.newMonitorPane(), 500, 400));
//            b.show();
//        });
    }

    @Override
    protected float act() {
        return 0;
    }


}
