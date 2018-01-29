package nars.gui.graph.run;

import jcog.data.graph.NodeGraph;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.derive.time.TimeGraph;
import spacegraph.SpaceGraph;
import spacegraph.SubOrtho;
import spacegraph.space.SpaceWidget;
import spacegraph.test.SimpleGraph1;
import spacegraph.widget.meta.AutoSurface;

import static nars.time.Tense.ETERNAL;
import static spacegraph.layout.Gridding.grid;

public class TestTimeGraphVis extends SimpleGraph1<TimeGraph.Event> {

    public TestTimeGraphVis() {
        super((SpaceWidget<TimeGraph.Event> n)->{
            defaultVis.each(n);

            if (n.id instanceof TimeGraph.Absolute) {

            } else {
                n.color(0.5f, 0.5f, 0.5f, 0.5f);
            }
        });
    }

    static NodeGraph dt() {
        TimeGraph A = new TimeGraph();
        A.know($.$safe("((one &&+1 two) ==>+1 (three &&+1 four))"), ETERNAL);
        A.know($.$safe("one"), 1);
        A.know($.$safe("two"), 20);
        A.solve($.$safe("\"(one &&+- two)\""), false, (x)->true);
        return A;
    }
    public static void main(String[] args) {

        NAR n = NARS.threadSafe();


        TestTimeGraphVis cs = new TestTimeGraphVis();


        SpaceGraph sg = cs.show(1400, 1000, true);


        sg.add(new SubOrtho(grid(
                //new AutoSurface<>(sg.dyn.broadConstraints.get(0) /* FD hack */),
                new AutoSurface<>(cs.vis)
        )).posWindow(0, 0, 1f, 0.2f));

        cs.commit(dt(/*..*/));
    }

}
