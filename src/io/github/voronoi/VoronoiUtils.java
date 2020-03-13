package io.github.voronoi;

import io.github.geojson.Feature;
import io.github.geojson.FeatureCollection;
import io.github.geojson.Geom;
import io.github.geojson.JtsUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.util.LineStringExtracter;
import org.locationtech.jts.operation.polygonize.Polygonizer;
import org.locationtech.jts.triangulate.VoronoiDiagramBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by EricLee on 2015/12/17.
 */
public class VoronoiUtils {

    /**
     * 如果只提供生成泰森多边形的空间对象集合，则以集合的最外围边界进行裁切
     */
    public static Geometry voronoi(Geometry geom) {
        Envelope clipEnv = geom.getEnvelopeInternal();
        return voronoi(geom, clipEnv);
    }

    /**
     * 根据GeometryCollection生成泰森多边形，返回裁切后的GeometryCollection对象
     *
     * @param geom    要生成泰森多边形的空间对象集合
     * @param clipEnv 对生成的泰森多边形裁切的多边形对象
     */
    public static Geometry voronoi(Geometry geom, Envelope clipEnv) {
        GeometryFactory factory = geom.getFactory();
        Geometry vgeom = getDiagram(factory, geom);
        Geometry clipPoly = factory.toGeometry(clipEnv);
        List<Geometry> list = new ArrayList<Geometry>(vgeom.getNumGeometries());
        for (int i = 0; i < vgeom.getNumGeometries(); i++) {
            Geometry g = vgeom.getGeometryN(i);
            Geometry result = null;
            if (clipEnv.contains(g.getEnvelopeInternal())) {
                result = g;
            } else if (clipEnv.intersects(g.getEnvelopeInternal())) {
                result = clipPoly.intersection(g);
                result.setUserData(g.getUserData());
            }
            list.add(result);
        }
        GeometryCollection gc = factory.createGeometryCollection(GeometryFactory.toGeometryArray(list));
        return gc;
    }

    /**
     * 根据FeatureCollection生成泰森多边形，并关联Feature的属性信息到泰森多边形上
     *
     * @param ftrc Feature对象的集合
     */
    public static FeatureCollection siteVoronoi(FeatureCollection ftrc) {
        GeometryFactory factory = Geom.factory;
        List<Geometry> geomList = new ArrayList<Geometry>();
        for (Feature ftr : ftrc.getFeatures()) {
            Geometry geom = ftr.getGeometry();
            geomList.add(geom);
        }
        GeometryCollection gc = factory.createGeometryCollection(GeometryFactory.toGeometryArray(geomList));
        Envelope clipEnv = gc.getEnvelopeInternal();
        return siteVoronoi(ftrc, clipEnv);
    }

    /**
     * 根据FeatureCollection对象生成泰森多边形 返回FeatureCollection对象，Geometry对象是裁切过的，
     * 并且附带上空间对象的属性信息，根据Geometry对象的hashCode匹配属性信息。
     */
    public static FeatureCollection siteVoronoi(FeatureCollection ftrc, Envelope clipEnv) {
        GeometryFactory factory = Geom.factory;
        Map<Integer, Map<String, Object>> map = new HashMap<Integer, Map<String, Object>>();
        List<Geometry> geomList = new ArrayList<Geometry>();
        for (Feature ftr : ftrc.getFeatures()) {
            Geometry geom = ftr.getGeometry();
            geomList.add(geom);
            Map<String, Object> prop = ftr.getProperties();
            map.put(geom.getCoordinate().hashCode(), prop);
        }
        GeometryCollection gc = factory.createGeometryCollection(GeometryFactory.toGeometryArray(geomList));
        Geometry vgeom = getDiagram(factory, gc);
        Geometry clipPoly = factory.toGeometry(clipEnv);
        List<Feature> ftrList = new ArrayList<Feature>(vgeom.getNumGeometries());
        for (int i = 0; i < vgeom.getNumGeometries(); i++) {
            Geometry g = vgeom.getGeometryN(i);
            Geometry result = null;
            if (clipEnv.contains(g.getEnvelopeInternal())) {
                result = g;
            } else if (clipEnv.intersects(g.getEnvelopeInternal())) {
                result = clipPoly.intersection(g);
                result.setUserData(g.getUserData());
            }
            Feature f = new Feature(result);
            Map<String, Object> prop = map.get(result.getUserData().hashCode());
            f.setProperties(prop);
            ftrList.add(f);
        }
        FeatureCollection ftrCollection = new FeatureCollection(ftrList);
        return ftrCollection;
    }

    /**
     * 如果不提供空间裁切的边界，则用集合的最小边界裁切
     */
    public static FeatureCollection cellVoronoi(FeatureCollection ftrc) {
        GeometryFactory factory = Geom.factory;
        List<Geometry> geomList = new ArrayList<Geometry>();
        for (Feature ftr : ftrc.getFeatures()) {
            Geometry geom = ftr.getGeometry();
            geomList.add(geom);
        }
        GeometryCollection gc = factory.createGeometryCollection(GeometryFactory.toGeometryArray(geomList));
        Envelope clipEnv = gc.getEnvelopeInternal();
        return cellVoronoi(ftrc, clipEnv);
    }

    /**
     * 根据FeatureCollection对象生成泰森多边形 返回FeatureCollection对象，Geometry对象是裁切过的，并且附带上空间对象的属性信息
     * ci:小区的编号，必须包含；azimuth：方位角，必须包含
     */
    public static FeatureCollection cellVoronoi(FeatureCollection ftrc, Envelope clipEnv) {
        final String strCi = "ci";
        final String strAzimuth = "azimuth";
        GeometryFactory factory = Geom.factory;
        //存储空间点对象跟属性信息的对应关系，如果存在共点，则存到集合中
        Map<Coordinate, List<Map<String, Object>>> ptMap = new HashMap<Coordinate, List<Map<String, Object>>>();
        //存储单个CI对应的属性信息
        Map<String, Map<String, Object>> cellMap = new HashMap<String, Map<String, Object>>();
        //聚合空间对象到集合中，并把CI跟空间对象的属性存入map中
        List<Geometry> geomList = new ArrayList<Geometry>();
        for (Feature ftr : ftrc.getFeatures()) {
            Geometry geom = ftr.getGeometry();
            geomList.add(geom);
            Map<String, Object> prop = ftr.getProperties();
            //根据指定的CI字段名，把CI对应的属性对放到map中
            cellMap.put(String.valueOf(prop.get(strCi)), prop);
            if (ptMap.containsKey(geom.getCoordinate())) {
                ptMap.get(geom.getCoordinate()).add(prop);
            } else {
                List<Map<String, Object>> vList = new ArrayList<Map<String, Object>>();
                vList.add(prop);
                ptMap.put(geom.getCoordinate(), vList);
            }
        }
        GeometryCollection gc = factory.createGeometryCollection(GeometryFactory.toGeometryArray(geomList));
        //计算泰森多边形
        Geometry vgeom = getDiagram(factory, gc);
        Geometry clipPoly = factory.toGeometry(clipEnv);
        List<Feature> ftrList = new ArrayList<Feature>(vgeom.getNumGeometries());
        for (int i = 0; i < vgeom.getNumGeometries(); i++) {
            Geometry g = vgeom.getGeometryN(i);
            Geometry result = null, rst = null;
            //对泰森多边形的边界进行裁切
            if (clipEnv.contains(g.getEnvelopeInternal())) {
                result = g;
            } else if (clipEnv.intersects(g.getEnvelopeInternal())) {
                result = clipPoly.intersection(g);
                result.setUserData(g.getUserData());//泰森来自哪个点对象存储在userdata中
            }
            List<Map<String, Object>> ptList = ptMap.get(result.getUserData());
            //按照方位角进行排序，为了更好的计算扇区的裁切
            Collections.sort(ptList, new Comparator<Map<String, Object>>() {
                @Override
                public int compare(Map<String, Object> o, Map<String, Object> t) {
                    Integer o1 = Integer.parseInt(String.valueOf(o.get(strAzimuth)));
                    Integer t1 = Integer.parseInt(String.valueOf(t.get(strAzimuth)));
                    return o1.compareTo(t1);
                }
            });
            int len = ptList.size();
            double firstAngle = Double.NaN;
            double lastAngle = Double.NaN;
            if (len == 1) {//单点泰森，直接生成Feature对象，并关联属性信息
                rst = result;
                Feature f = new Feature(rst);
                f.setProperties(ptList.get(0));
                ftrList.add(f);
            } else if (len == 2) {//泰森存在两个扇区共站的，用连接线切割
                Map<String, Object> r = ptList.get(0);
                Map<String, Object> r2 = ptList.get(1);
                Point pt = Geom.point(Double.parseDouble(r.get("longitude").toString()), Double.parseDouble(r.get("latitude").toString()));
                double df = Integer.parseInt(r.get(strAzimuth).toString()) + Integer.parseInt(r2.get(strAzimuth).toString());
                double d1 = df / 2.0;  //求取两个方向角的中间值
                double d2 = d1 + 180;  //反方向的角度，用来计算射线
                if (d2 > 360) {
                    d2 -= 360;
                }
                //计算两个方向上的5000米距离
                Point p1 = JtsUtils.computation(pt.getX(), pt.getY(), d1, 5000);
                Point p2 = JtsUtils.computation(pt.getX(), pt.getY(), d2, 5000);
                Geometry line3 = Geom.lineString(p1, pt, p2);
                //用线段分割多边形
                List<Geometry> gts = polygonize(result, line3);
                //用方位角计算10米远的一个点，用于判断此点位于哪个多边形内
                Point pt3 = JtsUtils.computation(pt.getX(), pt.getY(), Integer.parseInt(r.get(strAzimuth).toString()), 10);
                if (gts.size() == 2) {
                    if (pt3.within(gts.get(0))) {
                        Feature f1 = new Feature(gts.get(0));
                        f1.setProperties(cellMap.get(r.get(strCi)));
                        ftrList.add(f1);
                        Feature f = new Feature(gts.get(1));
                        f.setProperties(cellMap.get(r2.get(strCi)));
                        ftrList.add(f);
                    } else if (pt3.within(gts.get(1))) {
                        Feature f = new Feature(gts.get(1));
                        f.setProperties(cellMap.get(r.get(strCi)));
                        ftrList.add(f);
                        Feature f1 = new Feature(gts.get(0));
                        f1.setProperties(cellMap.get(r2.get(strCi)));
                        ftrList.add(f1);
                    }
                }
            } else {//如果是多个方位角分割多边形
                for (int k = 0; k < len; k++) {
                    double angle;
                    Map<String, Object> r = ptList.get(k);
                    Point pt = Geom.point(Double.parseDouble(r.get("longitude").toString()),
                            Double.parseDouble(r.get("latitude").toString()));
                    if (k == 0) {//计算第一个点跟最后一个点方位角的中间值，继而算出他们所覆盖的范围
                        Map<String, Object> r1 = ptList.get(len - 1);
                        double df1 = (Integer.parseInt(r.get(strAzimuth).toString()) + Integer.parseInt(r1.get(strAzimuth).toString()) - 360);
                        angle = firstAngle = df1 / 2.0;
                        Map<String, Object> r2 = ptList.get(k + 1);
                        double df2 = (Integer.parseInt(r2.get(strAzimuth).toString()) + Integer.parseInt(r.get(strAzimuth).toString()));
                        lastAngle = df2 / 2.0;
                    } else if (k == len - 1) {//计算出最后一个点跟第一个点所覆盖的范围
                        angle = lastAngle;
                        lastAngle = firstAngle;
                    } else {//中间点之间覆盖的范围计算
                        angle = lastAngle;
                        Map<String, Object> r4 = ptList.get(k + 1);
                        double df3 = Integer.parseInt(r4.get(strAzimuth).toString()) + Integer.parseInt(r.get(strAzimuth).toString());
                        lastAngle = df3 / 2.0;
                    }
                    Point p1 = JtsUtils.computation(pt.getX(), pt.getY(), angle, 5000);
                    Point p2 = JtsUtils.computation(pt.getX(), pt.getY(), lastAngle, 5000);
                    //组成简单的三角形，跟当前泰森进行裁切
                    Geometry gt = Geom.polygon(pt, p1, p2, pt);
                    rst = result.intersection(gt);
                    if (Geom.Type.from(rst) == Geom.Type.POLYGON) {
                        Feature f = new Feature(rst);
                        f.setProperties(cellMap.get(r.get(strCi)));
                        ftrList.add(f);
                    }
                }
            }
        }
        FeatureCollection ftrCollection = new FeatureCollection(ftrList);
        return ftrCollection;
    }

    /**
     * 根据factory和Geometry对象生成泰森多边形对象
     */
    private static Geometry getDiagram(GeometryFactory factory, Geometry geom) {
        VoronoiDiagramBuilder builder = new VoronoiDiagramBuilder();
        builder.setClipEnvelope(geom.getEnvelopeInternal());
        builder.setSites(geom);
        Geometry vgeom = builder.getDiagram(factory);
        return vgeom;
    }

    /**
     * 把一个多边形用一条或者多条线进行分割，分割为多个多边形对象
     *
     * @param poly 要分割的多边形对象
     * @param line 用来分割多边形的线
     */
    public static List<Geometry> polygonize(Geometry poly, Geometry line) {
        Geometry geometry = poly.getBoundary().union(line);
        List lines = LineStringExtracter.getLines(geometry);
        Polygonizer polygonizer = new Polygonizer();
        polygonizer.add(lines);
        Collection polys = polygonizer.getPolygons();
        Geometry[] geoms = GeometryFactory.toGeometryArray(polys);
        List<Geometry> list = new ArrayList<Geometry>();
        for (int i = 0; i < geoms.length; i++) {
            Polygon candpoly = (Polygon) geoms[i];
            if (poly.contains(candpoly.getInteriorPoint())) {
                list.add(candpoly);
            }
        }
        return list;
    }
}
