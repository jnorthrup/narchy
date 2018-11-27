package nars.experiment;

import jcog.Util;
import jcog.signal.wave2d.MonoBufImgBitmap2D;
import jcog.signal.wave2d.ScaledBitmap2D;
import nars.$;
import nars.NAR;
import nars.NAgentX;
import nars.experiment.pacman.PacmanGame;
import nars.sensor.Bitmap2DSensor;
import nars.term.atom.Atomic;
import nars.video.SwingBitmap2D;
import nars.video.VectorSensorView;
import spacegraph.space2d.container.grid.Gridding;

import static spacegraph.SpaceGraph.window;


public class Pacman extends NAgentX {

    private final PacmanGame g;

    public Pacman(NAR nar) {
        super("G", nar);

        this.g = new PacmanGame();



        Gridding gg = new Gridding();
        ScaledBitmap2D camScale = new ScaledBitmap2D(new SwingBitmap2D(g.view), 28, 28);
        for (MonoBufImgBitmap2D.ColorMode cm : new MonoBufImgBitmap2D.ColorMode[]{
                MonoBufImgBitmap2D.ColorMode.R,
                MonoBufImgBitmap2D.ColorMode.G,
                MonoBufImgBitmap2D.ColorMode.B
        }) {
            Bitmap2DSensor c = senseCamera("(G,c" + cm.name() + ")",
                    camScale.filter(cm)
            );

            gg.add(new VectorSensorView(c,this)/*.withControls()*/);
            c.resolution(0.1f);
        }
        window(gg, 900, 300);

        actionTriState($.p(id, Atomic.the("x")), (dh) -> {
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

        actionTriState($.p(id, Atomic.the("y")), (dh) -> {
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


        reward(()->{
            g.update();

            int nextScore = g.score;

            float r = (nextScore - lastScore);


            lastScore = nextScore;


            return 2f * (Util.sigmoid(r) - 0.5f);
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
