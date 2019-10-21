package nars.gui;

import jcog.pri.NLink;
import jcog.pri.PriReference;
import jcog.pri.Prioritized;
import jcog.pri.bag.Bag;
import nars.NAR;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.graph.NodeVis;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.menu.TabMenu;
import spacegraph.space2d.widget.meta.WindowToggleButton;
import spacegraph.space2d.widget.meter.BagChart;
import spacegraph.space2d.widget.meter.Plot2D;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.util.math.Color3f;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class BagView<X extends Prioritized> extends TabMenu {

    public BagView(Bag<?, X> bag, NAR nar) {
        super(Map.of(
                "info", new Supplier<Surface>() {
                    @Override
                    public Surface get() {
                        return new Gridding(
                                new VectorLabel(bag.getClass().toString()),
                                new PushButton("clear", bag::clear),
                                new PushButton("print", bag::print)
                        );
                    }
                },
                "stat", new Supplier<Surface>() {
                    @Override
                    public Surface get() {
                        Plot2D budgetChart = new Plot2D(64, Plot2D.Line)
                                .add("Mass", bag::mass)
                                .add("Pressure", bag::pressure);

                        return DurSurface.get(budgetChart, nar, budgetChart::commit);
                    }
                },
                "histo", new Supplier<Surface>() {
                    @Override
                    public Surface get() {
                        return bagHistogram(bag, 20, nar);
                    }
                },
                "treechart", new Supplier<Surface>() {
                    @Override
                    public Surface get() {
                        BagChart<X> b = new BagChart<X>(bag, new Consumer<NodeVis<X>>() {
                            @Override
                            public void accept(NodeVis<X> n) {
                                Prioritized p = n.id;
                                float pri = n.pri = Math.max(p.priElseZero(), 1f / (float) (2 * bag.capacity()));
                                n.color(pri, 0.25f, 0.25f);
                                if (!(n.the() instanceof PushButton)) {
                                    String label = p instanceof NLink ? ((NLink) p).get().toString() : p.toString();
                                    n.set(new PushButton(new VectorLabel(label)/*.click(()->{})*/));
                                }
                            }
                        });
                        return DurSurface.get(b, nar, (Runnable) (b::update));
                    }
                }
        ));

        set("histo", true);
    }

    public static <X extends Prioritized> Surface bagHistogram(Iterable<X> bag, int bins, NAR n) {
        float[] d = new float[bins];
        DurSurface hc = DurSurface.get(new HistogramChart(
                        new Supplier<float[]>() {
                            @Override
                            public float[] get() {
                                return d;
                            }
                        },
                        new Color3f(0.25f, 0.5f, 0f), new Color3f(1f, 0.5f, 0.1f)),
                n, new Runnable() {
                    @Override
                    public void run() {
                        PriReference.histogram(bag, d);
                    }
                });

        return Splitting.column(
                hc, 0.1f, new Gridding(
                        new WindowToggleButton("Sonify", new Supplier() {
                            @Override
                            public Object get() {
                                return new HistogramSonification(d);
                            }
                        }
                        )
                )
        );
    }
}
