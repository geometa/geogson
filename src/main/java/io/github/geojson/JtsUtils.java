package io.github.geojson;

import io.github.geom.Geom;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class JtsUtils {
    private static final Logger log = LoggerFactory.getLogger(JtsUtils.class);
    private static GeometryFactory geomFactory = Geom.factory;
    private static WKTReader reader = new WKTReader(geomFactory);
    private static WKTWriter writer = new WKTWriter();
    private static final double R = 6378137.0;

    /**
     * 把WKT字符串转成对应的空间对象以便使用
     *
     * @param str POINT (109.013388 32.715519) MULTIPOINT(109.013388 32.715519,119.32488 31.435678)
     *            LINESTRING (0 0, 1 1, 2 2,3 3) POLYGON((113.36678266526226 34.25828594044533,113.36678266526226
     *            35.2197546201551,115.08064985276006 35.2197546201551,115.08064985276006
     *            34.25828594044533,113.36678266526226 34.25828594044533))
     */
    public static Geometry wkt2Geometry(String str) {
        try {
            return reader.read(str);
        } catch (ParseException e) {
            log.error("WKT格式空间对象转换时出现异常：" + e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 把JTS对象转换成WKT文本字符串
     *
     * @param geometry
     * @return
     */
    public static String geometry2Wkt(Geometry geometry) {
        return writer.write(geometry);
    }

    /**
     * 返回(A)与(B)中距离最近的两个点的距离，这里不能直接使用经纬度进行计算
     */
    public static double distance(Geometry a, Geometry b) {
        return a.distance(b);
    }

    /**
     * The geometries have no points in common 几何对象没有交点(相邻)
     *
     * @throws ParseException
     */
    public static boolean disjoint(Geometry a, Geometry b) {
        return a.disjoint(b);
    }

    /**
     * 两个几何对象的交集
     */
    public static Geometry intersection(Geometry a, Geometry b) {
        return a.intersection(b);
    }

    /**
     * 几何对象合并
     */
    public static Geometry union(Geometry a, Geometry b) {
        return a.union(b);
    }

    /**
     * 在A几何对象中有的，但是B几何对象中没有
     */
    public static Geometry difference(Geometry a, Geometry b) {
        return a.difference(b);
    }

    /**
     * 根据空间对象，缓冲区半径计算缓冲区边界
     */
    public static Geometry buffer(Geometry geometry, double distance) {
        double dis = ProjUtils.meter2Degrees(distance);
        return geometry.buffer(dis, 8);
    }

    /**
     * 根据空间对象，缓冲区半径计算缓冲区边界
     */
    public static String bufferByWKT(String str, double distance) {
        Geometry geometry = wkt2Geometry(str);
        Geometry geo = buffer(geometry, distance);
        return writer.write(geo);
    }

    /**
     * 根据空间对象，缓冲区半径，曲线段数计算缓冲边界
     */
    public static Geometry buffer(Geometry geometry, double distance, int segments) {
        if (segments < 8) {
            return buffer(geometry, distance);
        } else {
            return buffer(geometry, distance, segments);
        }
    }

    /**
     * @param lon  点对象的经度
     * @param lat  点对象的纬度
     * @param srid 坐标系
     * @作者： EricLee
     * @version 功能描述： 根据空间参数经纬度，创建点状对象的空间点对象
     * @see
     */
    public static String toSdoPoint(double lon, double lat, int srid) {
        StringBuilder sbGeom = new StringBuilder();
        sbGeom.append("MDSYS.SDO_GEOMETRY(2001,");
        sbGeom.append(srid == 0 ? 4326 : srid);
        sbGeom.append(",MDSYS.SDO_POINT_TYPE(");
        sbGeom.append(lon);
        sbGeom.append(",");
        sbGeom.append(lat);
        sbGeom.append(",null),null,null)");
        return sbGeom.toString();
    }

    /**
     * 把空间线的字符串格式化为对象，并拼接成Oracle Spatial识别的Geo-Sql
     *
     * @param strLine 空间线对象的WKT字符串
     * @param srid    坐标系
     * @Author EricLee 2010-9-14
     */
    public static String toSdoLine(String strLine, int srid) {
        LineString line = (LineString) wkt2Geometry(strLine);
        return toSdoLine(line, srid);
    }

    /**
     * 把空间线的字符串格式化为对象，并拼接成Oracle Spatial识别的Geo-Sql
     *
     * @param line 空间线对象
     * @param srid 坐标系
     * @Author EricLee 2010-9-14
     */
    public static String toSdoLine(LineString line, int srid) {
        StringBuilder strBd = new StringBuilder("");
        strBd.append("MDSYS.SDO_GEOMETRY(");
        strBd.append("2002,");
        strBd.append(srid == 0 ? 4326 : srid);
        strBd.append(",NULL,MDSYS.SDO_ELEM_INFO_ARRAY(1,2,1),MDSYS.SDO_ORDINATE_ARRAY(");
        Coordinate[] pts = line.getCoordinates();
        for (int i = 0; i < pts.length; i++) {
            strBd.append(pts[i].x);
            strBd.append(",");
            strBd.append(pts[i].y);
            if (i != (pts.length - 1)) {
                strBd.append(",");
            }
        }
        strBd.append("))");
        return strBd.toString();
    }

    /**
     * 把空间面的字符串格式化为对象，并拼接成Oracle Spatial识别的Geo-Sql
     *
     * @param strPolygon 空间面对象的WKT字符串
     * @param srid       坐标系
     * @Author EricLee 2010-9-14
     */
    public static String toSdoPolygon(String strPolygon, int srid) {
        Polygon polygon = (Polygon) wkt2Geometry(strPolygon);
        return toSdoPolygon(polygon, srid);
    }

    /**
     * 把空间面的字符串格式化为对象，并拼接成Oracle Spatial识别的Geo-Sql
     *
     * @param polygon 空间面对象
     * @param srid    坐标系
     * @Author EricLee 2010-9-14
     */
    public static String toSdoPolygon(Polygon polygon, int srid) {
        StringBuilder strBd = new StringBuilder("");
        strBd.append("MDSYS.SDO_GEOMETRY(");
        strBd.append("2003,");
        strBd.append(srid == 0 ? 4326 : srid);
        strBd.append(",NULL,MDSYS.SDO_ELEM_INFO_ARRAY(1,1003,1),MDSYS.SDO_ORDINATE_ARRAY(");
        Coordinate[] pts = polygon.getCoordinates();
        for (int i = 0; i < pts.length; i++) {
            strBd.append(pts[i].x);
            strBd.append(",");
            strBd.append(pts[i].y);
            if (i != (pts.length - 1)) {
                strBd.append(",");
            }
        }
        strBd.append("))");
        return strBd.toString();
    }

    /**
     * 把空间矩形的字符串格式化为对象，并拼接成Oracle Spatial识别的Geo-Sql
     *
     * @param xmin 矩形最小X值
     * @param xmax 矩形最大X值
     * @param ymin 矩形最小Y值
     * @param ymax 矩形最大Y值
     * @param srid 坐标系
     * @Author EricLee
     */
    public static String toSdoRect(double xmin, double xmax, double ymin, double ymax, int srid) {
        StringBuilder strBd = new StringBuilder("");
        strBd.append("MDSYS.SDO_GEOMETRY(");
        strBd.append("2003,");
        strBd.append(srid == 0 ? 4326 : srid);
        strBd.append(",NULL,MDSYS.SDO_ELEM_INFO_ARRAY(1,1003,3),MDSYS.SDO_ORDINATE_ARRAY(");
        strBd.append(xmin);
        strBd.append(",");
        strBd.append(ymin);
        strBd.append(",");
        strBd.append(xmax);
        strBd.append(",");
        strBd.append(ymax);
        strBd.append("))");
        strBd.append("))");
        return strBd.toString();
    }

    /**
     * 计算两点之间的距离，参数均为WGS-84(EPSG:4326)
     *
     * @param lon1 起点经度
     * @param lat1 起点维度
     * @param lon2 终点经度
     * @param lat2 终点维度
     * @return 返回两点间距离，返回单位为米
     */
    public static double distance(double lon1, double lat1, double lon2, double lat2) {
        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);
        double a = radLat1 - radLat2;
        double b = Math.toRadians(lon1) - Math.toRadians(lon2);
        double s = 2 * Math.asin(
                Math.sqrt(Math.pow(Math.sin(a / 2), 2) + Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)));
        s = s * R;
        s = Math.round(s * 100000) / 100000;
        return s;
    }

    /**
     * 计算两点之间的距离，参数均为WGS-84(EPSG:4326)
     *
     * @param pts 起点
     * @param pte 终点
     * @return 返回两点间距离，返回单位为米
     */
    public static double distance(Point pts, Point pte) {
        return distance(pts.getX(), pts.getY(), pte.getX(), pte.getY());
    }

    /**
     * 计算两点之间的距离，参数均为WGS-84(EPSG:4326)
     *
     * @param lon1 起点经度
     * @param lat1 起点维度
     * @param lon2 终点经度
     * @param lat2 终点维度
     * @return 返回两点间距离，返回单位为米
     */
    public static double distanceHaversine(double lon1, double lat1, double lon2, double lat2) {
        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);
        double dLat = radLat1 - radLat2;
        double dLon = Math.toRadians(lon1) - Math.toRadians(lon2);
        double a =
                Math.pow(Math.sin(dLat / 2), 2) + Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(dLon / 2), 2);
        if (a > 1) {
            a = 1;
        }
        double s = 2 * R * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        s = Math.round(s * 100000) / 100000;
        return s;
    }

    /**
     * 计算两点之间的距离，参数均为WGS-84(EPSG:4326)
     *
     * @param pts 起点
     * @param pte 终点
     * @return 返回两点间距离，返回单位为米
     */
    public static double distanceHaversine(Point pts, Point pte) {
        return distanceHaversine(pts.getX(), pts.getY(), pte.getX(), pte.getY());
    }

    /**
     * create a Circle  创建一个圆，圆心(x,y) 半径RADIUS
     */
    public static Polygon createCircle(double x, double y, final double RADIUS) {
        final int SIDES = 32;//圆上面的点个数
        Coordinate coords[] = new Coordinate[SIDES + 1];
        for (int i = 0; i < SIDES; i++) {
            double angle = ((double) i / (double) SIDES) * Math.PI * 2.0;
            double dx = Math.cos(angle) * RADIUS;
            double dy = Math.sin(angle) * RADIUS;
            coords[i] = new Coordinate(x + dx, y + dy);
        }
        coords[SIDES] = coords[0];
        Polygon polygon = geomFactory.createPolygon(coords);
        return polygon;
    }

    /**
     * 绘制三叶草区域 根据经纬度、半径、方位角、半功率角、点数
     */
    public static Polygon cellClover(double x, double y, double r, double angle, double theta, int sides) {
        double w = r,//半径值 默认20
                beam = theta,//半功率角 默认 60度
                direct;//方位角
        int point_num = sides, rr;//边缘点数
        if (beam == 360) {
            point_num = 50;
            return cellSector(x, y, r, angle, 360, point_num);
        }
        direct = angle; // 0度
        double a, b, c, theta_start, theta_end, theta0, theta_delta;

        //'theta_start, theta_end分别对应第一瓣的起始角度和终止角度
        //a,b分别对应相应的参数
        direct = direct / 180 * Math.PI;
        a = 150 / (beam + 30);//(这里没有采用标准计算的 a = 60 / beam公式 ，因为 a = 60 / beam比较难看
        b = 1;
        c = 1 / 2 * Math.PI;//旋转角度（第一瓣归零参数）：r = w * Sin(a / b * theta + c)的图形
        theta_start = (0 - c) / a;
        theta_end = 1 / a * (Math.PI - c);
        theta_delta = (theta_end - theta_start) / point_num;
        List<Coordinate> list = new ArrayList<Coordinate>();
        list.add(new Coordinate(x, y));
        for (int j = 0; j < point_num; j++) {
            theta0 = theta_delta * (point_num - j) + theta_start;
            rr = (int) Math.floor(Math.abs(w * Math.sin(a / b * theta0 + c)));
            if (rr > 0.01) {
                list.add(computation(x, y, (theta0 + direct) * 180 / Math.PI, rr).getCoordinate());
            }
        }
        Polygon polygon = geomFactory.createPolygon(list.toArray(new Coordinate[]{}));
        return polygon;
    }

    /**
     * 绘制扇形区域 根据经纬度、半径、方位角、半功率角、点数
     */
    public static Polygon cellSector(double x, double y, double r, double angle, double theta, int sides) {
        List<Coordinate> list = new ArrayList<Coordinate>();
        int delta = (int) Math.floor(sides / 2.0);
        if (theta == 360) {
            sides = 50;
            delta = sides / 2;
        } else {
            list.add(new Coordinate(x, y));
        }
        double delta_ang = theta / sides;
        for (int i = -delta; i <= delta; i++) {
            list.add(computation(x, y, angle + delta_ang * -i, r).getCoordinate());
        }
        if (theta != 360) {
            list.add(new Coordinate(x, y));
        }
        Polygon polygon = geomFactory.createPolygon(list.toArray(new Coordinate[]{}));
        return polygon;
    }

    /**
     * @param lon
     * @param lat
     * @param angle
     * @param distance
     * @return
     */
    public static Point computation(double lon, double lat, double angle, double distance) {
        double b1 = lat, l1 = lon, a1 = angle, s = distance, b2, l2;
        double a = 6378245, b = 6356752.3142, e = 0.08202449712405484, e2 = 0.08230182848092589;
        b1 = b1 * Math.PI / 180;
        l1 = l1 * Math.PI / 180;
        a1 = a1 * Math.PI / 180;
        double w = Math.sqrt(1 - e * e * (Math.sin(b1) * Math.sin(b1)));
        double E1 = e;
        double W1 = w;
        double sinu1 = Math.sin(b1) * Math.sqrt(1 - E1 * E1) / W1;
        double cosu1 = Math.cos(b1) / W1;
        double sinA0 = cosu1 * Math.sin(a1);
        double cotq1 = cosu1 * Math.cos(a1);
        double sin2q1 = 2 * cotq1 / (cotq1 * cotq1 + 1);
        double cos2q1 = (cotq1 * cotq1 - 1) / (cotq1 * cotq1 + 1);
        double cos2A0 = 1 - sinA0 * sinA0;
        e2 = Math.sqrt(a * a - b * b) / b;
        double k2 = e2 * e2 * cos2A0;
        double aa = b * (1 + k2 / 4 - 3 * k2 * k2 / 64 + 5 * k2 * k2 * k2 / 256);
        double BB = b * (k2 / 8 - k2 * k2 / 32 + 15 * k2 * k2 * k2 / 1024);
        double cc = b * (k2 * k2 / 128 - 3 * k2 * k2 * k2 / 512);
        e2 = E1 * E1;
        double AAlpha = (e2 / 2 + e2 * e2 / 8 + e2 * e2 * e2 / 16) - (e2 * e2 / 16 + e2 * e2 * e2 / 16) * cos2A0 + (3 * e2 * e2 * e2 / 128) * cos2A0 * cos2A0;
        double BBeta = (e2 * e2 / 32 + e2 * e2 * e2 / 32) * cos2A0 - (e2 * e2 * e2 / 64) * cos2A0 * cos2A0;
        double q0 = (s - (BB + cc * cos2q1) * sin2q1) / aa;
        double sin2q1q0 = sin2q1 * Math.cos(2 * q0) + cos2q1 * Math.sin(2 * q0);
        double cos2q1q0 = cos2q1 * Math.cos(2 * q0) - sin2q1 * Math.sin(2 * q0);
        double q = q0 + (BB + 5 * cc * cos2q1q0) * sin2q1q0 / aa;
        double theta = (AAlpha * q + BBeta * (sin2q1q0 - sin2q1)) * sinA0;
        double sinu2 = sinu1 * Math.cos(q) + cosu1 * Math.cos(a1) * Math.sin(q);
        b2 = Math.atan(sinu2 / (Math.sqrt(1 - E1 * E1) * Math.sqrt(1 - sinu2 * sinu2))) * 180 / Math.PI;
        double lamuda = Math.atan(Math.sin(a1) * Math.sin(q) / (cosu1 * Math.cos(q) - sinu1 * Math.sin(q) * Math.cos(a1))) * 180 / Math.PI;
        if (Math.sin(a1) > 0) {
            if (Math.sin(a1) * Math.sin(q) / (cosu1 * Math.cos(q) - sinu1 * Math.sin(q) * Math.cos(a1)) > 0) {
                lamuda = Math.abs(lamuda);
            } else {
                lamuda = 180 - Math.abs(lamuda);
            }
        } else {
            if (Math.sin(a1) * Math.sin(q) / (cosu1 * Math.cos(q) - sinu1 * Math.sin(q) * Math.cos(a1)) > 0) {
                lamuda = Math.abs(lamuda) - 180;
            } else {
                lamuda = -Math.abs(lamuda);
            }
        }
        l2 = l1 * 180 / Math.PI + lamuda - theta * 180 / Math.PI;
        return Geom.point(l2, b2);
    }
}
