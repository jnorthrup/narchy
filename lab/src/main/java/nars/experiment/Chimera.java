package nars.experiment;

import nars.GameX;
import nars.gui.NARui;
import nars.gui.sensor.VectorSensorChart;
import spacegraph.space2d.container.grid.Gridding;

import static nars.$.$$;
import static nars.experiment.ArkaNAR.cam;
import static nars.experiment.ArkaNAR.numeric;
import static nars.experiment.Tetris.*;
import static spacegraph.SpaceGraph.window;

public
class Chimera {

    public static void main(String... args) {   //potential boredom without the down button.
        System.setProperty("tetris.fall.rate", "5");
        System.setProperty("tetris.can.fall", "true");
        //survival only.
        System.setProperty("tetris.use.density", "false");
        //dot
        System.setProperty("tetris.easy", "true");
//            reduce con io
        System.setProperty("avg.err", "false");
        GameX.runRT(n -> {


            var t = new Tetris(n, tetris_width, tetris_height);
            var g = new Gradius(n);

            ArkaNAR a = new ArkaNAR($$("(noid,a)"), n, cam, numeric);
            a.ballSpeed.set( 0.7f * a.ballSpeed.floatValue() );

            ArkaNAR b = new ArkaNAR($$("(noid,b)"), n, cam, numeric);
            b.ballSpeed.set( 0.33f * a.ballSpeed.floatValue() );
            n.add(a);
            n.add(b);
                window(new Gridding(
                        new Gridding(NARui.game(a), new VectorSensorChart(a.cc, a).withControls()),
                        new Gridding( NARui.game(b), new VectorSensorChart(b.cc, b).withControls())), 800, 800);


            var x = new NARio(n);
            n.add(x);
            n.add(t);

            window(new VectorSensorChart(t.gridVision, t).withControls(), 400, 800);
            n.add(g);
            return g;
        }, FPS * thinkPerFrame);

    }
}
