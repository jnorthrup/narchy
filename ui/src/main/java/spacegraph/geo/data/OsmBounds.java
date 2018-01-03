package spacegraph.geo.data;

/**
 * Created by unkei on 2017/04/26.
 */
public class OsmBounds {

    public final double minLat;
    public final double minLon;
    public final double maxLat;
    public final double maxLon;

    public OsmBounds(double minLat, double minLon, double maxLat, double maxLon) {
        this.minLat = minLat;
        this.minLon = minLon;
        this.maxLat = maxLat;
        this.maxLon = maxLon;
    }
}
