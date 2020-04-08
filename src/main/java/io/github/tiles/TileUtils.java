package io.github.tiles;

import io.github.geom.Geom;
import org.locationtech.jts.geom.Point;

/**
 * Created by EricLee on 2016/1/12.
 */
public class TileUtils {
    private static final double REM = 6378137;
    private static final int TILE_SIZE = 256;
    private static final double INITIAL_RES = 2 * Math.PI * REM / TILE_SIZE;
    //156543.03392804062 for tileSize 256 pixels
    private static final double ORIGIN_SHIFT = 2 * Math.PI * REM / 2.0;
    //20037508.342789244

    /**
     * Resolution (meters/Pixel) for given zoom level (measured at Equator)
     */
    public static double resolution(int zoom) {
        return INITIAL_RES / Math.pow(2, zoom);
    }

    /**
     * 根据经纬度坐标点，计算出来墨卡托坐标系位置
     */
    public static Point lonLatToMerc(Point point) {
        return lonLatToMerc(point.getX(), point.getY());
    }

    /**
     * 根据经纬度，计算出来墨卡托坐标系位置
     */
    public static Point lonLatToMerc(double lon, double lat) {
        double mx = lon * ORIGIN_SHIFT / 180.0;
        double my = Math.log(Math.tan((90 + lat) * Math.PI / 360.0)) / (Math.PI / 180.0);
        my = my * ORIGIN_SHIFT / 180.0;
        return Geom.point(mx, my);
    }

    /**
     * 根据墨卡托坐标点，计算出来经纬度坐标系位置，单位是米
     */
    public static Point mercToLonLat(Point point) {
        return mercToLonLat(point.getX(), point.getY());
    }

    /**
     * 根据墨卡托，计算出来经纬度坐标系位置，单位是米
     */
    public static Point mercToLonLat(double x, double y) {
        double lon = (x / ORIGIN_SHIFT) * 180.0;
        double lat = (y / ORIGIN_SHIFT) * 180.0;
        lat = 180 / Math.PI * (2 * Math.atan(Math.exp(lat * Math.PI / 180.0)) - Math.PI / 2.0);
        return Geom.point(lon, lat);
    }

    /**
     * 把经纬度转换为符合TMS切图规则的切片xy
     */
    public static Tile toTMSTile(final double lon, final double lat, final int zoom) {
        Tile tms = toTile(lon, lat, zoom);
        return tmsTile(tms.x(), tms.y(), tms.z());
    }

    /**
     * 根据经纬度直接算出切片的位置，并做边界校验
     */
    public static Tile toTile(final double lon, final double lat, final int zoom) {
        int xtile = (int) Math.floor((lon + 180) / 360 * (1 << zoom));
        int ytile = (int) Math.floor(
                (1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1 << zoom));
        if (xtile < 0) {
            xtile = 0;
        }
        if (xtile >= (1 << zoom)) {
            xtile = ((1 << zoom) - 1);
        }
        if (ytile < 0) {
            ytile = 0;
        }
        if (ytile >= (1 << zoom)) {
            ytile = ((1 << zoom) - 1);
        }
        return new Tile(xtile, ytile, zoom);
    }

    /**
     * 把tile切片转换为一个墨卡托的正四边形
     */
    public static Bound tile2MercBounds(final Tile tile) {
        double xmin = tile2Lon(tile.x(), tile.z());
        double ymin = tile2Lat(tile.y(), tile.z());
        double xmax = tile2Lon(tile.x() + 1, tile.z());
        double ymax = tile2Lat(tile.y() + 1, tile.z());
        Point ptMin = lonLatToMerc(xmin, ymin);
        Point ptMax = lonLatToMerc(xmax, ymax);
        return new Bound(ptMin.getX(), ptMin.getY(), ptMax.getX(), ptMax.getY());
    }

    /**
     * 把tile切片转换为一个墨卡托的正四边形
     */
    public static Bound tile2MercBounds(final int x, final int y, final int zoom) {
        double xmin = tile2Lon(x, zoom);
        double ymin = tile2Lat(y, zoom);
        double xmax = tile2Lon(x + 1, zoom);
        double ymax = tile2Lat(y + 1, zoom);
        Point ptMin = lonLatToMerc(xmin, ymin);
        Point ptMax = lonLatToMerc(xmax, ymax);
        return new Bound(ptMin.getX(), ptMin.getY(), ptMax.getX(), ptMax.getY());
    }

    /**
     * 把tile切片转换为一个正四边形
     */
    public static Bound tile2Bounds(final Tile tile) {
        double xmin = tile2Lon(tile.x(), tile.z());
        double ymin = tile2Lat(tile.y(), tile.z());
        double xmax = tile2Lon(tile.x() + 1, tile.z());
        double ymax = tile2Lat(tile.y() + 1, tile.z());
        return new Bound(xmin, ymin, xmax, ymax);
    }

    /**
     * 把tile切片转换为一个正四边形
     */
    public static Bound tile2Bounds(final int x, final int y, final int zoom) {
        double xmin = tile2Lon(x, zoom);
        double ymin = tile2Lat(y, zoom);
        double xmax = tile2Lon(x + 1, zoom);
        double ymax = tile2Lat(y + 1, zoom);
        return new Bound(xmin, ymin, xmax, ymax);
    }

    /**
     * 把tile切片的x坐标转换为经度
     */
    public static double tile2Lon(int x, int z) {
        return x / Math.pow(2.0, z) * 360.0 - 180;
    }

    /**
     * 把tile切片的y坐标转换为纬度
     */
    public static double tile2Lat(int y, int z) {
        double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z);
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }

    /**
     * Converts Google/slippy tile coordinates to TMS Tile coordinates
     */
    public static Tile tmsTile(int tx, int ty, int zoom) {
        return new Tile(tx, (int) (Math.pow(2, zoom) - 1 - ty), zoom);
    }
}
