package nars.experiment;

import jcog.exe.Loop;
import jcog.learn.LivePredictor;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.concept.Concept;
import nars.concept.signal.Signal;
import nars.gui.NARui;
import nars.time.clock.RealTime;
import nars.truth.Truth;
import nars.util.signal.BeliefPredict;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;
import org.jetbrains.annotations.Nullable;
import spacegraph.SpaceGraph;
import spacegraph.space2d.container.grid.Gridding;

import java.util.List;

import static nars.Op.BELIEF;

public class PredictDemo {
    public static void main(String[] args) {
        float fps = 50f;

        NAR n = new NARS.DefaultNAR(8, true)
                .time(new RealTime.CS(true).durFPS(fps))
                .get();





        float[] xf = new float[1];
        @Nullable Concept X = new Signal($.the("x"), ()->{
            return xf[0];
        }, n);
        n.on(X);
        

        final FloatFloatToObjectFunction<Truth> truther =
                (prev, next) -> $.t(next, n.confDefault(BELIEF));

        Loop.of(() -> {
            long rt = n.time();
            float t = ((float) rt) / n.dur();
            xf[0] = (float) (0.5f + 0.5f * Math.sin(t / 6f));
            

            ((Signal) X).update(truther, n);

            
        }).runFPS(15f);

        

        new BeliefPredict(List.of(X), 8, n.dur()*8, 8,
                
                new LivePredictor.LSTMPredictor(0.15f, 1),
                n
        );

        SpaceGraph.window(new Gridding(Gridding.VERTICAL,

                NARui.beliefCharts(64, List.of(X), n),
                
                NARui.beliefCharts(256, List.of(X), n),
                NARui.beliefCharts(2048, List.of(X), n)

        ), 800, 800);

        n.startFPS(fps);
    }
}
