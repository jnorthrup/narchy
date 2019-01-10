package spacegraph.space3d.test;

import jcog.User;
import spacegraph.util.geo.IRL;
import spacegraph.video.OsmSurface;

import static spacegraph.SpaceGraph.window;

public class OSMTest {

    public static void main(String[] args) {

        IRL i = new IRL(User.the());


        window(new OsmSurface().go(
                i, -80.65f, 28.58f, 0.003f*4, 0.003f*4
                //-80.65f, 28.58f
                //-80.65, 28.58
                //-73.993449f, 40.751029f
        ).view(), 800, 800);

        System.out.println(i.index.stats());

//        i.load(-80.65, 28.58, -80.60, 28.63);

//        SpaceGraphPhys3D sg = new SpaceGraphPhys3D(new OsmSpace(i.osm).volume());
//        sg.io.show(800, 800);


//        sg.add(new SubOrtho(WidgetTest.widgetDemo()).posWindow(0, 0, 0.3f, 1f));

    }
}
