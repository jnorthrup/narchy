package spacegraph.util.geo;

import jcog.Util;

class ECEF {
    /*
     *
     *  ECEF - Earth Centered Earth Fixed
     *
     *  LLA - Lat Lon Alt
     *
     *  ported from matlab code at
     *  https:
     *     and
     *  https:
     */

    
    private static final double a = 6378137; 
    private static final double e = 8.1819190842622e-2;  

    private static final double asq = Math.pow(a, 2);
    private static final double esq = Math.pow(e, 2);

    public static double[] ecef2latlon(double[] ecef) {
        double x = ecef[0];
        double y = ecef[1];
        double z = ecef[2];

        double b = Math.sqrt(asq * (1 - esq));
        double bsq = Util.sqr(b);
        double ep = Math.sqrt((asq - bsq) / bsq);
        double p = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
        double th = Math.atan2(a * z, b * p);

        double lon = Math.atan2(y, x);
        double lat = Math.atan2((z + Math.pow(ep, 2) * b * Math.pow(Math.sin(th), 3)), (p - esq * a * Math.pow(Math.cos(th), 3)));
        double N = a / (Math.sqrt(1 - esq * Util.sqr(Math.sin(lat))));
        double alt = p / Math.cos(lat) - N;

        
        lon = lon % (2 * Math.PI);

        

        double[] ret = {lat, lon, alt};

        return ret;
    }


    public static double[] latlon2ecef(double... lla) {
        double lat = lla[0];
        double lon = lla[1];
        double alt = lla[2];

        double sinLat = Math.sin(lat);
        double N = a / Math.sqrt(1 - esq * Util.sqr(sinLat));

        double cosLat = Math.cos(lat);
        double xy = (N + alt) * cosLat;
        double x = xy * Math.cos(lon);
        double y = xy * Math.sin(lon);
        double z = ((1 - esq) * N + alt) * sinLat;

        double[] ret = {x, y, z};
        return ret;
    }
}
