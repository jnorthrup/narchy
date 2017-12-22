package nars.audio;

import jcog.Util;
import jcog.exe.Loop;
import nars.*;
import nars.concept.SensorConcept;
import nars.task.NALTask;
import nars.term.Termed;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.audio.AudioSource;
import spacegraph.audio.WaveCapture;

import java.util.List;

import static spacegraph.layout.Grid.grid;
import static spacegraph.layout.Grid.row;

/**
 * Created by me on 11/29/16.
 */
public class NARHear extends NAgent {

    private final AudioSource audio;
    private final WaveCapture capture;

    public static void main(String[] args) {

        //init();

        NAR n = NARS.tmp();
        n.log();
        NARHear a = new NARHear(n);
        a.runFPS(1f);
        Loop loop = a.nar.startFPS(10);

        SpaceGraph.window(
                grid(
                    row(
                            a.newMonitorPane()
                    )
//                    new MatrixView(ae.xx, (v, gl) -> { Draw.colorBipolar(gl, v); return 0; }),
//                    new MatrixView(ae.y, (v, gl) -> { Draw.colorBipolar(gl, v); return 0; })
                    //new MatrixView(ae.W.length, ae.W[0].length, MatrixView.arrayRenderer(ae.W)),
                    //Vis.conceptLinePlot(nar, freqInputs, 64)
                ),
                1200, 1200);

//        this.loop = nar.exe.loop(fps, () -> {
//            if (enabled.get()) {
//                this.now = nar.time();
//                senseAndMotor();
//                predict();
//            }
//        });

    }

    public Surface newMonitorPane() {
        return capture.newMonitorPane();
    }

    @Override
    public NALTask alwaysWant(Termed x, float conf) {
        return null; //disable goals
    }

    public NARHear(NAR nar) {
        super(nar);
        audio = new AudioSource(7, 20);
        capture = new WaveCapture(
                audio,
                //new SineSource(128),
                20);

        List<SensorConcept> freqInputs = null; //absolute value unipolar
        try {
            freqInputs = senseNumber(0, capture.freqSamplesPerFrame,
                    i -> $.func("au", $.the(i)).toString(),

            //        i -> () -> (Util.clamp(au.history[i], -1f, 1f)+1f)/2f); //raw bipolar
                    i -> () -> (Util.sqr(Util.clamp(capture.data[i], 0f, 1f))));
        } catch (Narsese.NarseseException e) {
            e.printStackTrace();
        }


//        Autoencoder ae = new Autoencoder(au.data.length, 8, new XorShift128PlusRandom(1));
//        //DurService.on(nar, ())
//        onFrame(()-> {
//            ae.put(au.data, 0.15f, 0.01f, 0.1f, true, true, true);
//        });



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
