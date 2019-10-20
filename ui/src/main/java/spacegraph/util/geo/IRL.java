package spacegraph.util.geo;

import jcog.User;
import jcog.Util;
import jcog.exe.Exe;
import jcog.memoize.CaffeineMemoize;
import jcog.memoize.Memoize;
import jcog.tree.rtree.RTree;
import jcog.tree.rtree.Spatialization;
import jcog.tree.rtree.rect.RectFloat;
import jcog.tree.rtree.split.LinearSplit;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.util.geo.osm.Osm;
import spacegraph.util.geo.osm.OsmElement;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;

/**
 * "in real life" geospatial data cache
 */
public class IRL {

    private final User user;
    private static final Logger logger = LoggerFactory.getLogger(IRL.class);


    public final RTree<OsmElement> index =
            new RTree<>(new Spatialization<>((OsmElement e) -> e,
                    //new AxialSplitLeaf<>(),
                    new LinearSplit<>(),
                    //new QuadraticSplitLeaf(),
                    4));

    public IRL(User u) {
        this.user = u;
    }


    final Memoize<RectFloat, Osm> reqCache = CaffeineMemoize.build(bounds -> load((double) bounds.left(), (double) bounds.bottom(), (double) bounds.right(), (double) bounds.top()), 1024, false);


    /**
     * gets the Osm grid cell containing the specified coordinate, of our conventional size
     */
    @Deprecated
    public @Nullable Osm request(float lon, float lat, float lonRange, float latRange) {
        Osm o  = reqCache.apply(
                RectFloat.XYXY(
                    Util.round(lon - lonRange/(2.0F -Float.MIN_NORMAL), lonRange),
                    Util.round(lat - latRange/(2.0F -Float.MIN_NORMAL), latRange),
                    Util.round(lon + lonRange/(2.0F -Float.MIN_NORMAL), lonRange),
                    Util.round(lat + latRange/(2.0F -Float.MIN_NORMAL), latRange)
                )
        );

        return o;
    }

    private Osm load(double lonMin, double latMin, double lonMax, double latMax) {
        Osm osm = new Osm();

        URL u = Osm.url("https://api.openstreetmap.org", lonMin, latMin, lonMax, latMax);
        osm.id = u.toExternalForm();

        Exe.runLater(() -> user.get(u.toString(), () -> {
            try {
                logger.info("Downloading {}", u);
                return u.openStream().readAllBytes();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }, (data) -> {
            try {
                logger.info("Loading {} ({} bytes)", u, data.length);
                osm.load(new ByteArrayInputStream(data));
                osm.ways.forEachValue(index::add);
                osm.ready = true;
//                    osm.nodes.forEachValue(n->{
//                        index.addAt(n);
//                    });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));

        return osm;
    }


}
