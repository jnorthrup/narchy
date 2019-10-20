package spacegraph.util.geo;

import jcog.Util;

/*
 *  ECEF - Earth Centered Earth Fixed coordinate system
 *  https://en.wikipedia.org/wiki/ECEF
 *
 *  LLA - Lat Lon Alt
 */
public class ECEF {


    private static final double a = 6378137; 
    private static final double e = 8.1819190842622e-2;  

    private static final double asq = Util.sqr(a);
    private static final double esq = Util.sqr(e);
    private static final double esqInv = 1 - esq;



    public static double[] ecef2latlon(double[] ecef) {
        return ecef2latlon(ecef[0], ecef[1], ecef[2]);
    }

    public static double[] ecef2latlon(double x, double y, double z) {

        var b = Math.sqrt(asq * esqInv);
        var bsq = Util.sqr(b);
        var ep = Math.sqrt((asq - bsq) / bsq);
        var p = Math.sqrt(Util.sqr(x) + Util.sqr(y));
        var th = Math.atan2(a * z, b * p);

        var lat = Math.atan2((z + Util.sqr(ep) * b * Util.cube(Math.sin(th))), (p - esq * a * Util.cube(Math.cos(th))));

        var N = a / (Math.sqrt(1 - esq * Util.sqr(Math.sin(lat))));
        var alt = p / Math.cos(lat) - N;

        var lon = Math.atan2(y, x) % (2 * Math.PI);

        return new double[]{lat, lon, alt};
    }


    public static double[] latlon2ecef(double... lla) {
        return latlon2ecef(lla[0], lla[1], lla[2]);
    }

    public static double[] latlon2ecef(double lat, double lon, double alt) {
        return latlon2ecef(lat, lon, alt, new double[3]);
    }

    static final double DEG2RAD = Math.PI/180;

    public static double[] latlon2ecef(double lat, double lon, double alt, double[] target) {


        lat *= DEG2RAD;
        lon *= DEG2RAD;

        var sinLat = Math.sin(lat);
        var N = a / Math.sqrt(1 - esq * Util.sqr(sinLat));

        var cosLat = Math.cos(lat);
        var xy = (N + alt) * cosLat;
        var x = xy * Math.cos(lon);
        var y = xy * Math.sin(lon);

        target[0] = x;
        target[1] = y;
        var z = (esqInv * N + alt) * sinLat;
        target[2] = z;
        return target;
    }
}
