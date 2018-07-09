package spacegraph.util.geo.osm;

import org.w3c.dom.Element;

/**
 * Created by unkei on 2017/04/26.
 */
public class OsmBounds {

    public final double minLat;
    public final double minLon;
    public final double maxLat;
    public final double maxLon;

    public OsmBounds(Element childElement) {
        
        this(
            Double.parseDouble(childElement.getAttribute("minlat")),
            Double.parseDouble(childElement.getAttribute("minlon")),
            Double.parseDouble(childElement.getAttribute("maxlat")),
            Double.parseDouble(childElement.getAttribute("maxlon")));
    }

    private OsmBounds(double minLat, double minLon, double maxLat, double maxLon) {
        this.minLat = minLat;
        this.minLon = minLon;
        this.maxLat = maxLat;
        this.maxLon = maxLon;
    }
}
