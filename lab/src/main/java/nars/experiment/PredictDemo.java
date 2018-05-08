package nars.experiment;

import jcog.exe.Loop;
import jcog.learn.LivePredictor;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.concept.Concept;
import nars.concept.util.DefaultConceptState;
import nars.gui.Vis;
import nars.time.RealTime;
import nars.util.signal.BeliefPredict;
import org.jetbrains.annotations.Nullable;
import spacegraph.SpaceGraph;
import spacegraph.space2d.container.grid.Gridding;

import java.util.List;

public class PredictDemo {
    public static void main(String[] args) {
        float fps = 20f;

        NAR n = new NARS.DefaultNAR(8, true)
                .time(new RealTime.CS(true).durFPS(fps))
                .get();

        DefaultConceptState s = (DefaultConceptState) n.conceptBuilder.awake();
//        s.beliefsMaxTemp = 256;
//        s.beliefsMinTemp = 256;

        @Nullable Concept X = n.conceptualize($.the("x"));
        @Nullable Concept T = n.conceptualize($.the("t"));

        Loop.of(()->{
            long rt = n.time();
            float t = ((float)rt)/n.dur();
            float xf = (float) (0.5f + 0.5f * Math.sin(t/16f));
            n.believe(X.term(), rt, xf, 0.9f);

            n.believe(T.term(), rt, Math.sin(t/4f) > 0 ? 1f : 0f, 0.9f);
        }).runFPS(5f);

        //n.log();

        new BeliefPredict(List.of(X,T), 32, 32, List.of(X),
                new LivePredictor.MLPPredictor(0.02f),
                //new LivePredictor.LSTMPredictor(0.05f, 2),
                n
        );

        SpaceGraph.window(new Gridding(
            Vis.beliefCharts(64, List.of(T), n),
            Vis.beliefCharts(64, List.of(X), n),
            //Vis.beliefCharts(128, List.of(x), n),
            Vis.beliefCharts(256, List.of(X), n),
            Vis.beliefCharts(2048, List.of(X), n)

        ), 800, 800);

        n.startFPS(fps);
    }
}
