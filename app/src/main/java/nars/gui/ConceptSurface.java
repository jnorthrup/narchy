package nars.gui;

import jcog.bag.Bag;
import nars.$;
import nars.NAR;
import nars.concept.Concept;
import nars.control.Activate;
import nars.exe.AbstractExec;
import nars.task.NALTask;
import nars.term.Term;
import nars.term.Termed;
import spacegraph.space2d.container.Gridding;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.meter.Plot2D;
import spacegraph.space2d.widget.tab.TabPane;
import spacegraph.space2d.widget.text.Label;

import java.util.Map;

import static nars.Op.*;

public class ConceptSurface extends TabPane {

    public ConceptSurface(Termed id, NAR n) {
        this(id.term(), n);
    }

    public ConceptSurface(Term id, NAR n) {
        super(Map.of(
                id.toString(), () -> new Label(id.toString()),
                "budget", () -> {

                    Plot2D p = new Plot2D(64, Plot2D.Line)
                            .add("pri", () -> {

                                Bag<Activate, Activate> bag = ((AbstractExec) n.exe).active;
                                if (bag != null) {
                                    Concept ni = n.conceptualize(id);
                                    if (ni!=null) {
                                        Activate b = bag.get(ni);
                                        if (b != null)
                                            return b.priElseZero();
                                    }
                                }

                                return 0f; // Float.NaN;
                            });
                    CheckBox boost = new CheckBox("Boost");
                    return DurSurface.get(new Splitting(
//                            new PushButton("+ Boost").click((b) -> {
//                                n.activate(id, 1f);
//                            }),
                            boost, p
                            //new PushButton("- Drain").click((b)->{})
                    , 0.8f), n, (nn) -> {
                        p.update();
                        if (boost.get()) {
                            n.activate(id, 1f); //activate once per duration if the boost
                        }
                    });
                },
                "beliefs", () -> Vis.beliefCharts(64, n, n.concept(id)),
                "termlinks", () -> new BagView("TermLinks", n.concept(id).termlinks(), n),
                "tasklinks", () -> new BagView("TaskLinks", n.concept(id).tasklinks(), n),
                "goal", () -> {
                    return new Gridding(
                            new PushButton("gOAL tRUE").click((b) -> {
                                long now = n.time();
                                n.input(new NALTask(id, GOAL, $.t(1f, n.confDefault(GOAL)), now, now, now + n.dur(), n.time.nextStampArray()).pri(n.priDefault(GOAL)));
                            }),
                            new PushButton("gOAL fALSE").click((b) -> {
                                long now = n.time();
                                n.input(new NALTask(id, GOAL, $.t(0f, n.confDefault(GOAL)), now, now, now + n.dur(), n.time.nextStampArray()).pri(n.priDefault(GOAL)));
                            })
                    );
                },
                "predict", () -> {
                    return new Gridding(
                            new PushButton("What +1").click((b) -> {
                                long now = n.time();
                                n.input(new NALTask(id, QUESTION, null, now, now, now + n.dur(), n.time.nextStampArray()).pri(n.priDefault(QUESTION)));
                            }),
                            new PushButton("What +4").click((b) -> {
                                long now = n.time();
                                n.input(new NALTask(id, QUESTION, null, now, now + n.dur() * 3, now + n.dur() * 4, n.time.nextStampArray()).pri(n.priDefault(QUESTION)));
                            }),
                            new PushButton("How +1").click((b) -> {
                                long now = n.time();
                                n.input(new NALTask(id, QUEST, null, now, now, now + n.dur(), n.time.nextStampArray()).pri(n.priDefault(QUEST)));
                            })
                    );
                }
        ));
    }
}
