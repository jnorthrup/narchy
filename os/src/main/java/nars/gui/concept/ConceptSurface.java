package nars.gui.concept;

import nars.$;
import nars.NAR;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.gui.NARui;
import nars.task.NALTask;
import nars.term.Term;
import nars.term.Termed;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.menu.TabMenu;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space2d.widget.text.VectorLabel;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static nars.Op.*;

public class ConceptSurface extends TabMenu {

    public ConceptSurface(Termed id, NAR n) {
        this(id.term(), n);
    }

    public ConceptSurface(Term id, NAR n) {
        super(conceptMenu(id, n));
    }

    public static Map<String, Supplier<Surface>> conceptMenu(Term x, NAR n) {
        Map<String, Supplier<Surface>> m = Map.of(
                x.toString(), () -> new VectorLabel(x.toString()),
//                "budget", () -> {
//
//                    Term xx = x.concept();
//                    Plot2D p = new Plot2D(64, Plot2D.Line)
//                            .add("pri", () -> n.concepts.pri(xx, 0));
//
////                    CheckBox boost = new CheckBox("Boost");
//                    return DurSurface.get(//new Splitting<>(
//                            //boost,
//                            p
//                    //        , 0.8f)
//                    ,
//                    n, (nn) -> {
//                        p.update();
////                        if (boost.on()) {
////                            n.activate(xx, 1f);
////                        }
//                    });
//                },
                "beliefs", () -> NARui.beliefChart(n.conceptualizeDynamic(x), n),
//                        "termlinks", () -> new BagView("TermLinks", n.concept(id).termlinks(), n),
//                "tasklinks", () -> new LabeledPane("TaskLinks", new BagView(n.concept(x).tasklinks(), n)),
                "goal", () -> {
                    return new Gridding(
                            new PushButton("gOAL tRUE").click((b) -> {
                                long now = n.time();
                                n.input(NALTask.the(x, GOAL, $.t(1f, n.confDefault(GOAL)), now, now, now + n.dur(), n.evidence()).priSet(n.priDefault(GOAL)));
                            }),
                            new PushButton("gOAL fALSE").click((b) -> {
                                long now = n.time();
                                n.input(NALTask.the(x, GOAL, $.t(0f, n.confDefault(GOAL)), now, now, now + n.dur(), n.evidence()).priSet(n.priDefault(GOAL)));
                            })
                    );
                },
                "predict", () -> {
                    return new Gridding(
                            new PushButton("What +1").click((b) -> {
                                long now = n.time();
                                n.input(NALTask.the(x, QUESTION, null, now, now, now + n.dur(), n.evidence()).priSet(n.priDefault(QUESTION)));
                            }),
                            new PushButton("What +4").click((b) -> {
                                long now = n.time();
                                n.input(NALTask.the(x, QUESTION, null, now, now + n.dur() * 3, now + n.dur() * 4, n.evidence()).priSet(n.priDefault(QUESTION)));
                            }),
                            new PushButton("How +1").click((b) -> {
                                long now = n.time();
                                n.input(NALTask.the(x, QUEST, null, now, now, now + n.dur(), n.evidence()).priSet(n.priDefault(QUEST)));
                            })
                    );
                }
        );
        Concept c = n.conceptualizeDynamic(x);
        if (c instanceof PermanentConcept) {
            m = new HashMap(m);
            m.put(c.getClass().getSimpleName(), ()->{
                return new ObjectSurface(c);
            });
        }
        return m;
    }
}
