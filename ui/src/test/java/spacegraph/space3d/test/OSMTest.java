package spacegraph.space3d.test;

import jcog.User;
import spacegraph.space3d.SpaceGraphPhys3D;
import spacegraph.space3d.widget.OsmSpace;
import spacegraph.util.geo.IRL;

public class OSMTest {
    public static void main(String[] args) {


        
        
        

        IRL i = new IRL(User.the());
        i.load(-80.65, 28.58, -80.60, 28.63);

        SpaceGraphPhys3D sg = new SpaceGraphPhys3D(new OsmSpace(i.osm).volume());
        sg.io.show(800, 800);







//        sg.add(new SubOrtho(WidgetTest.widgetDemo()).posWindow(0, 0, 0.3f, 1f));

    }
}
