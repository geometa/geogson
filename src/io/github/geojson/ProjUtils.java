package io.github.geojson;

import org.locationtech.jts.geom.Point;

/**
 * Created by EricLee on 14-2-20.
 */
public class ProjUtils {
    private static double A = 6378245.0;
    private static double EE = 0.00669342162296594323;
    private static double X_PI = Math.PI * 3000.0 / 180.0;

    /**
     * 计算wgs84到火星坐标系转换
     */
    public static Point wgs2gcj(Point pt) {
        return wgs2gcj(pt.getX(), pt.getY());
    }

    /**
     * 计算wgs84到火星坐标系转换
     */
    public static Point wgs2gcj(double lon, double lat) {
        double tarLat, tarlon;
        if (outOfChina(lon, lat)) {
            tarLat = lat;
            tarlon = lon;
            return Geom.point(tarlon, tarLat);
        }
        Point pt = delta(lon, lat);
        tarLat = lat + pt.getY();
        tarlon = lon + pt.getX();
        return Geom.point(tarlon, tarLat);
    }

    /**
     * GCJ-02 to WGS-84
     */
    public static Point gcj2wgs(Point pt) {
        return gcj2wgs(pt.getX(), pt.getY());
    }

    /**
     * GCJ-02 to WGS-84
     */
    public static Point gcj2wgs(double lon, double lat) {
        double tarLat, tarlon;
        if (outOfChina(lon, lat)) {
            tarLat = lat;
            tarlon = lon;
            return Geom.point(tarlon, tarLat);
        }
        Point pt = delta(lon, lat);
        tarLat = lat - pt.getY();
        tarlon = lon - pt.getX();
        return Geom.point(tarlon, tarLat);
    }

    /**
     * GCJ-02 to WGS-84 exactly
     */
    public static Point gcj2wgsExact(Point pt) {
        return gcj2wgsExact(pt.getX(), pt.getY());
    }

    /**
     * GCJ-02 to WGS-84 exactly
     */
    public static Point gcj2wgsExact(double lon, double lat) {
        double initDelta = 0.01;
        double threshold = 0.000000001;
        double dLat = initDelta, dlon = initDelta;
        double mLat = lat - dLat, mlon = lon - dlon;
        double pLat = lat + dLat, plon = lon + dlon;
        double wgsLat, wgslon, i = 0;
        while (true) {
            wgsLat = (mLat + pLat) / 2;
            wgslon = (mlon + plon) / 2;
            Point tmp = gcj2wgs(wgslon, wgsLat);
            dLat = tmp.getY() - lat;
            dlon = tmp.getX() - lon;
            if ((Math.abs(dLat) < threshold) && (Math.abs(dlon) < threshold)) {
                break;
            }

            if (dLat > 0) {
                pLat = wgsLat;
            } else {
                mLat = wgsLat;
            }
            if (dlon > 0) {
                plon = wgslon;
            } else {
                mlon = wgslon;
            }

            if (++i > 10000) break;
        }
        return Geom.point(wgslon, wgsLat);
    }

    /**
     * 把02经纬度加密成百度坐标系
     */
    public static Point gcj2bd(Point pt) {
        return gcj2bd(pt.getX(), pt.getY());
    }

    /**
     * 把02经纬度加密成百度坐标系
     */
    public static Point gcj2bd(double gg_lon, double gg_lat) {
        double x = gg_lon, y = gg_lat;
        double z = Math.sqrt(x * x + y * y) + 0.00002 * Math.sin(y * X_PI);
        double theta = Math.atan2(y, x) + 0.000003 * Math.cos(x * X_PI);
        double bd_lon = z * Math.cos(theta) + 0.0065;
        double bd_lat = z * Math.sin(theta) + 0.006;
        return Geom.point(bd_lon, bd_lat);
    }

    /**
     * 把百度经纬度解密成02坐标系
     */
    public static Point bd2gcj(Point pt) {
        return bd2gcj(pt.getX(), pt.getY());
    }

    /**
     * 把百度经纬度解密成02坐标系
     */
    public static Point bd2gcj(double bd_lon, double bd_lat) {
        double x = bd_lon - 0.0065, y = bd_lat - 0.006;
        double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * X_PI);
        double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * X_PI);
        double gg_lon = z * Math.cos(theta);
        double gg_lat = z * Math.sin(theta);
        return Geom.point(gg_lon, gg_lat);
    }

    /**
     * 把84经纬度加密成百度坐标系
     */
    public static Point wgs2bd(Point pt) {
        return gcj2bd(wgs2gcj(pt));
    }

    /**
     * 把84经纬度加密成百度坐标系
     */
    public static Point wgs2bd(double lon, double lat) {
        return gcj2bd(wgs2gcj(lon, lat));
    }

    /**
     * 把百度经纬度解密成84坐标系
     */
    public static Point bd2wgs(Point pt) {
        return gcj2wgs(bd2gcj(pt));
    }

    /**
     * 把百度经纬度解密成84坐标系
     */
    public static Point bd2wgs(double bd_lon, double bd_lat) {
        return gcj2wgs(bd2gcj(bd_lon, bd_lat));
    }

    /**
     * 把米转换成对应的经纬度 单位的换算关系如下： 1英里= 63360 米 1米=1/1852 海里 1海里= 1/60度 如果要进行具体的运算， 需要进行一下单位换算，比如要求一个500米的范围，那么应该是
     * 500*1/1852*1/60（度）
     */
    public static double meter2Degrees(double meter) {
        return meter / 1852 / 60;
    }

    /**
     * 把经纬度转成对应的米
     */
    public static double degrees2Meter(double degrees) {
        return degrees * 1852 * 60;
    }

    /**
     * 判断经纬度是否在中国国内
     */
    public static boolean outOfChina(double lon, double lat) {
        if (lon < 72.004 || lon > 137.8347) {
            return true;
        }
        if (lat < 0.8293 || lat > 55.8271) {
            return true;
        }
        return false;
    }

    /**
     * 由高斯投影坐标反算成经纬度
     */
    public static Point gauss2wgs(Point pt) {
        return gauss2wgs(pt.getX(), pt.getY());
    }

    /**
     * 由高斯投影坐标反算成经纬度
     */
    public static Point gauss2wgs(double X, double Y) {
        int ProjNo;
        int ZoneWide; //带宽
        double longitude1, latitude1, longitude0, X0, Y0, xval, yval;//latitude0,
        double e1, e2, f, a, ee, NN, T, C, M, D, R, u, fai, iPI;
        iPI = 0.0174532925199433; ////3.1415926535898/180.0;
        //a = 6378245.0; f = 1.0/298.3; //54年北京坐标系参数
        a = 6378140.0;
        f = 1 / 298.257; //80年西安坐标系参数
        ZoneWide = 3; ////3度带宽
        ProjNo = (int) (X / 1000000L); //查找带号
        longitude0 = (ProjNo - 1) * ZoneWide + ZoneWide / 2;
        longitude0 = longitude0 * iPI; //中央经线

        X0 = ProjNo * 1000000L + 500000L;
        Y0 = 0;
        xval = X - X0;
        yval = Y - Y0; //带内大地坐标
        e2 = 2 * f - f * f;
        e1 = (1.0 - Math.sqrt(1 - e2)) / (1.0 + Math.sqrt(1 - e2));
        ee = e2 / (1 - e2);
        M = yval;
        u = M / (a * (1 - e2 / 4 - 3 * e2 * e2 / 64 - 5 * e2 * e2 * e2 / 256));
        fai = u
                + (3 * e1 / 2 - 27 * e1 * e1 * e1 / 32) * Math.sin(2 * u)
                + (21 * e1 * e1 / 16 - 55 * e1 * e1 * e1 * e1 / 32) * Math.sin(
                4 * u)
                + (151 * e1 * e1 * e1 / 96) * Math.sin(6 * u)
                + (1097 * e1 * e1 * e1 * e1 / 512) * Math.sin(8 * u);
        C = ee * Math.cos(fai) * Math.cos(fai);
        T = Math.tan(fai) * Math.tan(fai);
        NN = a / Math.sqrt(1.0 - e2 * Math.sin(fai) * Math.sin(fai));
        R = a * (1 - e2) / Math.sqrt(
                (1 - e2 * Math.sin(fai) * Math.sin(fai)) * (1 - e2 * Math.sin(fai) * Math.sin(fai)) * (1 - e2 * Math.sin
                        (fai) * Math.sin(fai)));
        D = xval / NN;
        //计算经度(Longitude) 纬度(Latitude)
        longitude1 =
                longitude0 + (D - (1 + 2 * T + C) * D * D * D / 6 + (5 - 2 * C + 28 * T - 3 * C * C + 8 * ee + 24 * T * T) * D
                        * D * D * D * D / 120) / Math.cos(fai);
        latitude1 =
                fai - (NN * Math.tan(fai) / R) * (D * D / 2 - (5 + 3 * T + 10 * C - 4 * C * C - 9 * ee) * D * D * D * D / 24
                        + (61 + 90 * T + 298 * C + 45 * T * T - 256 * ee - 3 * C * C) * D * D * D * D * D * D / 720);
        return Geom.point(longitude1 / iPI, latitude1 / iPI);
    }


    /**
     * 由经纬度反算成高斯投影坐标
     */
    public static Point wgs2gauss(Point pt) {
        return wgs2gauss(pt.getX(), pt.getY());
    }

    /**
     * 由经纬度反算成高斯投影坐标
     */
    public static Point wgs2gauss(double lon, double lat) {
        int ProjNo = 0;
        int ZoneWide; ////带宽
        double longitude1, latitude1, longitude0, latitude0, X0, Y0, xval, yval;
        double a, f, e2, ee, NN, T, C, A, M, iPI;
        iPI = 0.0174532925199433; ////3.1415926535898/180.0;
        ZoneWide = 3; ////3度带宽
    /*a = 6378245.0;
    f = 1.0 / 298.3; //54年北京坐标系参数*/
        a = 6378140.0;
        f = 1 / 298.257; //80年西安坐标系参数
        ProjNo = (int) (lon / ZoneWide);
        longitude0 = ProjNo * ZoneWide + ZoneWide / 2;
        longitude0 = longitude0 * iPI;
        longitude1 = lon * iPI; //经度转换为弧度
        latitude1 = lat * iPI; //纬度转换为弧度
        e2 = 2 * f - f * f;
        ee = e2 * (1.0 - e2);
        NN = a / Math.sqrt(1.0 - e2 * Math.sin(latitude1) * Math.sin(latitude1));
        T = Math.tan(latitude1) * Math.tan(latitude1);
        C = ee * Math.cos(latitude1) * Math.cos(latitude1);
        A = (longitude1 - longitude0) * Math.cos(latitude1);
        M = a * ((1 - e2 / 4 - 3 * e2 * e2 / 64 - 5 * e2 * e2 * e2 / 256) * latitude1
                - (3 * e2 / 8 + 3 * e2 * e2 / 32 + 45 * e2 * e2
                * e2 / 1024) * Math.sin(2 * latitude1)
                + (15 * e2 * e2 / 256 + 45 * e2 * e2 * e2 / 1024) * Math.sin(4 * latitude1)
                - (35 * e2 * e2 * e2 / 3072) * Math.sin(6 * latitude1));
        xval = NN * (A + (1 - T + C) * A * A * A / 6 + (5 - 18 * T + T * T + 72 * C - 58 * ee) * A * A * A * A * A / 120);
        yval = M + NN * Math.tan(latitude1) * (A * A / 2 + (5 - T + 9 * C + 4 * C * C) * A * A * A * A / 24
                + (61 - 58 * T + T * T + 600 * C - 330 * ee) * A * A * A * A * A * A / 720);
        X0 = 1000000L * (ProjNo + 1) + 500000L;
        Y0 = 0;
        xval = xval + X0;
        yval = yval + Y0;
        return Geom.point(xval, yval);
    }

    /**
     * 纬度计算wgs84到火星坐标系转换
     */
    private static double transform_earth_2_mars_lat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * Math.PI) + 40.0 * Math.sin(y / 3.0 * Math.PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * Math.PI) + 320 * Math.sin(y * Math.PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    /**
     * 经度计算wgs84到火星坐标系转换
     */
    private static double transform_earth_2_mars_lon(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * Math.PI) + 40.0 * Math.sin(x / 3.0 * Math.PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * Math.PI) + 300.0 * Math.sin(x / 30.0 * Math.PI)) * 2.0 / 3.0;
        return ret;
    }

    /**
     * 计算经纬度加密的偏移量
     */
    private static Point delta(double lon, double lat) {
        double dLat = transform_earth_2_mars_lat(lon - 105.0, lat - 35.0);
        double dLon = transform_earth_2_mars_lon(lon - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * Math.PI;
        double magic = Math.sin(radLat);
        magic = 1 - EE * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * Math.PI);
        dLon = (dLon * 180.0) / (A / sqrtMagic * Math.cos(radLat) * Math.PI);
        return Geom.point(dLon, dLat);
    }
}


