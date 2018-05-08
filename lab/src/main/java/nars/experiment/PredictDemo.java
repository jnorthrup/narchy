package nars.experiment;

import jcog.exe.Loop;
import jcog.learn.LivePredictor;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.concept.Concept;
import nars.concept.scalar.Scalar;
import nars.gui.Vis;
import nars.time.RealTime;
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

//        DefaultConceptState s = (DefaultConceptState) n.conceptBuilder.awake();
//        s.beliefsMaxTemp = 256;
//        s.beliefsMinTemp = 256;

        float[] xf = new float[1];
        @Nullable Concept X = new Scalar($.the("x"), ()->{
            return xf[0];
        }, n);
        n.on(X);
        //@Nullable Concept T = n.conceptualize($.the("t"));

        final FloatFloatToObjectFunction<Truth> truther =
                (prev, next) -> $.t(next, n.confDefault(BELIEF));

        Loop.of(() -> {
            long rt = n.time();
            float t = ((float) rt) / n.dur();
            xf[0] = (float) (0.5f + 0.5f * Math.sin(t / 6f));
            //n.believe(X.term(), rt, xf, 0.9f);

            ((Scalar) X).update(truther, n);

            //n.believe(T.term(), rt, Math.sin(t / 4f) > 0 ? 1f : 0f, 0.9f);
        }).runFPS(15f);

        //n.log();

        new BeliefPredict(List.of(X), 16, n.dur()*6, 6,
                //new LivePredictor.MLPPredictor(0.02f),
                new LivePredictor.LSTMPredictor(0.1f, 2),
                n
        );

        SpaceGraph.window(new Gridding(Gridding.VERTICAL,

                Vis.beliefCharts(64, List.of(X), n),
                //Vis.beliefCharts(128, List.of(x), n),
                Vis.beliefCharts(256, List.of(X), n),
                Vis.beliefCharts(2048, List.of(X), n)

        ), 800, 800);

        n.startFPS(fps);
    }
}
