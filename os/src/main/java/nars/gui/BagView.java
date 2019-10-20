package nars.gui;

import jcog.pri.NLink;
import jcog.pri.PriReference;
import jcog.pri.Prioritized;
import jcog.pri.bag.Bag;
import nars.NAR;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.menu.TabMenu;
import spacegraph.space2d.widget.meta.WindowToggleButton;
import spacegraph.space2d.widget.meter.BagChart;
import spacegraph.space2d.widget.meter.Plot2D;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.util.math.Color3f;

import java.util.Map;

public class BagView<X extends Prioritized> extends TabMenu {

    public BagView(Bag<?, X> bag, NAR nar) {
        super(Map.of(
                "info", () -> new Gridding(
                        new VectorLabel(bag.getClass().toString()),
                        new PushButton("clear", bag::clear),
                        new PushButton("print", bag::print)
                ),
                "stat", () -> {
                    Plot2D budgetChart = new Plot2D(64, Plot2D.Line)
                            .add("Mass", bag::mass)
                            .add("Pressure", bag::pressure);

                    return DurSurface.get(budgetChart, nar, budgetChart::commit);
                },
                "histo", () -> bagHistogram(bag, 20, nar),
                "treechart", () -> {
                    BagChart<X> b = new BagChart<X>(bag, n -> {
                        Prioritized p = n.id;
                        float pri = n.pri = Math.max(p.priElseZero(), 1f / (2 * bag.capacity()));
                        n.color(pri, 0.25f, 0.25f);
                        if (!(n.the() instanceof PushButton)) {
                            String label = p instanceof NLink ? ((NLink) p).get().toString() : p.toString();
                            n.set(new PushButton(new VectorLabel(label)/*.click(()->{})*/));
                        }
                    });
                    return DurSurface.get(b, nar, (Runnable) (b::update));
                }
        ));

        set("histo", true);
    }

    public static <X extends Prioritized> Surface bagHistogram(Iterable<X> bag, int bins, NAR n) {
        float[] d = new float[bins];
        DurSurface hc = DurSurface.get(new HistogramChart(
                        () -> d,
                        new Color3f(0.25f, 0.5f, 0f), new Color3f(1f, 0.5f, 0.1f)),
                n, () -> PriReference.histogram(bag, d));

        return Splitting.column(
                hc, 0.1f, new Gridding(
                        new WindowToggleButton("Sonify", () ->
                                new HistogramSonification(d)
                        )
                )
        );
    }
}
