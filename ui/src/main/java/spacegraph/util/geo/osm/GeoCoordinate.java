package spacegraph.util.geo.osm;

import org.w3c.dom.Element;

import static java.lang.Double.parseDouble;

/**
 * Created by unkei on 2017/04/25.
 */
public class GeoCoordinate {
    public final double latitude;
    public final double longitude;
    private final double altitude;

    public GeoCoordinate() {
        this(0, 0, 0);
    }

    private GeoCoordinate(double latitude, double longitude, double altitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
    }

    public GeoCoordinate(double latitude, double longitude) {
        this(latitude, longitude, 0);
    }

    public GeoCoordinate(Element element) {
        this(parseDouble(element.getAttribute("lat")),
            parseDouble(element.getAttribute("lon")),
            parseDoubleOr(element.getAttribute("alt"), 0)
        );
    }

    private static double parseDoubleOr(String alt, double otherwise) {
        return alt == null || alt.isEmpty() ? otherwise : parseDouble(alt);
    }
}
