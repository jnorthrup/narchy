package spacegraph.util.geo;

import jcog.Util;

/*
 *  ECEF - Earth Centered Earth Fixed coordinate system
 *  https://en.wikipedia.org/wiki/ECEF
 *
 *  LLA - Lat Lon Alt
 */
public enum ECEF {
	;


	private static final double a = 6378137;
    private static final double e = 8.1819190842622e-2;  

    private static final double asq = Util.sqr(a);
    private static final double esq = Util.sqr(e);
    private static final double esqInv = 1 - esq;



    public static double[] ecef2latlon(double[] ecef) {
        return ecef2latlon(ecef[0], ecef[1], ecef[2]);
    }

    public static double[] ecef2latlon(double x, double y, double z) {

        double b = Math.sqrt(asq * esqInv);
        double bsq = Util.sqr(b);
        double ep = Math.sqrt((asq - bsq) / bsq);
        double p = Math.sqrt(Util.sqr(x) + Util.sqr(y));
        double th = Math.atan2(a * z, b * p);

        double lat = Math.atan2((z + Util.sqr(ep) * b * Util.cube(Math.sin(th))), (p - esq * a * Util.cube(Math.cos(th))));

        double N = a / (Math.sqrt(1 - esq * Util.sqr(Math.sin(lat))));
        double alt = p / Math.cos(lat) - N;

        double lon = Math.atan2(y, x) % (2 * Math.PI);

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

        double sinLat = Math.sin(lat);
        double N = a / Math.sqrt(1 - esq * Util.sqr(sinLat));

        double cosLat = Math.cos(lat);
        double xy = (N + alt) * cosLat;
        double x = xy * Math.cos(lon);
        double y = xy * Math.sin(lon);

        target[0] = x;
        target[1] = y;
        double z = (esqInv * N + alt) * sinLat;
        target[2] = z;
        return target;
    }
}
