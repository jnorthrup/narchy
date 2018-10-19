package nars.gui;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.math.FloatRange;
import jcog.sort.TopN;
import jcog.tree.rtree.rect.RectFloat;
import nars.NAR;
import nars.control.Cause;
import nars.control.MetaGoal;
import nars.exe.NARLoop;
import nars.exe.UniExec;
import nars.time.clock.RealTime;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.*;
import spacegraph.space2d.widget.Widget;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.meta.LoopPanel;
import spacegraph.space2d.widget.meter.BitmapMatrixView;
import spacegraph.space2d.widget.meter.Plot2D;
import spacegraph.space2d.widget.slider.FloatSlider;
import spacegraph.space2d.widget.slider.SliderModel;
import spacegraph.space2d.widget.text.BitmapLabel;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.video.Draw;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static spacegraph.space2d.container.Gridding.grid;
import static spacegraph.space2d.container.Gridding.row;

public class ExeCharts {

    public static Surface metaGoalPlot(NAR nar) {

        int s = nar.causes.size();

        FloatRange gain = new FloatRange(1f, 0f, 5f);

        BitmapMatrixView bmp = new BitmapMatrixView(i ->
                Util.tanhFast(
                    gain.floatValue() * nar.causes.get(i).value()
                ),
                s, Draw::colorBipolar);

        return Splitting.column(DurSurface.get(bmp, nar), new FloatSlider("Display Gain", gain), 0.05f);
    }

    public static Surface metaGoalControls(NAR n) {
        CheckBox auto = new CheckBox("Auto");
        auto.set(false);

        float min = -1f;
        float max = +1f;

        float[] want = n.emotion.want;
        Gridding g = grid(


                IntStream.range(0, want.length).mapToObj(
                        w -> {
                            return new FloatSlider(want[w], min, max) {

                                @Override
                                protected void paintWidget(GL2 gl, RectFloat bounds) {
                                    if (auto.get()) {
                                        set(want[w]);
                                    }

                                }
                            }
                                    .text(MetaGoal.values()[w].name())
                                    .type(SliderModel.KnobHoriz)
                                    .on((s, v) -> {
                                        if (!auto.get())
                                            want[w] = v;
                                    });
                        }
                ).toArray(Surface[]::new));

        return g;
    }

    public static Surface exePanel(NAR n) {
        int plotHistory = 100;
        Plot2D exeQueue = new Plot2D(plotHistory, Plot2D.Line)
                .add("queueSize", ((UniExec) n.exe)::queueSize);
        Plot2D busy = new Plot2D(plotHistory, Plot2D.Line)
                .add("Busy", n.emotion.busyVol::getSum);
        return grid(
                DurSurface.get(exeQueue, n, exeQueue::update),
                DurSurface.get(busy, n, busy::update)
        );
    }

    public static Surface valuePanel(NAR n) {
        return row(
                metaGoalPlot(n),
                metaGoalControls(n)
        );
    }

    static class CausableWidget extends Widget {
        private final UniExec.InstrumentedCausable c;
        private final VectorLabel label;

        CausableWidget(UniExec.InstrumentedCausable c) {
            this.c = c;
            label = new VectorLabel(c.c.can.id);
            set(label);

        }

    }

    public static Surface causeProfiler(NAR nar) {
        UniExec.InstrumentedCausable[] cc = ((UniExec) nar.exe).can.valueArray();
        int history = 256;
        int n = cc.length;
        Plot2D pp = new Plot2D(history,
                Plot2D.LineLanes
                //Plot2D.Line
        );
        //Plot2D[] pp = new Plot2D[n];
        for (int i = 0, ccLength = cc.length; i < ccLength; i++) {
            UniExec.InstrumentedCausable c = cc[i];
            String label = c.c.id.toString();
            //pp[i] = new Plot2D(history, Plot2D.Line).add(label,
            pp.add(label,

                        ()->
                                //c.accumTimeNS.get()/1_000_000.0 //ms
                                (c.iterations.getMean() * c.iterTimeNS.getMean())/1_000_000.0 //ms
                                //c.valuePerSecondNormalized
                                //c.valueNext
                                //c.iterations.getN()
                                //c...

                        //,0,1
                );
        }
        return DurSurface.get(new Gridding(pp), nar, ()->{
            //for (Plot2D p : pp) {
                pp.update();
            //}
        });
    }

    public static Surface focusPanel(NAR nar) {

//        ForceDirected2D<UniExec.InstrumentedCausable> fd = new ForceDirected2D<>();
//        fd.repelSpeed.set(0.5f);

        Graph2D<UniExec.InstrumentedCausable> s = new Graph2D<UniExec.InstrumentedCausable>()
                .render((node, g) -> {
                    UniExec.InstrumentedCausable c = node.id;

                    final float epsilon = 0.01f;
                    float p = Math.max(c.priElse(epsilon), epsilon);
                    float v = c.c.value();
                    node.color(p, v, 0.25f);


                    //Graph2D G = node.parent(Graph2D.class);
//                float parentRadius = node.parent(Graph2D.class).radius(); //TODO cache ref
//                float r = (float) ((parentRadius * 0.5f) * (sqrt(p) + 0.1f));

                    node.pri = Math.max(epsilon, p);
                })
                //.layout(fd)
                .update(new TreeMap2D<>())
                .build((node) -> {
                    node.set(new Scale(new CausableWidget(node.id), 0.9f));
                });


        return DurSurface.get(
                new Splitting(s, new Gridding(new PushButton("Stats").click(()->causeSummary(nar, 10)), s.configWidget()), 0.1f),
                nar, () -> {
                    s.set(((UniExec) nar.exe).can::valueIterator);
                });
    }

    private static void causeSummary(NAR nar, int top) {
        TopN[] tops = Stream.of(MetaGoal.values()).map(v -> new TopN<>(new Cause[top], (c) ->
                (float)c.credit[v.ordinal()].total)).toArray(TopN[]::new);
        nar.causes.forEach((Cause c) -> {
            for (TopN t : tops)
                t.add(c);
        });

        for (int i = 0, topsLength = tops.length; i < topsLength; i++) {
            TopN t = tops[i];
            System.out.println(MetaGoal.values()[i]);
            t.forEach(tt->{
                System.out.println("\t" + tt);
            });
        }

    }


    /**
     * adds duration control
     */
    static class NARLoopPanel extends LoopPanel {

        private final NAR nar;
        final FloatRange dur = new FloatRange(1f, 0f, 8f);
        private final RealTime time;

        public NARLoopPanel(NARLoop loop) {
            super(loop);
            this.nar = loop.nar();
            if (nar.time instanceof RealTime) {
                time = ((RealTime) nar.time);
                add(
                        new FloatSlider("Dur*", dur),
                        new FloatSlider("Throttle", loop.throttle)
                );
                dur.set(time.durRatio(loop));
            } else {

                time = null;
            }
        }

        @Override
        public void update() {

            super.update();
            if (loop.isRunning()) {
                if (time != null) {
                    time.durRatio(loop, dur.floatValue());
                }
            }

        }
    }

    public static Surface runPanel(NAR n) {
        BitmapLabel nameLabel;
        LoopPanel control = new NARLoopPanel(n.loop);
        Surface p = new Splitting(
                nameLabel = new BitmapLabel(n.self().toString()),
                control,
                false, 0.25f
        );
        return DurSurface.get(p, n, control::update);
    }


}
