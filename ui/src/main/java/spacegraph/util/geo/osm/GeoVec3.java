package spacegraph.util.geo.osm;

import org.w3c.dom.Element;
import spacegraph.space2d.phys.common.Vec3;

import static java.lang.Double.parseDouble;

/**
 * Created by unkei on 2017/04/25.
 */
public class GeoVec3 extends Vec3 {
//    private final double latitude;
//    private final double longitude;
//    private final double altitude;

    public GeoVec3() {
        this(0, 0, 0);
    }

    public GeoVec3(double longitude, double latitude, double altitude) {
        super((float)longitude, (float)latitude, (float)altitude);
//        this.latitude = latitude;
//        this.longitude = longitude;
//        this.altitude = altitude;
    }

//    public GeoVec3(double latitude, double longitude) {
//        this(longitude, latitude, 0);
//    }

    public GeoVec3(Element e) {
        this(parseDouble(e.getAttribute("lon")), parseDouble(e.getAttribute("lat")),
                parseDoubleOr(e.getAttribute("alt"), 0));
    }

    private static double parseDoubleOr(String alt, double otherwise) {
        return alt == null || alt.isEmpty() ? otherwise : parseDouble(alt);
    }

    public double getLatitude() {
        return y;
    }

    public double getLongitude() {
        return x;
    }

    public double getAltitude() {
        return z;
    }
}
