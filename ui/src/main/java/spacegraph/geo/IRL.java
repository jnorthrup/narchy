package spacegraph.geo;

import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import jcog.User;
import jcog.tree.rtree.ConcurrentRTree;
import jcog.tree.rtree.RTree;
import jcog.tree.rtree.rect.RectDoubleND;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import spacegraph.geo.osm.Osm;

import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;

/** "in real life" geospatial data cache */
public class IRL {

    @Deprecated public final Osm osm = new Osm();
    private final User user;

    final ConcurrentRTree<RectDoubleND> tree = new ConcurrentRTree(new RTree(new RectDoubleND.Builder(), 2, 3, RTree.DEFAULT_SPLIT_TYPE));

    public IRL(User u) {
        this.user = u;
    }

    static final Logger logger = LoggerFactory.getLogger(IRL.class);

    public void load(double lonMin, double latMin, double lonMax, double latMax) {
        try {
            URL u = Osm.url("http://api.openstreetmap.org", lonMin, latMin, lonMax, latMax);
            user.get(u.toString(), ()->{
                try {
                    logger.info("reading {}", u);
                    return u.openStream().readAllBytes();
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }, (bb) -> {
                try {
                    osm.load(new ByteArrayInputStream(bb));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {


        //https://wiki.openstreetmap.org/wiki/API_v0.6
        //http://api.openstreetmap.org/api/0.6/changeset/#id/comment
                                   // /api/0.6/map?bbox=min_lon,min_lat,max_lon,max_lat (W,S,E,N)

            IRL i = new IRL(User.the());
            i.load(-80.65,28.58,-80.60,28.63);

            new OsmSpace(i.osm).show(800, 800).addWindowListener(new WindowAdapter() {
                @Override
                public void windowDestroyed(WindowEvent e) {
                    super.windowDestroyed(e);
                    System.exit(0);
                }
            });

    }
}
