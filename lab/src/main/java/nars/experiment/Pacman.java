package nars.experiment;

import jcog.signal.wave2d.Bitmap2D;
import jcog.signal.wave2d.BrightnessNormalize;
import jcog.signal.wave2d.ScaledBitmap2D;
import nars.$;
import nars.GameX;
import nars.NAR;
import nars.agent.GameTime;
import nars.experiment.pacman.PacmanGame;
import nars.gui.sensor.VectorSensorView;
import nars.sensor.Bitmap2DSensor;
import nars.video.SwingBitmap2D;
import spacegraph.SpaceGraph;
import spacegraph.space2d.container.grid.Gridding;


public class Pacman extends GameX {

    private final PacmanGame g;

    public Pacman(NAR nar) {
        super("Pac",
                //GameTime.durs(0.5f),
                GameTime.fps(20f),
                nar);
        Gridding gg = new Gridding();

        this.g = new PacmanGame();



        Bitmap2D camScale = new BrightnessNormalize(new ScaledBitmap2D(new SwingBitmap2D(g.view), 32, 32));
        onFrame(camScale::updateBitmap);

//        for (MonoBufImgBitmap2D.ColorMode cm : new MonoBufImgBitmap2D.ColorMode[]{
//                MonoBufImgBitmap2D.ColorMode.R,
//                MonoBufImgBitmap2D.ColorMode.G,
//                MonoBufImgBitmap2D.ColorMode.B
//        }) {
//            Bitmap2DSensor c = senseCamera(
//                    (x,y)->$.func((Atomic)id, $.the(cm.name()), $.the(x), $.the(y)),
//                    camScale.filter(cm)
//            );
//
//            VectorSensorView v = new VectorSensorView(c, this);
////            onFrame(v::update);
//            gg.add(v/*.withControls()*/);
//            c.resolution(0.1f);
//        }
        {
            Bitmap2DSensor c = senseCamera((x,y)->$.inh(id,$.p(x,y)), camScale/*, 0*/);
            VectorSensorView v = new VectorSensorView(c, this);
            gg.add(v/*.withControls()*/);
            c.resolution(0.02f);
        }
        SpaceGraph.window(gg, 300, 300);

//        actionPushButtonMutex($.inh(id,"left"), $.inh(id,"right"), ()->{
//            g.keys[1] = true;
//            g.keys[0] = false;
//        }, ()->{
//            g.keys[0] = true;
//            g.keys[1] = false;
//        });
        actionTriState($.p(id,$.p($.the("x"), $.varQuery(1))), (dh) -> {
            switch (dh) {
                case +1:
                    g.keys[1] = true;
                    g.keys[0] = false;
                    break;
                case -1:
                    g.keys[0] = true;
                    g.keys[1] = false;
                    break;
                case 0:
                    g.keys[0] = g.keys[1] = false;
                    break;
            }
        });

        actionTriState($.p(id,$.p($.the("y"), $.varQuery(1))), (dh) -> {
            switch (dh) {
                case +1:
                    g.keys[2] = true;
                    g.keys[3] = false;
                    break;
                case -1:
                    g.keys[3] = true;
                    g.keys[2] = false;
                    break;
                case 0:
                    g.keys[2] = g.keys[3] = false;
                    break;
            }
        });


        //TODO multiple reward signals: eat, alive, dist->ghost (cheat)
        reward("score", ()->{
            g.update();

            int nextScore = g.score;

            float r = (nextScore - lastScore);
//            if(r == 0)
//                return Float.NaN;


            lastScore = nextScore;
            if (r > 0) return +1;
            else if (r < 0) return 0;
            else
                return 0.5f;
            //return (Util.tanhFast(r) + 1)/2f;
        });
    }


    int lastScore;


    public static void main(String[] args) {
        GameX.runRT((n) -> {

            Pacman a = new Pacman(n);
            return a;

        }, 20);
    }

}
