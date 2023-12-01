package org.lccy.lucene.memory.util;

import org.apache.lucene.util.SloppyMath;

/**
 * 距离计算工具
 *
 * @Date: 2023/08/14 10:32 <br>
 * @author: liuchen11
 */
public class GeoUtil {

    /** Earth ellipsoid major axis defined by WGS 84 in meters */
    public static final double EARTH_SEMI_MAJOR_AXIS = 6378137.0;      // meters (WGS 84)

    /** Earth ellipsoid minor axis defined by WGS 84 in meters */
    public static final double EARTH_SEMI_MINOR_AXIS = 6356752.314245; // meters (WGS 84)

    /** Earth mean radius defined by WGS 84 in meters */
    public static final double EARTH_MEAN_RADIUS = 6371008.7714D;      // meters (WGS 84)

    /** Earth axis ratio defined by WGS 84 (0.996647189335) */
    public static final double EARTH_AXIS_RATIO = EARTH_SEMI_MINOR_AXIS / EARTH_SEMI_MAJOR_AXIS;

    /** Earth ellipsoid equator length in meters */
    public static final double EARTH_EQUATOR = 2*Math.PI * EARTH_SEMI_MAJOR_AXIS;

    /** Earth ellipsoid polar distance in meters */
    public static final double EARTH_POLAR_DISTANCE = Math.PI * EARTH_SEMI_MINOR_AXIS;

    /** Return the distance (in meters) between 2 lat,lon geo points using the haversine method implemented by lucene */
    public static double arcDistance(double lat1, double lon1, double lat2, double lon2) {
        return SloppyMath.haversinMeters(lat1, lon1, lat2, lon2);
    }

    /**
     * Return the distance (in meters) between 2 lat,lon geo points using a simple tangential plane
     * this provides a faster alternative to {@link GeoUtil#arcDistance} but is inaccurate for distances greater than
     * 4 decimal degrees
     */
    public static double planeDistance(double lat1, double lon1, double lat2, double lon2) {
        double x = (lon2 - lon1) * SloppyMath.TO_RADIANS * Math.cos((lat2 + lat1) / 2.0 * SloppyMath.TO_RADIANS);
        double y = (lat2 - lat1) * SloppyMath.TO_RADIANS;
        return Math.sqrt(x * x + y * y) * EARTH_MEAN_RADIUS;
    }
}
