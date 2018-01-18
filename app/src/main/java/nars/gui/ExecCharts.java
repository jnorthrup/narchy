package nars.gui;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.list.FasterList;
import jcog.math.FloatParam;
import nars.$;
import nars.NAR;
import nars.NAgent;
import nars.control.Cause;
import nars.control.DurService;
import nars.control.MetaGoal;
import nars.control.Traffic;
import nars.exe.Causable;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import spacegraph.Surface;
import spacegraph.layout.Grid;
import spacegraph.layout.VSplit;
import spacegraph.render.Draw;
import spacegraph.widget.button.CheckBox;
import spacegraph.widget.meta.AutoSurface;
import spacegraph.widget.meter.BitmapMatrixView;
import spacegraph.widget.meter.TreeChart;
import spacegraph.widget.slider.BaseSlider;
import spacegraph.widget.slider.FloatSlider;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static spacegraph.layout.Grid.*;

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

        return new VSplit(bmp, new AutoSurface<>(gain), 0.1f);
    }

    public static Surface metaGoalControls(NAR n) {
        CheckBox auto = new CheckBox("Auto");
        //auto.set(true);

        Grid g = grid(
//                Stream.concat(
//                        Stream.of(auto),
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
                        ).toArray(Surface[]::new));

        return g;
    }

    public static Surface exePanel(NAR n) {
        return new VSplit(new AutoSurface<>(n.loop),
                col(
                        //metaGoalChart(a),
                        row(
                                metaGoalPlot(n),
                                metaGoalControls(n)
                        )),
                0.9f);
    }

    public static Surface causePanel(NAR nar) {

        TreeChart<Causable> x = new TreeChart();

        return new DurSurface(x, nar) {

            Function<Causable, TreeChart.ItemVis<Causable>> updater = TreeChart.cached();

            List<Causable> s = $.newArrayList();

            @Override protected void update() {
                s.clear();
                nar.services.stream().filter(Causable.class::isInstance)
                        .map(x -> (Causable)x)
                        .collect(Collectors.toCollection(()->s));

                x.update(s, (x, y)-> {
                    float v = x.value();
                    y.updateMomentum((float) (x.can.write().time.get()/1E6),
                            0.1f, v < 0 ? -v : 0, 0,v > 0 ? +v : 0);

                }, updater);

            }
        };

    }

    private static Surface metaGoalChart(NAgent a) {

        return new TreeChart<Cause>() {
            final DurService on;

            final FasterList<ItemVis<Cause>> cache = new FasterList();

            final Function<Cause, TreeChart.ItemVis<Cause>> builder = ((i) -> {
                short id = i.id;
                ItemVis<Cause> item;
                if (cache.capacity() - 1 < id)
                    cache.ensureCapacity(id + 16);
                else {
                    item = cache.get(id);
                    if (item != null)
                        return item;
                }


                String str = i.toString();
                if (str.startsWith("class nars."))
                    str = str.substring("class nars.".length()); //skip default toString

                if (str.startsWith("class "))
                    str = str.substring(5); //skip default toString

                item = new CauseVis(i, str);
                cache.set(id, item);
                return item;
            });

            {

                on = a.onFrame(() -> {
                    update(a.nar.causes, (c, i) -> {
                        float v = c.value();
                        float r, g, b;
                        if (v < 0) {
                            r = 0.75f * Math.max(0.1f, Math.min(1f, -v));
                            g = 0;
                        } else {
                            g = 0.75f * Math.max(0.1f, Math.min(1f, +v));
                            r = 0;
                        }

                        float t = Util.sum(((FloatFunction<Traffic>) (p -> Math.abs(p.current + p.prev))), c.goal) / 2f;

                        b = Math.max(r, g) / 2f * Util.unitize(t);

                        i.update(v, r, g, b);
//                        i.updateMomentum(
//                                //0.01f + Util.sqr(Util.tanhFast(v)+1),
//                                //Math.signum(v) *(1+Math.abs(v))*(t),
//                                //Math.signum(v) * t,
//                                v,
//                                0.25f,
//                                r, g, b);

                    }, builder);
                });
            }

            @Override
            public void stop() {
                super.stop();
                on.off();
            }
        };
    }

    private static class CauseVis extends TreeChart.ItemVis<Cause> {
        public CauseVis(Cause i, String str) {
            super(i, StringUtils.abbreviate(str, 26));
        }

        @Override
        public float requestedArea() {
            return 0.01f + super.requestedArea();
        }
    }


}
