package spacegraph.space3d.test;

import jcog.User;
import org.xml.sax.SAXException;
import spacegraph.util.geo.IRL;
import spacegraph.util.geo.osm.Osm;
import spacegraph.util.geo.osm.OsmElement;
import spacegraph.video.OsmSurface;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.function.Consumer;

import static spacegraph.SpaceGraph.window;

public class OSMTest {

    static class OSMGraphTest {
        public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {

            IRL i = new IRL(User.the());

            Osm o = new Osm().load("/home/me/test.osm.bz2");
            Consumer<OsmElement> index = x -> {
                if (x.tags != null && !x.tags.isEmpty())
                    i.index.add(x);
            };
            o.nodes.values().forEach(index);
            o.ways.values().forEach(index);
//            o.relations.values().forEach(i.index::addAt);
//            System.out.println(o.nodes.size() + " nodes");
            o.ready = true;

            System.out.println(o);

            o.nodes.values().forEach(n -> {
                if (n.tags!=null)
                    System.out.println(n);
            });

            window(new OsmSurface(i).go(o).view(), 800, 800);

            i.index.stats().print(System.out);
        }
    }

    public static void main(String[] args) {

        IRL i = new IRL(User.the());


        window(new OsmSurface(i).go(
                 //-80.65f, 28.58f
                -73.9712488f, 40.7830603f
                //-73.9712488f, 40.7830603f
                //-73.993449f, 40.751029f
        , 0.001f*4, 0.001f*4).view(), 800, 800);

        i.index.stats().print(System.out);

//        i.load(-80.65, 28.58, -80.60, 28.63);

//        SpaceGraphPhys3D sg = new SpaceGraphPhys3D(new OsmSpace(i.osm).volume());
//        sg.io.show(800, 800);


//        sg.addAt(new SubOrtho(WidgetTest.widgetDemo()).posWindow(0, 0, 0.3f, 1f));

    }
}
