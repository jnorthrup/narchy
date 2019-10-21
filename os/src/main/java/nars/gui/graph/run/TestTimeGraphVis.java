package nars.gui.graph.run;

import jcog.data.graph.MapNodeGraph;
import nars.$;
import nars.time.TimeGraph;
import spacegraph.space3d.SpaceGraph3D;
import spacegraph.space3d.widget.SimpleGraph3D;
import spacegraph.space3d.widget.SpaceWidget;

import java.util.function.Predicate;

import static nars.time.Tense.ETERNAL;

public class TestTimeGraphVis extends SimpleGraph3D<TimeGraph.Event> {

    public TestTimeGraphVis() {
        super(new SpaceWidget.SimpleNodeVis<SpaceWidget<TimeGraph.Event>>() {
            @Override
            public void each(SpaceWidget<TimeGraph.Event> n) {
                defaultVis.each(n);

                if (n.id instanceof TimeGraph.Absolute) {

                } else {
                    n.color(0.5f, 0.5f, 0.5f, 0.5f);
                }
            }
        });
    }

    static MapNodeGraph dt() {
        TimeGraph A = new TimeGraph();
        A.know($.$$("((one &&+1 two) ==>+1 (three &&+1 four))"), ETERNAL);
        A.know($.$$("one"), 1L);
        A.know($.$$("two"), 20L);
        A.solve($.$$("\"(one &&+- two)\""), new Predicate<TimeGraph.Event>() {
            @Override
            public boolean test(TimeGraph.Event x) {
                return true;
            }
        });
        return A;
    }
    public static void main(String[] args) {


        TestTimeGraphVis cs = new TestTimeGraphVis();


        SpaceGraph3D sg = cs.show(1400, 1000, true);


//        sg.addAt(new SubOrtho(grid(
//
//                new ObjectSurface<>(cs.vis)
//        )).posWindow(0, 0, 1f, 0.2f));

        cs.commit(dt(/*..*/));








    }

}
