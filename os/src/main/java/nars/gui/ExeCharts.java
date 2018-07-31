package nars.gui;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.math.FloatRange;
import jcog.tree.rtree.rect.RectFloat2D;
import nars.NAR;
import nars.NARLoop;
import nars.control.DurService;
import nars.control.MetaGoal;
import nars.exe.UniExec;
import nars.time.clock.RealTime;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.ForceDirected2D;
import spacegraph.space2d.container.Scale;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.TreeMap2D;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.Graph2D;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.meta.LoopPanel;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space2d.widget.meter.BitmapMatrixView;
import spacegraph.space2d.widget.slider.FloatSlider;
import spacegraph.space2d.widget.slider.SliderModel;
import spacegraph.space2d.widget.text.Label;
import spacegraph.space2d.widget.windo.Widget;
import spacegraph.video.Draw;

import java.util.stream.IntStream;

import static java.lang.Math.sqrt;
import static spacegraph.space2d.container.grid.Gridding.*;

public class ExeCharts {

    public static Surface metaGoalPlot(NAR nar) {


        int s = nar.causes.size();

        FloatRange gain = new FloatRange(20f, 0f, 20f);

        BitmapMatrixView bmp = new BitmapMatrixView((i) ->
                Util.tanhFast(
                        gain.floatValue() * nar.causes.get(i).value()
                ),
                
                s, Math.max(1, (int) Math.ceil(sqrt(s))),
                Draw::colorBipolar) {

            DurService on;

            {
                on = DurService.on(nar, this::update);
            }

            @Override
            public boolean stop() {
                if (super.stop()) {
                    on.off();
                    on = null;
                    return true;
                }
                return false;
            }

        };

        return new Splitting(bmp, new ObjectSurface<>(gain), 0.1f);
    }

    public static Surface metaGoalControls(NAR n) {
        CheckBox auto = new CheckBox("Auto");
        auto.set(false);

        float min = -4f;
        float max = +4f;

        float[] want = n.emotion.want;
        Gridding g = grid(


                IntStream.range(0, want.length).mapToObj(
                        w -> {
                            return new FloatSlider(want[w], min, max) {

                                @Override
                                protected void paintWidget(GL2 gl, RectFloat2D bounds) {
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
        return new Splitting(new ObjectSurface<>(n.loop),
                col(
                        
                        row(
                                metaGoalPlot(n),
                                metaGoalControls(n)
                        )),
                0.9f);
    }

    static class CausableWidget extends Widget {
        private final UniExec.InstrumentedCausable c;
        private final Label label;

        CausableWidget(UniExec.InstrumentedCausable c) {
            this.c = c;
            label =new Label(c.c.can.id);
            add(label);

        }

    }

    public static Surface focusPanel(NAR nar) {

        ForceDirected2D<UniExec.InstrumentedCausable> fd = new ForceDirected2D<>();
        fd.repelSpeed.set(0.5f);

        Graph2D<UniExec.InstrumentedCausable> s = new Graph2D<UniExec.InstrumentedCausable>()
            .layer((node, g)->{
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
            .layout(new TreeMap2D<>())
            .nodeBuilder((node)->{
                node.add(new Scale(new CausableWidget(node.id), 0.9f));
            });


        return DurSurface.get(
            new Splitting(s, s.configWidget(), 0.1f),
            nar, () -> {
                s.set(((UniExec) nar.exe).can::valueIterator);
            });
    }


    /** adds duration control */
    static class NARLoopPanel extends LoopPanel {

        private final NAR nar;
        final FloatRange dur = new FloatRange(1f, 0f, 8f);
        private final RealTime time;

        public NARLoopPanel(NARLoop loop) {
            super(loop);
            this.nar = loop.nar;
            if (nar.time instanceof RealTime) {
                time = ((RealTime)nar.time);
                add(
                        new FloatSlider("Dur*", dur)
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
        Label nameLabel;
        LoopPanel control = new NARLoopPanel(n.loop);
        Surface p = new Gridding(
                nameLabel = new Label(n.self().toString()),
                control
        );
        return DurSurface.get(p, n, control::update);
    }



}
