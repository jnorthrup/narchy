package nars.gui;

import jcog.pri.Prioritized;
import jcog.pri.bag.Bag;
import nars.NAR;
import nars.control.DurService;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.container.graph.Graph2D;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.menu.TabMenu;
import spacegraph.space2d.widget.meter.BagChart;
import spacegraph.space2d.widget.text.VectorLabel;

import java.util.Map;

public class BagView<X extends Prioritized> extends TabMenu {

    public BagView(String label, Bag<?,X> bag, NAR nar) {
        super(Map.of(
                        label, () -> new VectorLabel(label),
                        "edit", () -> new Gridding(
                                new PushButton("clear", () -> bag.clear()),
                                new PushButton("print", () -> bag.print())
                        ),
                        "histo", () -> NARui.bagHistogram(bag::iterator, 10, nar),
                        "treechart", () -> {
                            BagChart<X> b = new BagChart<>(bag, (Graph2D.NodeVis<X> n) -> {
                                Prioritized p = n.id;
                                float pri = n.pri = Math.max(p.priElseZero(), 1/(2*bag.capacity()));
                                n.color(pri, 0.25f, 0.25f);

                                if (n.the()==null)
                                    n.set(new PushButton(new VectorLabel(p.toString())/*.click(()->{})*/));

                            }) {
                                DurService on;

                                @Override
                                public boolean start(SurfaceBase parent) {
                                    if (super.start(parent)) {
                                        on = DurService.on(nar, (Runnable) this::update);
                                        return true;
                                    }
                                    return false;
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
                            return b;
                        }
                ));
    }
}
