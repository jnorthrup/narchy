package spacegraph.space3d.test;

import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import jcog.User;
import spacegraph.space2d.WidgetTest;
import spacegraph.space2d.hud.SubOrtho;
import spacegraph.space3d.SpaceGraphPhys3D;
import spacegraph.space3d.widget.OsmSpace;
import spacegraph.util.geo.IRL;

public class OSMTest {
    public static void main(String[] args) {


        //https://wiki.openstreetmap.org/wiki/API_v0.6
        //http://api.openstreetmap.org/api/0.6/changeset/#id/comment
        // /api/0.6/map?bbox=min_lon,min_lat,max_lon,max_lat (W,S,E,N)

        IRL i = new IRL(User.the());
        i.load(-80.65, 28.58, -80.60, 28.63);

        SpaceGraphPhys3D sg = new SpaceGraphPhys3D(new OsmSpace(i.osm));
        sg.show(800, 800);
        sg.addWindowListener(new WindowAdapter() {
            @Override
            public void windowDestroyed(WindowEvent e) {
                super.windowDestroyed(e);
                System.exit(0);
            }
        });
        sg.add(new SubOrtho(WidgetTest.widgetDemo()).posWindow(0, 0, 0.3f, 1f));

    }
}
