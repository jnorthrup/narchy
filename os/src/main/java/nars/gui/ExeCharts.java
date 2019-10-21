package nars.gui;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.data.list.MetalConcurrentQueue;
import jcog.event.Off;
import jcog.math.FloatRange;
import jcog.math.IntRange;
import jcog.tree.rtree.rect.RectFloat;
import nars.NAR;
import nars.control.Cause;
import nars.control.MetaGoal;
import nars.exe.Exec;
import nars.exe.NARLoop;
import nars.exe.impl.ThreadedExec;
import nars.time.clock.RealTime;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectFloatProcedure;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.Widget;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.meta.LoopPanel;
import spacegraph.space2d.widget.meter.BitmapMatrixView;
import spacegraph.space2d.widget.meter.Plot2D;
import spacegraph.space2d.widget.slider.FloatSlider;
import spacegraph.space2d.widget.slider.IntSlider;
import spacegraph.space2d.widget.slider.SliderModel;
import spacegraph.space2d.widget.text.AbstractLabel;
import spacegraph.space2d.widget.text.BitmapLabel;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.video.Draw;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import static jcog.Util.lerpSafe;
import static spacegraph.space2d.container.grid.Gridding.grid;

public class ExeCharts {
    private static Surface metaGoalPlot2(NAR nar) {

        int s = nar.control.why.size();

        List<WhySurface> controls = new FasterList(s);
        for (Cause w : nar.control.why) {
			controls.add(new WhySurface(w));
        }

        Gridding g = new Gridding(controls);
//        BitmapMatrixView bmp = new BitmapMatrixView(i ->
//            //Util.tanhFast(
//            gain.floatValue() * nar.control.why.get(i).pri()
//            //)
//            , s, Draw::colorBipolar);

        return DurSurface.get(g, nar, new Runnable() {
            @Override
            public void run() {
                for (WhySurface control : controls) {
                    control.update();
                }
            }
        });
    }

    static class WhySurface extends Widget {
    	final Cause w;
    	public WhySurface(Cause w) {
    		this.w = w;
    		//Draw.colorHash(w.)
            VectorLabel l = new VectorLabel(w.toString());
            set(l);
            //set(new Bordering(l)
//                .south(new FloatSlider(0, -2, +2).on(x->{
//                float p = (float) Math.pow(x, 10);
//                w.setPri(p);
//            })));
		}
		public void update() {
            float p = w.pri();
            color.hsl( lerpSafe(1.0F -p, 0.1f, 0.8f), 0.75f,
                lerpSafe(p, 0.1f, 0.5f), 1f);

//            float v = w.value();
//            color.y = v!=v ? 0 : Util.unitizeSafe(v);
		}
	}

	private static Surface metaGoalPlot(NAR nar) {

        int s = nar.control.why.size();

        FloatRange gain = new FloatRange(1f, 0f, 100f);

        BitmapMatrixView bmp = new BitmapMatrixView(new IntToFloatFunction() {
            @Override
            public float valueOf(int i) {
                return gain.floatValue() * nar.control.why.get(i).pri();
            }
        }
                //)
                , s, Draw::colorBipolar);

        return Splitting.column(DurSurface.get(bmp, nar), 0.05f, new FloatSlider(gain, "Display Gain"));
    }

    private static Surface metaGoalControls(NAR n) {
        CheckBox auto = new CheckBox("Auto");
        auto.on(true);

        float min = -1f;
        float max = +1f;

        float[] want = n.emotion.want;
        //return DurSurface.get(
         return grid( IntStream.range(0, want.length).mapToObj(
                 new IntFunction<Surface>() {
                     @Override
                     public Surface apply(int w) {
                         return new FloatSlider(want[w], min, max) {

                             @Override
                             protected void paintWidget(RectFloat bounds, GL2 gl) {
                                 if (auto.on()) {
                                     set(want[w]);
                                 }

                             }
                         }
                                 .text(MetaGoal.values()[w].name())
                                 .type(SliderModel.KnobHoriz)
                                 .on(new ObjectFloatProcedure<SliderModel>() {
                                     @Override
                                     public void value(SliderModel s, float v) {
                                         if (!auto.on())
                                             want[w] = v;
                                     }
                                 });
                     }
                 }
                ));
    }

    static Surface exePanel(NAR n) {

        int plotHistory = 500;
        MetalConcurrentQueue busyBuffer = new MetalConcurrentQueue(plotHistory);
        MetalConcurrentQueue queueSize = new MetalConcurrentQueue(plotHistory);

        Plot2D exeQueue = new Plot2D(plotHistory, Plot2D.BarLanes)
                .add("queueSize", queueSize);
        Plot2D busy = new Plot2D(plotHistory, Plot2D.BarLanes)
                .add("Busy", busyBuffer);


        Gridding g = grid(exeQueue, busy);
        DurSurface d = DurSurface.get(g, n, new Consumer<>() {

            final Off c = n.onCycle(new Consumer<NAR>() {
                @Override
                public void accept(NAR nn) {
                    busyBuffer.offer(nn.emotion.busyVol.asFloat());
                    Exec nexe = n.exe;
                    if (nexe instanceof ThreadedExec)
                        queueSize.offer((float) ((ThreadedExec) nexe).queueSize());
                }
            });

            @Override
            public void accept(NAR nn) {
                if (g.parent!=null) {
                    exeQueue.commit();
                    busy.commit();
                } else{
                    c.close();
                }
            }
        });
        return d;

    }

    static Surface valuePanel(NAR n) {
        return new Splitting(
                //metaGoalPlot(n),
                metaGoalPlot2(n),
                0.9f, false,
                metaGoalControls(n)
        );
    }

    static class CausableWidget<X> extends Widget {

        CausableWidget(X c, String s) {
            set(new VectorLabel(s));
        }
    }

//    enum CauseProfileMode implements FloatFunction<How> {
//        Pri() {
//            @Override
//            public float floatValueOf(How w) {
//                return w.pri();
//            }
//        },
//        Value() {
//            @Override
//            public float floatValueOf(How w) {
//                return w.value;
//            }
//        },
//        ValueRateNormalized() {
//            @Override
//            public float floatValueOf(How w) {
//                return w.valueRateNormalized;
//            }
//        },
////        Time() {
////            @Override
////            public float floatValueOf(TimedLink w) {
////                return Math.max(0,w.time.get());
////            }
////        }
//        ;
//        /* TODO
//                                //c.accumTimeNS.get()/1_000_000.0 //ms
//                                //(c.iterations.getMean() * c.iterTimeNS.getMean())/1_000_000.0 //ms
//                                //c.valuePerSecondNormalized
//                                //c.valueNext
//                                //c.iterations.getN()
//                                //c...
//
//                        //,0,1
//         */
//    }
//
//    static Surface causeProfiler(AntistaticBag<How> cc, NAR nar) {
//
//        int history = 128;
//        Plot2D pp = new Plot2D(history,
//                //Plot2D.BarLanes
//                Plot2D.LineLanes
//                //Plot2D.Line
//        );
//
//        final MutableEnum<CauseProfileMode> mode = new MutableEnum<>(CauseProfileMode.Pri);
//
//        for (How c : cc) {
//            String label = c.toString();
//            //pp[i] = new Plot2D(history, Plot2D.Line).addAt(label,
//            pp.add(label, ()-> mode.get().floatValueOf(c));
//        }
//
//        Surface controls = new Gridding(
//                EnumSwitch.the(mode, "Mode"),
////                new PushButton("Print", ()-> {
////                    Appendable t = TextEdit.out();
////                    nar.exe.print(t);
////                    window(t, 400, 400);
////                }),
//                new PushButton("Clear", ()->pp.series.forEach(Plot2D.Series::clear))
//        );
//        return DurSurface.get(Splitting.column(pp, 0.1f, controls), nar, pp::commit);
//    }
//
//    public static Surface howChart(NAR n) {
//        return NARui.<How>focusPanel(n.how, h->h.pri(), h -> h.id.toString(), n);
//    }

    //    private static void causeSummary(NAR nar, int top) {
//        TopN[] tops = Stream.of(MetaGoal.values()).map(v -> new TopN<>(new Cause[top], (c) ->
//                (float) c.credit[v.ordinal()].total())).toArray(TopN[]::new);
//        nar.causes.forEach((Cause c) -> {
//            for (TopN t : tops)
//                t.add(c);
//        });
//
//        for (int i = 0, topsLength = tops.length; i < topsLength; i++) {
//            TopN t = tops[i];
//            System.out.println(MetaGoal.values()[i]);
//            t.forEach(tt->{
//                System.out.println("\t" + tt);
//            });
//        }
//    }


    /**
     * adds duration control
     */
    static class NARLoopPanel extends LoopPanel {

        final IntRange durMS = new IntRange(1, 1, 1000);

        NARLoopPanel(NARLoop loop) {
            super(loop);
            NAR nar = loop.nar;
            durMS.set(nar.dur());
            RealTime time;
            if (nar.time instanceof RealTime) {
                time = ((RealTime) nar.time);
                add(
                        new IntSlider("Dur(ms)", durMS)
                                .on(new FloatProcedure() {
                                    @Override
                                    public void value(float durMS) {
                                        nar.time.dur((float) Math.max((int) Math.round(durMS), 1));
                                    }
                                }),
                        new FloatSlider(loop.throttle, "Throttle")
                );
            } else {

                time = null;
            }
        }

        @Override
        public void update() {

            super.update();

            if (loop.isRunning()) {

                NAR n = ((NARLoop) loop).nar;
                if (n.time instanceof RealTime) {
                    double actualMS = (double) ((RealTime) n.time).durSeconds() * 1000.0;
                    if (!Util.equals(durMS.doubleValue(), actualMS, 0.1)) {
                        durMS.set(actualMS); //external change singificant
                    }
                }
            }

        }
    }

    static Surface runPanel(NAR n) {
        AbstractLabel nameLabel;
        LoopPanel control = new NARLoopPanel(n.loop);
        Surface p = new Splitting(
                nameLabel = new BitmapLabel(n.self().toString()),
                0.25f, false, control
        );
        return DurSurface.get(p, n, control::update);
    }



}
