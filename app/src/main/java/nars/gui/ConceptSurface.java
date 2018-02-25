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
import spacegraph.container.Gridding;
import spacegraph.container.Splitting;
import spacegraph.widget.button.CheckBox;
import spacegraph.widget.button.PushButton;
import spacegraph.widget.meter.Plot2D;
import spacegraph.widget.tab.TabPane;
import spacegraph.widget.text.Label;

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
                "termlinks", () -> Vis.bagHistogram((Iterable) (n.concept(id).termlinks()::iterator), 10),
                "tasklinks", () -> Vis.bagHistogram((Iterable) (n.concept(id).tasklinks()::iterator), 10),
                "goal", () -> {
                    return new Gridding(
                            new PushButton("gOAL tRUE").click((b) -> {
                                long now = n.time();
                                n.input(new NALTask(id, GOAL, $.t(1f, n.confDefault(GOAL)), now, now, now + n.dur(), n.time.nextInputStamp()).pri(n.priDefault(GOAL)));
                            }),
                            new PushButton("gOAL fALSE").click((b) -> {
                                long now = n.time();
                                n.input(new NALTask(id, GOAL, $.t(0f, n.confDefault(GOAL)), now, now, now + n.dur(), n.time.nextInputStamp()).pri(n.priDefault(GOAL)));
                            })
                    );
                },
                "predict", () -> {
                    return new Gridding(
                            new PushButton("What +1").click((b) -> {
                                long now = n.time();
                                n.input(new NALTask(id, QUESTION, null, now, now, now + n.dur(), n.time.nextInputStamp()).pri(n.priDefault(QUESTION)));
                            }),
                            new PushButton("What +4").click((b) -> {
                                long now = n.time();
                                n.input(new NALTask(id, QUESTION, null, now, now + n.dur() * 3, now + n.dur() * 4, n.time.nextInputStamp()).pri(n.priDefault(QUESTION)));
                            }),
                            new PushButton("How +1").click((b) -> {
                                long now = n.time();
                                n.input(new NALTask(id, QUEST, null, now, now, now + n.dur(), n.time.nextInputStamp()).pri(n.priDefault(QUEST)));
                            })
                    );
                }
        ));
    }
}
