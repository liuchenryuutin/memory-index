package org.lccy.lucene.memory.util;

import org.lccy.lucene.memory.util.geo.DistanceUnit;

/**
 * 类名称： <br>
 * 类描述： <br>
 *
 * @Date: 2023/12/04 15:04 <br>
 * @author: liuchen11
 */
public final class ScoreUtils {

    /**
     * 计算两点间的距离
     */
    public static double calculate(boolean isArc, double srcLat, double srcLon, double dstLat, double dstLon, DistanceUnit unit) {
        if (!isArc) {
            return DistanceUnit.convert(GeoUtil.planeDistance(srcLat, srcLon, dstLat, dstLon),
                    DistanceUnit.METERS, unit);
        }
        return DistanceUnit.convert(GeoUtil.arcDistance(srcLat, srcLon, dstLat, dstLon), DistanceUnit.METERS, unit);
    }

    /**
     * 距离线性衰减
     */
    public static final class DecayGeoExp {
        double originLat;
        double originLon;
        double offset;
        double scaling;

        public DecayGeoExp(double originLat, double originLon, String scaleStr, String offsetStr, double decay) {
            this.originLat = originLat;
            this.originLon = originLon;
            double scale = DistanceUnit.DEFAULT.parse(scaleStr, DistanceUnit.DEFAULT);
            this.offset = DistanceUnit.DEFAULT.parse(offsetStr, DistanceUnit.DEFAULT);
            this.scaling = Math.log(decay) / scale;
        }

        public double decayGeoExp(double lat, double lon) {
            double distance = calculate(true, originLat, originLon, lat, lon, DistanceUnit.METERS);
            distance = Math.max(0.0d, distance - offset);
            return Math.exp(scaling * distance);
        }
    }
}
