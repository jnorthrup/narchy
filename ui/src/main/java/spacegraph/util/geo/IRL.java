package spacegraph.util.geo;

import jcog.User;
import jcog.Util;
import jcog.exe.Exe;
import jcog.memoize.CaffeineMemoize;
import jcog.memoize.Memoize;
import jcog.tree.rtree.rect.RectFloat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.util.geo.osm.Osm;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;

/**
 * "in real life" geospatial data cache
 */
public class IRL {

    private final User user;

    public IRL(User u) {
        this.user = u;
    }

    private static final Logger logger = LoggerFactory.getLogger(IRL.class);

    final Memoize<RectFloat, Osm> cache = CaffeineMemoize.build(bounds -> {
        return load(bounds.left(), bounds.top(), bounds.right(), bounds.bottom());
    }, 1024, false);


    public static final float TILE_SIZE = 0.005f;

    /**
     * gets the Osm grid cell containing the specified coordinate, of our conventional size
     */
    public Osm tile(float lon, float lat) {
        return cache.apply(
                RectFloat.XYXY(
                    Util.round(lon - TILE_SIZE/(2-Float.MIN_NORMAL), TILE_SIZE),
                    Util.round(lat - TILE_SIZE/(2-Float.MIN_NORMAL), TILE_SIZE),
                    Util.round(lon + TILE_SIZE/(2-Float.MIN_NORMAL), TILE_SIZE),
                    Util.round(lat + TILE_SIZE/(2-Float.MIN_NORMAL), TILE_SIZE)
                )
        );
    }

    private Osm load(double lonMin, double latMin, double lonMax, double latMax) {
        Osm osm = new Osm();

        URL u = Osm.url("https://api.openstreetmap.org", lonMin, latMin, lonMax, latMax);
        osm.id = u.toExternalForm();

        Exe.invokeLater(() -> {

            user.get(u.toString(), () -> {
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
                    osm.ready = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });

        return osm;
    }


}
