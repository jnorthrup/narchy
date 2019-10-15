package nars.experiment;

import nars.GameX;
import nars.gui.sensor.VectorSensorChart;

import static nars.experiment.Tetris.*;
import static spacegraph.SpaceGraph.window;

public
class Chimera{

    public static void main(String... args) {  GameX.runRT(n -> {

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
