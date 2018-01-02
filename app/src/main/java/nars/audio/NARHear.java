package nars.audio;

import jcog.Services;
import jcog.exe.Loop;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.control.NARService;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.audio.AudioSource;
import spacegraph.audio.WaveCapture;
import spacegraph.widget.text.Label;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

import static spacegraph.layout.Grid.grid;
import static spacegraph.layout.Grid.row;

/**
 * Created by me on 11/29/16.
 */
public class NARHear extends NARService {


    final List<AudioSourceService> devices = new CopyOnWriteArrayList<>();

    static class AudioSourceService extends NARService {
        public final AudioSource audio;
        public final Supplier<WaveCapture> capture;
        private WaveCapture capturing;

        AudioSourceService(NAR nar, AudioSource audio, Supplier<WaveCapture> capture) {
            super(null, $.p($.the("audio"), $.the(audio.device)));
            this.audio = audio;
            this.capture = capture;
            nar.off(this); //default off
        }

        public Surface newMonitorPane() {
            return capturing != null ? capturing.newMonitorPane() : new Label("not enabled try again"); //HACK
        }

        @Override
        protected void start(NAR x) {
            synchronized (audio) {
                capturing = capture.get();
            }
        }

        @Override
        protected void stopping(NAR nar) {
            synchronized (audio) {
                capturing.stop();
                capturing = null;
            }
        }
    }

    public static void main(String[] args) {

        //init();

        NAR n = NARS.tmp();
        n.log();
        NARHear a = new NARHear(n);
        a.runFPS(1f);
        Loop loop = n.startFPS(10);

        SpaceGraph.window(
                grid(
                        row(
                                a.devices.get(7).newMonitorPane()
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

    private Loop runFPS(float fps) {
        return new Loop(fps) {

            @Override
            public boolean next() {
                update();
                return true;
            }
        };
    }

    protected void update() {

    }


    public NARHear(NAR nar, float fps) {
        this(nar);
        runFPS(fps);
    }

    public NARHear(NAR nar) {
        super(nar);

        AudioSource.print();

        Mixer.Info[] minfoSet = AudioSystem.getMixerInfo();


        for (int device = 0; device < minfoSet.length; device++) {

            AudioSource audio = new AudioSource(device, 20);
            AudioSourceService as = new AudioSourceService(nar, audio,
                    () -> new WaveCapture(audio,
                            //new SineSource(128),
                            20)
            );
            devices.add(as);



            //        List<SensorConcept> freqInputs = null; //absolute value unipolar
            //        try {
            //            freqInputs = senseNumber(0, capture.freqSamplesPerFrame,
            //                    i -> $.func("au", $.the(i)).toString(),
            //
            //            //        i -> () -> (Util.clamp(au.history[i], -1f, 1f)+1f)/2f); //raw bipolar
            //                    i -> () -> (Util.sqr(Util.clamp(capture.data[i], 0f, 1f))));
            //        } catch (Narsese.NarseseException e) {
            //            e.printStackTrace();
            //        }


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
    }


}
