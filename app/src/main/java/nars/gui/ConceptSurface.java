package nars.gui;

import jcog.bag.Bag;
import nars.$;
import nars.NAR;
import nars.control.Activate;
import nars.exe.AbstractExec;
import nars.task.NALTask;
import nars.term.Term;
import nars.term.Termed;
import spacegraph.layout.Grid;
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
                                    Activate b = bag.get(n.concept(id));
                                    if (b != null)
                                        return b.priElseZero();
                                }

                                return 0f; // Float.NaN;
                            });
                    return DurSurface.get(new Grid(
                            new PushButton("+ Boost").click((b) -> {
                                n.activate(id, 1f);
                            }),
                            p
                            //new PushButton("- Drain").click((b)->{})
                    ), n, (nn) -> {
                        p.update();
                    });
                },
                "beliefs", () -> Vis.beliefCharts(64, n, n.concept(id)),
                "termlinks", () -> Vis.bagHistogram((Iterable) (n.concept(id).termlinks()::iterator), 10),
                "tasklinks", () -> Vis.bagHistogram((Iterable) (n.concept(id).tasklinks()::iterator), 10),
                "goal", () -> {
                    return new Grid(
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
                    return new Grid(
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
