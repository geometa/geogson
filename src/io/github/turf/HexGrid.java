package io.github.turf;

import io.github.geojson.Feature;
import io.github.geojson.Geom;
import io.github.geojson.JtsUtils;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * Takes a bounding box and a cell size in degrees and returns a {@link } of flat-topped
 * hexagons ({@link } features) aligned in an "odd-q" vertical grid as
 * described in [Hexagonal Grids](http://www.redblobgames.com/grids/hexagons/).
 *
 * @param {Array<number>} bbox bounding box in [minX, minY, maxX, maxY] order
 * @param {Number}        cellWidth width of cell in specified units
 * @param {String}        units used in calculating cellWidth ('miles' or 'kilometers')
 * @name hexGrid
 * @category interpolation
 * @return {FeatureCollection<Polygon>} a hexagonal grid
 * @example var bbox = [-96,31,-84,40]; var cellWidth = 50; var units = 'miles';
 * <p>
 * var hexgrid = io.github.turf.hexGrid(bbox, cellWidth, units);
 * <p>
 * //=hexgrid
 */
public class HexGrid {
    public static List<Double> cosines = new ArrayList<Double>();
    public static List<Double> sines = new ArrayList<Double>();

    /**
     * Precompute cosines and sines of angles used in hexagon creation for performance gain
     */
    static {
        for (int i = 0; i < 6; i++) {
            double angle = 2 * Math.PI / 6 * i;
            cosines.add(Math.cos(angle));
            sines.add(Math.sin(angle));
        }
    }

    public static void hexGrid(Envelope bbox, int cell, boolean triangles) {
        double xFraction = cell / (JtsUtils.distanceHaversine(bbox.getMinX(), bbox.getMinY(), bbox.getMaxX(), bbox.getMinY()));
        double cellWidth = xFraction * (bbox.getMaxX() - bbox.getMinX());
        double yFraction = cell / (JtsUtils.distanceHaversine(bbox.getMinX(), bbox.getMinY(), bbox.getMinX(), bbox.getMaxY()));
        double cellHeight = yFraction * (bbox.getMaxY() - bbox.getMinY());
        double radius = cellWidth / 2;
        double hex_width = radius * 2;
        double hex_height = Math.sqrt(3) / 2 * cellHeight;
        double box_width = bbox.getMaxX() - bbox.getMinX();
        double box_height = bbox.getMaxY() - bbox.getMinY();
        double x_interval = 3 / 4 * hex_width;
        double y_interval = hex_height;
        double x_span = box_width / (hex_width - radius / 2);
        double x_count = Math.ceil(x_span);
        if (Math.round(x_span) == x_count) {
            x_count++;
        }
        double x_adjust = ((x_count * x_interval - radius / 2) - box_width) / 2 - radius / 2;
        double y_count = Math.ceil(box_height / hex_height);
        double y_adjust = (box_height - y_count * hex_height) / 2;
        boolean hasOffsetY = y_count * hex_height - box_height > hex_height / 2;
        if (hasOffsetY) {
            y_adjust -= hex_height / 4;
        }
        List<Feature> ftrList = new ArrayList<Feature>();
        for (int x = 0; x < x_count; x++) {
            for (int y = 0; y < y_count; y++) {
                boolean isOdd = x % 2 == 1;
                if (y == 0 && isOdd) {
                    continue;
                }
                if (y == 0 && hasOffsetY) {
                    continue;
                }
                double center_x = x * x_interval + bbox.getMinX() - x_adjust;
                double center_y = y * y_interval + bbox.getMinY() + y_adjust;
                if (isOdd) {
                    center_y -= hex_height / 2;
                }
                if (triangles) {
                    ftrList.add(hexTriangles(Geom.point(center_x, center_y), cellWidth / 2, cellHeight / 2));
                } else {
                    ftrList.add(hexagon(Geom.point(center_x, center_y), cellWidth / 2, cellHeight / 2));
                }
            }
        }
    }

    public static Feature hexagon(Point pt, double rx, double ry) {
        double[] vertices = new double[12];
        for (int i = 0; i < 6; i++) {
            double x = pt.getX() + rx * cosines.get(i);
            double y = pt.getY() + ry * sines.get(i);
            vertices[i] = x;
            vertices[i + 1] = y;
        }
        return new Feature(Geom.polygon(vertices));
    }

    public static Feature hexTriangles(Point pt, double rx, double ry) {
        Geometry[] list = new Geometry[6];
        for (int i = 0; i < 6; i++) {
            double[] vertices = new double[6];
            vertices[0] = pt.getX();
            vertices[1] = pt.getY();
            vertices[2] = pt.getY() + rx * cosines.get(i);
            vertices[3] = pt.getY() + ry * sines.get(i);
            vertices[4] = pt.getX() + rx * cosines.get((i + 1) % 6);
            vertices[5] = pt.getY() + ry * sines.get((i + 1) % 6);
            list[i] = Geom.polygon(vertices);
        }
        return new Feature(new GeometryCollection(list, Geom.factory));
    }
}
