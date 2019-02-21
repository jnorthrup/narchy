package nars.experiment;

import jcog.signal.wave2d.MonoBufImgBitmap2D;
import jcog.signal.wave2d.ScaledBitmap2D;
import nars.$;
import nars.NAR;
import nars.NAgentX;
import nars.agent.FrameTrigger;
import nars.experiment.pacman.PacmanGame;
import nars.gui.sensor.VectorSensorView;
import nars.sensor.Bitmap2DSensor;
import nars.video.SwingBitmap2D;
import spacegraph.space2d.container.grid.Gridding;

import static spacegraph.SpaceGraph.window;


public class Pacman extends NAgentX {

    private final PacmanGame g;

    public Pacman(NAR nar) {
        super("G", FrameTrigger.fps(20), nar);

        this.g = new PacmanGame();



        Gridding gg = new Gridding();
        ScaledBitmap2D camScale = new ScaledBitmap2D(new SwingBitmap2D(g.view), 15, 14);
        onFrame(camScale::update);

        for (MonoBufImgBitmap2D.ColorMode cm : new MonoBufImgBitmap2D.ColorMode[]{
                MonoBufImgBitmap2D.ColorMode.R,
                MonoBufImgBitmap2D.ColorMode.G,
                MonoBufImgBitmap2D.ColorMode.B
        }) {
            Bitmap2DSensor c = senseCamera(
                    $.p($.the("c"), $.the(cm.ordinal())),
                    camScale.filter(cm)
            );

            VectorSensorView v = new VectorSensorView(c, this);
//            onFrame(v::update);
            gg.add(v/*.withControls()*/);
            c.resolution(0.1f);
        }
        window(gg, 900, 300);

        actionTriState($.the("x") /*$.p(id, Atomic.the("x"))*/, (dh) -> {
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

        actionTriState($.the("y") /*$.p(id, Atomic.the("y"))*/, (dh) -> {
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
        reward(()->{
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
        NAgentX.runRT((n) -> {

            Pacman a = new Pacman(n);
            return a;

        }, 1000f / PacmanGame.periodMS);
    }

}
