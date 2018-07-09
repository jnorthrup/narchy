package spacegraph.util.geo;

import jcog.User;
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

    @Deprecated
    public final Osm osm = new Osm();
    private final User user;



    public IRL(User u) {
        this.user = u;
    }

    private static final Logger logger = LoggerFactory.getLogger(IRL.class);

    public void load(double lonMin, double latMin, double lonMax, double latMax) {
        try {
            URL u = Osm.url("https://api.openstreetmap.org", lonMin, latMin, lonMax, latMax);
            user.get(u.toString(), () -> {
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


}
