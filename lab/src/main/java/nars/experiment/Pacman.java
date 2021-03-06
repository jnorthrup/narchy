package nars.experiment;

import jcog.func.IntIntToObjectFunction;
import jcog.math.FloatSupplier;
import jcog.signal.wave2d.Bitmap2D;
import jcog.signal.wave2d.BrightnessNormalize;
import jcog.signal.wave2d.ScaledBitmap2D;
import nars.$;
import nars.GameX;
import nars.NAR;
import nars.experiment.pacman.PacmanGame;
import nars.experiment.pacman.maze.Maze;
import nars.game.Game;
import nars.game.GameTime;
import nars.sensor.Bitmap2DSensor;
import nars.term.Term;
import nars.video.SwingBitmap2D;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;

import java.util.function.Function;

import static nars.$.*;


public class Pacman extends GameX {

    private final PacmanGame g;

    static float fps = 20.0F;

    public Pacman(NAR nar) {
        super(INSTANCE.$$("Pac"),
                //GameTime.durs(0.5f),
                GameTime.fps(fps),nar
		);

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
            Bitmap2DSensor c = senseCamera(new IntIntToObjectFunction<Term>() {
                @Override
                public Term apply(int x, int y) {
                    return $.INSTANCE.inh(id, $.INSTANCE.p(x, y));
                }
            }, camScale/*, 0*/);
//            VectorSensorView v = new VectorSensorView(c, this);
//            gg.add(v/*.withControls()*/);
            c.resolution(0.02f);
        }
//        SpaceGraph.window(gg, 300, 300);

        actionPushButtonMutex($.INSTANCE.inh(id,"left"), $.INSTANCE.inh(id,"right"), new BooleanPredicate() {
            @Override
            public boolean accept(boolean b) {
                if (b) {
                    g.keys[1] = true;
                    g.keys[0] = false;
                    return !g.player.walled(Maze.Direction.left);
                } else return false;
            }
        }, new BooleanPredicate() {
            @Override
            public boolean accept(boolean b) {
                if (b) {
                    g.keys[0] = true;
                    g.keys[1] = false;
                    return !g.player.walled(Maze.Direction.right);
                } else return false;
            }
        });
        actionPushButtonMutex($.INSTANCE.inh(id,"up"), $.INSTANCE.inh(id,"down"), new BooleanPredicate() {
            @Override
            public boolean accept(boolean b) {
                if (b) {
                    g.keys[2] = true;
                    g.keys[3] = false;
                    return !g.player.walled(Maze.Direction.up);
                } else return false;
            }
        }, new BooleanPredicate() {
            @Override
            public boolean accept(boolean b) {
                if (b) {
                    g.keys[3] = true;
                    g.keys[2] = false;
                    return !g.player.walled(Maze.Direction.down);
                } else return false;
            }
        });
//        actionTriState($.p(id,$.p($.the("x"), $.varQuery(1))), (dh) -> {
//            switch (dh) {
//                case +1:
//                    g.keys[1] = true;
//                    g.keys[0] = false;
//                    break;
//                case -1:
//                    g.keys[0] = true;
//                    g.keys[1] = false;
//                    break;
//                case 0:
//                    g.keys[0] = g.keys[1] = false;
//                    break;
//            }
//        });
//
//        actionTriState($.p(id,$.p($.the("y"), $.varQuery(1))), (dh) -> {
//            switch (dh) {
//                case +1:
//                    g.keys[2] = true;
//                    g.keys[3] = false;
//                    break;
//                case -1:
//                    g.keys[3] = true;
//                    g.keys[2] = false;
//                    break;
//                case 0:
//                    g.keys[2] = g.keys[3] = false;
//                    break;
//            }
//        });


        //TODO multiple reward signals: eat, alive, dist->ghost (cheat)
        reward("score", new FloatSupplier() {
            @Override
            public float asFloat() {
                g.update();

                int nextScore = g.score;

                float r = (float) (nextScore - lastScore);
//            if(r == 0)
//                return Float.NaN;


                lastScore = nextScore;
                if (r > (float) 0) return (float) +1;
                else if (r < (float) 0) return (float) 0;
                else
                    return 0.5f;
                //return (Util.tanhFast(r) + 1)/2f;
            }
        });
    }


    int lastScore;


    public static void main(String[] args) {
        GameX.Companion.initFn(fps* 2.0F, new Function<NAR, Game>() {
            @Override
            public Game apply(NAR n) {

                Pacman a = new Pacman(n);
                n.add(a);
                return a;

            }
        });
    }

}
