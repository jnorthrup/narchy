package nars.gui.concept;

import nars.$;
import nars.NAR;
import nars.gui.BagView;
import nars.gui.DurSurface;
import nars.gui.NARui;
import nars.task.NALTask;
import nars.term.Term;
import nars.term.Termed;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.menu.TabMenu;
import spacegraph.space2d.widget.meter.Plot2D;
import spacegraph.space2d.widget.text.VectorLabel;

import java.util.Map;

import static nars.Op.*;

public class ConceptSurface extends TabMenu {

    public ConceptSurface(Termed id, NAR n) {
        this(id.term(), n);
    }

    public ConceptSurface(Term id, NAR n) {
        super(Map.of(
                        id.toString(), () -> new VectorLabel(id.toString()),
                        "budget", () -> {

                            Plot2D p = new Plot2D(64, Plot2D.Line)
                                    .add("pri", () -> n.concepts.pri(id, 0));
                            CheckBox boost = new CheckBox("Boost");
                            return DurSurface.get(new Splitting<>(

                                    boost, p

                            , 0.8f), n, (nn) -> {
                                p.update();
                                if (boost.on()) {
                                    n.activate(id, 1f);
                                }
                            });
                        },
                        "beliefs", () -> NARui.beliefCharts(n, n.concept(id)),
//                        "termlinks", () -> new BagView("TermLinks", n.concept(id).termlinks(), n),
                        "tasklinks", () -> new BagView("TaskLinks", n.concept(id).tasklinks(), n),
                        "goal", () -> {
                            return new Gridding(
                                    new PushButton("gOAL tRUE").click((b) -> {
                                        long now = n.time();
                                        n.input(new NALTask(id, GOAL, $.t(1f, n.confDefault(GOAL)), now, now, now + n.dur(), n.evidence()).priSet(n.priDefault(GOAL)));
                                    }),
                                    new PushButton("gOAL fALSE").click((b) -> {
                                        long now = n.time();
                                        n.input(new NALTask(id, GOAL, $.t(0f, n.confDefault(GOAL)), now, now, now + n.dur(), n.evidence()).priSet(n.priDefault(GOAL)));
                                    })
                            );
                        },
                        "predict", () -> {
                            return new Gridding(
                                    new PushButton("What +1").click((b) -> {
                                        long now = n.time();
                                        n.input(new NALTask(id, QUESTION, null, now, now, now + n.dur(), n.evidence()).priSet(n.priDefault(QUESTION)));
                                    }),
                                    new PushButton("What +4").click((b) -> {
                                        long now = n.time();
                                        n.input(new NALTask(id, QUESTION, null, now, now + n.dur() * 3, now + n.dur() * 4, n.evidence()).priSet(n.priDefault(QUESTION)));
                                    }),
                                    new PushButton("How +1").click((b) -> {
                                        long now = n.time();
                                        n.input(new NALTask(id, QUEST, null, now, now, now + n.dur(), n.evidence()).priSet(n.priDefault(QUEST)));
                                    })
                            );
                        }
                ));
    }
}
