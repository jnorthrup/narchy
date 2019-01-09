package nars.gui.graph.run;

import jcog.data.graph.MapNodeGraph;
import nars.$;
import nars.time.Event;
import nars.time.TimeGraph;
import spacegraph.space3d.SpaceGraphPhys3D;
import spacegraph.space3d.widget.SimpleGraph3D;
import spacegraph.space3d.widget.SpaceWidget;

import static nars.time.Tense.ETERNAL;

public class TestTimeGraphVis extends SimpleGraph3D<Event> {

    public TestTimeGraphVis() {
        super((SpaceWidget<Event> n)->{
            defaultVis.each(n);

            if (n.id instanceof TimeGraph.Absolute) {

            } else {
                n.color(0.5f, 0.5f, 0.5f, 0.5f);
            }
        });
    }

    static MapNodeGraph dt() {
        TimeGraph A = new TimeGraph();
        A.know($.$$("((one &&+1 two) ==>+1 (three &&+1 four))"), ETERNAL);
        A.know($.$$("one"), 1);
        A.know($.$$("two"), 20);
        A.solve($.$$("\"(one &&+- two)\""), (x)->true);
        return A;
    }
    public static void main(String[] args) {

        


        TestTimeGraphVis cs = new TestTimeGraphVis();


        SpaceGraphPhys3D sg = cs.show(1400, 1000, true);


//        sg.add(new SubOrtho(grid(
//
//                new ObjectSurface<>(cs.vis)
//        )).posWindow(0, 0, 1f, 0.2f));

        cs.commit(dt(/*..*/));








    }

}
