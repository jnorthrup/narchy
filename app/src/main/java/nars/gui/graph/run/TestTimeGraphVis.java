package nars.gui.graph.run;

import jcog.data.graph.MapNodeGraph;
import nars.$;
import nars.derive.time.TimeGraph;
import spacegraph.SubOrtho;
import spacegraph.render.JoglPhysics;
import spacegraph.space.SpaceWidget;
import spacegraph.test.SimpleGraph1;
import spacegraph.widget.meta.AutoSurface;

import static nars.time.Tense.ETERNAL;
import static spacegraph.container.Gridding.grid;

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

    static MapNodeGraph dt() {
        TimeGraph A = new TimeGraph();
        A.know($.$safe("((one &&+1 two) ==>+1 (three &&+1 four))"), ETERNAL);
        A.know($.$safe("one"), 1);
        A.know($.$safe("two"), 20);
        A.solve($.$safe("\"(one &&+- two)\""), false, (x)->true);
        return A;
    }
    public static void main(String[] args) {

        //NAR n = NARS.threadSafe();


        TestTimeGraphVis cs = new TestTimeGraphVis();


        JoglPhysics sg = cs.show(1400, 1000, true);


        sg.add(new SubOrtho(grid(
                //new AutoSurface<>(sg.dyn.broadConstraints.get(0) /* FD hack */),
                new AutoSurface<>(cs.vis)
        )).posWindow(0, 0, 1f, 0.2f));

        cs.commit(dt(/*..*/));

//        {
//            MapNodeGraph<Object, Object> h = new MapNodeGraph<>();
//            h.addEdge(h.addNode("y"), "yx", h.addNode("x"));
//
//            ObjectGraph o = new ObjectGraph(h);
//            cs.commit(o);
//        }
    }

}
