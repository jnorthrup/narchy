package nars.gui;

import jcog.pri.PriReference;
import jcog.pri.Prioritized;
import jcog.pri.bag.Bag;
import nars.NAR;
import nars.control.DurService;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.meter.BagChart;
import spacegraph.space2d.widget.tab.TabPane;
import spacegraph.space2d.widget.text.Label;

import java.util.Map;

public class BagView extends TabPane {

    public BagView(String label, Bag bag, NAR nar) {
        super(Map.of(
                label, () -> new Label(label),
                "edit", () -> {
                    return new Gridding(
                            new PushButton("clear", () -> bag.clear()),
                            new PushButton("print", () -> bag.print())
                    );
                },
                "histo", () -> {
                    return NARui.bagHistogram(bag::iterator, 10, nar);
                },
                "treechart", () -> {
                    BagChart b = new BagChart(bag) {
                        DurService on;

                        @Override
                        public boolean start(SurfaceBase parent) {
                            if (super.start(parent)) {
                                on = DurService.on(nar, ()->{
                                    update();
                                });
                                return true;
                            }
                            return false;
                        }

                        @Override
                        protected String label(Object i, int MAX_LEN) {
                            return super.label(((PriReference)i).get().toString(), MAX_LEN);
                        }

                        @Override
                        public void accept(Object o, ItemVis itemVis) {
                            if (o instanceof Prioritized) {
                                float pri = ((Prioritized) o).priElseZero();
                                itemVis.update(
                                        pri,
                                        pri,
                                        0,
                                        1f-pri
                                );
                            }
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
