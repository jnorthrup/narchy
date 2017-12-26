package nars.gui;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.math.FloatParam;
import nars.NAR;
import nars.NAgent;
import nars.control.DurService;
import nars.control.MetaGoal;
import spacegraph.Surface;
import spacegraph.layout.Grid;
import spacegraph.layout.VSplit;
import spacegraph.render.Draw;
import spacegraph.widget.button.CheckBox;
import spacegraph.widget.meter.BitmapMatrixView;
import spacegraph.widget.slider.BaseSlider;
import spacegraph.widget.slider.FloatSlider;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static spacegraph.layout.Grid.col;
import static spacegraph.layout.Grid.grid;
import static spacegraph.layout.Grid.row;

public class ExecCharts {

    public static Surface metaGoalPlot(NAR nar) {


        int s = nar.causes.size();

        FloatParam gain = new FloatParam(20f, 0f, 20f);

        BitmapMatrixView bmp = new BitmapMatrixView((i) ->
                Util.tanhFast(
                        gain.floatValue() * nar.causes.get(i).value()
                ),
                //Util.tanhFast(nar.causes.get(i).value()),
                s, Math.max(1, (int) Math.ceil(Math.sqrt(s))),
                Draw::colorBipolar) {

            final DurService on;

            {
                on = DurService.on(nar, this::update);
            }

            @Override
            public void stop() {
                super.stop();
                on.off();
            }
        };

        return new VSplit(bmp, Vis.reflect(gain), 0.1f);
    }

    public static Surface metaGoalControls(NAR n) {
        CheckBox auto = new CheckBox("Auto");
        //auto.set(true);

        Grid g = grid(
                Stream.concat(
                        Stream.of(auto),
                        IntStream.range(0, n.want.length).mapToObj(
                                w -> new FloatSlider(n.want[w], -2f, +2f) {

                                    @Override
                                    protected void paintIt(GL2 gl) {
                                        if (auto.on()) {
                                            value(n.want[w]);
                                        }
                                        super.paintIt(gl);
                                    }
                                }
                                        .text(MetaGoal.values()[w].name())
                                        .type(BaseSlider.Knob)
                                        .on((s, v) -> {
                                            if (!auto.on())
                                                n.want[w] = v;
                                        })
                        )).toArray(Surface[]::new));

        return g;
    }

    public static VSplit exePanel(NAR n) {
        return new VSplit(Vis.reflect(n.loop),
                col(
                        //metaGoalChart(a),
                        row(
                                metaGoalPlot(n),
                                metaGoalControls(n)
                        )),
                0.9f);
    }
}
