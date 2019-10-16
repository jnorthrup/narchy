package nars.experiment;

import nars.GameX;
import nars.gui.sensor.VectorSensorChart;

import static nars.experiment.Tetris.*;
import static spacegraph.SpaceGraph.window;

public
class Chimera {

    public static void main(String... args) {
        GameX.runRT(n -> {
            //potential boredom without the down button.
            System.setProperty("tetris.fall.rate", "5");
            System.setProperty("tetris.can.fall", "true");
            //survival only.
            System.setProperty("tetris.use.density", "false");
            //dot
            System.setProperty("tetris.easy", "true");
//            reduce con io
            System.setProperty("avg.err", "false");

            var t = new Tetris(n, tetris_width, tetris_height);
            var g = new Gradius(n);


            var x = new NARio(n);
            n.add(x);
            n.add(t);

            window(new VectorSensorChart(t.gridVision, t).withControls(), 400, 800);
            n.add(g);
            return g;
        }, FPS * thinkPerFrame);

    }
}
