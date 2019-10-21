package spacegraph.test;

import jcog.User;
import org.xml.sax.SAXException;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.OsmSurface;
import spacegraph.util.geo.IRL;
import spacegraph.util.geo.osm.Osm;
import spacegraph.util.geo.osm.OsmElement;
import spacegraph.util.geo.osm.OsmNode;
import spacegraph.util.geo.osm.OsmWay;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.function.Consumer;

public enum OSMTest {
	;

	enum OSMGraphTest {
		;

		public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {

            IRL i = new IRL(User.the());

            Osm o = new Osm().load("/home/me/m/test.osm.bz2");
            Consumer<OsmElement> index = x -> {
                if (x.tags != null && !x.tags.isEmpty())
                    i.index.add(x);
            };
            for (OsmNode osmNode : o.nodes.values()) {
                index.accept(osmNode);
            }
            for (OsmWay osmWay : o.ways.values()) {
                index.accept(osmWay);
            }
//            o.relations.values().forEach(i.index::addAt);
//            System.out.println(o.nodes.size() + " nodes");
            o.ready = true;

            System.out.println(o);

            for (OsmNode n : o.nodes.values()) {
                if (n.tags != null) {
                    System.out.println(n);
                }
            }

            SpaceGraph.window(new OsmSurface(i).go(o).widget(), 800, 800);

            i.index.stats().print(System.out);
        }
    }

    public static void main(String[] args) {
//        i.load(-80.65, 28.58, -80.60, 28.63);

//        SpaceGraphPhys3D sg = new SpaceGraphPhys3D(new OsmSpace(i.osm).volume());
//        sg.io.show(800, 800);

//        sg.addAt(new SubOrtho(WidgetTest.widgetDemo()).posWindow(0, 0, 0.3f, 1f));
        SpaceGraph.window(osmTest(), 800, 800);
    }

    public static Surface osmTest() {
        IRL i = new IRL(User.the());


        return new OsmSurface(i).go(
                 //-80.65f, 28.58f
                -73.9712488f, 40.7830603f
                //-73.9712488f, 40.7830603f
                //-73.993449f, 40.751029f
        , 0.001f*4, 0.001f*4).widget();

        //i.index.stats().print(System.out);
    }
}
